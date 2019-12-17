package sc.fiji.bdvpg.source.importer.samples;

import bdv.viewer.Source;
import net.imglib2.FinalInterval;

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
                ).getSource(new FinalInterval(new long[]{0,0,0}, new long[]{512,512,512}),"Wave 3D");
    }
}
