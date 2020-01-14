package sc.fiji.bdvpg.sourceandconverter.importer;

import bdv.util.Procedural3DImageShort;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.FinalInterval;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import java.util.function.Supplier;

public class Wave3DSourceGetter implements Runnable, Supplier<SourceAndConverter> {

    @Override
    public void run() {
        // Useless
    }

    @Override
    public SourceAndConverter get() {
        Source s = new Procedural3DImageShort(
                        p -> (int) ((Math.sin(p[0]/20)*Math.sin(p[1]/40)*Math.sin(p[2]/5)+1)*100)
                ).getSource(new FinalInterval(new long[]{0,0,0}, new long[]{512,512,512}),"Wave 3D");

        return SourceAndConverterUtils.createSourceAndConverter(s);
    }
}
