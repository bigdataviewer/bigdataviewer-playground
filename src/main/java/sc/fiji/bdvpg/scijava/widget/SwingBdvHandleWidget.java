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

import bdv.util.BdvHandle;
import org.scijava.Priority;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.widget.SwingInputWidget;
import org.scijava.widget.InputWidget;
import org.scijava.widget.WidgetModel;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

/**

 */

@Plugin(type = InputWidget.class, priority = Priority.EXTREMELY_HIGH)
public class SwingBdvHandleWidget extends SwingInputWidget<BdvHandle> implements
        BdvHandleWidget<JPanel> {

    @Override
    protected void doRefresh() {
    }

    @Override
    public boolean supports(final WidgetModel model) {
        return super.supports(model) && model.isType(BdvHandle.class);
    }

    @Override
    public BdvHandle getValue() {
        return getSelectedBdvHandle();
    }

    @Parameter
    SourceAndConverterService sourceAndConverterService;

    JList list;

    public BdvHandle getSelectedBdvHandle() {
        return ((RenamableBdvHandle) list.getSelectedValue()).bdvh;
    }

    @Parameter
    ObjectService os;

    @Override
    public void set(final WidgetModel model) {
        super.set(model);
        List<RenamableBdvHandle> bdvhs = os.getObjects(BdvHandle.class).stream().map(bdvh -> new RenamableBdvHandle(bdvh)).collect(Collectors.toList());
        RenamableBdvHandle[] data = bdvhs.toArray(new RenamableBdvHandle[bdvhs.size()]);
        list = new JList(data); //data has type Object[]
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setPreferredSize(new Dimension(250, 80));
        list.addListSelectionListener((e)-> model.setValue(getValue()));
        getComponent().add(listScroller);
    }

    public class RenamableBdvHandle {

        public BdvHandle bdvh;

        public RenamableBdvHandle(BdvHandle bdvh) {
            this.bdvh = bdvh;
        }

        public String toString() {
            return BdvHandleHelper.getWindowTitle(bdvh);
        }

    }

}
