package sc.fiji.bdvpg.bdv.source.transform;

import bdv.util.BdvHandle;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Source;
import com.google.common.collect.Lists;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import sc.fiji.bdvpg.bdv.source.append.SourceAdder;
import sc.fiji.bdvpg.services.BdvService;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransform;

import java.util.ArrayList;
import java.util.List;

public class AffineTransformSourceBatchDemo {
/*
    public static void main(String... args) {
        // Initializes static SourceService and Display Service
        BdvService.InitScijavaServices();

        // load and convert an image
        ImagePlus imp = IJ.openImage("src/test/resources/blobs.tif");
        RandomAccessibleInterval rai = ImageJFunctions.wrapReal(imp);
        // Adds a third dimension because Bdv needs 3D
        rai = Views.addDimension( rai, 0, 0 );

        // Makes Bdv Source
        Source source = new RandomAccessibleIntervalSource(rai, Util.getTypeFromInterval(rai), "blobs");

        // Creates a BdvHandle
        BdvHandle bdvHandle = BdvService.getSourceDisplayService().getActiveBdv();

        // Show the source
        BdvService.getSourceDisplayService().show(bdvHandle, source);

        // Make a grid of blobs
        List<Source> gridSources = makeGrid(bdvHandle, source, 5, 3, 400, 400);

        // Creates a transformer ( = an action described as in the readme, but it also implements Function<Source,Source> -> it has a Source input and a Source output
        // This affineTransformer is a function because it takes a Source as an input and outputs a transformed Source as an output
        AffineTransform3D at3d = new AffineTransform3D();
        at3d.rotate(2,1);
        at3d.scale(1,2,1);
        SourceAffineTransform affineTransformer = new SourceAffineTransform(null, at3d); // Not necessary to specify a source

        // Creates a bdv adder ( = an action described as in the readme but it also implements Consumer<Source> -> it has one Source input and no output
        // This bdvAdder is a Consumer because it takes one Source as an input and do not return anything
        // It is initializes with a null Source -> this source specified in the constructor is only useful for single action
        SourceAdder bdvAdder = new SourceAdder(bdvHandle, null);

        // Transform all the source, using the affineTransformer
        List<Source> transformedSources = Lists.transform(gridSources, affineTransformer::apply);

        // Display all the transformed source, using the bdvAdder
        transformedSources.forEach(bdvAdder::accept);
    }

    static public List<Source> makeGrid(BdvHandle bdvh, Source src, int nx, int ny, int shiftx, int shifty) {
        List<Source> sources = new ArrayList<>();
        AffineTransform3D at3D = new AffineTransform3D();
        for (int px=1;px<nx;px++) {
            for (int py=0;py<ny;py++) {
                at3D.identity();
                at3D.translate(px*shiftx, py*shifty, 0);
                SourceAffineTransform sat = new SourceAffineTransform(src, at3D); // Not necessary to specify a source
                sat.run();
                new SourceAdder(bdvh, sat.getSourceOut()).run();
                sources.add(sat.getSourceOut());
            }
        }
        return sources;
    }*/
}
