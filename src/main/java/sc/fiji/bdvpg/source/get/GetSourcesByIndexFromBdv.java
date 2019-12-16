package sc.fiji.bdvpg.source.get;

import bdv.util.BdvHandle;
import bdv.viewer.Source;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GetSourcesByIndexFromBdv implements Runnable {

    List<Source> srcsOut;
    BdvHandle bdvh;
    int[] indexes;

    public GetSourcesByIndexFromBdv(BdvHandle bdvh, int... indexes) {
        this.bdvh = bdvh;
        this.indexes = indexes;
    }

    public void run() {
        srcsOut = Arrays.stream(indexes)
                        .boxed()
                        .map(idx -> bdvh.getViewerPanel().getState().getSources().get(idx).getSpimSource())
                        .collect(Collectors.toList());
    }

    public List<Source> getSources() {
        return srcsOut;
    }
}
