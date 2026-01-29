/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2026 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.registration.ViewTransform;
import mpicbg.spim.data.sequence.TimePoint;
import net.imglib2.realtransform.AffineGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.scijava.command.CommandService;
import sc.fiji.bdvpg.scijava.command.spimdata.AddSpimDataTransformCommand;
import sc.fiji.bdvpg.scijava.command.spimdata.RemoveSpimDataTransformsCommand;
import sc.fiji.bdvpg.scijava.command.spimdata.SetSpimDataTransformsCommand;

/**
 * A viewer for SpimData transforms with configurable 3D dimensions.
 *
 * The data is a 3D cube:
 * - Sources (setups)
 * - Transform chain (transforms within each ViewRegistration)
 * - Timepoints
 *
 * The viewer allows configuring which dimension is shown as:
 * - Rows
 * - Columns
 * - Slider (third dimension)
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL
 */
public class SpimDataTransformViewer extends JFrame {

	protected static final Logger logger = LoggerFactory.getLogger(
		SpimDataTransformViewer.class);

	// Data structures
	private final List<SourceEntry> sourceEntries = new ArrayList<>();
	private final List<Integer> allTimepoints = new ArrayList<>();
	private int maxTransformChainLength = 0;
	private final SourceAndConverterService sacService;

	// UI components
	private JComboBox<Dimension> rowDimensionCombo;
	private JComboBox<Dimension> colDimensionCombo;
	private JComboBox<Dimension> sliderDimensionCombo;
	private JSlider dimensionSlider;
	private JLabel sliderLabel;
	private JTable transformTable;
	private TransformTableModel tableModel;

	// Current view configuration
	private Dimension rowDimension = Dimension.SOURCES;
	private Dimension colDimension = Dimension.TIMEPOINTS;
	private Dimension sliderDimension = Dimension.TRANSFORMS;
	private int currentSliderIndex = 0;

	/**
	 * Dimension options for the 3D data cube
	 */
	public enum Dimension {
		SOURCES("Sources"),
		TIMEPOINTS("Timepoints"),
		TRANSFORMS("Transform Chain");

		private final String displayName;

		Dimension(String displayName) {
			this.displayName = displayName;
		}

		@Override
		public String toString() {
			return displayName;
		}
	}

	/**
	 * Entry holding source info and its SpimData reference
	 */
	private static class SourceEntry {
		final SourceAndConverter<?> sac;
		final AbstractSpimData<?> spimData;
		final int setupId;
		final String name;

		SourceEntry(SourceAndConverter<?> sac, AbstractSpimData<?> spimData,
			int setupId)
		{
			this.sac = sac;
			this.spimData = spimData;
			this.setupId = setupId;
			this.name = sac.getSpimSource().getName();
		}
	}

	/**
	 * Creates a new transform viewer for the given sources.
	 *
	 * @param sacs array of SourceAndConverter to display
	 * @param sacService the SourceAndConverterService for metadata access
	 */
	public SpimDataTransformViewer(SourceAndConverter<?>[] sacs,
		SourceAndConverterService sacService)
	{
		super("SpimData Transform Viewer");
		this.sacService = sacService;

		// Collect valid sources (those with SpimData)
		collectSources(sacs, sacService);

		if (sourceEntries.isEmpty()) {
			JOptionPane.showMessageDialog(this,
				"No sources with SpimData found. Cannot display transforms.",
				"No SpimData Sources", JOptionPane.WARNING_MESSAGE);
			dispose();
			return;
		}

		// Collect all timepoints and max transform chain length
		collectTimepointsAndTransforms();

		// Build UI
		initializeUI();

		// Set up the window
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setSize(1000, 600);
		setLocationRelativeTo(null);
	}

	private void collectSources(SourceAndConverter<?>[] sacs,
		SourceAndConverterService sacService)
	{
		int excludedCount = 0;
		for (SourceAndConverter<?> sac : sacs) {
			Object metadata = sacService.getMetadata(sac,
				SourceAndConverterService.SPIM_DATA_INFO);
			if (metadata instanceof SourceAndConverterService.SpimDataInfo) {
				SourceAndConverterService.SpimDataInfo info =
					(SourceAndConverterService.SpimDataInfo) metadata;
				sourceEntries.add(new SourceEntry(sac, info.asd, info.setupId));
			}
			else {
				excludedCount++;
			}
		}

		if (excludedCount > 0) {
			logger.warn("{} source(s) excluded because they have no associated " +
				"SpimData", excludedCount);
		}
	}

