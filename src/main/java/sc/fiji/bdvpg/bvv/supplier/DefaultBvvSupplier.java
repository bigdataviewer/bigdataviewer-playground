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

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.Interpolation;
import bvv.vistools.BvvFunctions;
import bvv.vistools.BvvHandle;
import bvv.vistools.BvvOptions;
import bvv.vistools.BvvStackSource;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.ByteType;
import sc.fiji.bdvpg.viewer.navigate.TimepointAdapterAdder;

public class DefaultBvvSupplier implements IBvvSupplier {

	public final SerializableBvvOptions sOptions;

	public DefaultBvvSupplier(SerializableBvvOptions sOptions) {
		this.sOptions = sOptions;
	}

	@Override
	public BvvHandle get() {
		BvvOptions options = sOptions.getBvvOptions();

		// create dummy image to instantiate the BDV
		ArrayImg<ByteType, ByteArray> dummyImg = ArrayImgs.bytes(2, 2, 2);
		options = options.sourceTransform(new AffineTransform3D());
		BvvStackSource<ByteType> bss = BvvFunctions.show(dummyImg, "dummy",
			options);
		BvvHandle bvv = bss.getBvvHandle();

		// remove dummy image
		bvv.getViewerPanel().state().removeSource(bvv.getViewerPanel().state()
			.getCurrentSource());
		bvv.getViewerPanel().setNumTimepoints(sOptions.numTimePoints);

		// Should be helpful

		new TimepointAdapterAdder(bvv.getViewerPanel()).run();
		//new SourceNavigatorSliderAdder(bdv).run();

		return bvv;
	}

}
