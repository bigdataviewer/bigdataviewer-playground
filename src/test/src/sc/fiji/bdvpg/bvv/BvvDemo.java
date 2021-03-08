package sc.fiji.bdvpg.bvv;

import bdv.util.BdvFunctions;
import bvv.util.BvvFunctions;
import bvv.util.BvvHandle;
import bvv.util.BvvOptions;
import bvv.util.BvvStackSource;
import net.imagej.ImageJ;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import java.util.Random;

public class BvvDemo {
    static ImageJ ij;
    public static void main(String... args) {
        // Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
        ij = new ImageJ();
        ij.ui().showUI();

        Random random = new Random();
        Img<UnsignedShortType> img = ArrayImgs.unsignedShorts(10, 10, 10);
        img.forEach(t -> t.set(random.nextInt()));

        BdvFunctions.show(img, "Random box");

        BvvStackSource<?> bss = BvvFunctions.show( img, "Random Box");//, bvvOptions );

        bss.getConverterSetups().get(0).setDisplayRange(0,65535);

    }
}
