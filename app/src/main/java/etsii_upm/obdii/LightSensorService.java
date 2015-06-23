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



import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class LightSensorService {

    private Context context;
    private SensorManager mySensorManager;
    private Sensor lightSensor;
    private SensorEventListener lightListener;
    private String luminosidad;
    private boolean sensorActivado;

    public LightSensorService(Context context)
    {

        this.context =context;

        mySensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        lightSensor = mySensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        if(lightSensor != null){
            sensorActivado = true;
            debug("LightSensorService()", "Sensor.TYPE_LIGHT Available");
            lightListener = new MiLightSensorListener();
            mySensorManager.registerListener(
                    lightListener,
                    lightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }else{
            sensorActivado = false;
            debug("LightSensorService()", "Sensor.TYPE_LIGHT NOT Available");
        }
    }

    public class MiLightSensorListener implements SensorEventListener {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if(event.sensor.getType() == Sensor.TYPE_LIGHT){
                luminosidad = "'lu': " + event.values[0] ;
            }
        }

    }

    public String getLuminosidad()
    {
        if(sensorActivado){
            return luminosidad;
        }else{
            return "'lu': 'noD' ";
        }

    }

    public void stopLightSensorService()
    {

        if(mySensorManager !=null) {
            try {
                mySensorManager.unregisterListener(lightListener, lightSensor);
                mySensorManager = null;
                debug("LightSensorService()", "Finalizado correctamente");
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



