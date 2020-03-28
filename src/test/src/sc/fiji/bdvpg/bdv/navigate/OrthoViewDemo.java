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
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.List;

/**
 * ViewTransformSynchronizationDemo
 * <p>
 * <p>
 * <p>
 * Author: Nicolas Chiaruttini
 * 01 2020
 */
public class OrthoViewDemo {
    
    static boolean isSynchronizing;

    public static void main(String[] args) {

        // Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // Makes Bdv Source

        new SpimDataFromXmlImporter( "src/test/resources/mri-stack.xml" ).run();

        //Source source = new RandomAccessibleIntervalSource(rai, Util.getTypeFromInterval(rai), "blobs");
        //SourceAndConverter sac = SourceAndConverterUtils.createSourceAndConverter(source);

        // Creates a BdvHandle
        BdvHandle bdvHandleX = SourceAndConverterServices.getSourceAndConverterDisplayService().getNewBdv();
        // Creates a BdvHandle
        BdvHandle bdvHandleY = SourceAndConverterServices.getSourceAndConverterDisplayService().getNewBdv();
        // Creates a BdvHandles
        BdvHandle bdvHandleZ = SourceAndConverterServices.getSourceAndConverterDisplayService().getNewBdv();

        BdvHandle[] bdvhs = new BdvHandle[]{bdvHandleX,bdvHandleY,bdvHandleZ};

        // Get a handle on the sacs
        final List< SourceAndConverter > sacs = SourceAndConverterServices.getSourceAndConverterService().getSourceAndConverters();

        ViewerOrthoSyncStarter syncstart = new ViewerOrthoSyncStarter(bdvHandleX,bdvHandleY,bdvHandleZ);
        ViewerTransformSyncStopper syncstop = new ViewerTransformSyncStopper(syncstart.getSynchronizers());

        syncstart.run();
        isSynchronizing = true;

        for (BdvHandle bdvHandle:bdvhs) {

            sacs.forEach( sac -> {
                SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, sac);
                new ViewerTransformAdjuster(bdvHandle, sac).run();
                new BrightnessAutoAdjuster(sac, 0).run();
            });

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
