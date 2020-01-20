package sc.fiji.bdvpg.sourceandconverter.display;

import bdv.viewer.SourceAndConverter;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import sc.fiji.bdvpg.log.SystemLogger;
import sc.fiji.bdvpg.services.BdvService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

import static sc.fiji.bdvpg.bdv.projector.Projection.PROJECTION_MODE;

public class ProjectionModeChanger implements Runnable, Consumer<SourceAndConverter> {

    SourceAndConverter sac;
    private final String projectionMode;

    public ProjectionModeChanger(SourceAndConverter sac, String projectionMode) {
        this.sac = sac;
        this.projectionMode = projectionMode;
    }

    @Override
    public void run() {
        accept(sac);
    }

    @Override
    public void accept(SourceAndConverter sourceAndConverter) {

        changeProjectionMode();
        updateDisplays();
    }

    public void updateDisplays()
    {
        if ( BdvService.getSourceAndConverterDisplayService()!=null)
                BdvService.getSourceAndConverterDisplayService().updateDisplays( sac );
    }

    public void changeProjectionMode()
    {
        // TODO: change this in case we add methods to directly access the metadata
        final Map< String, Object > metadata = BdvService.getSourceAndConverterService().getSourceAndConverterToMetadata().get( sac );
        metadata.put( PROJECTION_MODE, projectionMode );
    }
}
