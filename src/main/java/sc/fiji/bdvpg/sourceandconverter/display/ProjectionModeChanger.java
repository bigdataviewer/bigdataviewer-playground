package sc.fiji.bdvpg.sourceandconverter.display;

import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.bdv.projector.Projection;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.function.Consumer;

import static sc.fiji.bdvpg.bdv.projector.Projection.PROJECTION_MODE;

public class ProjectionModeChanger implements Runnable, Consumer< SourceAndConverter[] > {

    private String projectionMode;
    private final boolean showSourcesExclusively;
    private SourceAndConverter[] sacs;

    public ProjectionModeChanger( SourceAndConverter[] sacs, String projectionMode, boolean showSourcesExclusively ) {
        this.sacs = sacs;
        this.projectionMode = projectionMode;
        this.showSourcesExclusively = showSourcesExclusively;
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
        {
            if ( showSourcesExclusively )
                projectionMode += Projection.PROJECTION_MODE_EXCLUSIVE;

            SourceAndConverterServices.getSourceAndConverterService().setMetadata( sac, PROJECTION_MODE, projectionMode );
        }
    }
}
