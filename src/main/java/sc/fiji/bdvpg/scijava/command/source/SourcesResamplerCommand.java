package sc.fiji.bdvpg.scijava.command.source;

import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Resample Source Based on Model Source")
public class SourcesResamplerCommand implements Command{

    @Parameter(label = "Select Source(s)")
    SourceAndConverter[] sourcesToResample;

    @Parameter
    SourceAndConverter model;

    @Parameter(label="Re-use MipMaps")
    boolean reuseMipMaps;

    @Parameter
    boolean interpolate;

    @Parameter
    boolean cache;

    @Override
    public void run() {
        // Should not be parallel
        for (int i=0;i<sourcesToResample.length;i++) {
            SourceAndConverterServices.getSourceAndConverterService().register(
                new SourceResampler(sourcesToResample[i], model, reuseMipMaps, cache, interpolate).get()
            );
        }

    }
}
