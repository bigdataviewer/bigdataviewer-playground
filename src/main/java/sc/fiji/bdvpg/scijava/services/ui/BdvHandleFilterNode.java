package sc.fiji.bdvpg.scijava.services.ui;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerStateChangeListener;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;

/**
 * SourceAndConverter filter node : Selects SpimData and allow for duplicate
 */

public class BdvHandleFilterNode extends SourceFilterNode {

    public BdvHandle bdvh;
    String name;

    public boolean filter(SourceAndConverter sac) {
        return bdvh.getViewerPanel().state().getSources().contains(sac);
    }

    public BdvHandleFilterNode(String name, BdvHandle bdvh) {
        super(name,null, false);
        this.name = name;
        this.filter = this::filter;
        this.bdvh = bdvh;

        ViewerStateChangeListener vscl = (change) -> {
            if (change.toString().equals("NUM_SOURCES_CHANGED")) {
                update(new SourceFilterNode.FilterUpdateEvent());
                SwingUtilities.invokeLater(() -> {
                        ((SourceAndConverterService)SourceAndConverterServices
                                .getSourceAndConverterService())
                                .getUI()
                                .getTreeModel()
                                .nodeStructureChanged(BdvHandleFilterNode.this);
                });
            }
        };

        bdvh.getViewerPanel().state().changeListeners().add(vscl);
    }

    public String toString() {
        return getName();
    }

    @Override
    public Object clone() {
        return new BdvHandleFilterNode(name, bdvh);
    }

}
