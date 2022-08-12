/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package sc.fiji.bdvpg.bdv.supplier;

import bdv.util.AxisOrder;
import bdv.util.BdvOptions;
import bdv.viewer.render.AccumulateProjectorARGB;
import bdv.viewer.render.AccumulateProjectorFactory;
import net.imglib2.type.numeric.ARGBType;

/**
 * Because BdvOptions is not directly serializable
 *
 * Not serialized:
 * TransformEventHandlerFactory transformEventHandlerFactory
 * InputTriggerConfig inputTriggerConfig
 * AffineTransform3D sourceTransform
 */
@SuppressWarnings("CanBeFinal")
public class SerializableBdvOptions {

    public int width = -1;

    public int height = -1;

    public double[] screenScales = new double[] { 1, 0.75, 0.5, 0.25, 0.125 };

    public long targetRenderNanos = 30 * 1000000L;

    public int numRenderingThreads = 3;

    public int numSourceGroups = 10;

    public String frameTitle = "BigDataViewer";

    public boolean is2D = false;

    public AxisOrder axisOrder = AxisOrder.DEFAULT;

    // Extra for the playground
    public boolean interpolate = false;

    public int numTimePoints = 1;

    public AccumulateProjectorFactory<ARGBType> accumulateProjectorFactory = AccumulateProjectorARGB.factory;

    public SerializableBdvOptions() {
    }

    public BdvOptions getBdvOptions() {
        BdvOptions o =
                BdvOptions.options()
                .screenScales(this.screenScales)
                .targetRenderNanos(this.targetRenderNanos)
                .numRenderingThreads(this.numRenderingThreads)
                .numSourceGroups(this.numSourceGroups)
                .axisOrder(this.axisOrder)
                .preferredSize(this.width, this.height)
                .frameTitle(this.frameTitle);
        if (this.accumulateProjectorFactory!=null) {
            o = o.accumulateProjectorFactory(this.accumulateProjectorFactory);
        }
        if (this.is2D) o = o.is2D();

        return o;
    }

}
