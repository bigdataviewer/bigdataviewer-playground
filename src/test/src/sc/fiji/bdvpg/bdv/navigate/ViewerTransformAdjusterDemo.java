package src.sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import sc.fiji.bdvpg.bdv.BDVSingleton;
import sc.fiji.bdvpg.bdv.source.append.SourcesLoaderAndAdder;

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
        //loaderAndAdder.setAutoAdjustViewerTransform( true );
        //loaderAndAdder.setAutoContrast( true );
        loaderAndAdder.run();
    }
}