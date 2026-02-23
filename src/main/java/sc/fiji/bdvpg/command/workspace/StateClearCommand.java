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

package sc.fiji.bdvpg.command.workspace;

import bdv.viewer.SourceAndConverter;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.services.SourceService;

import java.util.List;

/**
 * Command which clear all sources currently contained in the SourceAndConverter
 * service
 */
@SuppressWarnings({ "unused", "CanBeFinal" })
@Plugin(type = BdvPlaygroundActionCommand.class,
		menu = {
				@Menu(label = ScijavaBdvDefaults.RootMenuL1),
				@Menu(label = ScijavaBdvDefaults.RootMenuL2),
				@Menu(label = ScijavaBdvDefaults.WorkspaceMenu, weight = ScijavaBdvDefaults.WorkspaceW),
				@Menu(label = "State - Clear", weight = -8)
		},
	//menuPath = ScijavaBdvDefaults.RootMenu + "Workspace>State - Clear State",
	description = "Removes all sources from the SourceAndConverter service")
public class StateClearCommand implements
	BdvPlaygroundActionCommand
{

	@Parameter
    SourceService source_service;

	@Override
	public void run() {
		List<SourceAndConverter<?>> sources = source_service.getSources();
		source_service.remove(sources.toArray(new SourceAndConverter[0]));
	}
}
