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

package sc.fiji.bdvpg.viewers;

import bdv.util.BdvHandle;
import bdv.viewer.TimePointListener;
import bdv.viewer.TransformListener;
import bdv.viewer.ViewerPanel;
import bdv.viewer.ViewerState;
import bvv.util.BvvHandle;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * Class to avoid duplicated code in synchronizers
 */

public class ViewerAdapter {

	final tpietzsch.example2.VolumeViewerPanel bvvPanel;
	final ViewerPanel bdvPanel;

	public ViewerAdapter(bdv.viewer.ViewerPanel viewerPanel) {
		bvvPanel = null;
		bdvPanel = viewerPanel;
	}

	public ViewerAdapter(tpietzsch.example2.VolumeViewerPanel viewerPanel) {
		bvvPanel = viewerPanel;
		bdvPanel = null;
	}

	public ViewerAdapter(BdvHandle bdv) {
		bvvPanel = null;
		bdvPanel = bdv.getViewerPanel();
	}

	public ViewerAdapter(BvvHandle bvv) {
		bvvPanel = bvv.getViewerPanel();
		bdvPanel = null;
	}

	public double getWidth() {
		if (bdvPanel != null) return bdvPanel.getWidth();
		return bvvPanel.getWidth();
	}

	public double getHeight() {
		if (bdvPanel != null) return bdvPanel.getHeight();
		return bvvPanel.getHeight();
	}

	public ViewerState state() {
		if (bdvPanel != null) return bdvPanel.state();
		return bvvPanel.state();
	}

	public void requestRepaint() {
		if (bdvPanel != null) {
			bdvPanel.requestRepaint();
		}
		else {
			bvvPanel.requestRepaint();
		}
	}

	public void addTransformListener(
		TransformListener<AffineTransform3D> listener)
	{
		if (bdvPanel != null) {
			bdvPanel.transformListeners().add(listener);
		}
		else {
			bvvPanel.addTransformListener(listener);
		}
	}

	public void setTimepoint(int timepoint) {
		if (bdvPanel != null) {
			bdvPanel.setTimepoint(timepoint);
		}
		else {
			bvvPanel.setTimepoint(timepoint);
		}
	}

	public void addTimePointListener(TimePointListener timeListener) {
		if (bdvPanel != null) {
			bdvPanel.timePointListeners().add(timeListener);
		}
		else {
			bvvPanel.addTimePointListener(timeListener);
		}
	}

	public void removeTransformListener(
		TransformListener<AffineTransform3D> listener)
	{
		if (bdvPanel != null) {
			bdvPanel.transformListeners().remove(listener);
		}
		else {
			bvvPanel.removeTransformListener(listener);
		}
	}

	public void removeTimePointListener(TimePointListener listener) {
		if (bdvPanel != null) {
			bdvPanel.timePointListeners().remove(listener);
		}
		else {
			bvvPanel.removeTimePointListener(listener);
		}
	}

	// Override this so that two vieweradapter with the same object will be equals
	@Override
	public int hashCode() {
		if (bdvPanel != null) return bdvPanel.hashCode();
		return bvvPanel.hashCode();
	}

	// Overriding equals() to compare two Complex objects
	@Override
	public boolean equals(Object o) {

		// If the object is compared with itself then return true
		if (o == this) {
			return true;
		}

		/* Check if o is an instance of Complex or not
		  "null instanceof [type]" also returns false */
		if (!(o instanceof ViewerAdapter)) {
			return false;
		}

		// typecast o to Complex so that we can compare data members
		ViewerAdapter c = (ViewerAdapter) o;

		// Compare the data members and return accordingly
		if (c.bdvPanel != null) return c.bdvPanel.equals(this.bdvPanel);
		return c.bvvPanel.equals(this.bvvPanel);
	}
}
