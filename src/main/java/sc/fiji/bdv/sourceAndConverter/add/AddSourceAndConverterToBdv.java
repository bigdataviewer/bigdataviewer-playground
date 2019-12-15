package sc.fiji.bdv.sourceAndConverter.add;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;

import java.util.function.Consumer;

public class AddSourceAndConverterToBdv implements Runnable, Consumer<SourceAndConverter> {
    SourceAndConverter sacIn;
    BdvHandle bdvh;

    public AddSourceAndConverterToBdv(BdvHandle bdvh, SourceAndConverter sacIn) {
        this.sacIn=sacIn;
        this.bdvh=bdvh;
    }

    public void run() {
        accept(sacIn);
    }

    public void accept(SourceAndConverter sac) {
        bdvh.getViewerPanel().addSource(sac);
    }

}
