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
package sc.fiji.bdvpg.bdv.overlay;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvOverlaySource;
import bdv.viewer.ViewerStateChangeListener;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import javax.swing.SwingUtilities;
import java.awt.Font;

/**
 * Adds a {@link SourceNameOverlay} for BigDataViewer windows.
 *
 * @author Nicolas Chiaruttini, EPFL, 2022
 */
public class SourceNameOverlayAdder implements Runnable {

    final BdvHandle bdvh;

    public SourceNameOverlayAdder(BdvHandle bdvh, Font font) {
        this.bdvh = bdvh;
        this.font = font;
    }

    SourceNameOverlay nameOverlay;

    BdvOverlaySource bos;

    final Font font;

    final ViewerStateChangeListener changeListener = (viewerStateChange) -> updatePositions();

    @Override
    public void run() {
        nameOverlay = new SourceNameOverlay(bdvh.getViewerPanel(),font,SourceAndConverterHelper::sortDefault);
        addToBdv();
    }

    void updatePositions() {
        nameOverlay.update();
    }

    public BdvHandle getBdvh() {
        return bdvh;
    }

    public void removeFromBdv() {
        SwingUtilities.invokeLater(() -> {
            bdvh.getViewerPanel().state().changeListeners().remove(changeListener);
            bos.removeFromBdv();
            bdvh.getViewerPanel().revalidate();
        });
    }

    public void addToBdv() {
        SwingUtilities.invokeLater(() -> {
            int nTimepointIni = bdvh.getViewerPanel().state().getNumTimepoints();
            int iTimePoint = bdvh.getViewerPanel().state().getCurrentTimepoint();
            bos = BdvFunctions.showOverlay(nameOverlay, "Sources names", BdvOptions.options().addTo(bdvh));
            bdvh.getViewerPanel().state().changeListeners().add(changeListener);
            // Bug when an overlay is displayed
            bdvh.getViewerPanel().state().setNumTimepoints(nTimepointIni);
            bdvh.getViewerPanel().state().setCurrentTimepoint(iTimePoint);
        });
    }
}
