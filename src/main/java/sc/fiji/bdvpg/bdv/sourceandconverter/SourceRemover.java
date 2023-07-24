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

package sc.fiji.bdvpg.bdv.sourceandconverter;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.function.Consumer;

/**
 * BigDataViewer Playground Action -- Removes a {@link SourceAndConverter} from
 * a {@link BdvHandle} Note : - the functional interface allows to use this
 * action in a functional way, in this case, the constructor without
 * SourceAndConverter can be used TODO : think if this action is useful ? It
 * looks unused because the direct call to
 * SourceAndConverterServices.getSourceAndConverterDisplayService().remove is
 * more convenient
 */

public class SourceRemover implements Runnable,
	Consumer<SourceAndConverter<?>[]>
{

	final SourceAndConverter<?> sacIn;
	final BdvHandle bdvh;

	public SourceRemover(BdvHandle bdvh, SourceAndConverter<?> sacIn) {
		this.sacIn = sacIn;
		this.bdvh = bdvh;
	}

	public SourceRemover(SourceAndConverter<?> sacIn) {
		this.sacIn = sacIn;
		this.bdvh = null;
	}

	public SourceRemover() {
		this.sacIn = null;
		this.bdvh = null;
	}

	public void run() {
		accept(sacIn);
	}

	@Override
	public void accept(SourceAndConverter... sacs) {
		if (bdvh == null) {
			// Remove from all displays
			SourceAndConverterServices.getBDVService().removeFromAllViewers(sacs);
		}
		else {
			// Remove from a specific bdvHandle
			SourceAndConverterServices.getBDVService().remove(bdvh, sacs);
		}
	}
}
