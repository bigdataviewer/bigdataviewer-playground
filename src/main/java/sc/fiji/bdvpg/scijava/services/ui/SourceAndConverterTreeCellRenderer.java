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

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.net.URL;

public class SourceAndConverterTreeCellRenderer extends
	DefaultTreeCellRenderer
{

	// static ImageIcon sourceIcon;
	static final ImageIcon source2d;
	// static ImageIcon source2dwarped;

	static final ImageIcon source3d;
	// static ImageIcon source3dwarped;

	static final ImageIcon sourceFilterNode;

	static {
		URL iconSourceURL;
		iconSourceURL = SourceAndConverterTreeCellRenderer.class.getResource(
			"/images/source2d.png");
		assert iconSourceURL != null;
		source2d = new ImageIcon(iconSourceURL);
		iconSourceURL = SourceAndConverterTreeCellRenderer.class.getResource(
			"/images/source3d.png");
		source3d = new ImageIcon(iconSourceURL);
		iconSourceURL = SourceAndConverterTreeCellRenderer.class.getResource(
			"/images/sourceFilterNodeCentered.png");
		assert iconSourceURL != null;
		sourceFilterNode = new ImageIcon(iconSourceURL);
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
		boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
	{

		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row,
			hasFocus);

		if (isSourceAndConverterNode(value)) {
			setIcon(source3d);
			// Use a slightly dimmed foreground for source nodes
			if (!sel) {
				setForeground(getSecondaryForeground());
			}
			setToolTipText("Source node");
		}
		else if (value instanceof SourceFilterNode) {
			setIcon(sourceFilterNode);
			// Append the source count to the filter node text
			String originalText = value.toString();
			int sourceCount = ((SourceFilterNode) value).currentOutputSacs.size();
			setText(originalText + " [" + sourceCount + "]");
			setToolTipText("Filter node");
		}

		return this;
	}

	/**
	 * Returns a secondary (dimmed) foreground color that works with the current L&F.
	 * Falls back to a slightly transparent version of the default foreground.
	 */
	private Color getSecondaryForeground() {
		// Try to get the L&F's secondary/disabled text color
		Color secondary = UIManager.getColor("Label.disabledForeground");
		if (secondary != null) {
			// Make it a bit brighter than disabled for better readability
			return brighter(secondary, 0.3f);
		}
		// Fallback: use a dimmed version of the tree's foreground
		Color fg = UIManager.getColor("Tree.foreground");
		if (fg == null) {
			fg = getForeground();
		}
		return new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 180);
	}

	/**
	 * Makes a color brighter by the given factor (0.0 to 1.0).
	 */
	private Color brighter(Color color, float factor) {
		int r = color.getRed();
		int g = color.getGreen();
		int b = color.getBlue();
		int alpha = color.getAlpha();

		r = Math.min(255, r + (int) ((255 - r) * factor));
		g = Math.min(255, g + (int) ((255 - g) * factor));
		b = Math.min(255, b + (int) ((255 - b) * factor));

		return new Color(r, g, b, alpha);
	}

	protected boolean isSourceAndConverterNode(Object value) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		return node.getUserObject() instanceof RenamableSourceAndConverter;
	}

}
