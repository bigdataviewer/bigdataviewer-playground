package sc.fiji.bdv.sourceAndConverter.get;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;

public class GetSourceAndConverterByIndexFromBdv implements Runnable {

    SourceAndConverter srcOut;
    BdvHandle bdvh;
    int index;

    public GetSourceAndConverterByIndexFromBdv(BdvHandle bdvh, int index) {
        this.bdvh = bdvh;
        this.index = index;
    }

    public void run() {
        srcOut = bdvh.getViewerPanel().getState().getSources().get(index);
    }

    public SourceAndConverter getSource() {
        return srcOut;
    }
}
