package sc.fiji.bdvpg.bdv.source.append;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.Source;

import java.util.function.Consumer;
import java.util.function.Function;

public class SourceBdvAdder implements Runnable, Function<Source, BdvStackSource> {

    private final BdvHandle bdvHandle;
    private final Source source;
    BdvStackSource bdvStackSource;

    public SourceBdvAdder(BdvHandle bdvHandle, Source source) {
        this.source=source;
        this.bdvHandle=bdvHandle;
    }

    public void run() {
        bdvStackSource = apply(source);
    }

    public BdvStackSource getBdvStackSource() {
        return bdvStackSource;
    }

    @Override
    public BdvStackSource apply(Source source) {
        return BdvFunctions.show(source, BdvOptions.options().addTo(bdvHandle));
    }
}
