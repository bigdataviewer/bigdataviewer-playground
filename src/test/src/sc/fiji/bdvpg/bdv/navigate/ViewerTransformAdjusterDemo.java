package src.sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import sc.fiji.bdvpg.bdv.BDVSingleton;
import sc.fiji.bdvpg.bdv.source.append.SourceBdvAdder;
import sc.fiji.bdvpg.source.importer.SourceLoader;

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
        SourceLoader slmri = new SourceLoader( "src/test/resources/mri-stack.xml" );
        slmri.run();

        new SourceBdvAdder(bdvHandle, slmri.getSource(0)).run();


        SourceLoader sl = new SourceLoader( "src/test/resources/mri-stack-shiftedX.xml" );
        sl.run();

        new SourceBdvAdder(bdvHandle, sl.getSource(0)).run();

        //SourceBdvAdder adder =
        //new SourcesLoaderAndAdder( bdvHandle, "src/test/resources/mri-stack.xml" ).run();

        //final SourcesLoaderAndAdder loaderAndAdder = new SourcesLoaderAndAdder( bdvHandle, "src/test/resources/mri-stack-shiftedX.xml" );
        //loaderAndAdder.setAutoAdjustViewerTransform( true );
        //loaderAndAdder.setAutoContrast( true );
        //loaderAndAdder.run();
    }
}
