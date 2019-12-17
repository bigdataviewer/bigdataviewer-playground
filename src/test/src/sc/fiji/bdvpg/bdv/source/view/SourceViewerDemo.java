package sc.fiji.bdvpg.bdv.source.view;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import sc.fiji.bdvpg.bdv.BDVSingleton;
import sc.fiji.bdvpg.bdv.source.append.SourcesLoaderAndAdder;
import sc.fiji.bdvpg.bdv.source.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.bdv.source.displayopts.BrightnessAdjuster;
import sc.fiji.bdvpg.source.importer.SourceLoader;

/**
 * SourceViewerDemo
 * <p>
 * <p>
 * <p>
 * Author: @tischi
 * 15 2019
 */
public class SourceViewerDemo
{
    public static void main(String[] args)
    {
        final SourceLoader loader = new SourceLoader( "src/test/resources/mri-stack.xml" );
        loader.run();
        final Source source = loader.getSource( 0 );

        final SourceViewer viewer = new SourceViewer( source );
        viewer.run();
        final BdvHandle bdvHandle = viewer.getBdvHandle();

        new BrightnessAutoAdjuster( bdvHandle, source ).run();
    }
}
