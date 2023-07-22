package sc.fiji.bdvpg.viewer;

import bdv.util.BdvHandle;
import bdv.viewer.AbstractViewerPanel;
import bdv.viewer.Source;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Window;
import java.awt.event.WindowEvent;

public class ViewerHelper {

    protected static final Logger logger = LoggerFactory.getLogger(
            ViewerHelper.class);

    /**
     * @param viewer bdv or bvv window
     * @return an interval containing the bdv current view
     */
    public static FinalRealInterval getViewerGlobalBoundingInterval(
            AbstractViewerPanel viewer)
    {
        AffineTransform3D viewerTransform = new AffineTransform3D();
        viewer.state().getViewerTransform(viewerTransform);
        viewerTransform = viewerTransform.inverse();
        final long[] min = new long[3];
        final long[] max = new long[3];
        max[0] = viewer.getWidth();
        max[1] = viewer.getHeight();
        return viewerTransform.estimateBounds(new FinalInterval(min, max));
    }


    /**
     * @param intervalA 3d interval A
     * @param intervalB 3d interval B
     * @return the intersection 3D interval between A and B
     */
    public static FinalInterval intersect2D(final Interval intervalA,
                                            final Interval intervalB)
    {
        assert intervalA.numDimensions() == intervalB.numDimensions();

        final long[] min = new long[2];
        final long[] max = new long[2];
        for (int d = 0; d < 2; ++d) {
            min[d] = Math.max(intervalA.min(d), intervalB.min(d));
            max[d] = Math.min(intervalA.max(d), intervalB.max(d));
        }
        return new FinalInterval(min, max);
    }

    public static boolean isSourceIntersectingCurrentView(AbstractViewerPanel viewer,
                                                          Source<?> source, boolean is2D)
    {
        if (source.getSource(0, 0) == null) {
            // Overlays have no RAI -> discard them
            return false;
        }

        final Interval interval = getSourceGlobalBoundingInterval(source, viewer.state().getCurrentTimepoint());

        final Interval viewerInterval = Intervals.smallestContainingInterval(
                ViewerHelper.getViewerGlobalBoundingInterval(viewer));

        boolean intersects;
        if (is2D) {
            intersects = !Intervals.isEmpty(intersect2D(interval, viewerInterval));
        }
        else {
            intersects = !Intervals.isEmpty(Intervals.intersect(interval,
                    viewerInterval));
        }
        return intersects;
    }


    /**
     * @param source source to probe
     * @param timepoint timepoint probed for the source
     * @return an interval containing the source
     */
    public static Interval getSourceGlobalBoundingInterval(Source<?> source,
                                                           int timepoint)
    {
        final AffineTransform3D sourceTransform = getSourceTransform(source,
                timepoint, 0);
        final RandomAccessibleInterval<?> rai = source.getSource(timepoint, 0);
        return Intervals.smallestContainingInterval(sourceTransform.estimateBounds(
                rai));
    }


    public static void closeViewer(AbstractViewerPanel viewer) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(viewer);
        topFrame.dispatchEvent(new WindowEvent(topFrame,
                WindowEvent.WINDOW_CLOSING));
    }

    public static void setViewerTitle(AbstractViewerPanel viewer, String title) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(viewer);
        topFrame.setTitle(title);
    }

    public static JFrame getJFrame(AbstractViewerPanel viewer) {
        Window window = SwingUtilities.getWindowAncestor(viewer);
        if (window instanceof JFrame) {
            return (JFrame) SwingUtilities.getWindowAncestor(viewer);
        } else return null;
    }

    public static String getViewerTitle(AbstractViewerPanel viewer) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(viewer);
        return topFrame.getTitle();
    }

    public static AffineTransform3D getSourceTransform(Source<?> source, int t,
                                                       int level)
    {
        AffineTransform3D sourceTransform = new AffineTransform3D();
        source.getSourceTransform(t, level, sourceTransform);
        return sourceTransform;
    }

    public static AffineTransform3D getViewerTransformWithNewCenter(
            AbstractViewerPanel handle, double[] xyz)
    {
        final AffineTransform3D currentViewerTransform = new AffineTransform3D();
        handle.state().getViewerTransform(currentViewerTransform);

        AffineTransform3D adaptedViewerTransform = currentViewerTransform.copy();

        // ViewerTransform notes:
        // - applyInverse: coordinates in viewer => coordinates in image
        // - apply: coordinates in image => coordinates in viewer

        final double[] targetPositionInViewerInPixels = new double[3];
        currentViewerTransform.apply(xyz, targetPositionInViewerInPixels);

        for (int d = 0; d < 3; d++) {
            targetPositionInViewerInPixels[d] *= -1;
        }

        adaptedViewerTransform.translate(targetPositionInViewerInPixels);

        final double[] windowCentreInViewerInPixels = getWindowCentreInPixelUnits(
                handle);

        adaptedViewerTransform.translate(windowCentreInViewerInPixels);

        return adaptedViewerTransform;
    }

    public static double[] getWindowCentreInPixelUnits(AbstractViewerPanel handle) {
        final double[] windowCentreInPixelUnits = new double[3];
        windowCentreInPixelUnits[0] = handle.getWidth() / 2.0;
        windowCentreInPixelUnits[1] = handle.getHeight() / 2.0;
        return windowCentreInPixelUnits;
    }

    public static double[] getWindowCentreInCalibratedUnits(AbstractViewerPanel viewer) {
        final double[] centreInPixelUnits = getWindowCentreInPixelUnits(viewer);
        final AffineTransform3D affineTransform3D = new AffineTransform3D();
        viewer.state().getViewerTransform(affineTransform3D);
        final double[] centreInCalibratedUnits = new double[3];
        affineTransform3D.inverse().apply(centreInPixelUnits,
                centreInCalibratedUnits);
        return centreInCalibratedUnits;
    }

    public static double getViewerVoxelSpacing(AbstractViewerPanel viewer) {
        final int windowWidth = viewer.getWidth();
        final int windowHeight = viewer.getHeight();

        final AffineTransform3D viewerTransform = new AffineTransform3D();
        viewer.state().getViewerTransform(viewerTransform);

        final double[] physicalA = new double[3];
        final double[] physicalB = new double[3];

        viewerTransform.applyInverse(physicalA, new double[] { 0, 0, 0 });
        viewerTransform.applyInverse(physicalB, new double[] { 0, windowWidth, 0 });

        double viewerPhysicalWidth = LinAlgHelpers.distance(physicalA, physicalB);

        viewerTransform.applyInverse(physicalA, new double[] { 0, 0, 0 });
        viewerTransform.applyInverse(physicalB, new double[] { windowHeight, 0,
                0 });

        double viewerPhysicalHeight = LinAlgHelpers.distance(physicalA, physicalB);

        final double viewerPhysicalVoxelSpacingX = viewerPhysicalWidth /
                windowWidth;
        final double viewerPhysicalVoxelSpacingY = viewerPhysicalHeight /
                windowHeight;

        logger.debug("windowWidth = " + windowWidth);
        logger.debug("windowHeight = " + windowHeight);
        logger.debug("viewerPhysicalWidth = " + viewerPhysicalWidth);
        logger.debug("viewerPhysicalHeight = " + viewerPhysicalHeight);
        logger.debug("viewerPhysicalVoxelSpacingX = " +
                viewerPhysicalVoxelSpacingX);
        logger.debug("viewerPhysicalVoxelSpacingY = " +
                viewerPhysicalVoxelSpacingY);

        return viewerPhysicalVoxelSpacingX;
    }
}
