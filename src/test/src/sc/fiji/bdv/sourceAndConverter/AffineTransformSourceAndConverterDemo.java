package sc.fiji.bdv.sourceAndConverter;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.viewer.Source;
import mpicbg.spim.data.SpimData;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdv.BDVSingleton;
import sc.fiji.bdv.sources.add.AddSourceToBdv;
import sc.fiji.bdv.sources.get.GetSourceByIndexFromBdv;
import sc.fiji.bdv.sources.read.SourceLoader;
import sc.fiji.bdv.sources.transform.SourceAffineTransform;

public class AffineTransformSourceAndConverterDemo {

    public static void main(String... args) {

        // Open BigDataViewer
        BdvHandle bdvHandle = BDVSingleton.getInstance();

        final String filePath = "src/test/resources/mri-stack.xml";

        final SourceLoader sourceLoader = new SourceLoader( filePath );
        sourceLoader.run();
        final SpimData spimData = sourceLoader.getSpimData();

        BdvFunctions.show(spimData, BdvOptions.options().addTo(bdvHandle));

        GetSourceByIndexFromBdv gs = new GetSourceByIndexFromBdv(bdvHandle,1);
        gs.run();
        Source src = gs.getSource();

        AffineTransform3D at3d = new AffineTransform3D();
        at3d.rotate(2,1);
        at3d.scale(1,2,1);

        SourceAffineTransform sat = new SourceAffineTransform(src, at3d);
        sat.run();

        AddSourceToBdv addsrc = new AddSourceToBdv(bdvHandle, sat.getSourceOut());
        addsrc.run();
    }
}
