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

package sc.fiji.bdvpg.behaviour;

import bdv.util.BdvHandle;
import ch.epfl.biop.bdv.select.SourceSelectorBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

/**
 * Removes an editor behaviour installed See {@link EditorBehaviourInstaller}
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL 2020
 */

public class EditorBehaviourUnInstaller implements Runnable {

	protected static final Logger logger = LoggerFactory.getLogger(
		EditorBehaviourUnInstaller.class);

	final BdvHandle bdvh;

	public EditorBehaviourUnInstaller(BdvHandle bdvh) {
		this.bdvh = bdvh;
	}

	@Override
	public void run() {

		SourceSelectorBehaviour ssb =
			(SourceSelectorBehaviour) SourceAndConverterServices
				.getBdvDisplayService().getDisplayMetadata(bdvh,
					SourceSelectorBehaviour.class.getSimpleName());

		EditorBehaviourInstaller ebi =
			(EditorBehaviourInstaller) SourceAndConverterServices
				.getBdvDisplayService().getDisplayMetadata(bdvh,
					EditorBehaviourInstaller.class.getSimpleName());

		if ((ssb == null) || (ebi == null)) {
			logger.error(
				"SourceSelectorBehaviour or EditorBehaviourInstaller cannot be retrieved. Cannot uninstall EditorBehaviour");
			return;
		}

		ebi.getToggleListener().isDisabled();
		ssb.removeToggleListener(ebi.getToggleListener());

		// Cleans the MetaData hashMap
		SourceAndConverterServices.getBdvDisplayService().setDisplayMetadata(bdvh,
			EditorBehaviourInstaller.class.getSimpleName(), null);

	}

}
