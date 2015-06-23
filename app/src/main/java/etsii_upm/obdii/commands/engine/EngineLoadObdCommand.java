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
 * Calculated Engine Load value.
 */
public class EngineLoadObdCommand extends PercentageObdCommand {

    private float value = 0f;

    public EngineLoadObdCommand() {
        super("01 04");
    }

    /**
     * <p>Constructor for EngineLoadObdCommand.</p>
     *
     * @param other a {@link etsii_upm.obdii.commands.engine.EngineLoadObdCommand} object.
     */
    public EngineLoadObdCommand(EngineLoadObdCommand other) {
        super(other);
    }


    @Override
    protected void performCalculations() {
        // ignore first two bytes [01 0C] of the response
        value = (buffer.get(2) * 100.0f)/255.0f;
    }

    @Override
    public String getFormattedResult() {
        // determine time
        final String el = String.format("%.2d%s", value, "%");
        return String.format("%s", el);
    }

    /*
      * (non-Javadoc)
      *
      * @see etsii_upm.odbii.commands.ObdCommand#getName()
      */
    @Override
    public String getName() {
        return AvailableCommandNames.ENGINE_LOAD.getValue();
    }

    /**
     * @return a float.
     */
    public float getPercentage() {
        return value;
    }
}
