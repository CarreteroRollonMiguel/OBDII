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



import android.content.Intent;
import android.os.Bundle;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class GPSService {

    private Context context;
    private LocationManager locManager;
    private LocationListener locListener;
    private Location location;
    private boolean gpsActivado;
    public String posicionGPS;
    private MainActivity main_w;

    public GPSService(Context context,MainActivity main_w, int gpsPeriodo)
    {
        this.main_w = main_w;
        this.context = main_w.getApplicationContext();

        //Obtenemos una referencia al LocationManager
        locManager = (LocationManager)context.getSystemService(context.LOCATION_SERVICE);
        List<String> listaProviders = locManager.getAllProviders();

        LocationProvider provider = locManager.getProvider(listaProviders.get(0));
        int precision = provider.getAccuracy();
        boolean tieneAltitud = provider.supportsAltitude();

        //Si el GPS no está habilitado
        if (!locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gpsActivado = false;
            debug("GPSService()", "GPS deshabilitado");
        }else {
            gpsActivado = true;
            debug("GPSService()", "GPS activado: sensor altitud: " + String.valueOf(tieneAltitud));
            //Obtenemos la última posición conocida
            location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            posicionGPS = setPosicion(location);
        }

        //Nos registramos para recibir actualizaciones de la posición
        locListener = new MiLocationListener();

        // Actualiza posicion GPS con un periodo de gpsPeriodo segundos
        try {
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, gpsPeriodo, 0, locListener, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e("GPSService()", e.getMessage(), e);
            e.printStackTrace();
        }
    }

    public class MiLocationListener implements LocationListener {

        public void onLocationChanged(Location location)
        {
            //mostrarPosicion(location);
            posicionGPS = setPosicion(location);
        }

        public void onProviderDisabled(String provider)
        {
            // Se ha cortado el GPS mientras se hacia la lectura
            gpsActivado = false;
            debug("MiLocationListener()", "Provider OFF");
            // Pedimos que active el GPS del dispositivo
            main_w.crearDialogoGPS();

        }

        public void onProviderEnabled(String provider) {
            gpsActivado = true;
            debug("GPSService()", "Gps Activo. Provider " + provider);
            location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            posicionGPS = setPosicion(location);
            debug("MiLocationListener()", "Provider ON");
        }

        public void onStatusChanged(String provider, int status, Bundle extras){
            debug("MiLocationListener()", "Provider Status: " + status);
        }
    }

    public String setPosicion(Location location)
    {

        if (location != null) {
            debug("GPSService()",
                    "Latitud: " + String.valueOf(location.getLatitude()) +
                    "Longitud: " + String.valueOf(location.getLongitude()) +
                    "Precision: " + String.valueOf(location.getAccuracy()));

            return ( "'lat': " + String.valueOf(location.getLatitude()) +
                     ",'lng': "+ String.valueOf(location.getLongitude()) );

        } else {
            debug("GPSService()", "No hay cobertura GPS)");
            return "'lat': 'noC','lng': 'noC'";
        }

    }

    public String getPosicion()
    {
        if(gpsActivado){
            return posicionGPS;
        }else{
            // No hay GPS activo
            return "'lat': 'noD','lng': 'noD'";
        }
    }

    public void stopGPSService()
    {

        if(locManager!=null) {
            try {
                locManager.removeUpdates(locListener);
                locManager = null;
                debug("GPSService()", "Finalizado correctamente");
            } catch (SecurityException e) {
                Log.e("stopGPSService()", e.getMessage(), e);
                e.printStackTrace();
            }
        }

    }

    public void debug(String metodo, String msg)
    {
        if(MainActivity.DEBUG_MODE)
            Log.d(MainActivity.TAG, metodo + ": " + msg);
    }

}
