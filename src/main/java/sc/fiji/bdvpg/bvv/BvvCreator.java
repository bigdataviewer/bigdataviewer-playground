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

package sc.fiji.bdvpg.bvv;


import bvv.vistools.BvvFunctions;
import bvv.vistools.BvvHandle;
import bvv.vistools.BvvOptions;
import bvv.vistools.BvvStackSource;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import sc.fiji.bdvpg.bdv.navigate.TimepointAdapterAdder;
import sc.fiji.bdvpg.viewers.ViewerAdapter;

import java.util.function.Supplier;

/**
 * BigDataViewer Playground Action -- Creates a BigVolumeViewer - should be
 * replaced by Sciview
 */

public class BvvCreator implements Runnable, Supplier<BvvHandle> {

	private BvvOptions bvvOptions;
	private final int numTimePoints;

	public BvvCreator() {
		this.bvvOptions = BvvOptions.options();
		this.numTimePoints = 1;
	}

	public BvvCreator(BvvOptions bvvOptions) {
		this.bvvOptions = bvvOptions;
		this.numTimePoints = 1;
	}

	public BvvCreator(BvvOptions bvvOptions, int numTimePoints) {
		this.bvvOptions = bvvOptions;
		this.numTimePoints = numTimePoints;
	}

	public void run() {
		// Do nothing -> see get() method
	}

	/**
	 * Hack: add an image and remove it after the bvvHandle has been created. -
	 * not done for bvv
	 */
	public BvvHandle get() {
		// Random random = new Random();
		Img<UnsignedShortType> dummyImg = ArrayImgs.unsignedShorts(2, 2, 2);
		dummyImg.forEach(t -> t.set(0));

		bvvOptions = bvvOptions.sourceTransform(new AffineTransform3D());

		BvvStackSource<?> bss = BvvFunctions.show(dummyImg, "dummy", bvvOptions);

		BvvHandle bvv = bss.getBvvHandle();

		bvv.getViewerPanel().setNumTimepoints(numTimePoints);

		new TimepointAdapterAdder(new ViewerAdapter(bvv)).run();

		return bvv;
	}

}
