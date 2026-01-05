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

package bdv.util;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.transformation.ManualTransformationEditor;
import bdv.ui.appearance.AppearanceManager;
import bdv.ui.keymap.KeymapManager;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Wraps a {@link bdv.BigDataViewer} instance into a {@link BdvHandle} This
 * class NEEDS to be in bdv.util or else it cannot implement the createViewer
 * method Class used to wrap {@link bigwarp.BigWarp} BigDataViewer instances.
 */

public class ViewerPanelHandle extends BdvHandle {

	protected static final Logger logger = LoggerFactory.getLogger(
		ViewerPanelHandle.class);

	public final String name;

	public ViewerPanelHandle(ViewerPanel viewerPanel, String name) {
		super(BdvOptions.options());
		this.viewer = viewerPanel;
		this.name = name;
	}

	@Override
	public KeymapManager getKeymapManager() {
		logger.error(
			"Unsupported getKeymapManager call in ViewerPanel wrapped BdvHandle");
		return null;
	}

	@Override
	public AppearanceManager getAppearanceManager() {
		logger.error(
			"Unsupported getAppearanceManager call in ViewerPanel wrapped BdvHandle");
		return null;
	}

	@Override
	public void close() {
		// TODO : implement this ?
	}

	@Override
	public ManualTransformationEditor getManualTransformEditor() {
		logger.error(
			"Unsupported getManualTransformEditor call in ViewerPanel wrapped BdvHandle");
		return null;
	}

	@Override
	public InputActionBindings getKeybindings() {
		logger.error(
			"Unsupported getKeybindings call in ViewerPanel wrapped BdvHandle");
		return null;
	}

	@Override
	public TriggerBehaviourBindings getTriggerbindings() {
		logger.error(
			"Unsupported getTriggerbindings call in ViewerPanel wrapped BdvHandle");
		return null;
	}

	boolean createViewer(List<? extends ConverterSetup> converterSetups,
		List<? extends SourceAndConverter<?>> sources, int numTimepoints)
	{
		logger.error(
			"Cannot add sources in ViewerPanel wrapped BdvHandle");
		return false;
	}

	@Override
	public String toString() {
		return name;
	}

}
