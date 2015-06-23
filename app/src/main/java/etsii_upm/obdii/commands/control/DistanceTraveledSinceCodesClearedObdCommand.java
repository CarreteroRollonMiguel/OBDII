package etsii_upm.obdii.commands.control;



import etsii_upm.obdii.commands.ObdCommand;
import etsii_upm.obdii.commands.SystemOfUnits;
import etsii_upm.obdii.enums.AvailableCommandNames;

/**
 * Distance traveled since codes cleared-up.
 */
public class DistanceTraveledSinceCodesClearedObdCommand extends ObdCommand
        implements SystemOfUnits {

    private int km = 0;

    /**
     * Default ctor.
     */
    public DistanceTraveledSinceCodesClearedObdCommand() {
        super("01 31");
    }

    /**
     * Copy ctor.
     *
     * @param other a {@link etsii_upm.obdii.commands.control.DistanceTraveledSinceCodesClearedObdCommand} object.
     */
    public DistanceTraveledSinceCodesClearedObdCommand(
            DistanceTraveledSinceCodesClearedObdCommand other) {
        super(other);
    }

    @Override
    protected void performCalculations() {
        // ignore first two bytes [01 31] of the response
        km = buffer.get(2) * 256 + buffer.get(3);
    }

    @Override
    public String getFormattedResult() {
        return String.format("%.2f%s", (float)km, "km");
    }

    @Override
    public float getImperialUnit() {
        return new Double(km * 0.621371192).floatValue();
    }

    /**
     * @return a int.
     */
    public int getKm() {
        return km;
    }

    /**
     * <p>Setter for the field <code>km</code>.</p>
     *
     * @param km a int.
     */
    public void setKm(int km) {
        this.km = km;
    }

    @Override
    public String getName() {
        return AvailableCommandNames.DISTANCE_TRAVELED_AFTER_CODES_CLEARED
                .getValue();
    }

}
