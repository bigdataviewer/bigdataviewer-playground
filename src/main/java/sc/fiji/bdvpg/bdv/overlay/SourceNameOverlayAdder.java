package sc.fiji.bdvpg.bdv.overlay;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvOverlaySource;
import bdv.viewer.ViewerStateChangeListener;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

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

        int nTimepointIni = bdvh.getViewerPanel().state().getNumTimepoints();
        int iTimePoint = bdvh.getViewerPanel().state().getCurrentTimepoint();
        bos = BdvFunctions.showOverlay(nameOverlay, "Sources names", BdvOptions.options().addTo(bdvh));
        bdvh.getViewerPanel().state().changeListeners().add(changeListener);
        // Bug when an overlay is displayed
        bdvh.getViewerPanel().state().setNumTimepoints(nTimepointIni);
        bdvh.getViewerPanel().state().setCurrentTimepoint(iTimePoint);
    }

    void updatePositions() {
        nameOverlay.update();
    }

    public BdvHandle getBdvh() {
        return bdvh;
    }

    public void removeFromBdv() {
        bdvh.getViewerPanel().state().changeListeners().remove(changeListener);
        bos.removeFromBdv();
        bdvh.getViewerPanel().revalidate();
    }
}
