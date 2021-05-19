/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2021 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package sc.fiji.bdvpg.bdv.config;

import bdv.util.Prefs;
import org.mastodon.app.ui.settings.ModificationListener;
import org.mastodon.app.ui.settings.SettingsPage;
import org.scijava.listeners.Listeners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Properties;

/**
 * Settings Page to open, edit and resave the Bigdataviewer Preferences {@link Prefs} stored
 * within the bigdataviewer.properties file
 * {@link SettingsPage} comes from Mastodon
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL, 2020
 */

public class BdvPrefsSettingsPage implements SettingsPage {

    protected static Logger logger = LoggerFactory.getLogger(BdvPrefsSettingsPage.class);

    private final String treePath;

    private final JPanel panel;

    private final Listeners.List<ModificationListener> modificationListeners;

    // TODO make these field public in bdv.util.Prefs ? Or a Map ?
    private static final String SHOW_SCALE_BAR = "show-scale-bar";
    private static final String SHOW_MULTIBOX_OVERLAY = "show-multibox-overlay";
    private static final String SHOW_TEXT_OVERLAY = "show-text-overlay";
    private static final String SHOW_SCALE_BAR_IN_MOVIE = "show-scale-bar-in-movie";
    private static final String SCALE_BAR_COLOR = "scale-bar-color";
    private static final String SCALE_BAR_BG_COLOR = "scale-bar-bg-color";

    public BdvPrefsSettingsPage( final String treePath )
    {
        this.treePath = treePath;
        panel = new BdvPrefsEditorPanel();
        modificationListeners = new Listeners.SynchronizedList<>();
    }

    @Override
    public String getTreePath() {
        return treePath;
    }

    @Override
    public JPanel getJPanel() {
        return panel;
    }

    @Override
    public Listeners<ModificationListener> modificationListeners() {
        return modificationListeners;
    }

    /**
     * Restores current panel settings with the one present in bigdataviewer.properties file
     */
    @Override
    public void cancel() {
        ((BdvPrefsEditorPanel) panel).reinit();
    }

    /**
     * Stores current panel settings in bigdataviewer.properties file if it exists,
     * to default ones otherwise
     */
    @Override
    public void apply() {
        try {
            File f = new File( "bigdataviewer.properties" );
            final OutputStream stream;
            stream = new FileOutputStream(f);
            final Properties config = ((BdvPrefsEditorPanel) panel).getAndSetCurrentProperties();
            config.store(stream,"");
        } catch (IOException e) {
            logger.error("Could not create bigdataviewer.properties file");
        }
    }

    /**
     * Inner panel containing checkboxes and colorchoosers
     */
    static class BdvPrefsEditorPanel extends JPanel {

        JButton chooseScaleBarColor, chooseScaleBarBGColor;

        JCheckBox showScaleBar,showMultiboxOverlay, showTextOverlay, showScaleBarInMovie;

        Color scaleBarColor, scaleBarBGColor;

