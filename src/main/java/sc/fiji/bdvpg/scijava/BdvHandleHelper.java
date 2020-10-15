/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2020 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package sc.fiji.bdvpg.scijava;

import bdv.util.BdvHandle;
import org.scijava.cache.CacheService;
import org.scijava.object.ObjectService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.ref.WeakReference;
import java.util.List;

public class BdvHandleHelper {

    public static JFrame getJFrame(BdvHandle bdvh) {
        return (JFrame) SwingUtilities.getWindowAncestor(bdvh.getViewerPanel());
    }

    public static void setBdvHandleCloseOperation( BdvHandle bdvh, CacheService cs, SourceAndConverterBdvDisplayService bdvsds, boolean putWindowOnTop, Runnable runnable) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(bdvh.getViewerPanel());
        WindowAdapter wa;
        wa = new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                bdvsds.closeBdv(bdvh);
                if (runnable!=null) {
                    runnable.run();
                }
                topFrame.removeWindowListener(this); // Avoid memory leak
                e.getWindow().dispose();
                bdvh.close();
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
        };
        topFrame.addWindowListener(wa);

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
