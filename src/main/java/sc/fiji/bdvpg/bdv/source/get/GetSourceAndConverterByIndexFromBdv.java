package sc.fiji.bdvpg.bdv.source.get;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;

import java.util.function.Function;

public class GetSourceAndConverterByIndexFromBdv implements Runnable, Function<Integer, SourceAndConverter> {

    SourceAndConverter srcOut;
    BdvHandle bdvh;
    int index;

    public GetSourceAndConverterByIndexFromBdv(BdvHandle bdvh, int index) {
        this.bdvh = bdvh;
        this.index = index;
    }

    public void run() {
        srcOut = apply(index);
    }

    public SourceAndConverter getSource() {
        return srcOut;
    }

    @Override
    public SourceAndConverter apply(Integer integer) {
        return bdvh.getViewerPanel().getState().getSources().get(integer);
    }
}