        public BdvPrefsEditorPanel() {

            setLayout(new GridLayout(0,2));

            // Show Scale Bar
            add(new JLabel(SHOW_SCALE_BAR));
            showScaleBar = new JCheckBox("",Prefs.showScaleBar());
            add(showScaleBar);

            // Show Multibox Overlay
            add(new JLabel(SHOW_MULTIBOX_OVERLAY));
            showMultiboxOverlay = new JCheckBox("", Prefs.showMultibox());
            add(showMultiboxOverlay);

            add(new JLabel(SHOW_TEXT_OVERLAY));
            showTextOverlay = new JCheckBox("", Prefs.showTextOverlay());
            add(showTextOverlay);

            add(new JLabel(SHOW_SCALE_BAR_IN_MOVIE));
            showScaleBarInMovie = new JCheckBox("", Prefs.showScaleBarInMovie());
            add(showScaleBarInMovie);

            add(new JLabel(SCALE_BAR_COLOR));
            chooseScaleBarColor = new JButton("Set Scale Bar Color");
            chooseScaleBarColor.setForeground(new Color(Prefs.scaleBarColor()));
            chooseScaleBarColor.setBackground(new Color(Prefs.scaleBarColor()));
            chooseScaleBarColor.addActionListener(e -> {
                scaleBarColor = JColorChooser.showDialog(
                        this,
                        "Choose Scale Bar Color",
                        new Color(Prefs.scaleBarColor()));
                if (scaleBarColor==null) {
                    scaleBarColor = new Color(Prefs.scaleBarColor());
                }
                chooseScaleBarColor.setForeground(scaleBarColor);
                chooseScaleBarColor.setBackground(scaleBarColor);
            });
            add(chooseScaleBarColor);

            add(new JLabel(SCALE_BAR_BG_COLOR));
            chooseScaleBarBGColor = new JButton("Set Scale Bar Background Color");
            chooseScaleBarBGColor.setForeground(new Color(Prefs.scaleBarBgColor()));
            chooseScaleBarBGColor.setBackground(new Color(Prefs.scaleBarBgColor()));
            chooseScaleBarBGColor.addActionListener(e -> {
                scaleBarBGColor = JColorChooser.showDialog(
                        this,
                        "Choose Scale Bar Background Color",
                        new Color(Prefs.scaleBarBgColor()));
                if (scaleBarBGColor==null) {
                    scaleBarBGColor = new Color(Prefs.scaleBarBgColor());
                }
                chooseScaleBarBGColor.setForeground(scaleBarBGColor);
                chooseScaleBarBGColor.setBackground(scaleBarBGColor);

            });
            add(chooseScaleBarBGColor);
            // Set the initial state and put it into the GUI
            reinit();
        }

        void reinit() {
            showScaleBar.setSelected(Prefs.showScaleBar());
            showMultiboxOverlay.setSelected(Prefs.showMultibox());
            showTextOverlay.setSelected(Prefs.showTextOverlay());
            showScaleBarInMovie.setSelected(Prefs.showScaleBarInMovie());
            scaleBarColor = new Color(Prefs.scaleBarColor());
            chooseScaleBarColor.setForeground(scaleBarColor);
            chooseScaleBarColor.setBackground(scaleBarColor);
            scaleBarBGColor = new Color(Prefs.scaleBarBgColor());
            chooseScaleBarBGColor.setForeground(scaleBarBGColor);
            chooseScaleBarBGColor.setBackground(scaleBarBGColor);
        }

        Properties getAndSetCurrentProperties() {

            // Updates prefs with new values (if not Prefs is never updated)
            Prefs.showScaleBar(showScaleBar.isSelected());
            Prefs.showMultibox(showMultiboxOverlay.isSelected());
            Prefs.showTextOverlay(showTextOverlay.isSelected());
            Prefs.showScaleBarInMovie(showScaleBarInMovie.isSelected());
            Prefs.scaleBarColor(scaleBarColor.getRGB());
            Prefs.scaleBarBgColor(scaleBarBGColor.getRGB());

            Properties props = new Properties();
            props.setProperty(SHOW_SCALE_BAR, Boolean.toString(Prefs.showScaleBar()));
            props.setProperty(SHOW_MULTIBOX_OVERLAY, Boolean.toString(Prefs.showMultibox()));
            props.setProperty(SHOW_TEXT_OVERLAY, Boolean.toString(Prefs.showTextOverlay()));
            props.setProperty(SHOW_SCALE_BAR_IN_MOVIE, Boolean.toString(Prefs.showScaleBarInMovie()));
            props.setProperty(SCALE_BAR_COLOR, Integer.toString(Prefs.scaleBarColor()));
            props.setProperty(SCALE_BAR_BG_COLOR, Integer.toString(Prefs.scaleBarBgColor()));
            return props;
        }
    }
}
