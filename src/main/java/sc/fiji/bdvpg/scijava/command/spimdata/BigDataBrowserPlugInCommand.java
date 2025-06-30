/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2025 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.bdvpg.scijava.command.spimdata;

import com.google.gson.stream.JsonReader;
import org.apache.commons.lang.StringUtils;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author HongKee Moon &lt;moon@mpi-cbg.de&gt;
 * @author Nicolas Chiaruttini biop.epfl.ch
 */

@SuppressWarnings({ "CanBeFinal", "unused" }) // Because SciJava command fields
																							// are set by SciJava
																							// pre-processors

@Plugin(type = BdvPlaygroundActionCommand.class,
	menuPath = ScijavaBdvDefaults.RootMenu +
		"BDVDataset>List BigDataServer Datasets")
public class BigDataBrowserPlugInCommand implements BdvPlaygroundActionCommand {

	private final Map<String, ImageIcon> imageMap = new HashMap<>();

	private final Map<String, String> datasetUrlMap = new HashMap<>();

	@Parameter(required = false)
	String serverurl = "http://tomancak-srv1.mpi-cbg.de:8081";

	@Parameter
	CommandService cs;

	@Parameter
	LogService ls;

	@Override
	public void run() {
		final ArrayList<String> nameList = new ArrayList<>();
		try {
			getDatasetList(serverurl, nameList);
		}
		catch (final IOException e) {
			ls.error("Error connecting to server at " + serverurl);
			e.printStackTrace();
			return;
		}
		createDatasetListUI(serverurl, nameList.toArray());
	}

	private void getDatasetList(final String remoteUrl,
		final ArrayList<String> nameList) throws IOException
	{
		// Get JSON string from the server
		final URL url = new URL(remoteUrl + "/json/");

		final InputStream is = url.openStream();
		final JsonReader reader = new JsonReader(new InputStreamReader(is,
			StandardCharsets.UTF_8));

		reader.beginObject();

		while (reader.hasNext()) {
			// skipping id
			reader.nextName();

			reader.beginObject();

			String id = null, description = null, thumbnailUrl = null, datasetUrl =
				null;
			while (reader.hasNext()) {
				final String name = reader.nextName();
				switch (name) {
					case "id":
						id = reader.nextString();
						break;
					case "description":
						description = reader.nextString();
						break;
					case "thumbnailUrl":
						thumbnailUrl = reader.nextString();
						break;
					case "datasetUrl":
						datasetUrl = reader.nextString();
						break;
					default:
						reader.skipValue();
						break;
				}
			}

			if (id != null) {
				nameList.add(id);
				if (StringUtils.isNotEmpty(thumbnailUrl)) imageMap.put(id,
					new ImageIcon(new URL(thumbnailUrl)));
				if (datasetUrl != null) datasetUrlMap.put(id, datasetUrl);
			}

			reader.endObject();
		}

		reader.endObject();

		reader.close();

	}

	private void createDatasetListUI(final String remoteUrl,
		final Object[] values)
	{

		final JList<?> list = new JList<>(values);
		list.setCellRenderer(new ThumbnailListRenderer());
		list.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(final MouseEvent evt) {
				final JList<?> list = (JList<?>) evt.getSource();
				if (evt.getClickCount() == 2) {
					final int index = list.locationToIndex(evt.getPoint());
					final String key = String.valueOf(list.getModel().getElementAt(
						index));
					final String filename = datasetUrlMap.get(key);
					final String title = new File(filename).getName();

					cs.run(SpimdataBigDataServerImportCommand.class, true, "urlserver",
						remoteUrl, "datasetname", title);
				}
			}
		});

		final JScrollPane scroll = new JScrollPane(list);
		scroll.setPreferredSize(new Dimension(600, 800));

		final JFrame frame = new JFrame();
		frame.setTitle("BigDataServer Browser - " + remoteUrl);
		frame.add(scroll);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	public class ThumbnailListRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = 1L;

		Font font = new Font("helvetica", Font.BOLD, 12);

		@Override
		public Component getListCellRendererComponent(final JList<?> list,
			final Object value, final int index, final boolean isSelected,
			final boolean cellHasFocus)
		{

			final JLabel label = (JLabel) super.getListCellRendererComponent(list,
				value, index, isSelected, cellHasFocus);
			label.setIcon(imageMap.get(value));
			label.setHorizontalTextPosition(JLabel.RIGHT);
			label.setFont(font);
			return label;
		}
	}

}
