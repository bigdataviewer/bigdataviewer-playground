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

package sc.fiji.bdvpg.bdv.overlay;

import bdv.tools.boundingbox.RenderBoxHelper;
import bdv.tools.boundingbox.TransformedBox;
import bdv.util.BdvOverlay;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Displays names on top of visible sources of all visible {@link SourceAndConverter} of a
 * {@link ViewerPanel}
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL, 2022
 */
public class SourceNameOverlay extends BdvOverlay {
	final ViewerPanel viewer;
	private int canvasWidth;
	private int canvasHeight;

	private List<SourceBoxOverlay> sourcesBoxOverlay = new ArrayList<>();

	final Font font;

	final Function<SourceAndConverter<?>[], SourceAndConverter<?>[]> sorter;

	public SourceNameOverlay(ViewerPanel viewer, Font font, Function<SourceAndConverter<?>[], SourceAndConverter<?>[]> sorter)
	{
		this.viewer = viewer;
		this.font = font;
		this.sorter = sorter;
		update();
	}

	@Override
	public synchronized void draw(Graphics2D g) {
		Map<Integer,Set<Integer>> occupied = new HashMap<>();
		g.setFont(font);
		if (SourceNameOverlay.this.info!=null) {
			g.setColor(new Color(SourceNameOverlay.this.info.getColor().get()));
		} else {
			g.setColor(Color.GRAY);
		}
		for (SourceBoxOverlay source : sourcesBoxOverlay) {
			source.drawSourceNameOverlay(g, occupied);
		}

	}

	@Override
	public void setCanvasSize(final int width, final int height) {
		this.canvasWidth = width;
		this.canvasHeight = height;
	}

	public void update() {
		List<SourceBoxOverlay> newSourcesBoxOverlay = new ArrayList<>();
		Set<SourceAndConverter<?>> sources = viewer.state().getVisibleSources();
		if (sources!=null && !sources.isEmpty() && sorter!=null) {
			SourceAndConverter<?>[] sorted = sorter.apply(sources.toArray(new SourceAndConverter<?>[0]));
			if (sorted != null) {
				for (SourceAndConverter<?> sac : sorted) {
					if (sac.getSpimSource().getSource(viewer.state().getCurrentTimepoint(),
							0) != null) { // TODO : fix hack to avoid dirty overlay filter
						newSourcesBoxOverlay.add(new SourceBoxOverlay(sac));
					}
				}
			}
		}

		synchronized (sourcesBoxOverlay) {
			sourcesBoxOverlay = newSourcesBoxOverlay;
		}

	}

	private static int getStringWidth(String str, Graphics2D g2d) {
		FontMetrics fontMetrics = g2d.getFontMetrics();
		return fontMetrics.stringWidth(str);
	}

	private Map<Integer,Set<Integer>> displayNameAt(SourceAndConverter<?> sac, Graphics2D graphics, double xp, double yp, String name, Map<Integer,Set<Integer>> occupied) {
		double binSizeX = 100;
		double binSizeY = this.font.getSize();
		int binX = (int) (xp/binSizeX);
		int binY = (int) (yp/binSizeY);
		int shiftX = 0;
		int shiftY = 0;
		if (!occupied.containsKey(binX)) {
			occupied.put(binX, new HashSet<>());
		}
		Set<Integer> occupiedY = occupied.get(binX);

		while (occupiedY.contains(binY)) {
			binY++;
			shiftY += (int) binSizeY;
		}
		occupiedY.add(binY);
		String str = sac.getSpimSource().getName();
		graphics.drawString(str,(int) (xp+shiftX-getStringWidth(str, graphics)/2.0),(int) (yp+shiftY));
		return occupied;
	}

	class SourceBoxOverlay implements TransformedBox {

		final SourceAndConverter<?> sac;

		final RenderBoxHelper rbh;

		public SourceBoxOverlay(SourceAndConverter<?> sac) {
			this.sac = sac;
			rbh = new RenderBoxHelper();
		}

		private	Map<Integer,Set<Integer>> drawSourceNameOverlay(Graphics2D graphics, Map<Integer,Set<Integer>> occupied) {
			
			final GeneralPath front = new GeneralPath();
			final GeneralPath back = new GeneralPath();
			final GeneralPath intersection = new GeneralPath();

			final RealInterval interval = getInterval();
			if (interval != null) {
				final double ox = canvasWidth / 2.0;
				final double oy = canvasHeight / 2.0;
				AffineTransform3D viewerTransform = new AffineTransform3D();
				viewer.state().getViewerTransform(viewerTransform);
				AffineTransform3D transform = new AffineTransform3D();

				getTransform(transform);
				transform.preConcatenate(viewerTransform);

				rbh.setOrigin(ox, oy);
				rbh.setScale(1);

				rbh.renderBox(interval, transform, front, back, intersection);
				Rectangle screen = new Rectangle(0,0,canvasWidth, canvasHeight);
				Rectangle rectBounds = intersection.getBounds();
				if ((rectBounds.x + rectBounds.width > 0) &&
					(rectBounds.x < canvasWidth))
				{
					if ((rectBounds.y + rectBounds.height > 0) &&
						(rectBounds.y < canvasHeight))
					{						
						Area a = new Area(intersection);
						a.intersect(new Area(screen));
						double cx = a.getBounds2D().getCenterX();
						double cy = a.getBounds2D().getCenterY();
						//graphics.drawString(sac.getSpimSource().getName(),(int) cx,(int)cy);


						occupied = displayNameAt(sac, graphics, cx, cy, sac.getSpimSource().getName(), occupied);
					}
				}
			}

			return occupied;

		}

		@Override
		public RealInterval getInterval() {
			long[] dims = new long[3];
			int currentTimePoint = viewer.state().getCurrentTimepoint();
			if (sac.getSpimSource().isPresent(currentTimePoint)) {
				sac.getSpimSource().getSource(currentTimePoint, 0).dimensions(dims);
				return new FinalRealInterval(new double[] { -0.5, -0.5, -0.5 },
					new double[] { dims[0] - 0.5, dims[1] - 0.5, dims[2] - 0.5 });
			}
			else return null;
		}

		@Override
		public void getTransform(AffineTransform3D transform) {
			sac.getSpimSource().getSourceTransform(viewer.state()
				.getCurrentTimepoint(), 0, transform);
		}

	}

}
