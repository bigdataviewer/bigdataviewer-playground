package sc.fiji.bdvpg.bdv.source.append;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;

import java.util.function.Consumer;

public class SourceAndConverterBdvAdder implements Runnable, Consumer<SourceAndConverter>
{
    private final BdvHandle bdvHandle;
    private final SourceAndConverter sac;

    public SourceAndConverterBdvAdder(BdvHandle bdvHandle, SourceAndConverter sac) {
        this.bdvHandle=bdvHandle;
        this.sac = sac;
    }

    public void run() {
        accept(sac);
    }

    @Override
    public void accept(SourceAndConverter sourceAndConverter) {
        bdvHandle.getViewerPanel().addSource( sourceAndConverter );
    }
}
