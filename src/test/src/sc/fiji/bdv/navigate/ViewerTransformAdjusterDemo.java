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
    public static void main(String[] args)
    {
        BdvHandle bdvHandle = BDVSingleton.getInstance( );

        new SourcesLoaderAndAdder( bdvHandle, "src/test/resources/mri-stack.xml" ).run();

        final SourcesLoaderAndAdder loaderAndAdder = new SourcesLoaderAndAdder( bdvHandle, "src/test/resources/mri-stack-shiftedX.xml" );
        loaderAndAdder.setAutoAdjustViewerTransform( true );
        loaderAndAdder.setAutoContrast( true );
        loaderAndAdder.run();
    }
}
