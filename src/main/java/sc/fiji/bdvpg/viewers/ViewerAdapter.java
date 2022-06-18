package sc.fiji.bdvpg.viewers;

import bdv.viewer.TimePointListener;
import bdv.viewer.TransformListener;
import bdv.viewer.ViewerPanel;
import bdv.viewer.ViewerState;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * Class to avoid duplicated code in synchronizers
 */

public class ViewerAdapter {

    final tpietzsch.example2.VolumeViewerPanel bvvPanel;
    final ViewerPanel bdvPanel;

    public ViewerAdapter(bdv.viewer.ViewerPanel viewerPanel) {
        bvvPanel = null;
        bdvPanel = viewerPanel;
    }

    public ViewerAdapter(tpietzsch.example2.VolumeViewerPanel viewerPanel) {
        bvvPanel = viewerPanel;
        bdvPanel = null;
    }

    public double getWidth() {
        if (bdvPanel!=null) return bdvPanel.getWidth();
        return bvvPanel.getWidth();
    }

    public double getHeight() {
        if (bdvPanel!=null) return bdvPanel.getHeight();
        return bvvPanel.getHeight();
    }

    public ViewerState state() {
        if (bdvPanel!=null) return bdvPanel.state();
        return bvvPanel.state();
    }

    public void requestRepaint() {
        if (bdvPanel!=null) {
            bdvPanel.requestRepaint();
        } else {
            bvvPanel.requestRepaint();
        }
    }

    public void addTransformListener(TransformListener<AffineTransform3D> listener) {
        if (bdvPanel!=null) {
            bdvPanel.addTransformListener(listener);
        } else {
            bvvPanel.addTransformListener(listener);
        }
    }

    public void setTimepoint(int timepoint) {
        if (bdvPanel!=null) {
            bdvPanel.setTimepoint(timepoint);
        } else {
            bvvPanel.setTimepoint(timepoint);
        }
    }

    public void addTimePointListener(TimePointListener timeListener) {
        if (bdvPanel!=null) {
            bdvPanel.addTimePointListener(timeListener);
        } else {
            bvvPanel.addTimePointListener(timeListener);
        }
    }

    public void removeTransformListener(TransformListener<AffineTransform3D> listener) {
        if (bdvPanel!=null) {
            bdvPanel.removeTransformListener(listener);
        } else {
            bvvPanel.removeTransformListener(listener);
        }
    }

    public void removeTimePointListener(TimePointListener listener) {
        if (bdvPanel!=null) {
            bdvPanel.removeTimePointListener(listener);
        } else {
            bvvPanel.removeTimePointListener(listener);
        }
    }
}
