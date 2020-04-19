package sc.fiji.bdvpg.scijava;

import bdv.util.BdvHandle;
import bvv.util.BvvHandle;
import org.scijava.cache.CacheService;
import org.scijava.object.ObjectService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.ref.WeakReference;
import java.util.List;

public class BvvHandleHelper {

    public static JFrame getJFrame(BvvHandle bvvh) {
        return (JFrame) SwingUtilities.getWindowAncestor(bvvh.getViewerPanel());
    }

    public static void setBvvHandleCloseOperation( BvvHandle bvvh, CacheService cs, ObjectService os, SourceAndConverterBdvDisplayService bdvsds, boolean putWindowOnTop) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(bvvh.getViewerPanel());

        topFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);

                os.removeObject(bvvh);
                //bdvsds.closeBdv(bdvh);
                //e.getWindow().dispose();
                /*if (Recorder.record) {
                    // run("Select Bdv Window", "bdvh=bdv.util.BdvHandleFrame@e6c7718");
                    String cmdrecord = "run(\"Close Bdv Window\", \"bdvh=" + getWindowTitle(bdvh) + "\");\n";
                    Recorder.recordString(cmdrecord);
                }*/
            }

            @Override
            public void windowActivated(WindowEvent e) {
                super.windowActivated(e);
                cs.put("LAST_ACTIVE_BVVH", new WeakReference<>(bvvh));
                // Very old school
                /*if (Recorder.record) {
                    // run("Select Bdv Window", "bdvh=bdv.util.BdvHandleFrame@e6c7718");
                    String cmdrecord = "run(\"Select Bdv Window\", \"bdvh=" + getWindowTitle(bdvh) + "\");\n";
                    Recorder.recordString(cmdrecord);
                }*/
            }
        });

        if (putWindowOnTop) {
            cs.put("LAST_ACTIVE_BVVH", new WeakReference<>(bvvh));// why a weak reference ? because we want to dispose the bdvhandle if it is closed
        }
    }

    public static void activateWindow(BvvHandle bvvh) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(bvvh.getViewerPanel());
        topFrame.toFront();
        topFrame.requestFocus();
    }

    public static void closeWindow(BvvHandle bvvh) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(bvvh.getViewerPanel());
        topFrame.dispatchEvent( new WindowEvent(topFrame, WindowEvent.WINDOW_CLOSING));
    }

    public static void setWindowTitle(BvvHandle bvvh, String title) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(bvvh.getViewerPanel());
        topFrame.setTitle(title);
    }

    public static String getWindowTitle(BvvHandle bvvh) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(bvvh.getViewerPanel());
        return topFrame.getTitle();
    }

    public static String getUniqueWindowTitle(ObjectService os, String iniTitle) {
        List<BvvHandle> bvvs = os.getObjects(BvvHandle.class);
        boolean duplicateExist;
        String uniqueTitle = iniTitle;
        duplicateExist = bvvs.stream().filter(bvv ->
                (bvv.toString().equals(iniTitle))||(getWindowTitle(bvv).equals(iniTitle)))
                .findFirst().isPresent();
        while (duplicateExist) {
            if (uniqueTitle.matches(".+(_)\\d+")) {
                int idx = Integer.valueOf(uniqueTitle.substring(uniqueTitle.lastIndexOf("_")+1));
                uniqueTitle = uniqueTitle.substring(0, uniqueTitle.lastIndexOf("_")+1);
                uniqueTitle += String.format("%02d", idx+1);
            } else {
                uniqueTitle+="_00";
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String uTTest = uniqueTitle;
            duplicateExist = bvvs.stream().filter(bvv ->
                    (bvv.toString().equals(uTTest))||(getWindowTitle(bvv).equals(uTTest)))
                    .findFirst().isPresent();
        }
        return uniqueTitle;
    }
}
