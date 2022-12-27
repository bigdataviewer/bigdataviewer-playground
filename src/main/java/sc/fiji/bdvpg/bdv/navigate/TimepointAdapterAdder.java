package sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.viewers.ViewerAdapter;

import static bdv.viewer.ViewerStateChange.NUM_SOURCES_CHANGED;

public class TimepointAdapterAdder implements Runnable {

    private final ViewerAdapter handle;

    public TimepointAdapterAdder(BdvHandle bdvHandle)
    {
        this.handle = new ViewerAdapter(bdvHandle);
    }

    @Override
    public void run() {
        handle.state().changeListeners().add( change -> {
                if (change.equals(NUM_SOURCES_CHANGED)) {
                    //Number of sources changed
                    int nTps = SourceAndConverterHelper.getNTimepoints(handle.state().getSources().toArray(new SourceAndConverter[0]));
                    if ((nTps!=handle.state().getNumTimepoints()) && (nTps>0)) {
                        handle.state().setNumTimepoints(nTps);
                    }
                }
            }
        );
    }
}
