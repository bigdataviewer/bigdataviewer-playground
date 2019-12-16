package sc.fiji.source.add;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.viewer.Source;

import java.util.function.Consumer;

public class AddSourceToBdv implements Runnable, Consumer<Source> {

    Source srcIn;
    BdvHandle bdvh;

    public AddSourceToBdv(BdvHandle bdvh, Source srcIn) {
        this.srcIn=srcIn;
        this.bdvh=bdvh;
    }

    public void run() {
        accept(srcIn);
    }

    @Override
    public void accept(Source source) {
        BdvFunctions.show(source, BdvOptions.options().addTo(bdvh));
    }
}
