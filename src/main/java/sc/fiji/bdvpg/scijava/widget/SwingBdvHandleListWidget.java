package sc.fiji.bdvpg.scijava.widget;

import bdv.util.BdvHandle;
import org.scijava.Priority;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.widget.SwingInputWidget;
import org.scijava.widget.InputWidget;
import org.scijava.widget.WidgetModel;
import sc.fiji.bdvpg.scijava.BdvHandleHelper;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Swing implementation of {@link BdvHandleListWidget}.
 *
 * @author Nicolas Chiaruttini
 */

@Plugin(type = InputWidget.class, priority = Priority.EXTREMELY_HIGH)
public class SwingBdvHandleListWidget extends SwingInputWidget<BdvHandle[]> implements
        BdvHandleListWidget<JPanel> {

    @Override
    protected void doRefresh() {
    }

    @Override
    public boolean supports(final WidgetModel model) {
        return super.supports(model) && model.isType(BdvHandle[].class);
    }

    @Override
    public BdvHandle[] getValue() {
        return getSelectedBdvHandles();
    }

    JList list;

    public BdvHandle[] getSelectedBdvHandles() {
        List<RenamableBdvHandle> selected = list.getSelectedValuesList();
        return  selected.stream().map((e) -> e.bdvh)
                .collect(Collectors.toList()).toArray(new BdvHandle[selected.size()]);
    }

    @Parameter
    ObjectService os;

    @Override
    public void set(final WidgetModel model) {
        super.set(model);
        List<RenamableBdvHandle> bdvhs = os.getObjects(BdvHandle.class).stream().map(bdvh -> new RenamableBdvHandle(bdvh)).collect(Collectors.toList());
        RenamableBdvHandle[] data = bdvhs.toArray(new RenamableBdvHandle[bdvhs.size()]);
        list = new JList(data); //data has type Object[]
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
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
