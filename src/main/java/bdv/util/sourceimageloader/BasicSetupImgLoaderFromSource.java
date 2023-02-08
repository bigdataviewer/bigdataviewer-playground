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

package bdv.util.sourceimageloader;

import bdv.viewer.Source;
import mpicbg.spim.data.generic.sequence.BasicSetupImgLoader;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.Type;

/**
 * SetupLoader from a source coming from an {@link ImgLoaderFromSources} This is
 * convenient in order to save some sources which are not originating from an
 * existing dataset. See its use in
 * {@link sc.fiji.bdvpg.sourceandconverter.exporter.XmlHDF5SpimdataExporter}
 * Associated SetupLoader {@link BasicSetupImgLoaderFromSource}
 *
 * @param <T> : Type of the pixel used, should be
 *          {@link net.imglib2.type.numeric.integer.UnsignedShortType} for xml /
 *          hdf5 export
 */

public class BasicSetupImgLoaderFromSource<T extends Type<T>> implements
	BasicSetupImgLoader<T>
{

	final Source<T> src;

	public BasicSetupImgLoaderFromSource(Source<T> src) {
		this.src = src;
	}

	@Override
	public RandomAccessibleInterval<T> getImage(int timepointId,
		ImgLoaderHint... hints)
	{
		return src.getSource(timepointId, 0);
	}

	@Override
	public T getImageType() {
		return src.getType();
	}
}
