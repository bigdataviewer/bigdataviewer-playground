package sc.fiji.bdv.navigate;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdv.BDVSingleton;
import sc.fiji.bdv.ClickBehaviourInstaller;
import sc.fiji.bdv.screenshot.ScreenShotMaker;
import sc.fiji.bdv.sources.read.SourceAdder;
import sc.fiji.bdv.sources.read.SourceLoader;
import sc.fiji.bdv.sources.read.SourcesLoaderAndAdder;

/**
 * ViewerTransformAdjusterDemo
 * <p>
 * <p>
 * <p>
 * Author: @tischi
 * 12 2019
 */
public class ViewerTransformAdjusterDemo {
    public static void main(String[] args) throws SpimDataException
    {
        final SpimData mriSource = new XmlIoSpimData().load( "src/test/resources/mri-stack.xml" );

        BdvHandle bdvHandle = BDVSingleton.getInstance( mriSource );

        final SourceLoader sourceLoader = new SourceLoader( "src/test/resources/mri-stack-shiftedX.xml" );
        sourceLoader.run();

        // This is the point here:
        final boolean adjustViewerTransform = true;

        final SourceAdder sourceAdder = new SourceAdder( bdvHandle, sourceLoader.getSource( 0 ), true, adjustViewerTransform );
        sourceAdder.run();
    }
}
