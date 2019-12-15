package sc.fiji.bdv.sources.get;

import bdv.util.BdvHandle;
import bdv.viewer.Source;

public class GetSourceByIndexFromBdv implements Runnable {

    Source srcOut;
    BdvHandle bdvh;
    int index;

    public GetSourceByIndexFromBdv(BdvHandle bdvh, int index) {
        this.bdvh = bdvh;
        this.index = index;
    }

    public void run() {
        srcOut = bdvh.getViewerPanel().getState().getSources().get(index).getSpimSource();
    }

    public Source getSource() {
        return srcOut;
    }
}
