package sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import sc.fiji.bdvpg.behaviour.ClickBehaviourInstaller;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

/**
 * ViewTransformSynchronizationDemo
 * <p>
 * <p>
 * <p>
 * Author: Nicolas Chiaruttini
 * 01 2020
 */
public class ViewTransformSynchronizationDemo {
    
    static boolean isSynchronizing;

    public static void main(String[] args) {

        // Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // load and convert an image
        ImagePlus imp = IJ.openImage("src/test/resources/blobs.tif");
        RandomAccessibleInterval rai = ImageJFunctions.wrapReal(imp);
        // Adds a third dimension because Bdv needs 3D
        rai = Views.addDimension( rai, 0, 0 );

        // Makes Bdv Source
        Source source = new RandomAccessibleIntervalSource(rai, Util.getTypeFromInterval(rai), "blobs");
        SourceAndConverter sac = SourceAndConverterUtils.createSourceAndConverter(source);

        // Creates a BdvHandle
        BdvHandle bdvHandle1 = SourceAndConverterServices.getSourceAndConverterDisplayService().getNewBdv();
        // Creates a BdvHandle
        BdvHandle bdvHandle2 = SourceAndConverterServices.getSourceAndConverterDisplayService().getNewBdv();
        // Creates a BdvHandles
        BdvHandle bdvHandle3 = SourceAndConverterServices.getSourceAndConverterDisplayService().getNewBdv();

        BdvHandle[] bdvhs = new BdvHandle[]{bdvHandle1,bdvHandle2,bdvHandle3};

        ViewerTransformSyncStarter syncstart = new ViewerTransformSyncStarter(bdvhs);
        ViewerTransformSyncStopper syncstop = new ViewerTransformSyncStopper(syncstart.getSynchronizers());

        syncstart.run();
        isSynchronizing = true;

        for (BdvHandle bdvHandle:bdvhs) {
            // Show the sourceandconverter
            SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, sac);

            // Adjust view on sourceandconverter
            new ViewerTransformAdjuster(bdvHandle, sac).run();

            new ClickBehaviourInstaller(bdvHandle, (x,y) -> {
                if (isSynchronizing) {
                    syncstop.run();
                } else {
                    syncstart.setBdvHandleInitialReference(bdvHandle);
                    syncstart.run();
                }
                isSynchronizing = !isSynchronizing;
            }).install("Toggle Synchronization", "ctrl S");
        }




    }
}
