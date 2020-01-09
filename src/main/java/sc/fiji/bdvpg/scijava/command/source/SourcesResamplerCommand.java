package sc.fiji.bdvpg.scijava.command.source;

import bdv.viewer.SourceAndConverter;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Resample Sources")
public class SourcesResamplerCommand implements Command{

    @Parameter
    SourceAndConverter[] sourcesToResample;

    @Parameter
    SourceAndConverter model;

    @Parameter
    boolean reuseMipMaps;

    @Override
    public void run() {

        for (int i=0;i<sourcesToResample.length;i++) {
            new SourceResampler(sourcesToResample[i], model, reuseMipMaps).get();
        }

    }
}
