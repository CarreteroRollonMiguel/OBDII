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


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TabHost;

public class MainActivity extends Activity
        implements OnClickListener	{

    public static final String TAG = "Debug APP Miguel";
    public static final boolean DEBUG_MODE = false;

    // Declaramos una constante para lanzar los Intent de activacion de Bluetooth
    private static final int 	REQUEST_ENABLE_BT 	= 1;
    private static final String ALERTA	= "alerta";

    // Declaramos una variable privada para elemento UI
    public TabHost tabHost;
    public Button btnRecibir;
    private Button btnBluetooth;
    private Button btnBuscarDispositivo;
    private TextView tvConsola;
    private ScrollView scrConsola;
    private TextView tvConexion;
    private CheckBox cb_3g;
    private ListView lvDispositivos;

    // Periodos de los servicios
    public final int gpsPeriodo = 5;    //Cada cuanto actualiza localizacion GPS (seg) Bateria!!!
    public final int pLectura = 1;      //Cada lee OBD (seg), tiene que ser menor que 60s
    public final int pEscritura = 1;    //Cada cuanto crea un archivo (min)

    // Variables de la app
    private LeerService leerService;
    private boolean enviarCon3g = false;
    private EnviarService mEnviarService;
    private boolean mEnviarServiceBound = false;

    private BluetoothAdapter bAdapter;					// Adapter para uso del Bluetooth
    private BroadcastReceiver bReceiver;
    private Handler bHandler;
    private ArrayList<BluetoothDevice> arrayDevices;	// Listado de dispositivos
    private ArrayAdapter arrayAdapter;					// Adaptador para el listado de dispositivos

    private BluetoothService 	bService = null;				// Servicio de mensajes de Bluetooth
    private BluetoothDevice		ultimoDispositivo;		        // Ultimo dispositivo conectado


    // Handlers y Bradcasts
    //----------------------------------------------------

    // BroadcastReceiver, detecta cambios en el estado del Bluetooth del dispositivo
    public class bBroadcastR extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();

            // BluetoothAdapter.ACTION_STATE_CHANGED
            // Codigo que se ejecutara cuando el Bluetooth cambie su estado.
            // Manejaremos los siguientes estados:
            //		- STATE_OFF: El Bluetooth se desactiva
            //		- STATE ON: El Bluetooth se activa
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action))
            {
                final int estado = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (estado)
                {
                    // Apagado
                    case BluetoothAdapter.STATE_OFF:
                    {
                        Log.v(TAG, "onReceive: Apagando");
                        ((Button)findViewById(R.id.btnBluetooth)).setText(R.string.ActivarBluetooth);
                        ((Button)findViewById(R.id.btnBuscarDispositivo)).setEnabled(false);
                        leerService.pararMonitorizacion();
                        ((Button)findViewById(R.id.btnRecibir)).setEnabled(false);
                        break;
                    }

                    // Encendido
                    case BluetoothAdapter.STATE_ON:
                    {
                        Log.v(TAG, "onReceive: Encendiendo");
                        ((Button)findViewById(R.id.btnBluetooth)).setText(R.string.DesactivarBluetooth);
                        ((Button)findViewById(R.id.btnBuscarDispositivo)).setEnabled(true);
                        // Activar solo para pruebas:
                        //((Button)findViewById(R.id.btnRecibir)).setEnabled(true);

                        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                        //discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 90);
                        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60);
                        startActivity(discoverableIntent);

                        break;
                    }
                    default:
                        break;
                } // Fin switch

            } // Fin if

            // BluetoothDevice.ACTION_FOUND
            // Cada vez que se descubra un nuevo dispositivo por Bluetooth, se ejecutara
            // este fragmento de codigo
            else if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                if(arrayDevices == null)
                    arrayDevices = new ArrayList<BluetoothDevice>();

                BluetoothDevice dispositivo = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                arrayDevices.add(dispositivo);
                String descripcionDispositivo = dispositivo.getName() + " [" + dispositivo.getAddress() + "]";
                Toast.makeText(getBaseContext(),R.string.DetectadoDispositivo + " " + descripcionDispositivo, Toast.LENGTH_SHORT).show();
                Log.v(TAG, "ACTION_FOUND: Dispositivo encontrado: " + descripcionDispositivo);
            }

            // BluetoothAdapter.ACTION_DISCOVERY_FINISHED
            // Codigo que se ejecutara cuando el Bluetooth finalice la busqueda de dispositivos.
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                // Instanciamos un nuevo adapter para el ListView
                arrayAdapter = new BluetoothDeviceArrayAdapter(getBaseContext(), android.R.layout.simple_list_item_2, arrayDevices);
                lvDispositivos.setAdapter(arrayAdapter);
                Toast.makeText(getBaseContext(), R.string.FinBusqueda, Toast.LENGTH_SHORT).show();
            }

        } // Fin onReceive

    }

    // Handler que obtendra informacion de BluetoothService
    private class bluetoohtHandler extends Handler {

        @Override
        public void handleMessage(Message msg)
        {
            String mensaje 	= null;

            // Atendemos al tipo de mensaje
            switch(msg.what)
            {

                // Mensaje de cambio de estado
                case BluetoothService.MSG_CAMBIO_ESTADO:
                {
                    switch(msg.arg1)
                    {
                        // ATENDIENDO PETICIONES: OBDII listo para leer
                        case BluetoothService.ESTADO_ATENDIENDO_PETICIONES:
                            break;

                        // CONECTADO: Se muestra el dispositivo al que se ha conectado y se activa el boton de enviar
                        case BluetoothService.ESTADO_CONECTADO:
                        {
                            mensaje = getString(R.string.ConexionActual) + " " + ultimoDispositivo.getName();
                            Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT).show();
                            tvConexion.setText(mensaje);
                            ((Button)findViewById(R.id.btnRecibir)).setEnabled(true);
                            break;
                        }

                        // REALIZANDO CONEXION: Se muestra el dispositivo al que se esta conectando
                        case BluetoothService.ESTADO_REALIZANDO_CONEXION:
                        {
                            mensaje = getString(R.string.ConectandoA) + " " + ultimoDispositivo.getName() + " [" + ultimoDispositivo.getAddress() + "]";
                            Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT).show();
                            break;
                        }

                        // NINGUNO: Mensaje por defecto. Desactivacion del boton de enviar y para lectura si hace falta
                        case BluetoothService.ESTADO_NINGUNO:
                        {
                            mensaje = getString(R.string.SinConexion);
                            Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT).show();
                            tvConexion.setText(mensaje);
                            // Puentear para pruebas
                            // /*
                            leerService.pararMonitorizacion();
                            ((Button)findViewById(R.id.btnRecibir)).setEnabled(false);
                            // */
                            break;
                        }
                        default:
                            break;
                    }
                    break;
                }

                // Mensaje de alerta: se mostrara en el Toast
                case BluetoothService.MSG_ALERTA:
                {
                    mensaje = msg.getData().getString(ALERTA);
                    Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT).show();
                    break;
                }

                default:
                    break;
            }
        }
    }

    // Handler que obtendra informacion de enviarService
    private class enviarHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String str = (String)msg.obj;
            escribirConsola(str);
        }
    }

    //----------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializacion de los componentes
        //------------------------------------------------------------------------
        // Instanciamos el array de dispositivos
        arrayDevices = new ArrayList<BluetoothDevice>();

        // Referenciamos los controles y registramos los listeners
        referenciarControles();
        registrarEventosControles();

        // Por defecto, desactivamos ciertos botones hasta establecer las conexiones
        cb_3g.setChecked(false);
        btnBuscarDispositivo.setEnabled(false);
        btnRecibir.setEnabled(false);

        // Configuramos el adaptador bluetooth y nos suscribimos a sus eventos
        bHandler = new bluetoohtHandler();
        configurarAdaptadorBluetooth();

        // Suscribe el BroadcastReceiver que instanciamos previamente a los
        // eventos relacionados con Bluetooth que queremos controlar
        IntentFilter filtro = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        filtro.addAction(BluetoothDevice.ACTION_FOUND);
        filtro.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        bReceiver = new bBroadcastR();
        this.registerReceiver(bReceiver, filtro);


        // Inicializamos Tarea monitorizacion:
        // Periodo de consulta OBD en seg.,
        // Periodo grabación a archivo en minutos
        leerService = new LeerService(this, bService);

        // Comenzamos Service encargado de la comunicacion con el servidor:
        Messenger mActivityMessenger = new Messenger(new enviarHandler());
        Intent enviarService = new Intent(this, EnviarService.class);
        enviarService.putExtra("Messenger", mActivityMessenger);
        startService(enviarService);
        bindService(enviarService, mServiceConnection, Context.BIND_AUTO_CREATE);
        // Hemos bindeado ahora podemos llamar a los methodos publicos
        //mEnviarService.gestionThread();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //Simplemente para crear el contenido de la etiqueta de las pestañas
    private static View createTabView(final Context context, final String text)
    {
        View view = LayoutInflater.from(context).inflate(R.layout.tabs_bg, null);
        TextView tv = (TextView) view.findViewById(R.id.tabsText);
        tv.setText(text);
        return view;
    }

    // Referencia los elementos de interfaz
    private void referenciarControles()
    {
        // Referenciamos y creamos tabs
        tabHost=(TabHost)findViewById(android.R.id.tabhost);
        tabHost.setup();
        tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator(createTabView(tabHost.getContext(), "Monitorizar")).setContent(R.id.tab1));
        tabHost.addTab(tabHost.newTabSpec("tab2").setIndicator(createTabView(tabHost.getContext(), "Bluetooth")).setContent(R.id.tab2));
        //Si queremos imposibilitart una pestaña
        //tabHost.getTabWidget().getChildAt(0).setEnabled(false);
        tabHost.setCurrentTab(1);

        // Referenciamos los elementos transmision datos
        tvConsola = (TextView)findViewById(R.id.tvConsola);
        scrConsola = (ScrollView) findViewById(R.id.ScrConsola);
        btnRecibir = (Button)findViewById(R.id.btnRecibir);

        // Referenciamos los elementos Bluetooth
        lvDispositivos = (ListView)findViewById(R.id.lvDispositivos);
        btnBluetooth = (Button)findViewById(R.id.btnBluetooth);
        btnBuscarDispositivo = (Button)findViewById(R.id.btnBuscarDispositivo);

        // Referenciamos los elementos Pie
        tvConexion = (TextView)findViewById(R.id.tvConexion);
        cb_3g = (CheckBox)findViewById(R.id.cb_3g);
    }

    // Registra los eventos de interfaz (eventos onClick, onItemClick, etc.)
    private void registrarEventosControles()
    {
        // Asignamos los handlers de los botones tab1
        btnRecibir.setOnClickListener(this);

        // Asignamos los handlers de los botones tab2
        btnBluetooth.setOnClickListener(this);
        btnBuscarDispositivo.setOnClickListener(this);

        // Asignamos handler radious check
        cb_3g.setOnClickListener(this);

        // Configuramos la lista de dispositivos para que cuando seleccionemos
        // uno de sus elementos realice la conexion al dispositivo
        configurarListaDispositivos();
    }

    // Configura el ListView para que responda a los eventos de pulsacion
    private void configurarListaDispositivos()
    {
        lvDispositivos.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView adapter, View view, int position, long arg) {
                // El ListView tiene un adaptador de tipo BluetoothDeviceArrayAdapter.
                // Invocamos el metodo getItem() del adaptador para recibir el dispositivo
                // bluetooth y realizar la conexion.
                BluetoothDevice dispositivo = (BluetoothDevice) lvDispositivos.getAdapter().getItem(position);

                AlertDialog dialog = crearDialogoConexion(getString(R.string.Conectar),
                        getString(R.string.MsgConfirmarConexion) + " " + dispositivo.getName() + "?",
                        dispositivo.getAddress());

                dialog.show();
            }
        });
    }

    // Elementos de la interfaz grafica
    //----------------------------------------------------

    // Dialogo conexion dispositivo bluetooth
    private AlertDialog crearDialogoConexion(String titulo, String mensaje, final String direccion)
    {
        // Instanciamos un nuevo AlertDialog Builder y le asociamos titulo y mensaje
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(titulo);
        alertDialogBuilder.setMessage(mensaje);

        // Creamos un nuevo OnClickListener para el boton OK que realice la conexion
        DialogInterface.OnClickListener listenerOk = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Toast.makeText(this, "Conectando a " + direccion, Toast.LENGTH_LONG).show();
                if(bService != null)
                {
                    BluetoothDevice dispositivoRemoto = bAdapter.getRemoteDevice(direccion);
                    ultimoDispositivo = dispositivoRemoto;
                    bService.solicitarConexion(dispositivoRemoto);
                    //Toast.makeText(getApplicationContext(), "LLego aqui", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        };
        // Creamos un nuevo OnClickListener para el boton Cancelar
        DialogInterface.OnClickListener listenerCancelar = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        };

        // Asignamos los botones positivo y negativo a sus respectivos listeners
        alertDialogBuilder.setPositiveButton(R.string.Conectar, listenerOk);
        alertDialogBuilder.setNegativeButton(R.string.Cancelar, listenerCancelar);

        return alertDialogBuilder.create();
    }

    // Dialogo activacion GPS
    public void crearDialogoGPS()
    {
        final AlertDialog.Builder builder =  new AlertDialog.Builder(this);
        final String action = Settings.ACTION_LOCATION_SOURCE_SETTINGS;
        final String message = "Es necesario que active el GPS de su dispositivo. \n"
                + "Acceda a ajustes para tramitar la activación";

        builder.setMessage(message)
                .setPositiveButton("Ajustes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int id) {
                                Intent intent = new Intent(action);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                getApplicationContext().startActivity(intent);
                                d.dismiss();
                            }
                        })
                .setNegativeButton("Cancelar",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int id) {
                                d.cancel();
                            }
                        });
        builder.create().show();
    }

    // Lista de dispositivos bluetooth encontrados
    private class BluetoothDeviceArrayAdapter extends ArrayAdapter{

        private List<BluetoothDevice> deviceList;	// Contendra el listado de dispositivos
        private Context context;					// Contexto activo

        public BluetoothDeviceArrayAdapter(Context context, int textViewResourceId, List<BluetoothDevice> objects)
        {
            super(context, textViewResourceId, objects);
            this.deviceList = objects;
            this.context = context;
        }

        @Override
        public int getCount()
        {
            if(deviceList != null)
                return deviceList.size();
            else
                return -1;
        }

        @Override
        public Object getItem(int position)
        {
            return (deviceList == null ? null : deviceList.get(position));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            if((deviceList == null) || (context == null))
                return null;

            // Usamos un LayoutInflater para crear las vistas
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            // Creamos una vista a partir de simple_list_item_2, que contiene dos TextView.
            // El primero (text1) lo usaremos para el nombre, mientras que el segundo (text2)
            // lo utilizaremos para la direccion del dispositivo.
            View elemento = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);

            // Referenciamos los TextView
            TextView txtNombre = (TextView)elemento.findViewById(android.R.id.text1);
            TextView txtDireccion = (TextView)elemento.findViewById(android.R.id.text2);

            // Obtenemos el dispositivo del array y obtenemos su nombre y direccion, asociandosela
            // a los dos TextView del elemento
            BluetoothDevice dispositivo = (BluetoothDevice)getItem(position);
            if(dispositivo != null)
            {
                txtNombre.setText(dispositivo.getName());
                txtDireccion.setText(dispositivo.getAddress());
            }
            else
            {
                txtNombre.setText("ERROR");
            }

            // Devolvemos el elemento con los dos TextView cumplimentados
            return elemento;
        }
    }

    //----------------------------------------------------

    // Configura el BluetoothAdapter y los botones asociados
    private void configurarAdaptadorBluetooth()
    {
        // Obtenemos el adaptador Bluetooth. Si es NULL, significara que el
        // dispositivo no posee Bluetooth, por lo que deshabilitamos el boton
        // encargado de activar/desactivar esta caracteristica.
        bAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bAdapter == null)
        {
            btnBluetooth.setEnabled(false);
            return;
        }

        // Comprobamos si el Bluetooth esta activo y cambiamos el texto de los botones
        // dependiendo del estado. Tambien activamos o desactivamos los botones
        // asociados a la conexion
        if(bAdapter.isEnabled())
        {
            btnBluetooth.setText(R.string.DesactivarBluetooth);
            btnBuscarDispositivo.setEnabled(true);

            //Creamos el servivio por primera vez
            if(bService != null)
            {
                bService.finalizarHiloOBD();
            }
            else
                bService = new BluetoothService(this, bHandler, bAdapter);
        }
        else
        {
            btnBluetooth.setText(R.string.ActivarBluetooth);
        }
    }


    //-- Funciones para el resto de funcionalidades no Bluetooth ---------------------
    //--------------------------------------------------------------------------------

    private ServiceConnection mServiceConnection = new ServiceConnection()
    {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mEnviarServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            EnviarService.MyBinder myBinder = (EnviarService.MyBinder) service;
            mEnviarService = myBinder.getService();
            mEnviarServiceBound = true;
        }
    };

    public void escribirConsola(String t_add)
    {
        String t_now = (String)tvConsola.getText();
        tvConsola.setText(t_now + "\n"+ t_add);
        scrConsola.smoothScrollTo(0, tvConsola.getBottom());
    }

    //--------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------


    // Handler para manejar los eventos onClick de los botones.
    @Override
    public void onClick(View v) {
        switch(v.getId())
        {
            // Codigo ejecutado al pulsar los botones empezar leer/parar.
            case R.id.btnRecibir:
            {
                leerService.gestionMonitorizacion();
                break;
            }

            // Codigo ejecutado al pulsar el Button que se va a encargar de activar y
            // desactivar el Bluetooth.
            case R.id.btnBluetooth:
            {
                if(bAdapter.isEnabled())
                {
                    if(bService != null)
                        bService.finalizarHiloOBD();

                    bAdapter.disable();
                }
                else
                {
                    // Lanzamos el Intent que mostrara la interfaz de activacion del
                    // Bluetooth. La respuesta de este Intent se manejara en el metodo
                    // onActivityResult
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
                break;
            }

            // Codigo ejecutado al pulsar el Button que se va a encargar de descubrir nuevos
            // dispositivos
            case R.id.btnBuscarDispositivo:
            {
                arrayDevices.clear();

                // Comprobamos si existe un descubrimiento en curso. En caso afirmativo, se
                // cancela.
                if(bAdapter.isDiscovering())
                    bAdapter.cancelDiscovery();

                // Iniciamos la busqueda de dispositivos
                if(bAdapter.startDiscovery())
                    // Mostramos el mensaje de que el proceso ha comenzado
                    Toast.makeText(this, R.string.IniciandoDescubrimiento, Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, R.string.ErrorIniciandoDescubrimiento, Toast.LENGTH_SHORT).show();
                break;
            }

            // Codigo ejecutado 3g o no
            case R.id.cb_3g:
            {
                if(cb_3g.isChecked()){
                    enviarCon3g = true;
                }else{
                    enviarCon3g = false;
                }
                mEnviarService.gestion3g(enviarCon3g);
                break;
            }

            default:
                break;
        }
    }

    /**
     * Handler del evento desencadenado al retornar de una actividad. En este caso, se utiliza
     * para comprobar el valor de retorno al lanzar la actividad que activara el Bluetooth.
     * En caso de que el usuario acepte, resultCode sera RESULT_OK
     * En caso de que el usuario no acepte, resultCode valdra RESULT_CANCELED
     */
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        switch(requestCode)
        {
            case REQUEST_ENABLE_BT:
            {
                Log.v(TAG, "onActivityResult: REQUEST_ENABLE_BT");
                if(resultCode == RESULT_OK)
                {
                    btnBluetooth.setText(R.string.DesactivarBluetooth);
                    if(bService != null)
                    {
                        bService.finalizarHiloOBD();
                    }
                    else
                        bService = new BluetoothService( this, bHandler, bAdapter);
                }
                break;
            }
            default:
                break;
        }
    }

    // Ademas de realizar la destruccion de la actividad,
    // eliminamos el registro del BroadcastReceiver.
    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(bReceiver);
        if(bService != null)
            bService.finalizarHiloOBD();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(bService != null)
        {
            if(bService.getEstado() == BluetoothService.ESTADO_NINGUNO)
            {
                bService.finalizarHiloOBD();
            }
        }
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    public void debug(String metodo, String msg)
    {
        if(DEBUG_MODE)
            Log.d(TAG, metodo + ": " + msg);
    }

}
