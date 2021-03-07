package sc.fiji.bdvpg;

import bdv.util.BdvHandle;
import bdv.util.BigWarpHelper;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imagej.ImageJ;
import net.imglib2.realtransform.*;
import org.junit.After;
import org.junit.Test;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceRealTransformer;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WarpedSourceDemo {

    static ImageJ ij;

    static public void main(String... args) throws Exception {
        // Arrange
        // create the ImageJ application context with all available services
        ij = new ImageJ();
        ij.ui().showUI();

        demo();
    }

    @Test
    public void demoRunOk() throws Exception {
        main(new String[]{""});
    }

    @After
    public void closeFiji() {
        TestHelper.closeFijiAndBdvs(ij);
    }

    public static void demo() throws Exception {

        // Get Model Source
        SpimDataFromXmlImporter importer = new SpimDataFromXmlImporter("src/test/resources/mri-stack-multilevel.xml");
        AbstractSpimData asd = importer.get();

        SourceAndConverter sac = SourceAndConverterServices
                .getSourceAndConverterService()
                .getSourceAndConverterFromSpimdata(asd)
                .get(0);

        SourceAndConverterServices
                .getSourceAndConverterDisplayService()
                .show(sac);

        // Gets active BdvHandle instance
        BdvHandle bdvHandle = SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();

        SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, sac);
        new ViewerTransformAdjuster(bdvHandle, sac).run();
        new BrightnessAutoAdjuster(sac, 0).run();

        List<RealTransform> transform_tested = new ArrayList<>();

        // Warp 3D
        RealTransform warp3d = BigWarpHelper.realTransformFromBigWarpFile(new File("src/test/resources/landmarks3d-demo.csv"), true);
        transform_tested.add(warp3d);

        // Warp 2D
        RealTransform warp2d = BigWarpHelper.realTransformFromBigWarpFile(new File("src/test/resources/landmarks2d-demoSlice.csv"), true);
        transform_tested.add(warp2d);

        // Affine transform
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.translate(-150,0,0);
        transform_tested.add(at3D);

        // Real transform sequence
        RealTransformSequence rts = new RealTransformSequence();
        rts.add(at3D);
        rts.add(warp3d);
        transform_tested.add(rts);

        // --- Invertible real transform sequence

        InvertibleRealTransformSequence irts = new InvertibleRealTransformSequence();
        irts.add(at3D.inverse());
        irts.add(at3D.inverse());
        //irts.add(((InvertibleRealTransform) warp3d).inverse()); //unfortunately this doesn't work because of
        //estimatebounds function which returns somewhere a singular matrix
        irts.add((InvertibleRealTransform) warp3d);
        transform_tested.add(irts);

        // TODO : Send bug report for singular matrix in estimate bounds

        // Register sources and display source
        for (RealTransform rt : transform_tested) {
            SourceAndConverter transformed_source = new SourceRealTransformer(rt).apply(sac);

            SourceAndConverterServices
                    .getSourceAndConverterService()
                    .register(transformed_source);

            SourceAndConverterServices
                    .getSourceAndConverterDisplayService()
                    .show(bdvHandle, transformed_source);

        }

    }

}