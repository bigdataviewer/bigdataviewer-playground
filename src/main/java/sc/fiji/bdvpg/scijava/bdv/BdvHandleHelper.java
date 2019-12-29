package sc.fiji.bdvpg.scijava.bdv;

import bdv.util.BdvHandle;
import ij.plugin.frame.Recorder;
import org.scijava.cache.CacheService;
import org.scijava.object.ObjectService;
import sc.fiji.bdvpg.scijava.services.BdvSourceDisplayService;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.ref.WeakReference;
import java.util.List;

public class BdvHandleHelper {

    public static void setBdvHandleCloseOperation(BdvHandle bdvh, CacheService cs, BdvSourceDisplayService bdvsds, boolean putWindowOnTop) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(bdvh.getViewerPanel());

        topFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                bdvsds.closeBdv(bdvh);
                e.getWindow().dispose();
                /*if (Recorder.record) {
                    // run("Select Bdv Window", "bdvh=bdv.util.BdvHandleFrame@e6c7718");
                    String cmdrecord = "run(\"Close Bdv Window\", \"bdvh=" + getWindowTitle(bdvh) + "\");\n";
                    Recorder.recordString(cmdrecord);
                }*/
            }

            @Override
            public void windowActivated(WindowEvent e) {
                super.windowActivated(e);
                cs.put("LAST_ACTIVE_BDVH", new WeakReference<>(bdvh));
                // Very old school
                /*if (Recorder.record) {
                    // run("Select Bdv Window", "bdvh=bdv.util.BdvHandleFrame@e6c7718");
                    String cmdrecord = "run(\"Select Bdv Window\", \"bdvh=" + getWindowTitle(bdvh) + "\");\n";
                    Recorder.recordString(cmdrecord);
                }*/
            }
        });

        if (putWindowOnTop) {
            cs.put("LAST_ACTIVE_BDVH", new WeakReference<>(bdvh));// why a weak reference ? because we want to dispose the bdvhandle if it is closed
        }
    }

    public static void activateWindow(BdvHandle bdvh) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(bdvh.getViewerPanel());
        topFrame.toFront();
        topFrame.requestFocus();
    }

    public static void closeWindow(BdvHandle bdvh) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(bdvh.getViewerPanel());
        topFrame.dispatchEvent( new WindowEvent(topFrame, WindowEvent.WINDOW_CLOSING));
    }

    public static void setWindowTitle(BdvHandle bdvh, String title) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(bdvh.getViewerPanel());
        topFrame.setTitle(title);
    }

    public static String getWindowTitle(BdvHandle bdvh) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(bdvh.getViewerPanel());
        return topFrame.getTitle();
    }

    public static String getUniqueWindowTitle(ObjectService os, String iniTitle) {
        List<BdvHandle> bdvs = os.getObjects(BdvHandle.class);
        boolean duplicateExist;
        String uniqueTitle = iniTitle;
        duplicateExist = bdvs.stream().filter(bdv ->
                (bdv.toString().equals(iniTitle))||(getWindowTitle(bdv).equals(iniTitle)))
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
            duplicateExist = bdvs.stream().filter(bdv ->
                    (bdv.toString().equals(uTTest))||(getWindowTitle(bdv).equals(uTTest)))
                    .findFirst().isPresent();
        }
        return uniqueTitle;
    }
}
