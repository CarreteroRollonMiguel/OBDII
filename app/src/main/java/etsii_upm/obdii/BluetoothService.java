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


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.os.Handler;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;

import etsii_upm.obdii.commands.SpeedObdCommand;
import etsii_upm.obdii.commands.VinObdCommand;
import etsii_upm.obdii.commands.engine.EngineLoadObdCommand;
import etsii_upm.obdii.commands.engine.EngineRPMObdCommand;
import etsii_upm.obdii.commands.engine.EngineRuntimeObdCommand;
import etsii_upm.obdii.commands.engine.MassAirFlowObdCommand;
import etsii_upm.obdii.commands.engine.ThrottlePositionObdCommand;
import etsii_upm.obdii.commands.fuel.FuelConsumptionRateObdCommand;
import etsii_upm.obdii.commands.fuel.FuelLevelObdCommand;
import etsii_upm.obdii.commands.fuel.FuelTrimObdCommand;
import etsii_upm.obdii.commands.pressure.BarometricPressureObdCommand;
import etsii_upm.obdii.commands.pressure.FuelPressureObdCommand;
import etsii_upm.obdii.commands.pressure.IntakeManifoldPressureObdCommand;
import etsii_upm.obdii.commands.pressure.PressureObdCommand;
import etsii_upm.obdii.commands.protocol.EchoOffObdCommand;
import etsii_upm.obdii.commands.protocol.LineFeedOffObdCommand;
import etsii_upm.obdii.commands.protocol.SelectProtocolObdCommand;
import etsii_upm.obdii.commands.protocol.TimeoutObdCommand;
import etsii_upm.obdii.commands.temperature.AirIntakeTemperatureObdCommand;
import etsii_upm.obdii.commands.temperature.AmbientAirTemperatureObdCommand;
import etsii_upm.obdii.commands.temperature.EngineCoolantTemperatureObdCommand;
import etsii_upm.obdii.commands.temperature.TemperatureObdCommand;
import etsii_upm.obdii.enums.ObdProtocols;
import etsii_upm.obdii.commands.ObdCommand;


public class BluetoothService {

    private final Handler handler;
    private final Context context;
    private final BluetoothAdapter bAdapter;

    public static final String NOMBRE_SEGURO = "BluetoothServiceSecure";
    public static final String NOMBRE_INSEGURO = "BluetoothServiceInsecure";
    public static UUID UUID_SEGURO; //= UUID.fromString("org.danigarcia.examples.bluetooth.BluetoothService.Secure");
    public static UUID UUID_INSEGURO; //= UUID.fromString("org.danigarcia.examples.bluetooth.BluetoothService.Insecure");

    public static final int	ESTADO_NINGUNO				 = 0;
    public static final int	ESTADO_CONECTADO			 = 1;
    public static final int	ESTADO_REALIZANDO_CONEXION	 = 2;
    public static final int	ESTADO_ATENDIENDO_PETICIONES = 3;
    public static final int MSG_CAMBIO_ESTADO            = 10;
    public static final int MSG_ALERTA                   = 14;

    private int 	    estado;
    private HiloOBD	    hiloOBD;
    private String      ultimoDispositivo;

    //Variables de lectura OBD
    //Probar definiendo variables de clase y haciendo los new al final del try OBDII lib

    // Vehicle Id Number
    private VinObdCommand vinCommand;
    private String  vehicleIN = "unknown";

    //Speed
    private SpeedObdCommand speedCommand;
    private boolean  speedCommandNoData = false;

    //Engine
    private EngineLoadObdCommand engineLoadObdCommand;
    private boolean  engineLoadObdCommandNoData = false;
    private EngineRPMObdCommand engineRPMObdCommand;
    private boolean  engineRPMObdCommandNoData = false;
    private EngineRuntimeObdCommand engineRuntimeObdCommand ;
    private boolean  engineRuntimeObdCommandNoData = false;
    private MassAirFlowObdCommand massAirFlowObdCommand ;
    private boolean  massAirFlowObdCommandNoData = false;
    private ThrottlePositionObdCommand throttlePositionObdCommand;
    private boolean  throttlePositionObdCommandNoData = false;


