package sc.fiji.bdvpg.scijava.services.ui;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.net.URL;

public class SourceAndConverterTreeCellRenderer extends DefaultTreeCellRenderer {

    private static final String SPAN_FORMAT = "<span style='color:%s'>%s</span>";

    //static ImageIcon sourceIcon;
    static ImageIcon source2d;
    static ImageIcon source2dwarped;

    static ImageIcon source3d;
    static ImageIcon source3dwarped;

    static ImageIcon sourceFilterNode;

    static {
        URL iconSourceURL;
        iconSourceURL = SourceAndConverterTreeCellRenderer.class.getResource("/images/source2d.png");
        source2d = new ImageIcon(iconSourceURL);
        iconSourceURL = SourceAndConverterTreeCellRenderer.class.getResource("/images/source3d.png");
        source3d = new ImageIcon(iconSourceURL);
        iconSourceURL = SourceAndConverterTreeCellRenderer.class.getResource("/images/sourceFilterNodeCentered.png");
        sourceFilterNode = new ImageIcon(iconSourceURL);
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
            setIcon(source3d);
            String text = String.format(SPAN_FORMAT, "rgb(80,80,80)", getText());
            setText("<html>"+text+"</html>");
            setToolTipText("Source node");
        } else if (value instanceof SourceFilterNode) {
            setIcon(sourceFilterNode);
            String text = String.format(SPAN_FORMAT, "rgb(0,0,0)", getText()+" ["+((SourceFilterNode)value).currentOutputSacs.size()+"]");
            setText("<html>"+text+"</html>");
            setToolTipText("Filter node");
        }

        return this;
    }

    protected boolean isSourceAndConverterNode(Object value) {
        DefaultMutableTreeNode node =
                (DefaultMutableTreeNode)value;
        return node.getUserObject() instanceof RenamableSourceAndConverter;
    }

}
