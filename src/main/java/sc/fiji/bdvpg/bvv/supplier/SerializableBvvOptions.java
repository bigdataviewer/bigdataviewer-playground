/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2023 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.bdvpg.bvv.supplier;

import bdv.util.AxisOrder;
import bdv.util.BdvOptions;
import bdv.viewer.render.AccumulateProjectorARGB;
import bdv.viewer.render.AccumulateProjectorFactory;
import bvv.vistools.BvvOptions;
import net.imglib2.type.numeric.ARGBType;

/**
 * Because BdvOptions is not directly serializable Not serialized:
 * TransformEventHandlerFactory transformEventHandlerFactory InputTriggerConfig
 * inputTriggerConfig AffineTransform3D sourceTransform
 */
@SuppressWarnings("CanBeFinal")
public class SerializableBvvOptions {

	public int width = -1;

	public int height = -1;

	public int numSourceGroups = 10;

	public String frameTitle = "BigVolumeViewer";

	public AxisOrder axisOrder = AxisOrder.DEFAULT;

	public int numTimePoints = 1;

	public SerializableBvvOptions() {}

	public BvvOptions getBvvOptions() {
		BvvOptions o = BvvOptions.options()
			//.screenScales(this.screenScales)
			//.targetRenderNanos(this.targetRenderNanos)
			//.numRenderingThreads(this.numRenderingThreads)
			.numSourceGroups(this.numSourceGroups)
			.axisOrder(this.axisOrder)
			.preferredSize(this.width, this.height)
				// add other stuff
			.frameTitle(this.frameTitle);

		return o;
	}

}
