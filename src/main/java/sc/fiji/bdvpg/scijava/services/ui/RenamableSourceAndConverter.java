package sc.fiji.bdvpg.scijava.services.ui;

import bdv.viewer.SourceAndConverter;

/**
 * Wraps a SourceAndConverter and allow to change its name
 */

public class RenamableSourceAndConverter {
    public SourceAndConverter sac;
    public RenamableSourceAndConverter(SourceAndConverter sac) {
        this.sac = sac;
    }
    public String toString() {
        return sac.getSpimSource().getName();
    }
}