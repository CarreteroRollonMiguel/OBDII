/**
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

package etsii_upm.obdii.commands.pressure;

import etsii_upm.obdii.enums.AvailableCommandNames;

/**
 * Barometric pressure.
 */
public class BarometricPressureObdCommand extends PressureObdCommand {

    private int value = -1;

    public BarometricPressureObdCommand() {
        super("01 33");
    }

    /**
     * @param other a {@link etsii_upm.obdii.commands.pressure.PressureObdCommand} object.
     */
    public BarometricPressureObdCommand(PressureObdCommand other) {
        super(other);
    }


    @Override
    protected void performCalculations() {
        // ignore first two bytes [hh hh] of the response
        value = buffer.get(2);
    }

    @Override
    public String getFormattedResult() {
        return String.format("%02d%s", value, "kPa");
    }

    /**
     * @return the pressure in kPa
     */
    @Override
    public int  getMetricUnit() {
        return (value);
    }

    @Override
    public String getName() {
        return AvailableCommandNames.BAROMETRIC_PRESSURE.getValue();
    }

}
