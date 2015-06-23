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

package etsii_upm.obdii.commands.engine;

import etsii_upm.obdii.commands.PercentageObdCommand;
import etsii_upm.obdii.enums.AvailableCommandNames;

/**
 * Read the throttle position in percentage.
 */
public class ThrottlePositionObdCommand extends PercentageObdCommand {

       private float value = 0f;
    /**
     * Default ctor.
     */
    public ThrottlePositionObdCommand() {
        super("01 11");
    }

    /**
     * Copy ctor.
     *
     * @param other a {@link etsii_upm.obdii.commands.engine.ThrottlePositionObdCommand} object.
     */
    public ThrottlePositionObdCommand(ThrottlePositionObdCommand other) {
        super(other);
    }


    @Override
    protected void performCalculations() {
        // ignore first two bytes [hh hh] of the response
        value = (buffer.get(2) * 100.0f) / 255.0f;
    }

    @Override
    public String getFormattedResult() {
        return String.format("%.2f%s", value, "%");
    }

    /**
     * @return a float.  Migeul  to overide the generic getPercentage
     */
    public float getPercentage() {
        return value;
    }
    /**
     *
     */
    @Override
    public String getName() {
        return AvailableCommandNames.THROTTLE_POS.getValue();
    }

}
