package sc.fiji.bdvpg;

import bigwarp.BigWarp;
import ij.IJ;
import net.imagej.ImageJ;
import org.scijava.util.DefaultTreeNode;
import org.scijava.util.VersionUtils;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterServiceUI;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import static sc.fiji.bdvpg.BigWarpDemo.demo2d;

public class DemoTreeSourcesManipulation {
    public static void main(String... args) {
        // Initializes static SourceService and Display Service
        ImageJ ij = new ImageJ();
        ij.ui().showUI();
        System.out.println("BigWarp version:"+ VersionUtils.getVersion(BigWarp.class));
        demo2d();

        SourceAndConverterServiceUI treeUI = ij.get(SourceAndConverterService.class).getUI();

        DefaultTreeModel model = treeUI.getTreeModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeUI.getTreeModel().getRoot();

        int nChildren = model.getChildCount(root);
        IJ.log("There are "+nChildren+" children in the root node");

        // Easier interface:
        SourceAndConverterServiceUI.Node r = treeUI.getRoot();

        IJ.log("There are "+r.sources().length+" sources in the whole tree.");
        IJ.log("The node "+r+" has "+r.children().size()+" children");
        IJ.log("Their names are:");
        r.children().forEach(n -> IJ.log("- "+n.name()+" | path = "+n.path()));
        IJ.log("nSources = "+r.child("src/test/resources/demoSlice.xml").sources().length);
        ;
    }
}
