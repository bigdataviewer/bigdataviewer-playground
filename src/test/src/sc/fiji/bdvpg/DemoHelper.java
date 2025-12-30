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
package sc.fiji.bdvpg;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import com.formdev.flatlaf.FlatDarkLaf;
import net.imagej.ImageJ;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class DemoHelper {

    public static void closeFijiAndBdvs(ImageJ ij) {
        try {

            // Closes bdv windows
            SourceAndConverterBdvDisplayService sac_display_service =
                    ij.context().getService(SourceAndConverterBdvDisplayService.class);
            sac_display_service.getDisplays().forEach(BdvHandle::close);

            // Clears all sources
            SourceAndConverterService sac_service =
                    ij.context().getService(SourceAndConverterService.class);
            sac_service.remove(sac_service.getSourceAndConverters().toArray(new SourceAndConverter[0]));

            // Closes ij context
            ij.context().close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startFiji(ImageJ ij) {
        try {
            SwingUtilities.invokeAndWait(() -> ij.ui().showUI());
            try {
                // Increase default font size
                UIManager.put("defaultFont", new Font("SansSerif", Font.PLAIN, 16));
                FlatDarkLaf.setup();
            } catch (Exception e) {
                System.err.println("Failed to set FlatLaf look and feel: " + e.getMessage());
            }

            updateAllFramesLookAndFeel();

        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    /**
     * Updates all existing Swing frames/windows to use the current Look and Feel.
     * This is necessary when the L&F is changed after some windows were already created.
     */
    private static void updateAllFramesLookAndFeel() {
        SwingUtilities.invokeLater(() -> {
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
                window.pack();
            }
        });
    }

    /** Default output directory for screenshots */
    public static final File DEFAULT_OUTPUT_DIR = new File("documentation/resources");

    /** Default wait time in milliseconds before capturing */
    public static final long DEFAULT_WAIT_MS = 4000;

    // ==================== ONE-LINER METHODS ====================

    /**
     * One-liner to capture all visible windows with a prefix.
     * Waits for rendering, captures all JFrames, and prints results.
     * <p>
     * Example usage: {@code ScreenshotUtility.shot("MyDemo");}
     *
     * @param prefix prefix for screenshot filenames (e.g., demo class name)
     */
    public static void shot(String prefix) {
        shot(prefix, DEFAULT_WAIT_MS);
    }

    /**
     * One-liner to capture all visible windows with a prefix and custom wait time.
     * <p>
     * Example usage: {@code ScreenshotUtility.shot("MyDemo", 2000);}
     *
     * @param prefix prefix for screenshot filenames
     * @param waitMs milliseconds to wait before capturing
     */
    public static void shot(String prefix, long waitMs) {
        shot(DEFAULT_OUTPUT_DIR, prefix, waitMs);
    }

    /**
     * One-liner to capture all visible windows to a custom directory.
     *
     * @param outputDir directory to save screenshots
     * @param prefix prefix for screenshot filenames
     * @param waitMs milliseconds to wait before capturing
     */
    public static void shot(File outputDir, String prefix, long waitMs) {
        try {
            waitFor(waitMs);
            java.util.List<File> files = captureAllFramesOffscreen(outputDir, prefix);
            System.out.println("[Screenshot] Captured " + files.size() + " frame(s) with prefix '" + prefix + "':");
            for (File f : files) {
                System.out.println("  -> " + f.getPath());
            }
        } catch (Exception e) {
            System.err.println("[Screenshot] Failed to capture: " + e.getMessage());
        }
    }

    // ==================== DETAILED METHODS ====================

    /**
     * Captures a screenshot of a specific JFrame.
     *
     * @param frame the frame to capture
     * @param outputFile the file to save the screenshot to (PNG format)
     * @throws AWTException if the platform doesn't support screen capture
     * @throws IOException if the file cannot be written
     */
    public static void captureFrame(JFrame frame, File outputFile) throws AWTException, IOException {
        // Ensure parent directories exist
        outputFile.getParentFile().mkdirs();

        // Get the frame bounds on screen
        Rectangle bounds = frame.getBounds();

        // Use Robot to capture the screen region
        Robot robot = new Robot();
        BufferedImage screenshot = robot.createScreenCapture(bounds);

        // Write to file
        ImageIO.write(screenshot, "png", outputFile);
    }

    /**
     * Captures a screenshot of a specific JFrame using component painting.
     * This method doesn't require the window to be on top and works better
     * in headless/virtual display environments.
     *
     * @param frame the frame to capture
     * @param outputFile the file to save the screenshot to (PNG format)
     * @throws IOException if the file cannot be written
     */
    public static void captureFrameOffscreen(JFrame frame, File outputFile) throws IOException {
        // Ensure parent directories exist
        outputFile.getParentFile().mkdirs();

        // Create a buffered image with the frame's size
        int width = frame.getWidth();
        int height = frame.getHeight();

        if (width <= 0 || height <= 0) {
            throw new IllegalStateException("Frame has invalid dimensions: " + width + "x" + height);
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Paint the frame content
        frame.paint(g2d);
        g2d.dispose();

        // Write to file
        ImageIO.write(image, "png", outputFile);
    }

    /**
     * Gets all visible JFrames in the application.
     *
     * @return list of all visible JFrame instances
     */
    public static java.util.List<JFrame> getAllVisibleFrames() {
        java.util.List<JFrame> frames = new ArrayList<>();
        for (Window window : Window.getWindows()) {
            if (window instanceof JFrame && window.isVisible()) {
                frames.add((JFrame) window);
            }
        }
        return frames;
    }

    /**
     * Captures screenshots of all visible JFrames and saves them to a directory.
     * Files are named based on the frame title or a generated index.
     *
     * @param outputDir the directory to save screenshots to
     * @param prefix optional prefix for filenames (can be null)
     * @param useRobot if true, uses Robot for screen capture; if false, uses offscreen painting
     * @return list of files that were created
     * @throws AWTException if Robot capture is used and platform doesn't support it
     * @throws IOException if files cannot be written
     */
    public static java.util.List<File> captureAllFrames(File outputDir, String prefix, boolean useRobot)
            throws AWTException, IOException {
        java.util.List<File> capturedFiles = new ArrayList<>();
        java.util.List<JFrame> frames = getAllVisibleFrames();

        int index = 0;
        for (JFrame frame : frames) {
            String filename = generateFilename(frame, prefix, index);
            File outputFile = new File(outputDir, filename);

            if (useRobot) {
                captureFrame(frame, outputFile);
            } else {
                captureFrameOffscreen(frame, outputFile);
            }

            capturedFiles.add(outputFile);
            index++;
        }

        return capturedFiles;
    }

    /**
     * Captures screenshots of all visible JFrames using offscreen painting.
     * This is the preferred method for CI/automated environments.
     *
     * @param outputDir the directory to save screenshots to
     * @param prefix optional prefix for filenames (can be null)
     * @return list of files that were created
     * @throws IOException if files cannot be written
     */
    public static List<File> captureAllFramesOffscreen(File outputDir, String prefix) throws IOException {
        try {
            return captureAllFrames(outputDir, prefix, false);
        } catch (AWTException e) {
            // This shouldn't happen with offscreen capture, but just in case
            throw new IOException("Unexpected AWTException during offscreen capture", e);
        }
    }

    /**
     * Generates a sanitized filename for a frame screenshot.
     */
    private static String generateFilename(JFrame frame, String prefix, int index) {
        String title = frame.getTitle();
        String baseName;

        if (title != null && !title.trim().isEmpty()) {
            // Sanitize the title for use as filename
            baseName = title.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            // Remove consecutive underscores
            baseName = baseName.replaceAll("_+", "_");
            // Trim underscores from start/end
            baseName = baseName.replaceAll("^_|_$", "");
        } else {
            baseName = "frame_" + index;
        }

        if (prefix != null && !prefix.isEmpty()) {
            baseName = prefix + "_" + baseName;
        }

        return baseName + ".png";
    }

    /**
     * Waits for all pending Swing events to be processed.
     * Useful to ensure windows are fully rendered before capturing.
     */
    public static void waitForSwingToSettle() {
        try {
            // Process all pending Swing events
            SwingUtilities.invokeAndWait(() -> {});
            // Small additional delay to ensure rendering is complete
            Thread.sleep(500);
        } catch (Exception e) {
            // Ignore interruption
        }
    }

    /**
     * Waits for a specific duration (in milliseconds).
     * Useful when windows need time to fully initialize.
     *
     * @param millis time to wait in milliseconds
     */
    public static void waitFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
