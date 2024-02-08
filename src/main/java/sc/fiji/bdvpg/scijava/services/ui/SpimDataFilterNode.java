/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2024 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.bdvpg.scijava.services.ui;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

import javax.swing.tree.DefaultTreeModel;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_INFO;

/**
 * SourceAndConverter filter node : Selects SpimData and allow for duplicate
 */
public class SpimDataFilterNode extends SourceFilterNode {

	final AbstractSpimData<?> asd;
	final SourceAndConverterService sourceAndConverterService;

	public boolean filter(SourceAndConverter<?> sac) {
		return (sourceAndConverterService.containsMetadata(sac, SPIM_DATA_INFO)) &&
			((SourceAndConverterService.SpimDataInfo) sourceAndConverterService
				.getMetadata(sac, SPIM_DATA_INFO)).asd.equals(asd);
	}

	public SpimDataFilterNode(DefaultTreeModel model, String defaultName,
		AbstractSpimData<?> spimdata,
		SourceAndConverterService sourceAndConverterService)
	{
		super(model, defaultName, null, false);
		this.sourceAndConverterService = sourceAndConverterService;
		this.filter = this::filter;
		asd = spimdata;
	}

	String getName(AbstractSpimData<?> spimdata, String defaultName) {
		return defaultName;
	}

	public String toString() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Object clone() {
		return new SpimDataFilterNode(model, name, asd, sourceAndConverterService);
	}
}
