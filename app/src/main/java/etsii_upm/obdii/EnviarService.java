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



import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;


public class EnviarService extends Service {

    // Interface for clients that bind
    private IBinder mBinder = new MyBinder();
    private Messenger messenger;

    //Variables para status de Internet
    private IntentFilter iFiltro;
    private BroadcastReceiver iReceiver;
    private ConnectivityManager mConnectivityManager;

    //Thread
    private Handler mHandler;
    private EnviarThread enviarThread;
    private boolean rThread = false;

    // Otras variables
    private boolean enviarCon3g = false;

    // Periodo de envio en segundos
    private final int pEnvio = 120;  //Cada cuanto comprueba si hay archivos para enviar

    // Handlers y Bradcasts
    //----------------------------------------------------

    // BroadcastReceiver, detecta cambios en el estado de la conexion del dispositivo
    public class iBroadcastR extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

                gestionThread();

            }

        }

    }

    private class threadHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String str = (String) msg.obj;
            consolaUpdate(str);
        }
    }

    //----------------------------------------------------

    // Binder: Metodos llamables por MainActivity
    //----------------------------------------------------

    @Override
    public IBinder onBind(Intent arg0) {
        debug("BinderService", "Binding Service...");
        return mBinder;
    }

    public class MyBinder extends Binder {
        EnviarService getService() {
            return EnviarService.this;
        }
    }

    public void gestion3g(boolean enviarCon3g) {
        this.enviarCon3g = enviarCon3g;

        if(enviarCon3g){
            consolaUpdate("Enviar con 3G activado.");
        }else{
            consolaUpdate("Enviar con 3G desactivado.");
        }

        gestionThread();
    }
    //----------------------------------------------------

    @Override
    public void onCreate() {
        super.onCreate();

        // Suscribe el BroadcastReceiver que instanciamos previamente a los
        // eventos relacionados con Internet que queremos controlar
        mConnectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        iFiltro = new IntentFilter();
        iFiltro.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        iReceiver = new iBroadcastR();
        this.registerReceiver(iReceiver, iFiltro);

        //Inicializamos el thread de envio
        mHandler = new threadHandler();
        enviarThread = new EnviarThread(mHandler);

    }

    // Controla cuando empezamos o paramos el envio
    // Puentear para pruebas
    ///*
    private void gestionThread()
    {

        NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (mNetworkInfo == null) {
            // Pruebas :
            // consolaUpdate("iReceive: sin conexion a internet");

            if(rThread) {
                enviarThread.interrupt();
                rThread = false;
                consolaUpdate(this.getString(R.string.no_enviamos));
            }

            return;
        } else {

            if (mNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                // Pruebas :
                // consolaUpdate("iReceive: conexion WIFI network");

                if(!rThread) {
                    enviarThread = new EnviarThread(mHandler);
                    enviarThread.start();
                    rThread = true;
                    consolaUpdate(this.getString(R.string.enviamos));
                }

            } else if (mNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                // Pruebas :
                // consolaUpdate("iReceive: conexion MOBILE");

                if(enviarCon3g) {
                    if (!rThread) {
                        enviarThread = new EnviarThread(mHandler);
                        enviarThread.start();
                        rThread = true;
                        consolaUpdate(this.getString(R.string.enviamos));
                    }
                }else{
                    if(rThread) {
                        enviarThread.interrupt();
                        rThread = false;
                        consolaUpdate(this.getString(R.string.no_enviamos));
                    }
                }

            }
        }

    }
    //*/

    //Puenteo que realizamos para pruebas
    /*
    private void gestionThread() {

        if (!rThread) {
            enviarThread = new EnviarThread(mHandler);
            enviarThread.start();
            consolaUpdate(this.getString(R.string.enviamos));
            rThread = true;
        }
    }
    */


    private class EnviarThread extends Thread {

        private final Handler mHandler;
        private final String server = "http://138.100.72.5";
        private final String serverPath = "/~carretero/mw_obd_a.php";
        private final HttpClient httpclient;

        public EnviarThread(Handler mHandler)
        {
            this.mHandler = mHandler;
            httpclient = new DefaultHttpClient();
        }

        public void run()
        {
            // Pruebas
            // consolaThread("Iniciamos el servicio de comunicación con el servidor.");

            try {
                // Mientras no se corte la conexion
                while (!enviarThread.isInterrupted()) {

                    // consolaThread("Servicio de envio funciona");
                    buscarArchivos();
                    Thread.sleep(pEnvio*1000);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        private void buscarArchivos()
        {

            // Array TEXTO donde guardaremos los nombres de los ficheros
            List<String> item = new ArrayList<String>();

            //Defino la ruta donde busco los ficheros
            File f = EnviarService.this.getFilesDir();
            //Creo el array de tipo File con el contenido de la carpeta
            File[] files = f.listFiles();

            //Hacemos un Loop por cada fichero para extraer el nombre de cada uno
            //consolaThread("Siempre buscando");
            for (int i = 0; i < files.length; i++) {
                //Sacamos del array files un fichero File file = files[i];
                //Comprimimos
                File file = comprimir(files[i]);
                //Enviamos y borramos
                enviarArchivo(file);
            }

        }

        private File comprimir(File file)
        {

            String fileName = file.getName();
            String ext = null;

            if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0) {
                ext = fileName.substring(fileName.lastIndexOf(".") + 1);
            }

            // Si es un txt lo comprimimos antes de enviarlo
            if (ext.equals("txt")) {

                try {

                    byte[] buffer = new byte[1024];

                    GZIPOutputStream gzos =
                            new GZIPOutputStream(
                                    EnviarService.this.openFileOutput(fileName + ".gz", Context.MODE_PRIVATE)
                            );

                    FileInputStream in = EnviarService.this.openFileInput(fileName);

                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        gzos.write(buffer, 0, len);
                    }

                    in.close();

                    gzos.finish();
                    gzos.close();

                    // Una vez comprimido borramos el .txt y trabajamos con el comprimido
                    file.delete();

                    File fileGZ = new File(EnviarService.this.getFilesDir() + "/" + fileName + ".gz");
                    //consolaThread("Comprimido: " + fileGZ.getName());
                    return fileGZ;

                } catch (IOException e) {
                    e.printStackTrace();
                    //debug("comprimirArchivo()", "Error al comprimir el archivo txt");
                    return file;
                }

            } else {

                return file;
            }

        }

        private void enviarArchivo(File file)
        {

            boolean enviado = false;

            String fileName = file.getName();
            String ext = null;

            if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0) {
                ext = fileName.substring(fileName.lastIndexOf(".") + 1);
            }

            // Si ya esta comprimido, como deberia, lo enviamos
            if (ext.equals("gz")) {

                //Aqui llamamo al post de http
                try {

                    HttpPost httpPost = new HttpPost(server + serverPath);
                    FileBody fileBody = new FileBody(file);

                    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                    builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                    builder.addPart("myFile", fileBody);

                    // solicitud para subir fichero al servidor
                    httpPost.setEntity(builder.build());
                    consolaThread("Subiendo al servidor el archivo: " + file.getName());
                    HttpResponse serverResponseCode = httpclient.execute(httpPost);
                    // HttpEntity resEntity = serverResponseCode.getEntity();



                    try {

                        HttpEntity resEntity = serverResponseCode.getEntity();


                        // 200 si se ha enviado corectamente
                        if (serverResponseCode.getStatusLine().getStatusCode() == 200) {
                            consolaThread("Subido al servidor el archivo: " + file.getName());
                            enviado = true;
                        }
                        else {
                            consolaThread("Error al subir archivo al servidor : " + file.getName());

                        }

                        resEntity .consumeContent();
                        // Este metodo es mejor pero no funciona con la libreria de android
                        //EntityUtils.consume(serverResponseCode);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                    //consolaThread("Error al enviar el archivo al servidor");
                }

            }

            if (enviado)
                file.delete();

        }

        private void consolaThread(String texto)
        {
            Message lMsg = new Message();
            lMsg.obj = texto;
            mHandler.sendMessage(lMsg);
        }

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Log.d(TAG, "onStartCommand");
        messenger = (Messenger) intent.getParcelableExtra("Messenger");
        gestionThread();
        return super.onStartCommand(intent, flags, startId);
    }

    // En este metodo enviamos mensage al MainActivity
    private void consolaUpdate(String texto)
    {
        Message lMsg = new Message();
        lMsg.obj=texto;
        try {
            messenger.send(lMsg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void debug(String metodo, String msg)
    {
        if(MainActivity.DEBUG_MODE)
            Log.d(MainActivity.TAG, metodo + ": " + msg);
    }

}
