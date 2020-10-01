package sc.fiji.bdvpg.scijava.services.ui;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerStateChangeListener;
import javax.swing.tree.DefaultTreeModel;

/**
 * Filter nodes which filters based on the presence or not of a {@link SourceAndConverter}
 * in the {@link BdvHandle}
 *
 * A listener to the state of the BdvHandle {@link ViewerStateChangeListener} allows to trigger a
 * {@link FilterUpdateEvent} to the node which in turns triggers the recomputation of the
 * downstream part of the UI tree see {@link SourceFilterNode} and {@link SourceAndConverterServiceUI}
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL, 2020
 */

public class BdvHandleFilterNode extends SourceFilterNode {

    public BdvHandle bdvh;
    String name;

    public boolean filter(SourceAndConverter sac) {
        return bdvh.getViewerPanel().state().getSources().contains(sac);
    }

    public BdvHandleFilterNode(DefaultTreeModel model, String name, BdvHandle bdvh) {
        super(model, name,null, false);
        this.name = name;
        this.filter = this::filter;
        this.bdvh = bdvh;

        ViewerStateChangeListener vscl = (change) -> {
            if (change.toString().equals("NUM_SOURCES_CHANGED")) {
                update(new SourceFilterNode.FilterUpdateEvent());
            }
        };

        bdvh.getViewerPanel().state().changeListeners().add(vscl);
    }

    public String toString() {
        return getName();
    }

    @Override
    public Object clone() {
        return new BdvHandleFilterNode(model, name, bdvh);
    }

}