	private void collectTimepointsAndTransforms() {
		// Collect all unique timepoints across all SpimData
		Map<Integer, Boolean> timepointSet = new HashMap<>();

		for (SourceEntry entry : sourceEntries) {
			List<TimePoint> tps = entry.spimData.getSequenceDescription()
				.getTimePoints().getTimePointsOrdered();
			for (TimePoint tp : tps) {
				timepointSet.put(tp.getId(), true);
			}

			// Find max transform chain length
			ViewRegistrations vrs = entry.spimData.getViewRegistrations();
			for (TimePoint tp : tps) {
				ViewRegistration vr = vrs.getViewRegistration(tp.getId(),
					entry.setupId);
				if (vr != null) {
					int chainLength = vr.getTransformList().size();
					maxTransformChainLength = Math.max(maxTransformChainLength,
						chainLength);
				}
			}
		}

		allTimepoints.addAll(timepointSet.keySet());
		allTimepoints.sort(Integer::compareTo);

		if (maxTransformChainLength == 0) {
			maxTransformChainLength = 1; // At least show something
		}
	}

	private void initializeUI() {
		setLayout(new BorderLayout());

		// Top panel with dimension configuration
		JPanel configPanel = createConfigPanel();
		add(configPanel, BorderLayout.NORTH);

		// Center panel with table
		tableModel = new TransformTableModel();
		transformTable = new JTable(tableModel);
		transformTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		transformTable.setDefaultRenderer(Object.class,
			new TransformCellRenderer());

		// Set row height to accommodate 4 lines (name + 3 matrix rows)
		int lineHeight = transformTable.getFontMetrics(transformTable.getFont())
			.getHeight();
		transformTable.setRowHeight(lineHeight * 4 + 8); // 4 lines + padding

		// Enable cell selection (rectangular selection with Ctrl/Shift modifiers)
		transformTable.setCellSelectionEnabled(true);

		// Add mouse listener for row selection when clicking on first column (row labels)
		transformTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int col = transformTable.columnAtPoint(e.getPoint());
				int row = transformTable.rowAtPoint(e.getPoint());
				if (col == 0 && row >= 0) {
					// Click on first column: select entire row
					int colCount = transformTable.getColumnCount();
					if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
						// Ctrl+click: add row to selection
						transformTable.addRowSelectionInterval(row, row);
						transformTable.addColumnSelectionInterval(0, colCount - 1);
					} else if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
						// Shift+click: extend selection to this row
						int anchorRow = transformTable.getSelectionModel().getAnchorSelectionIndex();
						if (anchorRow >= 0) {
							transformTable.setRowSelectionInterval(Math.min(anchorRow, row),
								Math.max(anchorRow, row));
							transformTable.setColumnSelectionInterval(0, colCount - 1);
						}
					} else {
						// Simple click: select only this row
						transformTable.setRowSelectionInterval(row, row);
						transformTable.setColumnSelectionInterval(0, colCount - 1);
					}
				}
			}
		});

		// Add mouse listener to table header for column selection
		transformTable.getTableHeader().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int col = transformTable.columnAtPoint(e.getPoint());
				if (col >= 0) {
					int rowCount = transformTable.getRowCount();
					if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
						// Ctrl+click: add column to selection
						transformTable.addColumnSelectionInterval(col, col);
						transformTable.addRowSelectionInterval(0, rowCount - 1);
					} else if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
						// Shift+click: extend selection to this column
						int anchorCol = transformTable.getColumnModel().getSelectionModel()
							.getAnchorSelectionIndex();
						if (anchorCol >= 0) {
							transformTable.setColumnSelectionInterval(Math.min(anchorCol, col),
								Math.max(anchorCol, col));
							transformTable.setRowSelectionInterval(0, rowCount - 1);
						}
					} else {
						// Simple click: select only this column
						transformTable.setColumnSelectionInterval(col, col);
						transformTable.setRowSelectionInterval(0, rowCount - 1);
					}
				}
			}
		});

		// Set column widths
		updateColumnWidths();

		// Add popup menu for transform operations
		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem setTransformsItem = new JMenuItem("Set Transforms...");
		setTransformsItem.addActionListener(e -> launchSetTransformsCommand());
		popupMenu.add(setTransformsItem);

		JMenuItem addTransformItem = new JMenuItem("Add Transform...");
		addTransformItem.addActionListener(e -> launchAddTransformCommand());
		popupMenu.add(addTransformItem);

		JMenuItem removeTransformsItem = new JMenuItem("Remove Transforms...");
		removeTransformsItem.addActionListener(e -> launchRemoveTransformsCommand());
		popupMenu.add(removeTransformsItem);

		transformTable.setComponentPopupMenu(popupMenu);

		JScrollPane scrollPane = new JScrollPane(transformTable);
		scrollPane.setHorizontalScrollBarPolicy(
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		add(scrollPane, BorderLayout.CENTER);

		// Bottom panel with info
		JPanel infoPanel = createInfoPanel();
		add(infoPanel, BorderLayout.SOUTH);
	}

	private JPanel createConfigPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.setBorder(BorderFactory.createTitledBorder("Dimension Configuration"));

		// Row dimension
		panel.add(new JLabel("Rows:"));
		rowDimensionCombo = new JComboBox<>(Dimension.values());
		rowDimensionCombo.setSelectedItem(rowDimension);
		rowDimensionCombo.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				onDimensionChanged();
			}
		});
		panel.add(rowDimensionCombo);

		panel.add(Box.createHorizontalStrut(20));

		// Column dimension
		panel.add(new JLabel("Columns:"));
		colDimensionCombo = new JComboBox<>(Dimension.values());
		colDimensionCombo.setSelectedItem(colDimension);
		colDimensionCombo.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				onDimensionChanged();
			}
		});
		panel.add(colDimensionCombo);

		panel.add(Box.createHorizontalStrut(20));

		// Slider dimension
		panel.add(new JLabel("Slider:"));
		sliderDimensionCombo = new JComboBox<>(Dimension.values());
		sliderDimensionCombo.setSelectedItem(sliderDimension);
		sliderDimensionCombo.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				onDimensionChanged();
			}
		});
		panel.add(sliderDimensionCombo);

		panel.add(Box.createHorizontalStrut(20));

		// Slider
		sliderLabel = new JLabel("Transform 0:");
		panel.add(sliderLabel);

		dimensionSlider = new JSlider(0, Math.max(0, maxTransformChainLength - 1),
			0);
		dimensionSlider.setMajorTickSpacing(1);
		dimensionSlider.setPaintTicks(true);
		dimensionSlider.setPaintLabels(true);
		dimensionSlider.addChangeListener(e -> {
			currentSliderIndex = dimensionSlider.getValue();
			updateSliderLabel();
			tableModel.fireTableDataChanged();
		});
		panel.add(dimensionSlider);

		panel.add(Box.createHorizontalStrut(20));

		// Refresh button
		JButton refreshButton = new JButton("Refresh");
		refreshButton.setToolTipText("Refresh table data (if transforms were modified externally)");
		refreshButton.addActionListener(e -> refreshData());
		panel.add(refreshButton);

		return panel;
	}

	/**
	 * Refreshes the table data. Call this if transforms were modified externally.
	 */
	public void refreshData() {
		// Recalculate max transform chain length (it might have changed)
		int oldMax = maxTransformChainLength;
		maxTransformChainLength = 0;

		for (SourceEntry entry : sourceEntries) {
			ViewRegistrations vrs = entry.spimData.getViewRegistrations();
			for (int tp : allTimepoints) {
				ViewRegistration vr = vrs.getViewRegistration(tp, entry.setupId);
				if (vr != null) {
					int chainLength = vr.getTransformList().size();
					maxTransformChainLength = Math.max(maxTransformChainLength, chainLength);
				}
			}
		}

		if (maxTransformChainLength == 0) {
			maxTransformChainLength = 1;
		}

		// Update slider if max changed
		if (oldMax != maxTransformChainLength) {
			dimensionSlider.setMaximum(Math.max(0, maxTransformChainLength - 1));
			if (currentSliderIndex >= maxTransformChainLength) {
				currentSliderIndex = maxTransformChainLength - 1;
				dimensionSlider.setValue(currentSliderIndex);
			}
			updateSliderLabel();
		}

		// Refresh table
		tableModel.fireTableStructureChanged();
		updateColumnWidths();
	}

	/**
	 * Launches the SetSpimDataTransformsCommand with ranges pre-filled from selection.
	 */
	private void launchSetTransformsCommand() {
		// Get selected cells
		int[] selectedRows = transformTable.getSelectedRows();
		int[] selectedCols = transformTable.getSelectedColumns();

		if (selectedRows.length == 0 || selectedCols.length == 0) {
			JOptionPane.showMessageDialog(this,
				"Please select cells in the table first.",
				"No Selection", JOptionPane.WARNING_MESSAGE);
			return;
		}

		// Compute ranges based on current dimension configuration
		Set<Integer> sourceIndices = new HashSet<>();
		Set<Integer> timepointIndices = new HashSet<>();
		Set<Integer> transformIndices = new HashSet<>();

		for (int row : selectedRows) {
			for (int col : selectedCols) {
				if (col == 0) continue; // Skip the row header column

				int colIdx = col - 1; // Adjust for header column
				int[] indices = tableModel.resolveIndices(row, colIdx, currentSliderIndex);

				if (indices[0] >= 0) sourceIndices.add(indices[0]);
				if (indices[1] >= 0) timepointIndices.add(indices[1]);
				if (indices[2] >= 0) transformIndices.add(indices[2]);
			}
		}

		// Get selected sources
		SourceAndConverter<?>[] selectedSacs = sourceIndices.stream()
			.filter(i -> i < sourceEntries.size())
			.map(i -> sourceEntries.get(i).sac)
			.toArray(SourceAndConverter[]::new);

		if (selectedSacs.length == 0) {
			JOptionPane.showMessageDialog(this,
				"No valid sources in selection.",
				"Invalid Selection", JOptionPane.WARNING_MESSAGE);
			return;
		}

		// Build range strings
		String tpRange = buildRangeString(timepointIndices.stream()
			.map(i -> allTimepoints.get(i))
			.collect(Collectors.toSet()));
		String trRange = buildRangeString(transformIndices);

		// Get CommandService and launch the command
		CommandService commandService = sacService.getContext()
			.getService(CommandService.class);

		if (commandService == null) {
			logger.error("CommandService not available");
			return;
		}

		// Run command with pre-filled values
		commandService.run(SetSpimDataTransformsCommand.class, true,
			"sacs", selectedSacs,
			"timepoint_range", tpRange,
			"transform_index_range", trRange);
	}

	/**
	 * Launches the AddSpimDataTransformCommand with timepoint range pre-filled from selection.
	 */
	private void launchAddTransformCommand() {
		// Get selected cells
		int[] selectedRows = transformTable.getSelectedRows();
		int[] selectedCols = transformTable.getSelectedColumns();

		if (selectedRows.length == 0 || selectedCols.length == 0) {
			JOptionPane.showMessageDialog(this,
				"Please select cells in the table first.",
				"No Selection", JOptionPane.WARNING_MESSAGE);
			return;
		}

		// Compute ranges based on current dimension configuration
		Set<Integer> sourceIndices = new HashSet<>();
		Set<Integer> timepointIndices = new HashSet<>();

		for (int row : selectedRows) {
			for (int col : selectedCols) {
				if (col == 0) continue; // Skip the row header column

				int colIdx = col - 1; // Adjust for header column
				int[] indices = tableModel.resolveIndices(row, colIdx, currentSliderIndex);

				if (indices[0] >= 0) sourceIndices.add(indices[0]);
				if (indices[1] >= 0) timepointIndices.add(indices[1]);
			}
		}

		// Get selected sources
		SourceAndConverter<?>[] selectedSacs = sourceIndices.stream()
			.filter(i -> i < sourceEntries.size())
			.map(i -> sourceEntries.get(i).sac)
			.toArray(SourceAndConverter[]::new);

		if (selectedSacs.length == 0) {
			JOptionPane.showMessageDialog(this,
				"No valid sources in selection.",
				"Invalid Selection", JOptionPane.WARNING_MESSAGE);
			return;
		}

		// Build timepoint range string
		String tpRange = buildRangeString(timepointIndices.stream()
			.map(i -> allTimepoints.get(i))
			.collect(Collectors.toSet()));

		// Get CommandService and launch the command
		CommandService commandService = sacService.getContext()
			.getService(CommandService.class);

		if (commandService == null) {
			logger.error("CommandService not available");
			return;
		}

		// Run command with pre-filled values
		commandService.run(AddSpimDataTransformCommand.class, true,
			"sacs", selectedSacs,
			"timepoint_range", tpRange);
	}

	/**
	 * Launches the RemoveSpimDataTransformsCommand with ranges pre-filled from selection.
	 */
	private void launchRemoveTransformsCommand() {
		// Get selected cells
		int[] selectedRows = transformTable.getSelectedRows();
		int[] selectedCols = transformTable.getSelectedColumns();

		if (selectedRows.length == 0 || selectedCols.length == 0) {
			JOptionPane.showMessageDialog(this,
				"Please select cells in the table first.",
				"No Selection", JOptionPane.WARNING_MESSAGE);
			return;
		}

		// Compute ranges based on current dimension configuration
		Set<Integer> sourceIndices = new HashSet<>();
		Set<Integer> timepointIndices = new HashSet<>();
		Set<Integer> transformIndices = new HashSet<>();

		for (int row : selectedRows) {
			for (int col : selectedCols) {
				if (col == 0) continue; // Skip the row header column

				int colIdx = col - 1; // Adjust for header column
				int[] indices = tableModel.resolveIndices(row, colIdx, currentSliderIndex);

				if (indices[0] >= 0) sourceIndices.add(indices[0]);
				if (indices[1] >= 0) timepointIndices.add(indices[1]);
				if (indices[2] >= 0) transformIndices.add(indices[2]);
			}
		}

		// Get selected sources
		SourceAndConverter<?>[] selectedSacs = sourceIndices.stream()
			.filter(i -> i < sourceEntries.size())
			.map(i -> sourceEntries.get(i).sac)
			.toArray(SourceAndConverter[]::new);

		if (selectedSacs.length == 0) {
			JOptionPane.showMessageDialog(this,
				"No valid sources in selection.",
				"Invalid Selection", JOptionPane.WARNING_MESSAGE);
			return;
		}

		// Build range strings
		String tpRange = buildRangeString(timepointIndices.stream()
			.map(i -> allTimepoints.get(i))
			.collect(Collectors.toSet()));
		String trRange = buildRangeString(transformIndices);

		// Get CommandService and launch the command
		CommandService commandService = sacService.getContext()
			.getService(CommandService.class);

		if (commandService == null) {
			logger.error("CommandService not available");
			return;
		}

		// Run command with pre-filled values
		commandService.run(RemoveSpimDataTransformsCommand.class, true,
			"sacs", selectedSacs,
			"timepoint_range", tpRange,
			"transform_index_range", trRange);
	}

	/**
	 * Builds a compact range string from a set of integers.
	 * Consecutive values are collapsed into ranges using ':'.
	 */
	private String buildRangeString(Set<Integer> values) {
		if (values.isEmpty()) return "0";

		List<Integer> sorted = values.stream().sorted().collect(Collectors.toList());

		StringBuilder sb = new StringBuilder();
		int rangeStart = sorted.get(0);
		int rangeEnd = rangeStart;

		for (int i = 1; i < sorted.size(); i++) {
			int val = sorted.get(i);
			if (val == rangeEnd + 1) {
				rangeEnd = val;
			} else {
				appendRange(sb, rangeStart, rangeEnd);
				sb.append(",");
				rangeStart = val;
				rangeEnd = val;
			}
		}
		appendRange(sb, rangeStart, rangeEnd);

		return sb.toString();
	}

	private void appendRange(StringBuilder sb, int start, int end) {
		if (start == end) {
			sb.append(start);
		} else {
			sb.append(start).append(":").append(end);
		}
	}

	private JPanel createInfoPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(new JLabel(String.format(
			"Sources: %d | Timepoints: %d | Max Transform Chain: %d",
			sourceEntries.size(), allTimepoints.size(), maxTransformChainLength)));
		return panel;
	}

	private void onDimensionChanged() {
		Dimension newRow = (Dimension) rowDimensionCombo.getSelectedItem();
		Dimension newCol = (Dimension) colDimensionCombo.getSelectedItem();
		Dimension newSlider = (Dimension) sliderDimensionCombo.getSelectedItem();

		// Check for duplicates
		if (newRow == newCol || newRow == newSlider || newCol == newSlider) {
			// Find the missing dimension and fix the conflict
			Dimension[] all = Dimension.values();
			for (Dimension d : all) {
				if (d != newRow && d != newCol) {
					newSlider = d;
					sliderDimensionCombo.setSelectedItem(d);
					break;
				}
			}
		}

		rowDimension = newRow;
		colDimension = newCol;
		sliderDimension = newSlider;

		// Update slider range based on selected dimension
		updateSliderRange();
		updateSliderLabel();
		currentSliderIndex = 0;
		dimensionSlider.setValue(0);

		tableModel.fireTableStructureChanged();
		updateColumnWidths();
	}

	private void updateSliderRange() {
		int max = 0;
		switch (sliderDimension) {
			case SOURCES:
				max = sourceEntries.size() - 1;
				break;
			case TIMEPOINTS:
				max = allTimepoints.size() - 1;
				break;
			case TRANSFORMS:
				max = maxTransformChainLength - 1;
				break;
		}
		dimensionSlider.setMaximum(Math.max(0, max));
		dimensionSlider.setMajorTickSpacing(Math.max(1, max / 10));
	}

	private void updateSliderLabel() {
		String label = "";
		switch (sliderDimension) {
			case SOURCES:
				if (currentSliderIndex < sourceEntries.size()) {
					label = "Source: " + sourceEntries.get(currentSliderIndex).name;
				}
				break;
			case TIMEPOINTS:
				if (currentSliderIndex < allTimepoints.size()) {
					label = "Timepoint: " + allTimepoints.get(currentSliderIndex);
				}
				break;
			case TRANSFORMS:
				label = "Transform: " + currentSliderIndex;
				break;
		}
		sliderLabel.setText(label);
	}

	private void updateColumnWidths() {
		for (int i = 0; i < transformTable.getColumnCount(); i++) {
			transformTable.getColumnModel().getColumn(i).setPreferredWidth(
				i == 0 ? 150 : 200);
		}
	}

	/**
	 * Table model for displaying transforms with configurable dimensions
	 */
	private class TransformTableModel extends AbstractTableModel {

		@Override
		public int getRowCount() {
			switch (rowDimension) {
				case SOURCES:
					return sourceEntries.size();
				case TIMEPOINTS:
					return allTimepoints.size();
				case TRANSFORMS:
					return maxTransformChainLength;
			}
			return 0;
		}

		@Override
		public int getColumnCount() {
			int count = 1; // First column is the row header
			switch (colDimension) {
				case SOURCES:
					count += sourceEntries.size();
					break;
				case TIMEPOINTS:
					count += allTimepoints.size();
					break;
				case TRANSFORMS:
					count += maxTransformChainLength;
					break;
			}
			return count;
		}

		@Override
		public String getColumnName(int column) {
			if (column == 0) {
				return rowDimension.toString();
			}
			int idx = column - 1;
			switch (colDimension) {
				case SOURCES:
					if (idx < sourceEntries.size()) {
						return sourceEntries.get(idx).name;
					}
					break;
				case TIMEPOINTS:
					if (idx < allTimepoints.size()) {
						return "T" + allTimepoints.get(idx);
					}
					break;
				case TRANSFORMS:
					return "Tr" + idx;
			}
			return "";
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex == 0) {
				// Row header
				switch (rowDimension) {
					case SOURCES:
						if (rowIndex < sourceEntries.size()) {
							return sourceEntries.get(rowIndex).name;
						}
						break;
					case TIMEPOINTS:
						if (rowIndex < allTimepoints.size()) {
							return "T" + allTimepoints.get(rowIndex);
						}
						break;
					case TRANSFORMS:
						return "Transform " + rowIndex;
				}
				return "";
			}

			int colIdx = columnIndex - 1;

			// Resolve indices based on dimension configuration
			int[] indices = resolveIndices(rowIndex, colIdx, currentSliderIndex);
			int sourceIdx = indices[0];
			int timepointIdx = indices[1];
			int transformIdx = indices[2];

			// Get the transform
			return getTransformString(sourceIdx, timepointIdx, transformIdx);
		}

		private int[] resolveIndices(int rowIdx, int colIdx, int sliderIdx) {
			int sourceIdx = -1;
			int timepointIdx = -1;
			int transformIdx = -1;

			// Row dimension
			switch (rowDimension) {
				case SOURCES:
					sourceIdx = rowIdx;
					break;
				case TIMEPOINTS:
					timepointIdx = rowIdx;
					break;
				case TRANSFORMS:
					transformIdx = rowIdx;
					break;
			}

			// Column dimension
			switch (colDimension) {
				case SOURCES:
					sourceIdx = colIdx;
					break;
				case TIMEPOINTS:
					timepointIdx = colIdx;
					break;
				case TRANSFORMS:
					transformIdx = colIdx;
					break;
			}

			// Slider dimension
			switch (sliderDimension) {
				case SOURCES:
					sourceIdx = sliderIdx;
					break;
				case TIMEPOINTS:
					timepointIdx = sliderIdx;
					break;
				case TRANSFORMS:
					transformIdx = sliderIdx;
					break;
			}

			return new int[] { sourceIdx, timepointIdx, transformIdx };
		}

		private String getTransformString(int sourceIdx, int timepointIdx,
			int transformIdx)
		{
			if (sourceIdx < 0 || sourceIdx >= sourceEntries.size()) {
				return "-";
			}
			if (timepointIdx < 0 || timepointIdx >= allTimepoints.size()) {
				return "-";
			}

			SourceEntry entry = sourceEntries.get(sourceIdx);
			int timepoint = allTimepoints.get(timepointIdx);

			ViewRegistrations vrs = entry.spimData.getViewRegistrations();
			ViewRegistration vr = vrs.getViewRegistration(timepoint, entry.setupId);

			if (vr == null) {
				return "N/A";
			}

			List<ViewTransform> transforms = vr.getTransformList();
			if (transformIdx < 0 || transformIdx >= transforms.size()) {
				return "-";
			}

			// Reverse order: index 0 shows the last transform in the chain
			ViewTransform vt = transforms.get(transforms.size() - 1 - transformIdx);
			AffineGet transform = vt.asAffine3D();

			// Format the matrix values
			return formatMatrix(transform, vt.getName());
		}

		private String formatMatrix(AffineGet t, String name) {
			StringBuilder sb = new StringBuilder();
			sb.append("<html>");
			if (name != null && !name.isEmpty()) {
				sb.append("<b>[").append(name).append("]</b><br>");
			}
			// Row 0
			sb.append(String.format("%.3f, %.3f, %.3f, %.3f<br>",
				t.get(0, 0), t.get(0, 1), t.get(0, 2), t.get(0, 3)));
			// Row 1
			sb.append(String.format("%.3f, %.3f, %.3f, %.3f<br>",
				t.get(1, 0), t.get(1, 1), t.get(1, 2), t.get(1, 3)));
			// Row 2
			sb.append(String.format("%.3f, %.3f, %.3f, %.3f",
				t.get(2, 0), t.get(2, 1), t.get(2, 2), t.get(2, 3)));
			sb.append("</html>");
			return sb.toString();
		}

	}

	/**
	 * Custom cell renderer for transform cells
	 */
	private static class TransformCellRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column)
		{
			Component c = super.getTableCellRendererComponent(table, value,
				isSelected, hasFocus, row, column);

			if (!isSelected) {
				if (column == 0) {
					// Use table header colors for the first column (row labels)
					setBackground(UIManager.getColor("TableHeader.background"));
					setForeground(UIManager.getColor("TableHeader.foreground"));
				}
				else {
					// Use default table colors
					setBackground(UIManager.getColor("Table.background"));
					setForeground(UIManager.getColor("Table.foreground"));
				}
			}

			// Bold font for first column
			if (column == 0) {
				setFont(getFont().deriveFont(Font.BOLD));
			}
			else {
				setFont(getFont().deriveFont(Font.PLAIN));
			}

			// Tooltip with full value
			if (value != null) {
				setToolTipText(value.toString());
			}

			return c;
		}
	}

	/**
	 * Shows the viewer
	 */
	public void showViewer() {
		if (!sourceEntries.isEmpty()) {
			setVisible(true);
		}
	}
}
