package sc.fiji.bdvpg.sourceandconverter.display;

import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.services.SacServices;

import java.util.List;
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
        if ( SacServices.getSacDisplayService()!=null)
            SacServices.getSacDisplayService().updateDisplays( sacs );
    }

    private void changeProjectionMode()
    {
        for ( SourceAndConverter sac : sacs )
            SacServices.getSacService().setMetadata( sac, PROJECTION_MODE, projectionMode );
    }
}