    //Fuel
    private FuelConsumptionRateObdCommand fuelConsumptionRateObdCommand;
    private boolean  fuelConsumptionRateObdCommandNoData = false;
    private FuelLevelObdCommand fuelLevelObdCommand ;
    private boolean   fuelLevelObdCommandNoData = false;

    //Presure
    private BarometricPressureObdCommand barometricPressure ;
    private boolean   barometricPressureNoData = false;
    private FuelPressureObdCommand fuelPressure ;
    private boolean   fuelPressureNoData = false;
    private IntakeManifoldPressureObdCommand intakeManifoldPressure ;
    private boolean   intakeManifoldPressureNoData = false;

    //Temperature
    private AirIntakeTemperatureObdCommand airIntakeTemperature ;
    private boolean   airIntakeTemperatureNoData = false;
    private AmbientAirTemperatureObdCommand ambientAirTemperature ;
    private boolean   ambientAirTemperatureNoData = false;
    private EngineCoolantTemperatureObdCommand engineCoolantTemperature;
    private boolean   engineCoolantTemperatureNoData = false;

    public BluetoothService(Context context, Handler handler, BluetoothAdapter adapter)
    {
        debug("BluetoothService()", "Iniciando metodo");
        this.context	= context;
        this.handler 	= handler;
        this.bAdapter 	= adapter;
        this.estado 	= ESTADO_NINGUNO;
        this.hiloOBD    = null;
        this.ultimoDispositivo = "";

        UUID_SEGURO = generarUUID();
        UUID_INSEGURO = generarUUID();
    }

    // Miguel: Function to set noData to false for all commands
    // to force trying commands everytime the user restart monitoring.
    public void setNoDataFalse (){
        speedCommandNoData = false;
        engineLoadObdCommandNoData = false;
        engineRPMObdCommandNoData = false;
        engineRuntimeObdCommandNoData = false;
        massAirFlowObdCommandNoData = false;
        throttlePositionObdCommandNoData = false;
        fuelConsumptionRateObdCommandNoData = false;
        fuelLevelObdCommandNoData = false;
        barometricPressureNoData = false;
        fuelPressureNoData = false;
        intakeManifoldPressureNoData = false;
        airIntakeTemperatureNoData = false;
        ambientAirTemperatureNoData = false;
        engineCoolantTemperatureNoData = false;
    }
    private synchronized void setEstado(int estado_t)
    {
        estado = estado_t;
        handler.obtainMessage(MSG_CAMBIO_ESTADO, estado, -1).sendToTarget();
    }

    public synchronized int getEstado()
    {
        return estado;
    }

    // Finaliza el  servicio, borrando el HiloOBD de conexion.
    public void finalizarHiloOBD()
    {

        if(hiloOBD != null)
        {
            hiloOBD.cancelarConexionOBD(true);
            hiloOBD = null;
        }

        setEstado(ESTADO_NINGUNO);
        debug("finalizarServicio()", "Finalizando metodo");
    }

    // -----------------------------------------
    // Aqui llegamos cuando apretamos Conectar
    // -----------------------------------------
    public synchronized void solicitarConexion(BluetoothDevice dispositivo)
    {
        debug("solicitarConexion()", "Iniciando metodo");

        // Si existia una conexion abierta, se cierra y se inicia una nueva
        if(hiloOBD != null)
        {
            hiloOBD.cancelarConexionOBD(true);
            hiloOBD = null;
        }

        // Se instancia un nuevo hilo conector, encargado de solicitar una conexion
        // al servidor, que sera la otra parte.
        setEstado(ESTADO_REALIZANDO_CONEXION);
        hiloOBD = new HiloOBD(dispositivo);
        //Toast.makeText(context, "Hemos intentado abrir la hiloOBD", Toast.LENGTH_SHORT).show();
    }


