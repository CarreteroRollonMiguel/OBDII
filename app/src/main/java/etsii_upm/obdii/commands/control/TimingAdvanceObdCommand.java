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

package etsii_upm.obdii.commands.control;

import etsii_upm.obdii.commands.PercentageObdCommand;
import etsii_upm.obdii.enums.AvailableCommandNames;

/**
 * Timing Advance
 */
public class TimingAdvanceObdCommand extends PercentageObdCommand {

    public TimingAdvanceObdCommand() {
        super("01 0E");
    }

    /**
     * @param other a {@link etsii_upm.obdii.commands.control.TimingAdvanceObdCommand} object.
     */
    public TimingAdvanceObdCommand(TimingAdvanceObdCommand other) {
        super(other);
    }

    @Override
    public String getName() {
        return AvailableCommandNames.TIMING_ADVANCE.getValue();
    }

}
