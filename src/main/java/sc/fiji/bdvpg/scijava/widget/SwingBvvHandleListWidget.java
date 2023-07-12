/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2023 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

import bvv.vistools.BvvHandle;
import org.scijava.Priority;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.widget.SwingInputWidget;
import org.scijava.widget.InputWidget;
import org.scijava.widget.WidgetModel;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.Dimension;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Swing implementation of {@link BvvHandleListWidget}.
 *
 * @author Nicolas Chiaruttini
 */

@SuppressWarnings("unused")
@Plugin(type = InputWidget.class, priority = Priority.EXTREMELY_HIGH)
public class SwingBvvHandleListWidget extends SwingInputWidget<BvvHandle[]>
	implements BvvHandleListWidget<JPanel>
{

	@Override
	protected void doRefresh() {}

	@Override
	public boolean supports(final WidgetModel model) {
		return super.supports(model) && model.isType(BvvHandle[].class);
	}

	@Override
	public BvvHandle[] getValue() {
		return getSelectedBvvHandles();
	}

	JList<SwingBvvHandleWidget.RenamableBvvHandle> list;

	public BvvHandle[] getSelectedBvvHandles() {
		List<SwingBvvHandleWidget.RenamableBvvHandle> selected = list
			.getSelectedValuesList();
		return selected.stream().map((e) -> e.bvvh).collect(Collectors.toList())
			.toArray(new BvvHandle[selected.size()]);
	}

	@Parameter
	ObjectService os;

	@Override
	public void set(final WidgetModel model) {
		super.set(model);
		SwingBvvHandleWidget.RenamableBvvHandle[] data = os.getObjects(
			BvvHandle.class).stream().map(
				SwingBvvHandleWidget.RenamableBvvHandle::new).toArray(
					SwingBvvHandleWidget.RenamableBvvHandle[]::new);
		list = new JList<>(data); // data has type Object[]
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		JScrollPane listScroller = new JScrollPane(list);
		listScroller.setPreferredSize(new Dimension(250, 80));
		list.addListSelectionListener((e) -> model.setValue(getValue()));
		getComponent().add(listScroller);
	}

}