    //----------------------------------------
    // Hilo encargado de solicitar una conexion a un dispositivo Servidor [OBD]
    // Incluye la funcion de lectura
    //----------------------------------------
    private class HiloOBD extends Thread {

        private final BluetoothDevice dispositivo;
        private BluetoothSocket socket;
        private InputStream inputStream;	// Flujo de entrada (lecturas)
        private OutputStream outputStream;	// Flujo de salida (escrituras)
        private int contador;

        public HiloOBD(BluetoothDevice dispositivo)
        {
            debug("HiloOBD()", "Iniciando metodo");
            // BluetoothSocket tmpSocket = null;
            socket = null;
            this.dispositivo = dispositivo;
            ultimoDispositivo = dispositivo.getAddress();
            contador = 10;

            if (bAdapter.isDiscovering())
                bAdapter.cancelDiscovery();

            // Obtenemos un socket para el dispositivo con el que se quiere conectar
            debug("HiloOBD.connect()", "Iniciando metodo");
            connectSocket();
        }

        private void connectSocket(){

            try {
                socket = dispositivo.createRfcommSocketToServiceRecord(UUID_SEGURO);
            } catch (Exception e) {
                Log.e(MainActivity.TAG, "HiloOBD(): Error al abrir el socket", e);
            }

            try {
                socket.connect();
                setEstado(ESTADO_CONECTADO);
                debug("HiloOBD.connect()", "Conectado");
            } catch (IOException e) {
                Log.e(MainActivity.TAG, e.getMessage(), e);
                try {
                    debug("HiloOBD.connect()", "Trying fallback...");
                    socket = (BluetoothSocket) dispositivo.getClass().getMethod("createRfcommSocket",
                            new Class[] {int.class}).invoke(dispositivo,1);
                    socket.connect();
                    setEstado(ESTADO_CONECTADO);
                    debug("HiloOBD.connect()", "Conectado");
                } catch (Exception e2) {
                    Log.e("", e2.getMessage(), e2);
                    debug("HiloOBD.connect()", "Couldn't establish Bluetooth connection!");
                    setEstado(ESTADO_NINGUNO);
                }
            }

        }

