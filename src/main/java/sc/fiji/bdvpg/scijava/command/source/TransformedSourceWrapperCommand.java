package sc.fiji.bdvpg.scijava.command.source;

import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.util.Arrays;
import java.util.stream.Collectors;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Transform>Wrap as Transformed Source")
public class TransformedSourceWrapperCommand implements Command {
    @Parameter(label = "Select Source(s)")
    SourceAndConverter[] sources_in;

    @Override
    public void run() {
        SourceAffineTransformer sat = new SourceAffineTransformer(null, new AffineTransform3D());
        Arrays.asList(sources_in).stream().map(sat::apply).forEach(sac ->
                        SourceAndConverterServices.getSourceAndConverterService().register(sac)
                );
    }
}
