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
package sc.fiji.bdvpg.scijava.widget;

import bdv.viewer.SourceAndConverter;
import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.widget.SwingInputWidget;
import org.scijava.widget.InputWidget;
import org.scijava.widget.WidgetModel;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.RenamableSourceAndConverter;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterTreeCellRenderer;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.TreeSelectionListener;
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
 * Note the rather complex {@link SwingSourceAndConverterListWidget#set} method to avoid memory leak
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
	SourceAndConverterService bss;

    JTree tree;

    public SourceAndConverter getSelectedSourceAndConverter() {
        ArrayList<SourceAndConverter> sacList = new ArrayList<>(); // A set avoids duplicate SourceAndConverter
        for (TreePath tp : tree.getSelectionModel().getSelectionPaths()) {
            if (((DefaultMutableTreeNode) tp.getLastPathComponent()).getUserObject() instanceof RenamableSourceAndConverter) {
                Object userObj = ((RenamableSourceAndConverter) ((DefaultMutableTreeNode) tp.getLastPathComponent()).getUserObject()).sac;
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
            if (child.getUserObject() instanceof RenamableSourceAndConverter) {
                Object userObj = ((RenamableSourceAndConverter) (child.getUserObject())).sac;
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
        tree.setCellRenderer(new SourceAndConverterTreeCellRenderer());
        // Only one node selected (needs to be a leaf
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(350, 200));
        getComponent().add(scrollPane);
        refreshWidget();
        model.setValue(null);
        TreeSelectionListener tsl = (e)-> model.setValue(getValue());
        tree.addTreeSelectionListener(tsl);

        // -------------------------------- Memory leak! Cut heads of the Hydra of Lerna
        // The part below helps solve the memory leak:
        // with JTree not released the lastly selected path
        // with Listeners holding references with objects of potentially big memory footprint (SourceAndConverters)

        tree.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                tree.removeTreeSelectionListener(tsl);
                tree.clearSelection();
                tree.cancelEditing();
                //tree.clearToggledPaths();
                tree.resetKeyboardActions();
                tree.updateUI();
                scrollPane.remove(tree);
                getComponent().remove(scrollPane);
                tree.setModel(null);
                tree.removeAncestorListener(this);
                tree = null;
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
            }
        });
        // -------------------------------- All heads cut (hopefully)
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