        private void initOBDCommands () {

            int waitTime = 20;



                    ;  // sleep between send commmand and get data

            try {
                //Vehicle Id Number
                vinCommand = new VinObdCommand();
                vinCommand.setWaitDataTime(150);

            }catch (Exception e) {
                Log.e("", e.getMessage(), e);
                vinCommand = null;
            }

            try {
                //Speed
                speedCommand = new SpeedObdCommand();
                speedCommand.setWaitDataTime(waitTime);
                speedCommandNoData = false;

            }catch (Exception e) {
                Log.e("", e.getMessage(), e);
                speedCommand = null;
            }

            try {
                //Engine
                engineLoadObdCommand = new EngineLoadObdCommand();
                engineLoadObdCommand.setWaitDataTime(waitTime);
                engineLoadObdCommandNoData = false;
            }catch (Exception e) {
                Log.e("", e.getMessage(), e);
                engineLoadObdCommand = null;
            }
            try {
                engineRPMObdCommand = new EngineRPMObdCommand();
                engineRPMObdCommand.setWaitDataTime(waitTime);
                engineRPMObdCommandNoData = false;
            }catch (Exception e) {
                Log.e("", e.getMessage(), e);
                engineRPMObdCommand = null;
            }
            try {
                engineRuntimeObdCommand = new EngineRuntimeObdCommand();
                engineRuntimeObdCommand.setWaitDataTime(waitTime);
                engineRuntimeObdCommandNoData = false;
            }catch (Exception e) {
                Log.e("", e.getMessage(), e);
                engineRuntimeObdCommand = null;
            }
            try {
                massAirFlowObdCommand= new MassAirFlowObdCommand();
                massAirFlowObdCommand.setWaitDataTime(waitTime);
                massAirFlowObdCommandNoData = false;
            }catch (Exception e) {
                Log.e("", e.getMessage(), e);
                massAirFlowObdCommand = null;
            }
            try {
                throttlePositionObdCommand= new ThrottlePositionObdCommand();
                throttlePositionObdCommand.setWaitDataTime(waitTime);
                throttlePositionObdCommandNoData = false;
            }catch (Exception e) {
                Log.e("", e.getMessage(), e);
                throttlePositionObdCommand = null;
            }
            try {
                //Fuel
                fuelConsumptionRateObdCommand = new FuelConsumptionRateObdCommand();
                fuelConsumptionRateObdCommand.setWaitDataTime(waitTime);
                fuelConsumptionRateObdCommandNoData = false;
            }catch (Exception e) {
                Log.e("", e.getMessage(), e);
                fuelConsumptionRateObdCommand = null;
            }

            try {
                //Fuel
                fuelLevelObdCommand = new FuelLevelObdCommand();
                fuelLevelObdCommand.setWaitDataTime(waitTime);
                fuelLevelObdCommandNoData = false;
            } catch (Exception e) {
                Log.e("", e.getMessage(), e);
                fuelLevelObdCommand = null;
            }
            try {
                //Presure
                barometricPressure = new BarometricPressureObdCommand();
                barometricPressure.setWaitDataTime(waitTime);
                barometricPressureNoData = false;
            } catch (Exception e) {
                Log.e("", e.getMessage(), e);
                barometricPressure = null;
            }
            try {
                //Presure
                fuelPressure = new FuelPressureObdCommand();
                fuelPressure.setWaitDataTime(waitTime);
                fuelPressureNoData = false;
            } catch (Exception e) {
                Log.e("", e.getMessage(), e);
                fuelPressure = null;
            }
            try {
                //Presure
                intakeManifoldPressure= new IntakeManifoldPressureObdCommand();
                intakeManifoldPressure.setWaitDataTime(waitTime);
                intakeManifoldPressureNoData = false;
            } catch (Exception e) {
                Log.e("", e.getMessage(), e);
                intakeManifoldPressure = null;
            }
            try {
                //Temperature
                airIntakeTemperature = new AirIntakeTemperatureObdCommand();
                airIntakeTemperature.setWaitDataTime(waitTime);
                airIntakeTemperatureNoData = false;
            } catch (Exception e) {
                Log.e("", e.getMessage(), e);
                airIntakeTemperature = null;
            }
            try {
                //Temperature
                ambientAirTemperature = new AmbientAirTemperatureObdCommand();
                ambientAirTemperature.setWaitDataTime(waitTime);
                ambientAirTemperatureNoData = false;
            } catch (Exception e) {
                Log.e("", e.getMessage(), e);
                ambientAirTemperature = null;
            }
            try {
                //Temperature
                engineCoolantTemperature = new EngineCoolantTemperatureObdCommand();
                engineCoolantTemperature.setWaitDataTime(waitTime);
                engineCoolantTemperatureNoData = false;
            } catch (Exception e) {
                Log.e("", e.getMessage(), e);
                engineCoolantTemperature = null;
            }

        }
        // Inicializamos OBD antes de empezar a leer
        private void connectOBD(){

            if(estado == ESTADO_CONECTADO && socket != null) {

                // Esto da fallos :
                // socket.getInputStream(), socket.getOutputStream()
                try {
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();
                } catch (IOException e) {
                    Log.e(MainActivity.TAG, "HiloConexion(): Error al obtener flujos de E/S", e);
                }

                // OBDII lib
                try {
                    new EchoOffObdCommand().run(inputStream, outputStream, 0);
                    new LineFeedOffObdCommand().run(inputStream, outputStream, 0);
                    new TimeoutObdCommand(255).run(inputStream, outputStream, 0);
                    new SelectProtocolObdCommand(ObdProtocols.AUTO).run(inputStream, outputStream, 0);
                    setEstado(ESTADO_ATENDIENDO_PETICIONES);

                    initOBDCommands();



                    contador = 0;

                } catch (Exception e) {
                    Log.e("", e.getMessage(), e);
                }

            }else if(socket ==null){
                setEstado(ESTADO_NINGUNO);
            }

        }

