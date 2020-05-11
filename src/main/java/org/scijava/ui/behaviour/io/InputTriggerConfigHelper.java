package org.scijava.ui.behaviour.io;

import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.InputTriggerMap;

public class InputTriggerConfigHelper {

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
