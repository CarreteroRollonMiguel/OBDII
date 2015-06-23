/**
 *
 * Copyright 2015 Miguel Carretero
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package etsii_upm.obdii;


import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;

public class LeerService{

    private final MainActivity main_w;
    private final Context context;
    private int contador;
    private StringBuffer buffer;
    private boolean leyendo = false;

    private LeerThread leerThread;
    private Timer timer;
    private TimerTask myTimerTask;

    // Servicios
    private BluetoothService bService;
    private GPSService gpsService;
    private LightSensorService lightService;

    // Periodos de los servicios
    private final int gpsPeriodo;
    private final int pLectura;
    private final int pEscritura;

    private SimpleDateFormat formatoFecha;


    //Constructor
    public LeerService(MainActivity main_w, BluetoothService bService)
    {
        debug("LeerService()", "Iniciando servicio");
        this.main_w	    = main_w;
        this.context	= main_w.getApplicationContext();
        this.bService	= bService;
        this.contador   = 1;

        this.pLectura = main_w.pLectura*1000;
        this.pEscritura  = main_w.pEscritura*((int)60/main_w.pLectura);
        this.gpsPeriodo = main_w.gpsPeriodo*1000;

        formatoFecha = new SimpleDateFormat("dd-MM-yyyy_HH:mm:ss");

    }

    public void gestionMonitorizacion(){

        if(leyendo){
            //Cancelamos la lectura
            pararMonitorizacion();
        }else{
            //Bloqueamos interfaz
            main_w.tabHost.getTabWidget().getChildAt(1).setEnabled(false);
            main_w.btnRecibir.setText(R.string.parar_lect);

            //Iniciamos tarea asincrona
            leyendo = true;
            leerThread = new LeerThread();
            leerThread.start();
        }

    }

    // Paramos la monitorizacion
    public void pararMonitorizacion()
    {
        /*

        main_w.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Siempre paramos la ejecucion desde el thread UI
                */
        if(leyendo){

            if(bService != null) {

                if (timer != null) {
                    timer.cancel();
                    timer.purge();
                    timer = null;
                }
                if (gpsService != null) {
                    gpsService.stopGPSService();
                }
                if (gpsService != null) {
                    lightService.stopLightSensorService();
                }
                bService.cancelarConexionOBD();

            }

            contador = 1;
            buffer.setLength(0);

            Toast.makeText(main_w, "Recogida de datos finalizada", Toast.LENGTH_SHORT).show();
            main_w.escribirConsola("Recogida de datos finalizada");

            if(bService != null && bService.contadorOBD() == 10)
                main_w.escribirConsola("A causa de un error en la conexión con OBDII ");

            leyendo = false;

            //Reestablecemos interfaz
            main_w.tabHost.getTabWidget().getChildAt(1).setEnabled(true);
            main_w.btnRecibir.setText(R.string.empez_lect);

        }

    }

    private class LeerThread extends Thread {

        public void run() {

            //Looper.prepare();

            consolaUpdate("Empezamos la recogida de datos");
            //borrarDir();
            buffer = new StringBuffer(0);

            if(bService != null) {
                bService.connectOBD();
                bService.setNoDataFalse();
                lightService = new LightSensorService(context);
                gpsService = new GPSService(context, main_w, gpsPeriodo);
                timer = new Timer();
                myTimerTask = new MyTimerTask();
                timer.scheduleAtFixedRate(myTimerTask, pLectura, pLectura);
            }else{
                pararMonitorizacion();
            }

        }




    }

    private class MyTimerTask extends TimerTask {

        //Funcion periodica a ejecutar
        //------------------------------------
        @Override
        public void run() {
            /*
            Aqui van todas las lecturas
            que se realizan cada periodo de tiempo pLectura
            */

            Calendar calendar = Calendar.getInstance();

            String linea =
                    "{'t': '"+ formatoFecha.format(calendar.getTime()) +"',"
                     + "'obd': '" + bService.getNombreDispositivo() + "',"
                     + lightService.getLuminosidad() + ","
                     + gpsService.getPosicion() + ","
                     + bService.lecturaOBD() + "}\n";

            buffer.append(linea);

            //Cada pEscritura vaciamos el buffer en un fichero
            if (contador % pEscritura == 0) {
                //consolaUpdate(n_fichero);
                grabarFichero();
                buffer.setLength(0);
            }

            //Solo para pruebas, vemos si el fichero se ha creado
            if (contador > pEscritura && contador % pEscritura == 1) {
                //consolaUpdate(context.getFilesDir().toString());
                listarDir();
            }

            contador++;
        }

        private void grabarFichero() {

            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat simpleDateFormat =
                    new SimpleDateFormat("dd-MM-yyyy_HH-mm");
            String strDate = simpleDateFormat.format(calendar.getTime());

            String strfichero = strDate + "_"+ bService.getNombreDispositivo() + ".txt";

            try {
                //   crearFichero(nombre);
                // MODE_PRIVATE para acceso privado desde nuestra aplicación
                //  [crea el fichero o lo sobrescribe si ya existe]
                // Direccion de creacion por defecto
                // ./data/data/paquete.java/files/strfichero
                OutputStreamWriter fout =
                        new OutputStreamWriter(context.openFileOutput(strfichero, Context.MODE_PRIVATE));

                fout.write(buffer.toString());
                fout.close();

                // traza para depurar y dar indicaciones usuario
                consolaUpdate("Creado archivo: " + strfichero);
            } catch (Exception e) {
                Log.e("grabarFichero()", "Error al crear o escribir fichero a memoria interna", e);
                consolaUpdate("Error al crear o escribir ficheros a memoria interna");
            }

            //Solo para pruebas, leemos el fichero creado
            leerFichero(strfichero);

        }

        //Funciones auxiliares
        //-----------------------------------------------

        // Lista de fichero en el directorio de la aplicacion
        private File[] listarDir() {

            // Array TEXTO donde guardaremos los nombres de los ficheros
            List<String> item = new ArrayList<String>();

            //Defino la ruta donde busco los ficheros
            //File f = new File(context.getFilesDir().toString());
            File f = context.getFilesDir();
            //Creo el array de tipo File con el contenido de la carpeta
            File[] files = f.listFiles();

            //Hacemos un Loop por cada fichero para extraer el nombre de cada uno
            consolaUpdate("Lista de ficheros:");
            for (int i = 0; i < files.length; i++) {
                //Sacamos del array files un fichero
                File file = files[i];
                // traza para depurar y dar indicaciones usuario
                consolaUpdate(file.getName());
                //enviarArchivo(file);
            }
            return files;
        }

        //Leer un fichero
        private void leerFichero(String strfichero) {
            try {
                BufferedReader fin =
                        new BufferedReader(new InputStreamReader(context.openFileInput(strfichero)));

                StringBuffer texto = new StringBuffer(0);
                String line;

                while ((line = fin.readLine()) != null) {
                    texto.append("\n" + line);
                }
                fin.close();

                consolaUpdate("Contenido: " + texto.toString());
                texto.setLength(0);
            } catch (Exception ex) {
                Log.e("leer_fichero()", "Error al leer fichero desde memoria interna");
            }
        }

    }

    private void consolaUpdate(final String nom_fichero) {
        main_w.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Escribimos por consola el fichero creado
                main_w.escribirConsola(nom_fichero);
            }
        });
    }

    //Funciones auxiliares
    //-----------------------------------------------
    private void borrarDir() {

        // Array TEXTO donde guardaremos los nombres de los ficheros
        List<String> item = new ArrayList<String>();

        //Defino la ruta donde busco los ficheros
        File f = context.getFilesDir();
        //Creo el array de tipo File con el contenido de la carpeta
        File[] files = f.listFiles();
        //Hacemos un Loop por cada fichero para extraer el nombre de cada uno
        for (int i = 0; i < files.length; i++) {
            //Sacamos del array files un fichero
            File file = files[i];
            file.delete();
        }
        return;
    }

    public void debug(String metodo, String msg) {
        if (MainActivity.DEBUG_MODE)
            Log.d(MainActivity.TAG, metodo + ": " + msg);
    }

}