        // Realizamos la lectura pertinente
        private String lecturaOBD()
        {
            StringBuffer lectura;
            // number of commands to read
            int numCommandsTested = 14;
            // Values to compute fuel consumption from maf if there is not data
            float maf = (float) -1.0;
            float calculatedFCR = (float) -1.0;

            // 10 lecturas fallidas con intento de reconexion, damos por muerta la conexion
            if (contador == 10)
            {
                lectura = new StringBuffer();
                lectura.append("'vin': 'noD',");
                lectura.append("'sO': 'noD',");
                lectura.append("'el': 'noD','erpm': 'noD','ert': 'noD','maf': 'noD','tp': 'noD',");
                lectura.append("'fcr': 'noD','fl': 'noD',");
                lectura.append("'bp': 'noD','fp': 'noD','imp': 'noD',");
                lectura.append("'ait': 'noD','aat': 'noD','ect': 'noD'");

                setEstado(ESTADO_NINGUNO);
            }

            if(estado == ESTADO_ATENDIENDO_PETICIONES) {

                //Variables de lectura OBD  iniciadas y run en connect OBD
                //Probar definiendo variables de clase y haciendo los new al final del try OBDII lib

                int fallo = 0;
                lectura = new StringBuffer();

                if (vinCommand != null) {
                    if (vehicleIN.equals("unknown") ) {
                        // send the command only first time. Twice, as it sometimes take more time
                        try {
                            vinCommand.run(inputStream, outputStream, 9);
                            vehicleIN = vinCommand.getVehicleName();
                            //vehicleIN = "VF1BZ110652142446";
                            lectura.append("'vin': '" + vehicleIN + "',");

                        } catch (Exception e) {
                            Log.e("lecturaOBD()", e.getMessage(), e);
                        }
                        try {
                            vinCommand.run(inputStream, outputStream, 9);
                            vehicleIN = vinCommand.getVehicleName();
                            lectura.append("'vin': '" + vehicleIN + "',");

                        } catch (Exception e) {
                            Log.e("lecturaOBD()", e.getMessage(), e);
                            lectura.append("'vin': 'noD',");
                        }
                    } else {
                        lectura.append("'vin': '" + vehicleIN + "',");
                    }
                }


                if ((speedCommand != null) && (!speedCommandNoData )){
                    try {
                        //speedCommand.getFormattedResult();
                        speedCommand.run(inputStream, outputStream, 1);
                        lectura.append("'sO': " + speedCommand.getMetricSpeed() + ",");

                    } catch (Exception e) {
                        Log.e("lecturaOBD()", e.getMessage(), e);
                        fallo++;
                        lectura.append("'sO': 'noD',");
                        speedCommandNoData = true;
                    }
                }else{
                    if (speedCommandNoData)
                        lectura.append("'sO': 'noD',");
                    else
                        fallo++;
                }

                //Engine
                if ((engineLoadObdCommand != null) && (!engineLoadObdCommandNoData)){
                    try {
                        engineLoadObdCommand.run(inputStream, outputStream, 1);
                        lectura.append( "'el': " + engineLoadObdCommand.getPercentage() + ",");
                    } catch (Exception e) {
                        Log.e("lecturaOBD()", e.getMessage(), e);
                        fallo++;
                        lectura.append("'el': 'noD',");
                        engineLoadObdCommandNoData = true;
                    }
                }else{
                    if (engineLoadObdCommandNoData)
                        lectura.append("'el': 'noD',");
                    else
                        fallo++;
                }

                if ((engineRPMObdCommand != null) && (!engineRPMObdCommandNoData)){
                    try {
                        engineRPMObdCommand.run(inputStream, outputStream, 1);
                        lectura.append( "'erpm': " + engineRPMObdCommand.getRPM() + ",");
                    } catch (Exception e) {
                        Log.e("lecturaOBD()", e.getMessage(), e);
                        fallo++;
                        lectura.append("'erpm': 'noD',");
                        engineRPMObdCommandNoData = true;
                    }
                }else{
                    if (engineRPMObdCommandNoData)
                        lectura.append("'erpm': 'noD',");
                    else
                        fallo++;
                }

                if ((engineRuntimeObdCommand != null) && (!engineRuntimeObdCommandNoData)){
                    try {
                        engineRuntimeObdCommand.run(inputStream, outputStream, 1);
                        lectura.append( "'ert': '" + engineRuntimeObdCommand.getRuntimeInSeconds() + "',");
                    } catch (Exception e) {
                        Log.e("lecturaOBD()", e.getMessage(), e);
                        fallo++;
                        lectura.append("'ert': 'noD',");
                        engineRuntimeObdCommandNoData = true;
                    }
                }else{
                    if (engineRuntimeObdCommandNoData)
                        lectura.append("'ert': 'noD',");
                    else
                        fallo++;
                }

                if ((massAirFlowObdCommand != null) && (!massAirFlowObdCommandNoData)){
                    try {
                        massAirFlowObdCommand.run(inputStream, outputStream, 1);
                        maf = (float) massAirFlowObdCommand.getMAF();
                        lectura.append( "'maf': " + maf + ",");
                    } catch (Exception e) {
                        Log.e("lecturaOBD()", e.getMessage(), e);
                        fallo++;
                        lectura.append("'maf': 'noD',");
                        massAirFlowObdCommandNoData = true;
                    }
                }else{
                    if (massAirFlowObdCommandNoData)
                        lectura.append("'maf': 'noD',");
                    else
                        fallo++;
                }

                if ((throttlePositionObdCommand != null) && (!throttlePositionObdCommandNoData)){
                    try {
                        throttlePositionObdCommand.run(inputStream, outputStream, 1);
                        lectura.append( "'tp': " + throttlePositionObdCommand.getPercentage() + ",");
                    } catch (Exception e) {
                        Log.e("lecturaOBD()", e.getMessage(), e);
                        fallo++;
                        lectura.append("'tp': 'noD',");
                        throttlePositionObdCommandNoData = true;
                    }
                }else{
                    if (throttlePositionObdCommandNoData)
                        lectura.append("'tp': 'noD',");
                    else
                        fallo++;
                }

                //Fuel
                if ((fuelConsumptionRateObdCommand != null) &&(!fuelConsumptionRateObdCommandNoData)) {
                    try {
                        fuelConsumptionRateObdCommand.run(inputStream, outputStream, 1);
                        lectura.append( "'fcr': " + fuelConsumptionRateObdCommand.getLitersPerHour() + ",");

                    } catch (Exception e) {
                        // If there is data for MAF, compute
                        if (maf >=0) {
                            calculatedFCR = (float)((3600 * maf) / (14.75*840.0));
                            lectura.append( "'fcr': " + calculatedFCR + ",");
                        } else {
                            Log.e("lecturaOBD()", e.getMessage(), e);
                            fallo++;
                            lectura.append("'fcr': 'noD',");
                        }
                        fuelConsumptionRateObdCommandNoData = true;
                    }
                }else{
                    if (fuelConsumptionRateObdCommandNoData)
                        // If there is data for MAF, compute
                        if (maf >=0) {
                            calculatedFCR = (float)((3600 * maf) / (14.75*840.0));
                            lectura.append( "'fcr': " + calculatedFCR + ",");
                        } else {
                            lectura.append("'fcr': 'noD',");
                        }
                    else
                        fallo++;
                }


                if ((fuelLevelObdCommand != null) && (!fuelLevelObdCommandNoData)){
                    try {
                        fuelLevelObdCommand.run(inputStream, outputStream, 1);
                        lectura.append( "'fl': " + fuelLevelObdCommand.getFuelLevel() + ",");
                    } catch (Exception e) {
                        Log.e("lecturaOBD()", e.getMessage(), e);
                        fallo++;
                        lectura.append("'fl': 'noD',");
                        fuelLevelObdCommandNoData = true;
                    }
                }else{
                    if (fuelLevelObdCommandNoData)
                        lectura.append("'fl': 'noD',");
                    else
                        fallo++;
                }
                //Pressure
                if ((barometricPressure != null) && (!barometricPressureNoData)) {
                    try {
                        barometricPressure.run(inputStream, outputStream, 1);
                        lectura.append("'bp': " + barometricPressure.getMetricUnit() +",");
                    } catch (Exception e) {
                        Log.e("lecturaOBD()", e.getMessage(), e);
                        fallo++;
                        lectura.append("'bp': 'noD',");
                        barometricPressureNoData = true;
                    }
                }else{
                    if (barometricPressureNoData)
                        lectura.append("'bp': 'noD',");
                    else
                        fallo++;
                }

                if ((fuelPressure != null) && (!fuelPressureNoData)){
                    try {
                        fuelPressure.run(inputStream, outputStream, 1);
                        lectura.append("'fp': " + fuelPressure.getMetricUnit()  + ",");
                    } catch (Exception e) {
                        Log.e("lecturaOBD()", e.getMessage(), e);
                        fallo++;
                        lectura.append("'fp': 'noD',");
                        fuelPressureNoData = true;
                    }
                }else{
                    if (fuelPressureNoData)
                        lectura.append("'fp': 'noD',");
                    else
                        fallo++;
                }
                if ((intakeManifoldPressure != null) && (!intakeManifoldPressureNoData)) {
                    try {
                        intakeManifoldPressure.run(inputStream, outputStream, 1);
                        lectura.append("'imp': " + intakeManifoldPressure.getMetricUnit()  + ",");
                    } catch (Exception e) {
                        Log.e("lecturaOBD()", e.getMessage(), e);
                        fallo++;
                        lectura.append("'imp': 'noD',");
                        intakeManifoldPressureNoData = true;
                    }
                }else{
                    if (intakeManifoldPressureNoData)
                        lectura.append("'imp': 'noD',");
                    else
                        fallo++;
                }

                if ((airIntakeTemperature != null) && (!airIntakeTemperatureNoData)){
                    try {
                        airIntakeTemperature.run(inputStream, outputStream, 1);
                        // lectura.append("'ait': " + airIntakeTemperature.getKelvin() + ",");
                        lectura.append("'ait': " + airIntakeTemperature.getTemperature() + ",");
                    } catch (Exception e) {
                        Log.e("lecturaOBD()", e.getMessage(), e);
                        fallo++;
                        lectura.append("'ait': 'noD',");
                        airIntakeTemperatureNoData = true;
                    }
                }else{
                    if (airIntakeTemperatureNoData)
                        lectura.append("'ait': 'noD',");
                    else
                        fallo++;
                }

                if ((ambientAirTemperature != null) && (!ambientAirTemperatureNoData)){
                    try {
                        ambientAirTemperature.run(inputStream, outputStream, 1);
                        //lectura.append("'aat': " + ambientAirTemperature.getKelvin() + ",");
                        lectura.append("'aat': " + ambientAirTemperature.getTemperature() + ",");
                    } catch (Exception e) {
                        Log.e("lecturaOBD()", e.getMessage(), e);
                        fallo++;
                        lectura.append("'aat': 'noD',");
                        ambientAirTemperatureNoData = true;
                    }
                }else{
                    if (ambientAirTemperatureNoData)
                        lectura.append("'aat': 'noD',");
                    else
                        fallo++;
                }
                if ((engineCoolantTemperature != null) && (!engineCoolantTemperatureNoData)){
                    try {
                        engineCoolantTemperature.run(inputStream, outputStream, 1);
                        //lectura.append("'ect': " + engineCoolantTemperature.getKelvin() );
                        lectura.append("'ect': " + engineCoolantTemperature.getTemperature() );
                    } catch (Exception e) {
                        Log.e("lecturaOBD()", e.getMessage(), e);
                        fallo++;
                        lectura.append("'ect': 'noD'");
                        engineCoolantTemperatureNoData = true;
                    }
                }else{
                    if (engineCoolantTemperatureNoData)
                        lectura.append("'ect': 'noD'");
                    else
                        fallo++;
                }

                // Si fallan todas las sublecturas el into es fallido
                if(fallo == numCommandsTested){
                    contador++;
                    // Si fallan cinco intentos se intenta reestablecer conexion
                    if (contador == 5){
                        cancelarConexionOBD(false);
                        connectOBD();
                    }
                }

            }else{
                lectura = new StringBuffer();
                lectura.append("'vin': 'noD',");
                lectura.append("'sO': 'noD',");
                lectura.append("'el': 'noD','erpm': 'noD','ert': 'noD','maf': 'noD','tp': 'noD',");
                lectura.append("'fcr': 'noD','fl': 'noD',");
                lectura.append("'bp': 'noD','fp': 'noD','imp': 'noD',");
                lectura.append("'ait': 'noD','aat': 'noD','ect': 'noD'");

                contador++;
                // Si fallan cinco intentos se intenta reestablecer conexion
                if (contador == 5){
                    cancelarConexionOBD(false);
                    connectOBD();
                }
            }

            return lectura.toString();
        }

