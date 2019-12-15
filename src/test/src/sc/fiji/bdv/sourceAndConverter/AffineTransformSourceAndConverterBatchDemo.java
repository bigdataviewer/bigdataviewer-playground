package src.sc.fiji.bdv.sourceAndConverter;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import com.google.common.collect.Lists;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdv.BDVSingleton;
import sc.fiji.bdv.source.add.AddSourceToBdv;
import sc.fiji.bdv.source.get.GetSourceByIndexFromBdv;
import sc.fiji.bdv.source.get.GetSourcesByIndexFromBdv;
import sc.fiji.bdv.source.transform.SourceAffineTransform;

import java.util.List;

public class AffineTransformSourceAndConverterBatchDemo {

    public static void main(String... args) {
        // Gets blobs
        ImagePlus imp = IJ.openImage("src/test/resources/blobs.tif");
        RandomAccessibleInterval rai = ImageJFunctions.wrapReal(imp);

        // Open BigDataViewer and show the blobs image
        BdvHandle bdvHandle = BDVSingleton.getInstance(rai, "Blobs");

        // Make a grid of blobs
        makeGrid(bdvHandle, 0, 5, 3, 400, 400);

        // Creates a transformer ( = an action described as in the readme, but it also implements Function<Source,Source> -> it has a Source input and a Source output
        // This affineTransformer is a function because it takes a Source as an input and outputs a transformed Source as an output
        AffineTransform3D at3d = new AffineTransform3D();
        at3d.rotate(2,1);
        at3d.scale(1,2,1);
        SourceAffineTransform affineTransformer = new SourceAffineTransform(null, at3d); // Not necessary to specify a source

        // Creates a bdv adder ( = an action described as in the readme but it also implements Consumer<Source> -> it has one Source input and no output
        // This bdvAdder is a Consumer because it takes one Source as an input and do not return anything
        // It is initializes with a null Source -> this source specified in the constructor is only useful for single action
        AddSourceToBdv bdvAdder = new AddSourceToBdv(bdvHandle, null);

        // Construct source getter based on their indexes and on a BdvHandle
        GetSourcesByIndexFromBdv srcGetter = new GetSourcesByIndexFromBdv(bdvHandle, 2,4,6,8,10,12);
        // Actually get the source
        srcGetter.run();

        // Transform all the source, using the affineTransformer
        List<Source> transformedSources = Lists.transform(srcGetter.getSources(), affineTransformer::apply);

        // Display all the transformed source, using the bdvAdder
        transformedSources.forEach(bdvAdder::accept);

    }

    static public void makeGrid(BdvHandle bdvh, int sourceIndex, int nx, int ny, int shiftx, int shifty) {
        GetSourceByIndexFromBdv gs = new GetSourceByIndexFromBdv(bdvh,sourceIndex);
        gs.run();
        Source src = gs.getSource();
        AffineTransform3D at3D = new AffineTransform3D();
        for (int px=1;px<nx;px++) {
            for (int py=0;py<ny;py++) {
                at3D.identity();
                at3D.translate(px*shiftx, py*shifty, 0);
                SourceAffineTransform sat = new SourceAffineTransform(src, at3D); // Not necessary to specify a source
                sat.run();
                new AddSourceToBdv(bdvh, sat.getSourceOut()).run();
            }
        }
    }
}
