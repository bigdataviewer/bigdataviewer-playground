/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2026 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

import javax.swing.tree.DefaultTreeModel;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_INFO;

/**
 * SourceAndConverter filter node : Selected a SourceAndConverter which is
 * linked to a particular Entity
 */
public class SpimDataElementFilter extends SourceFilterNode {

	final Entity e;
	final SourceAndConverterService sourceAndConverterService;

	public SpimDataElementFilter(DefaultTreeModel model, String name, Entity e,
		SourceAndConverterService sourceAndConverterService)
	{
		super(model, name, null, false);
		this.filter = this::filter;
		this.e = e;
		this.sourceAndConverterService = sourceAndConverterService;
	}

	public boolean filter(SourceAndConverter<?> sac) {
		if (sourceAndConverterService.containsMetadata(sac, SPIM_DATA_INFO)) {
			AbstractSpimData<AbstractSequenceDescription<BasicViewSetup, ?, ?>> asd =
				(AbstractSpimData<AbstractSequenceDescription<BasicViewSetup, ?, ?>>) ((SourceAndConverterService.SpimDataInfo) sourceAndConverterService
					.getMetadata(sac, SPIM_DATA_INFO)).asd;
			Integer idx =
				((SourceAndConverterService.SpimDataInfo) sourceAndConverterService
					.getMetadata(sac, SPIM_DATA_INFO)).setupId;
			return asd.getSequenceDescription().getViewSetups().get(idx)
				.getAttributes().values().contains(e);
		}
		else {
			return false;
		}
	}

	@Override
	public Object clone() {
		return new SpimDataElementFilter(model, name, e, sourceAndConverterService);
	}

}
