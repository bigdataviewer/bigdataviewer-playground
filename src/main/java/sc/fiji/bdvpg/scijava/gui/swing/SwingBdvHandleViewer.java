package sc.fiji.bdvpg.scijava.gui.swing;

import bdv.util.BdvHandle;
import bdv.viewer.state.SourceState;
import net.imglib2.Volatile;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.viewer.EasySwingDisplayViewer;
import org.scijava.ui.viewer.DisplayViewer;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Plugin(type = DisplayViewer.class)
public class SwingBdvHandleViewer extends
        EasySwingDisplayViewer<BdvHandle> {

    @Parameter
    CommandService cmds;

    public SwingBdvHandleViewer()
    {
        super( BdvHandle.class );
    }

    @Override
    protected boolean canView(BdvHandle bdv_h) {
        return true;
    }

    @Override
    protected void redoLayout() {

    }

    @Override
    protected void setLabel(String s) {

    }

    @Override
    protected void redraw() {
        // Needs to update the display
        textInfo.setText(bdv_h.toString());//BdvHandleHelper.getWindowTitle(bdv_h)+":"+bdv_h.toString());
        nameLabel.setText(bdv_h.toString());//BdvHandleHelper.getWindowTitle(bdv_h)+":"+bdv_h.toString());
        DefaultListModel<SourceState<?>> listModel = new DefaultListModel();
        bdv_h.getViewerPanel().getState().getSources().forEach(src -> {
            listModel.addElement(src);
        });
        listOfSources.setModel(listModel);
    }

    BdvHandle bdv_h = null;

    JPanel panelInfo;
    JLabel nameLabel;
    JTextArea textInfo;

    JList<SourceState<?>> listOfSources;

    public List<Integer> getSelectedIds() {
        HashSet<Integer> selectedIndexes = new HashSet<>();
        for (int i:listOfSources.getSelectedIndices()) {
            selectedIndexes.add(i);
        }
        return selectedIndexes.stream().sorted().collect(Collectors.toList());
    }

    public String updateSelectedViewSetupsIds(List<Integer> orderedVSIds) {
        String betterListOfIndexes = "";
        if (orderedVSIds.size()>0) {
            int lastIndex = orderedVSIds.get(0);
            betterListOfIndexes = betterListOfIndexes+lastIndex;
            boolean buildingRange = false;
            for (int i=1;i<orderedVSIds.size();i++) {
                int nextIndex = orderedVSIds.get(i);
                if (nextIndex==lastIndex+1) {
                    if (buildingRange) {
                        lastIndex = nextIndex;
                    } else {
                        betterListOfIndexes = betterListOfIndexes+":";
                        lastIndex = nextIndex;
                        buildingRange = true;
                    }
                } else {
                    if (buildingRange) {
                        betterListOfIndexes = betterListOfIndexes + lastIndex+","+nextIndex;
                        buildingRange=false;
                        lastIndex = nextIndex;
                    } else {
                        betterListOfIndexes = betterListOfIndexes +","+nextIndex;
                        lastIndex = nextIndex;
                    }
                }
            }
            if (buildingRange) {
                betterListOfIndexes = betterListOfIndexes+orderedVSIds.get(orderedVSIds.size()-1);
            }
        }
        //textAreaMessage.setText(betterListOfIndexes);
        return betterListOfIndexes;
    }

    public Map<String, Object> getPreFilledParameters() {
        Map<String, Object> out = new HashMap<>();
        out.put("sourceIndexString", updateSelectedViewSetupsIds(getSelectedIds()));
        out.put("bdvh", bdv_h);
        return out;
    }

    @Override
    protected JPanel createDisplayPanel(BdvHandle bdv_h) {
        this.bdv_h = bdv_h;
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panelInfo = new JPanel();
        panel.add(panelInfo, BorderLayout.CENTER);
        nameLabel = new JLabel(bdv_h.toString());//BdvHandleHelper.getWindowTitle(bdv_h));
        panel.add(nameLabel, BorderLayout.NORTH);
        textInfo = new JTextArea();
        textInfo.setEditable(false);

        DefaultListModel<SourceState<?>> listModel = new DefaultListModel();
        bdv_h.getViewerPanel().getState().getSources().forEach(src -> {
           listModel.addElement(src);
        });

        listOfSources = new JList(listModel);

        listOfSources.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        /*final JPopupMenu popup = (new SwingBdvPopupMenu(cmds, () -> getPreFilledParameters())).getPopup();

        listOfSources.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (SwingUtilities.isRightMouseButton(e)) {
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }

                if (e.getClickCount()==2 && !e.isConsumed()) {
                    e.consume();
                    cmds.run(BdvWindowTranslateOnSource.class, true,
                            "bdvh", bdv_h,
                            "sourceIndex", listOfSources.locationToIndex(new Point(e.getX(), e.getY())));
                }
            }
        });*/

        listOfSources.addListSelectionListener(e -> updateSelectedViewSetupsIds(getSelectedIds()));

        listOfSources.setLayoutOrientation(JList.VERTICAL);
        listOfSources.setVisibleRowCount(-1);

        SourceStateCellRenderer sscr = new SourceStateCellRenderer();
        listOfSources.setCellRenderer(sscr);


        JScrollPane listScroller = new JScrollPane(listOfSources);
        listScroller.setPreferredSize(new Dimension(250, 300));
        panelInfo.setLayout(new BorderLayout());
        panelInfo.add(listScroller, BorderLayout.CENTER);

        this.redraw();

        return panel;
    }

    class SourceStateCellRenderer extends JLabel implements ListCellRenderer<SourceState<?>> {
        public SourceStateCellRenderer() {
            setOpaque(true);
            setHorizontalAlignment(CENTER);
            setVerticalAlignment(CENTER);
        }
        @Override
        public Component getListCellRendererComponent(JList<? extends SourceState<?>> list, SourceState<?> value, int index, boolean isSelected, boolean cellHasFocus) {

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            if (value.getSpimSource().getType() instanceof Volatile) {
                if (value.getSpimSource().getName().endsWith("(Volatile)")) {
                    setText(index+":"+value.getSpimSource().getName());
                } else {
                    setText(index+":"+value.getSpimSource().getName() + " (Volatile)");
                }
            } else {
                setText(index+":"+value.getSpimSource().getName());
            }

            return this;
        }
    }

}