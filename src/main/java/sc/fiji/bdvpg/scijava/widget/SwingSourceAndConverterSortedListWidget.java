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
package sc.fiji.bdvpg.scijava.widget;

import bdv.viewer.SourceAndConverter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.widget.SwingInputWidget;
import org.scijava.widget.InputWidget;
import org.scijava.widget.WidgetModel;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.RenamableSourceAndConverter;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterTreeCellRenderer;
import sc.fiji.bdvpg.scijava.services.ui.swingdnd.JListTransferHandler;
import sc.fiji.bdvpg.scijava.services.ui.swingdnd.SourceAndConverterServiceUITransferHandler;

import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

/**
 * Swing implementation of {@link SourceAndConverterListWidget}
 *
 * This widget allows the user to control the order of the sources
 * that are selected, contrary to the other widget {@link SwingSourceAndConverterListWidget}
 *
 * @author Nicolas Chiaruttini
 */

@Plugin(type = InputWidget.class)
public class SwingSourceAndConverterSortedListWidget extends SwingInputWidget<SourceAndConverter<?>[]> implements
        SourceAndConverterListWidget<JPanel> {

    @Override
    protected void doRefresh() {
    }

    @Override
    public boolean supports(final WidgetModel model) {
        return (model.getItem().getWidgetStyle().contains("sorted")) && (super.supports(model)) && (model.isType(SourceAndConverter[].class));
    }

    @Override
    public SourceAndConverter<?>[] getValue() {
        return getSelectedSourceAndConverters();
    }

    @Parameter
	SourceAndConverterService bss;

    public SourceAndConverter<?>[] getSelectedSourceAndConverters() {
       int nSources = sortedSourceList.getModel().getSize();
       SourceAndConverter<?>[] selection = new SourceAndConverter[nSources];
       for (int i=0; i<nSources; i++) {
           selection[i] = (sortedSourceList.getModel().getElementAt(i)).sac;
       }
       return selection;
    }

    JTree tree;
    JList<RenamableSourceAndConverter> sortedSourceList;

    @Override
    public void set(final WidgetModel model) {
        super.set(model);
        getComponent().setLayout(new BorderLayout());
        JPanel innerPanel = new JPanel(new BorderLayout());
        innerPanel.setLayout(new GridLayout(0,2));
        getComponent().add(innerPanel, BorderLayout.CENTER);
        tree = new JTree(bss.getUI().getTreeModel());
        tree.setCellRenderer(new SourceAndConverterTreeCellRenderer());
        tree.setDragEnabled(true);
        tree.setTransferHandler(new SourceAndConverterServiceUITransferHandler());
        JScrollPane scrollPaneTree = new JScrollPane(tree);
        scrollPaneTree.setPreferredSize(new Dimension(350, 200));
        innerPanel.add(scrollPaneTree);
        model.setValue(null);

        sortedSourceList = new JList<>();
        sortedSourceList.setDropMode(DropMode.INSERT);
        sortedSourceList.setModel(new CustomSourceListModel());
        sortedSourceList.setDragEnabled(true);
        sortedSourceList.setTransferHandler(new JListTransferHandler());
        innerPanel.add(new JScrollPane(sortedSourceList));

        JLabel label = new JLabel("Drag and Drop sources from left to right");
        getComponent().add(label, BorderLayout.NORTH);
        JButton clearButton = new JButton("Clear selection");
        clearButton.addActionListener((e) -> ((DefaultListModel) sortedSourceList.getModel()).clear());
        getComponent().add(clearButton, BorderLayout.SOUTH);

        ListDataListener ldl = new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                model.setValue(getValue());
            }
            @Override
            public void intervalRemoved(ListDataEvent e) {
                model.setValue(getValue());

            }
            @Override
            public void contentsChanged(ListDataEvent e) {
                model.setValue(getValue());
            }
        };

        sortedSourceList.getModel().addListDataListener(ldl);
        refreshWidget();
        // Memory leak... How ot solve this ?

        // -------------------------------- Memory leak! Cut heads of the Hydra of Lerna
        // The part below helps solve the memory leak:
        // with JTree not released the lastly selected path
        // with Listeners holding references with objects of potentially big memory footprint (SourceAndConverters)
        // Maybe related:
        // https://bugs.openjdk.java.net/browse/JDK-6472844
        // https://stackoverflow.com/questions/4517931/java-swing-jtree-is-not-garbage-collected
        // this one more particularly :

        sortedSourceList.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {

            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                sortedSourceList.clearSelection();
                sortedSourceList.getModel().removeListDataListener(ldl);
                DefaultListModel model = (DefaultListModel) sortedSourceList.getModel();
                model.clear();
                sortedSourceList.resetKeyboardActions();
                sortedSourceList.removeAncestorListener(this);
                sortedSourceList = null;
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {

            }
        });

        tree.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                tree.clearSelection();
                tree.cancelEditing();
                tree.resetKeyboardActions();
                tree.updateUI();
                scrollPaneTree.remove(tree);
                getComponent().remove(scrollPaneTree);
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

    static class CustomSourceListModel extends DefaultListModel {
        @Override
        public void add(int index, Object element) {
            if (element instanceof SourceAndConverter) {
                super.add(index, new RenamableSourceAndConverter((SourceAndConverter) element));
            }
        }
    }

}
