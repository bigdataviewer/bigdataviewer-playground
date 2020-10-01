package org.scijava.ui.behaviour.io;

import org.scijava.ui.behaviour.InputTriggerMap;

/**
 * Helper class that can access {@link InputTriggerConfig#actionToInputsMap}
 * because it is located in the right package {@link org.scijava.ui.behaviour.io}
 *
 * This is useful to retrieve all actions of an {@link InputTriggerConfig},
 * but TBH I'm still confused about this InputTriggerConfig class...
 *
 * @author Nicolas Chiaruttini, EPFL, BIOP, 2020
 */

public class InputTriggerConfigHelper {

    /**
     * @param config
     * @return {@link InputTriggerMap} associated to this input
     */
    public static InputTriggerMap getInputTriggerMap(InputTriggerConfig config) {
        InputTriggerMap inputMap = new InputTriggerMap();

        config.actionToInputsMap.forEach((key, inputs) -> {

            inputs.forEach(input -> {
                inputMap.put(input.trigger, key);
            });

        });

        return inputMap;
    }
}
