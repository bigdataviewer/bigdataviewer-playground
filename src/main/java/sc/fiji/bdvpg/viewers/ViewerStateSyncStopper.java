package sc.fiji.bdvpg.viewers;

import bdv.viewer.ViewerStateChangeListener;

import java.util.Map;

public class ViewerStateSyncStopper implements Runnable {

    final Map<ViewerAdapter, ViewerStateChangeListener> listenerMap;

    public ViewerStateSyncStopper(Map<ViewerAdapter, ViewerStateChangeListener> listenerMap) {
        this.listenerMap = listenerMap;
    }

    @Override
    public void run() {
        listenerMap.forEach((viewer, listener) -> viewer.state().changeListeners().remove(listener));
    }

}
