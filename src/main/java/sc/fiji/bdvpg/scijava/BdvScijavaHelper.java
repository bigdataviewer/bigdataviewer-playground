package sc.fiji.bdvpg.scijava;

import bdv.util.BdvHandle;
import bdv.util.BdvHandleFrame;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class that can be used to put a scijava Command into the menu bar of a BdvHandle
 */

public class BdvScijavaHelper {

    static public void clearBdvHandleMenuBar(BdvHandle bdvh) {
        if (bdvh instanceof BdvHandleFrame) {
            final JMenuBar bdvMenuBar = ( ( BdvHandleFrame ) bdvh ).getBigDataViewer().getViewerFrame().getJMenuBar();
            int componentsNumber = bdvMenuBar.getComponentCount();
            for (int i=0; i<componentsNumber; i++) {
                bdvMenuBar.remove(0);
            }
            bdvMenuBar.revalidate();
            bdvMenuBar.updateUI();
        }
    }

    // https://github.com/scijava/scijava-ui-swing/blob/557d79230d0f70eaae8573e242f0a02c66b07d62/src/main/java/org/scijava/ui/swing/AbstractSwingUI.java
    static public void addCommandToBdvHandleMenu(BdvHandle bdvh, Context ctx, Class<? extends Command> commandClass, int skipTopLevels, Object... args) {
        Plugin plugin = commandClass.getDeclaredAnnotation(Plugin.class);
        addActionToBdvHandleMenu(bdvh,plugin.menuPath(), skipTopLevels, () -> ctx.getService(CommandService.class).run(commandClass, true, args));

    }

    static public void addActionToBdvHandleMenu(BdvHandle bdvh, String pathHierarchy, int skipTopLevels, Runnable runnable) {
        if (bdvh instanceof BdvHandleFrame) {
            final JMenuBar bdvMenuBar = ( ( BdvHandleFrame ) bdvh ).getBigDataViewer().getViewerFrame().getJMenuBar();
            List<String> path = Arrays.asList(pathHierarchy.split(">"))
                    .stream()
                    .map(String::trim)
                    .collect(Collectors.toList());

            for (int i=0;i<skipTopLevels;i++) {path.remove(0);}

            JMenuItem jmenuItemRoot = findOrCreateJMenu(bdvMenuBar, path);
            final JMenuItem jMenuItem = new JMenuItem( path.get(path.size()-1));
            jMenuItem.addActionListener(e -> runnable.run());
            jmenuItemRoot.add( jMenuItem );
            bdvMenuBar.updateUI();
        } else {
            System.err.println(BdvScijavaHelper.class.getName()+" : Cannot put command on menu : the bdvhandle is not a frame.");
        }
    }

    private static JMenu findOrCreateJMenu(JMenuBar bdvMenuBar, List<String> path) {
        if (path.size()==0) {
            System.err.println(BdvScijavaHelper.class+": No Path specified!");
        }
        if (path.size()==1) {
            // TODO or not ? It means the action is on the top level window
            return null;
        }
        boolean found = false;
        int idx = 0;
        while ((idx<bdvMenuBar.getMenuCount())&&(!found)) {
            JMenu jmenuTest = bdvMenuBar.getMenu(idx);
            if (jmenuTest.getText().equals(path.get(0))) {
                found = true;
            } else {
                idx++;
            }
        }
        JMenu jmenu;
        if (found) {
            jmenu = bdvMenuBar.getMenu(idx);
        } else {
            jmenu = new JMenu( path.get(0) );
        }
        bdvMenuBar.add(jmenu);
        path.remove(0);
        return findOrCreateJMenu(jmenu, path);
    }

    private static JMenu findOrCreateJMenu(JMenu jMenu, List<String> path) {
        if (path.size()==0) {
            System.err.println(BdvScijavaHelper.class+": Reached unreachable statement !");
        }
        if (path.size()==1) {
            return jMenu;
        }
        boolean found = false;
        int idx = 0;
        while ((idx<jMenu.getMenuComponentCount())&&(!found)) {
            JMenu jmenuTest = (JMenu) jMenu.getMenuComponent(idx);
            if (jmenuTest.getText().equals(path.get(0))) {
                found = true;
            } else {
                idx++;
            }
        }
        JMenu jmenuFound;
        if (found) {
            jmenuFound = (JMenu) jMenu.getMenuComponent(idx);
        } else {
            jmenuFound = new JMenu( path.get(0) );
        }
        jMenu.add(jmenuFound);
        path.remove(0);
        return findOrCreateJMenu(jmenuFound, path);
    }
}
