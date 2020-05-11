package org.scijava.ui.behaviour.io;

import org.scijava.ui.behaviour.InputTriggerMap;

/**
 * Helper class that can access actionToInputsMap
 * because it is located in the right package {@link org.scijava.ui.behaviour.io}
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
