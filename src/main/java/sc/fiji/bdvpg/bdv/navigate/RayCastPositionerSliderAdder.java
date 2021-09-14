package sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.TimePointListener;
import bdv.viewer.TransformListener;
import bdv.viewer.ViewerStateChangeListener;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adds a Z slider for BigDataViewer windows. It works by casting a ray at the middle
 * of the bdv window and finding all intersecting planes.
 *
 * The slider updates itself to the current plane when the user moves in a 'regular'
 * way the bdv window. The slider can be moved by the user in order to update the viewer
 * to a different plane.
 *
 * @author Nicolas Chiaruttini, EPFL, 2021
 *
 */
public class RayCastPositionerSliderAdder implements Runnable {

    final BdvHandle bdvh;

    private List<Double> zLocations  = new ArrayList<>();

    final JSlider slider;

    public RayCastPositionerSliderAdder(BdvHandle bdvh) {
        this.bdvh = bdvh;
        slider = new JSlider(JSlider.VERTICAL);
    }

    int nPositions;
    int currentPosition;
    RealPoint lastDirection = new RealPoint();

    synchronized void setPositions(List<Double> zLocations) {
        //if (zLocations.size()>0) {
            this.zLocations = new ArrayList<>(zLocations);
            if (nPositions != zLocations.size()) {
                // Change of number of planes
                nPositions = zLocations.size();
                slider.setMaximum(nPositions - 1);
            }
            int i = 0;
            while ((i < nPositions) && (this.zLocations.get(i) < 0)) i++;
            if (currentPosition != i) {
                currentPosition = i;
                slider.setValue(i);
            }
        //}
    }

    final TransformListener<AffineTransform3D> transformListener = (transform) -> updatePositions();
    final ViewerStateChangeListener changeListener = (viewerStateChange) -> updatePositions();
    final TimePointListener timePointListener = (timepoint) -> updatePositions();

    @Override
    public void run() {

        bdvh.getViewerPanel().addTransformListener(transformListener);
        bdvh.getViewerPanel().addTimePointListener(timePointListener);
        bdvh.getViewerPanel().state().changeListeners().add(changeListener);
        bdvh.getViewerPanel().add(slider, BorderLayout.WEST);
        bdvh.getViewerPanel().revalidate();

        slider.addChangeListener((e) -> {
            if (zLocations.size()>0) {
                JSlider slider = (JSlider) e.getSource();
                int newValue = slider.getValue();
                //Plane : slider.getValue() / slider.getMaximum()
                if ((currentPosition != newValue)&&(newValue!=-1)) {
                    //User slider action: needs update of viewer from currentPosition to newValue
                    currentPosition = newValue;
                    double shiftZ = zLocations.get(currentPosition);
                    //Need to shift z by shiftZ

                    AffineTransform3D at3d = new AffineTransform3D();

                    // Change the position of the viewer with the new offset
                    bdvh.getViewerPanel().state().getViewerTransform(at3d);
                    double[] currentCenter = BdvHandleHelper.getWindowCentreInCalibratedUnits(bdvh);
                    double[] newCenter = new double[3];
                    newCenter[0] = currentCenter[0]+lastDirection.getDoublePosition(0) * shiftZ;
                    newCenter[1] = currentCenter[1]+lastDirection.getDoublePosition(1) * shiftZ;
                    newCenter[2] = currentCenter[2]+lastDirection.getDoublePosition(2) * shiftZ;
                    bdvh.getViewerPanel().state().setViewerTransform(BdvHandleHelper.getViewerTransformWithNewCenter(bdvh, newCenter));

                } // else: Bdv user movement: no update required

            }
        });

    }

    public void updatePositions() {

        // Find origin and direction of ray - center of the bdv window
        double[] c = BdvHandleHelper.getWindowCentreInCalibratedUnits(bdvh);
        RealPoint origin = new RealPoint(3);
        origin.setPosition(c[0],0);
        origin.setPosition(c[1],1);
        origin.setPosition(c[2],2);

        RealPoint direction = new RealPoint(0,0,1);
        final AffineTransform3D affineTransform3D = new AffineTransform3D();
        bdvh.getViewerPanel().state().getViewerTransform( affineTransform3D );
        affineTransform3D.setTranslation(0,0,0);
        affineTransform3D.inverse().apply(direction,direction);
        SourceAndConverterHelper.normalize3(direction);

        lastDirection = new RealPoint(direction);

        // Initializes zLocations : empty
        List<Double> zLocations = new ArrayList<>();
        int timepoint = bdvh.getViewerPanel().state().getCurrentTimepoint();

        for (SourceAndConverter<?> source : bdvh.getViewerPanel().state().getActiveSources()) {
            zLocations.addAll(SourceAndConverterHelper.rayIntersect(source,timepoint,origin, direction));
        }

        // Precision loss for efficient duplicates removal
        zLocations = zLocations.stream().map(d -> (double) (d.floatValue())).collect(Collectors.toList());

        // Fast
        zLocations = zLocations
                .stream()
                .sorted()
                .distinct() // Removes duplicate z positions
                .collect(Collectors.toList());

        setPositions(zLocations);
    }

    public BdvHandle getBdvh() {
        return bdvh;
    }

    public void removeFromBdv() {
        bdvh.getViewerPanel().removeTransformListener(transformListener);
        bdvh.getViewerPanel().removeTimePointListener(timePointListener);
        bdvh.getViewerPanel().state().changeListeners().remove(changeListener);
        bdvh.getViewerPanel().remove(slider);
        bdvh.getViewerPanel().revalidate();
    }
}
