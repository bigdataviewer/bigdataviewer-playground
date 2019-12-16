package sc.fiji.bdvpg.source.importer.samples;

import bdv.viewer.Source;

import java.util.function.Supplier;

public class Wave3DSourceGetter implements Runnable, Supplier<Source> {

    @Override
    public void run() {
        // Useless
    }

    @Override
    public Source get() {
        return new Procedural3DImageShort(
                        p -> (int) ((Math.sin(p[0]/20)*Math.sin(p[1]/40)*Math.sin(p[2]/5)+1)*100)
                ).getSource("Wave 3D");
    }
}
