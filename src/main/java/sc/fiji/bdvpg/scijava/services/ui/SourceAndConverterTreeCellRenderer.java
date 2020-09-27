package sc.fiji.bdvpg.scijava.services.ui;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.net.URL;

public class SourceAndConverterTreeCellRenderer extends DefaultTreeCellRenderer {

    static ImageIcon sourceIcon;

    static {
        URL iconSourceURL = SourceAndConverterTreeCellRenderer.class.getResource("/images/SourceIcon50px.png");
        System.out.println(iconSourceURL);
        sourceIcon = new ImageIcon(iconSourceURL);
    }

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        super.getTreeCellRendererComponent(
                tree, value, sel,
                expanded, leaf, row,
                hasFocus);
        /*if (leaf && isTutorialBook(value)) {
            setIcon(tutorialIcon);
            setToolTipText("This book is in the Tutorial series.");
        } else {
            setToolTipText(null); //no tool tip
        }*/
        if (isSourceAndConverterNode(value)) {
            setIcon(sourceIcon);
        }

        return this;
    }

    protected boolean isSourceAndConverterNode(Object value) {
        DefaultMutableTreeNode node =
                (DefaultMutableTreeNode)value;
        return node.getUserObject() instanceof RenamableSourceAndConverter;
    }
}
