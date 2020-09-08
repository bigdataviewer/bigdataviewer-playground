package sc.fiji.bdvpg.bdv.sourceandconverter.transform;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imagej.ImageJ;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.ArrayList;

public class AffineTransformSourceDemo {

    public static void main(String... args) {
        // Initializes static SourceService and Display Service

        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // Creates a BdvHandle
        BdvHandle bdvHandle = SourceAndConverterServices
                .getSourceAndConverterDisplayService().getActiveBdv();

        final String filePath = "src/test/resources/mri-stack.xml";
        // Import SpimData
        SpimDataFromXmlImporter importer = new SpimDataFromXmlImporter(filePath);
        //importer.run();

        final AbstractSpimData spimData = importer.get();

        SourceAndConverter sac = SourceAndConverterServices
                .getSourceAndConverterService()
                .getSourceAndConverterFromSpimdata(spimData)
                .get(0);

        new ViewerTransformAdjuster(bdvHandle, sac).run();
        new BrightnessAutoAdjuster(sac, 0).run();

        ArrayList<SourceAndConverter> sacs = new ArrayList<>();
        for (int x = 0; x < 20*0.25;x++) {
            for (int y = 0; y < 20*0.25; y++) {

                if (Math.random()>0.0) {
                    AffineTransform3D at3d = new AffineTransform3D();
                    at3d.rotate(2, Math.random());
                    at3d.scale(0.5 + Math.random() / 4, 0.5 + Math.random() / 4, 1);
                    at3d.translate(200 * x, 200 * y, 0);

                    SourceAffineTransformer sat = new SourceAffineTransformer(sac, at3d);
                    sat.run();

                    SourceAndConverter transformedSac = sat.getSourceOut();

                    sacs.add(transformedSac);
                }
            }
        }

        SourceAndConverterServices
                .getSourceAndConverterDisplayService()
                .show(bdvHandle, sacs.toArray(new SourceAndConverter[0]));

    }
}
