package sc.fiji.bdvpg.scijava.widget;

import bdv.viewer.SourceAndConverter;
import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.widget.SwingInputWidget;
import org.scijava.widget.InputWidget;
import org.scijava.widget.WidgetModel;
import sc.fiji.bdvpg.scijava.services.BdvSourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.BdvSourceServiceUI;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Swing implementation of {@link SourceAndConverterWidget}.
 *
 * @author Nicolas Chiaruttini
 */

@Plugin(type = InputWidget.class, priority = Priority.EXTREMELY_HIGH)
public class SwingSourceAndConverterWidget extends SwingInputWidget<SourceAndConverter> implements
        SourceAndConverterWidget<JPanel> {

    @Override
    protected void doRefresh() {
    }

    @Override
    public boolean supports(final WidgetModel model) {
        return super.supports(model) && model.isType(SourceAndConverter.class);
    }

    @Override
    public SourceAndConverter getValue() {
        return getSelectedSourceAndConverter();
    }

    @Parameter
    BdvSourceAndConverterService bss;

    JTree tree;

    public SourceAndConverter getSelectedSourceAndConverter() {
        ArrayList<SourceAndConverter> sacList = new ArrayList<>(); // A set avoids duplicate SourceAndConverter
        for (TreePath tp : tree.getSelectionModel().getSelectionPaths()) {
            if (((DefaultMutableTreeNode) tp.getLastPathComponent()).getUserObject() instanceof BdvSourceServiceUI.RenamableSourceAndConverter) {
                Object userObj = ((BdvSourceServiceUI.RenamableSourceAndConverter) ((DefaultMutableTreeNode) tp.getLastPathComponent()).getUserObject()).sac;
                sacList.add((SourceAndConverter) userObj);
            } else {
                sacList.addAll(getSourceAndConvertersFromChildrenOf((DefaultMutableTreeNode) tp.getLastPathComponent()));
            }
        }
        if (sacList.size()>0) {
            return sacList.get(0);
        } else {
            return null;
        }
    }

    private Set<SourceAndConverter> getSourceAndConvertersFromChildrenOf(DefaultMutableTreeNode node) {
        Set<SourceAndConverter> sacs = new HashSet<>();
        for (int i=0;i<node.getChildCount();i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            if (child.getUserObject() instanceof BdvSourceServiceUI.RenamableSourceAndConverter) {
                Object userObj = ((BdvSourceServiceUI.RenamableSourceAndConverter) (child.getUserObject())).sac;
                sacs.add((SourceAndConverter) userObj);
            } else {
                sacs.addAll(getSourceAndConvertersFromChildrenOf(child));
            }
        }
        return sacs;
    }

    @Override
    public void set(final WidgetModel model) {
        super.set(model);
        tree = new JTreeLeavesOnlySelectable(bss.getUI().getTreeModel());
        // Only one node selected (needs to be a leaf
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(350, 200));
        getComponent().add(scrollPane);
        refreshWidget();
        model.setValue(null);
        tree.addTreeSelectionListener((e)-> model.setValue(getValue()));
    }

    public class JTreeLeavesOnlySelectable extends JTree {

        JTreeLeavesOnlySelectable(TreeModel model) {
            super(model);
        }

        public boolean isPathSelected( TreePath path )
        {
            return ((DefaultMutableTreeNode )path.getLastPathComponent()).isLeaf();
        }
    }

}