        private void cancelarConexionOBD(boolean matarHiloOBD)
        {
            debug("cancelarConexion()", "Iniciando metodo");
            try {

                if(estado == ESTADO_ATENDIENDO_PETICIONES) {
                    inputStream  = null;
                    outputStream = null;
                    setEstado(ESTADO_CONECTADO);
                }

                if(matarHiloOBD){
                    if(estado == ESTADO_CONECTADO){
                        socket.close();
                    }
                }

            }
            catch(IOException e) {
                Log.e(MainActivity.TAG, "HiloConexion.cancelarConexion(): Error al cerrar el socket", e);
            }
        }

    }

    //Interface de HiloOBD
    // ----------------------------------------

    public String getNombreDispositivo()
    {
        return ultimoDispositivo;
    }

    public void connectOBD()
    {
        if(hiloOBD != null) {
            hiloOBD.connectOBD();
        }else{
            setEstado(ESTADO_NINGUNO);
        }
    }

    public String lecturaOBD()
    {
        if(hiloOBD != null) {
            return hiloOBD.lecturaOBD();
        }else{
            StringBuffer lectura = new StringBuffer();
            lectura.append("'vin': 'noD',");
            lectura.append("'sO': 'noD',");
            lectura.append("'el': 'noD','erpm': 'noD','ert': 'noD','maf': 'noD','tp': 'noD',");
            lectura.append("'fcr': 'noD','fl': 'noD',");
            lectura.append("'bp': 'noD','fp': 'noD','imp': 'noD',");
            lectura.append("'ait': 'noD','aat': 'noD','ect': 'noD'");

            setEstado(ESTADO_NINGUNO);
            return lectura.toString();
        }
    }

    public void cancelarConexionOBD()
    {
        if(hiloOBD != null)
            hiloOBD.cancelarConexionOBD(false);
    }

    public int contadorOBD()
    {
        if(hiloOBD != null) {
            return hiloOBD.contador;
        }else{
            return 10;
        }
    }


    // ----------------------------------------

    private UUID generarUUID()
    {
        ContentResolver appResolver = context.getApplicationContext().getContentResolver();
        String id = Secure.getString(appResolver, Secure.ANDROID_ID);
        final TelephonyManager tManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        final String deviceId = String.valueOf(tManager.getDeviceId());
        final String simSerialNumber = String.valueOf(tManager.getSimSerialNumber());
        final String androidId	= android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        UUID uuid = new UUID(androidId.hashCode(), ((long)deviceId.hashCode() << 32) | simSerialNumber.hashCode());
        uuid = new UUID((long)1000, (long)23);
        return uuid;
    }

    public void debug(String metodo, String msg)
    {
        if(MainActivity.DEBUG_MODE)
            Log.d(MainActivity.TAG, metodo + ": " + msg);
    }

}

