package sc.fiji.bdvpg.source.importer.samples;

import bdv.viewer.Source;
import net.imglib2.FinalInterval;

import java.util.function.Supplier;

public class MandelbrotSourceGetter implements Runnable, Supplier<Source> {

    int maxIterations = 255;

    public void run() {
        // Useless
    }

    @Override
    public Source get() {
        return new Procedural3DImageShort(
                    p -> {
                        double re = p[0];
                        double im = p[1];
                        int i = 0;
                        for ( ; i < maxIterations; ++i )
                        {
                            final double squre = re * re;
                            final double squim = im * im;
                            if ( squre + squim > 4 )
                                break;
                            im = 2 * re * im + p[1];
                            re = squre - squim + p[0];
                        }
                        return i;
                    }
                ).getSource(new FinalInterval(new long[]{ -2, -1, -0}, new long[]{ 1, 1, 0 }), "Mandelbrot Set");
    }
}
