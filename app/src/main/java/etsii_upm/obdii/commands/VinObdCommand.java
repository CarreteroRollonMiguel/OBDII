package etsii_upm.obdii.commands;

/**
 * Created by Miguel on 11/6/15.
 */

import etsii_upm.obdii.enums.AvailableCommandNames;
import android.util.Log;


/**
 * Vehicle Identificaction Number.
 */
public class VinObdCommand extends ObdCommand implements SystemOfUnits {

    private String vin;

    private String convertHexToString(String hex){

        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();

        //49204c6f7665204a617661 split into two characters 49, 20, 4c...
        for( int i=0; i<hex.length()-1; i+=2 ){

            //grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            //convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            //convert the decimal to character
            sb.append((char)decimal);

            temp.append(decimal);
        }

        return sb.toString();
    }


    /**
     * Default ctor.
     */
    public VinObdCommand() {
        super("09 02");
    }

    /**
     * Copy ctor.
     *
     * @param other a {@link etsii_upm.obdii.commands.VinObdCommand} object.
     */
    public VinObdCommand(VinObdCommand other) {
        super(other);
    }


    @Override
    protected void performCalculations() {
        // Ignore first 10 bytes [hh .. hh] of the response.
        int begin = 10;
        int end = 44;

        //vin = rawData.substring(begin, end);
        //vin = rawData.substring(vin.lastIndexOf(13) + 1);
        vin = convertHexToString(rawData.substring(begin, end));
        Log.i("VIN Perform Calculations: ", vin);
    }

    /**
     * @return a {@link java.lang.String} object.
     */
    public String getFormattedResult() {
        return(vin);
    }

    @Override
    public String getName() {
        return(vin);
    }

    /**
     *
     * @return a float. It is name length.
     */
    public float getImperialUnit() {
        return (float) vin.length();
    };


    public String getVehicleName() {
        Log.i("Vehicle Name: ", vin);
        return (vin);
    }

}
