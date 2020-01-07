package sc.fiji.bdvpg.bdv.sourceandconverter.transform;

public class AffineTransformSourceDemo {
/*
    public static void main(String... args) {
        // Initializes static SourceService and Display Service
        BdvService.InitScijavaServices();

        // Creates a BdvHandle
        BdvHandle bdvHandle = BdvService.getSourceDisplayService().getActiveBdv();

        final String filePath = "src/test/resources/mri-stack.xml";

        final SourceLoader sourceLoader = new SourceLoader( filePath );
        sourceLoader.run();
        final SpimData spimData = sourceLoader.getSpimData();

        BdvService.getSourceService().register(spimData);

        Source src = BdvService.getSourceService().getSourcesFromSpimdata(spimData).get(0);

        BdvService.getSourceDisplayService().show(bdvHandle, src);

        AffineTransform3D at3d = new AffineTransform3D();
        at3d.rotate(2,1);
        at3d.scale(1,2,1);

        SourceAffineTransform sat = new SourceAffineTransform(src, at3d);
        sat.run();

        BdvService.getSourceDisplayService().show(bdvHandle, sat.getSourceOut());

        new ViewerTransformAdjuster(bdvHandle, sat.getSourceOut()).run();
        new BrightnessAutoAdjuster(sat.getSourceOut(), 0).run();
    }*/
}
