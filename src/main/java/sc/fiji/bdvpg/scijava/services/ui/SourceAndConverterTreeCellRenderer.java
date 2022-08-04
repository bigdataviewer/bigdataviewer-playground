/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
    //static ImageIcon source2dwarped;

    static ImageIcon source3d;
    //static ImageIcon source3dwarped;

    static ImageIcon sourceFilterNode;

    static {
        URL iconSourceURL;
        iconSourceURL = SourceAndConverterTreeCellRenderer.class.getResource("/images/source2d.png");
        assert iconSourceURL != null;
        source2d = new ImageIcon(iconSourceURL);
        iconSourceURL = SourceAndConverterTreeCellRenderer.class.getResource("/images/source3d.png");
        source3d = new ImageIcon(iconSourceURL);
        iconSourceURL = SourceAndConverterTreeCellRenderer.class.getResource("/images/sourceFilterNodeCentered.png");
        assert iconSourceURL != null;
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
