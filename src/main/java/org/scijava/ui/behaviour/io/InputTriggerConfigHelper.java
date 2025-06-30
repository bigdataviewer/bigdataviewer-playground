/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2025 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package org.scijava.ui.behaviour.io;

import org.scijava.ui.behaviour.InputTriggerMap;

/**
 * Helper class that can access {@link InputTriggerConfig#actionToInputsMap}
 * because it is located in the right package
 * {@link org.scijava.ui.behaviour.io} This is useful to retrieve all actions of
 * an {@link InputTriggerConfig}, but TBH I'm still confused about this
 * InputTriggerConfig class...
 *
 * @author Nicolas Chiaruttini, EPFL, BIOP, 2020
 */

public class InputTriggerConfigHelper {

	/**
	 * @param config the input trigger config obviously
	 * @return {@link InputTriggerMap} associated to this input
	 */
	public static InputTriggerMap getInputTriggerMap(InputTriggerConfig config) {
		InputTriggerMap inputMap = new InputTriggerMap();

		config.actionToInputsMap.forEach((key, inputs) -> inputs.forEach(
			input -> inputMap.put(input.trigger, key)));

		return inputMap;
	}
}
