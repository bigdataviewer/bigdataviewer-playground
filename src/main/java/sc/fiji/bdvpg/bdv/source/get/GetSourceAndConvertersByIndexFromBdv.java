package sc.fiji.bdvpg.bdv.source.get;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GetSourceAndConvertersByIndexFromBdv implements Runnable {

    List<SourceAndConverter> srcsOut;
    BdvHandle bdvh;
    int[] indexes;

    public GetSourceAndConvertersByIndexFromBdv(BdvHandle bdvh, int... indexes) {
        this.bdvh = bdvh;
        this.indexes = indexes;
    }

    public void run() {
        srcsOut = Arrays.stream(indexes)
                        .boxed()
                        .map(idx -> bdvh.getViewerPanel().getState().getSources().get(idx))
                        .collect(Collectors.toList());
    }

    public List<SourceAndConverter> getSources() {
        return srcsOut;
    }
}
