package sc.fiji.bdvpg.sourceandconverter.display;

import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.function.Consumer;

import static sc.fiji.bdvpg.bdv.projector.Projection.PROJECTION_MODE;

public class ProjectionModeChanger implements Runnable, Consumer< SourceAndConverter[] > {

    private final String projectionMode;
    private SourceAndConverter[] sacs;

    public ProjectionModeChanger(SourceAndConverter[] sacs, String projectionMode) {
        this.sacs = sacs;
        this.projectionMode = projectionMode;
    }

    @Override
    public void run() {
        accept( sacs );
    }

    @Override
    public void accept(SourceAndConverter[] sourceAndConverter) {

        changeProjectionMode();
        updateDisplays();
    }

    private void updateDisplays()
    {
        if ( SourceAndConverterServices.getSourceAndConverterDisplayService()!=null)
            SourceAndConverterServices.getSourceAndConverterDisplayService().updateDisplays( sacs );

    }

    private void changeProjectionMode()
    {
        for ( SourceAndConverter sac : sacs )
            SourceAndConverterServices.getSourceAndConverterService().setMetadata( sac, PROJECTION_MODE, projectionMode );
    }
}
