/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package sc.fiji.bdvpg.bdv.navigate;

import bdv.util.Affine3DHelpers;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerState;
import net.imglib2.*;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.viewers.ViewerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * BigDataViewer Playground Action --
 * Action which adjust the view of a {@link BdvHandle} to span a {@link SourceAndConverter}
 * See {@link ViewerTransformAdjuster#getTransform()} for details
 *
 * TODO : support the adjustement on a series of SourceAndConverter
 *
 * Usage example see ViewerTransformAdjusterDemo
 *
 * @author Christian Tischer, EMBL
 */

public class ViewerTransformAdjuster implements Runnable
{
	private final ViewerAdapter handle;
	private final SourceAndConverter[] sources;

	public ViewerTransformAdjuster( BdvHandle bdvHandle, SourceAndConverter source )
	{
		this(bdvHandle, new SourceAndConverter[]{source});
	}

	public ViewerTransformAdjuster( BdvHandle bdvHandle, SourceAndConverter[] sources )
	{
		this.handle = new ViewerAdapter(bdvHandle);
		this.sources = sources;
	}

	public ViewerTransformAdjuster(ViewerAdapter handle, SourceAndConverter[] sources )
	{
		this.handle = handle;
		this.sources = sources;
	}

	public void run()
	{
		AffineTransform3D transform;
		if (sources.length==0) {

		} else if (sources.length==1) {
		    transform = getTransform();
			handle.state().setViewerTransform(transform);
		} else {
			transform = getTransformMultiSources();
			handle.state().setViewerTransform(transform);
		}

	}

	/**
	 * Get a "good" initial viewer transform. The viewer transform is chosen
	 * such that for the {@link SourceAndConverter},
	 * <ul>
	 * <li>the XY plane is aligned with the screen plane,
	 * <li>the <em>z = dim_z / 2</em> slice is shown,
	 * <li>centered and scaled such that the full <em>dim_x</em> by
	 * <em>dim_y</em> is visible.
	 * </ul>
	 * @return the view which is equivalent to the transform of the bdv window
	 */
	public AffineTransform3D getTransform( )
	{
		final ViewerState state = handle.state();

		final int viewerWidth = (int) handle.getWidth();
		final int viewerHeight = (int) handle.getHeight();

		final double cX = viewerWidth / 2.0;
		final double cY = viewerHeight / 2.0;

		final int timepoint = state.getCurrentTimepoint();

		SourceAndConverter source = sources[0];

		if ( !source.getSpimSource().isPresent( timepoint ) )
			return new AffineTransform3D();

		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSpimSource().getSourceTransform( timepoint, 0, sourceTransform );

		final Interval sourceInterval = source.getSpimSource().getSource( timepoint, 0 );
		final double sX0 = sourceInterval.min( 0 );
		final double sX1 = sourceInterval.max( 0 );
		final double sY0 = sourceInterval.min( 1 );
		final double sY1 = sourceInterval.max( 1 );
		final double sZ0 = sourceInterval.min( 2 );
		final double sZ1 = sourceInterval.max( 2 );
		final double sX = ( sX0 + sX1 ) / 2;
		final double sY = ( sY0 + sY1 ) / 2;
		final double sZ = Math.round( ( sZ0 + sZ1 ) / 2 ); // z-slice in the middle of a pixel

		final double[][] m = new double[ 3 ][ 4 ];

		// rotation
		final double[] qSource = new double[ 4 ];
		final double[] qViewer = new double[ 4 ];
		Affine3DHelpers.extractApproximateRotationAffine( sourceTransform, qSource, 2 );
		LinAlgHelpers.quaternionInvert( qSource, qViewer );
		LinAlgHelpers.quaternionToR( qViewer, m );

		// translation
		final double[] centerSource = new double[] { sX, sY, sZ };
		final double[] centerGlobal = new double[ 3 ];
		final double[] translation = new double[ 3 ];
		sourceTransform.apply( centerSource, centerGlobal );
		LinAlgHelpers.quaternionApply( qViewer, centerGlobal, translation );
		LinAlgHelpers.scale( translation, -1, translation );
		LinAlgHelpers.setCol( 3, translation, m );

		final AffineTransform3D viewerTransform = new AffineTransform3D();
		viewerTransform.set( m );

		// scale
		final double[] pSource = new double[] { sX1 + 0.5, sY1 + 0.5, sZ };
		final double[] pGlobal = new double[ 3 ];
		final double[] pScreen = new double[ 3 ];
		sourceTransform.apply( pSource, pGlobal );
		viewerTransform.apply( pGlobal, pScreen );
		final double scaleX = cX / pScreen[ 0 ];
		final double scaleY = cY / pScreen[ 1 ];
		final double scale;
		// TODO: what's the point of this?
		/*boolean zoomedIn = false;
		if (zoomedIn)
			scale = Math.max( scaleX, scaleY );
		else*/
		scale = Math.min( scaleX, scaleY );
		viewerTransform.scale( scale );

		// window center offset
		viewerTransform.set( viewerTransform.get( 0, 3 ) + cX - 0.5, 0, 3 );
		viewerTransform.set( viewerTransform.get( 1, 3 ) + cY - 0.5, 1, 3 );
		return viewerTransform;
	}

	public AffineTransform3D getTransformMultiSources() {
		final ViewerState state = handle.state();

		final int timepoint = state.getCurrentTimepoint();

		List<RealInterval> intervalList = Arrays.asList(sources).stream()
				.filter(sourceAndConverter -> sourceAndConverter.getSpimSource()!=null)
				.filter(sourceAndConverter -> sourceAndConverter.getSpimSource().isPresent(timepoint))
				.map(sourceAndConverter -> {
					Interval interval = sourceAndConverter.getSpimSource().getSource(timepoint,0);
					AffineTransform3D sourceTransform = new AffineTransform3D();
					sourceAndConverter.getSpimSource().getSourceTransform( timepoint, 0, sourceTransform );
					RealPoint corner0 = new RealPoint(interval.min(0), interval.min(1), interval.min(2));
					RealPoint corner1 = new RealPoint(interval.max(0), interval.max(1), interval.max(2));
					sourceTransform.apply(corner0, corner0);
					sourceTransform.apply(corner1, corner1);
					return new FinalRealInterval(new double[]{
								Math.min(corner0.getDoublePosition(0), corner1.getDoublePosition(0)),
								Math.min(corner0.getDoublePosition(1), corner1.getDoublePosition(1)),
								Math.min(corner0.getDoublePosition(2), corner1.getDoublePosition(2))},
							new double[]{
								Math.max(corner0.getDoublePosition(0), corner1.getDoublePosition(0)),
								Math.max(corner0.getDoublePosition(1), corner1.getDoublePosition(1)),
								Math.max(corner0.getDoublePosition(2), corner1.getDoublePosition(2))});
				})
				.filter(object -> object!=null)
				.collect(Collectors.toList());

		RealInterval boundingInterval = intervalList.stream().reduce(Intervals::union).get();

		RealPoint center = new RealPoint(
					(boundingInterval.realMin(0)+ boundingInterval.realMax(0))/2.0,
					(boundingInterval.realMin(1)+ boundingInterval.realMax(1))/2.0,
					(boundingInterval.realMin(2)+ boundingInterval.realMax(2))/2.0
				);


		final double[] centerGlobal = {center.getDoublePosition(0), center.getDoublePosition(1), center.getDoublePosition(2)};

		final int viewerWidth = (int) handle.getWidth();
		final int viewerHeight = (int) handle.getHeight();

		AffineTransform3D viewerTransform = new AffineTransform3D();

		handle.state().getViewerTransform(viewerTransform);

		viewerTransform = BdvHandleHelper.getViewerTransformWithNewCenter(handle, centerGlobal);

		// Let's scale: we need to find the coordinates on the screen of the big bounding box
		RealPoint screenCoord = new RealPoint(0,0,0);
		List<RealPoint> boxCorners = getBox(boundingInterval);

		double currentMinScale = Double.MAX_VALUE;

		final double cX = viewerWidth / 2.0;
		final double cY = viewerHeight / 2.0;

		for (RealPoint corner:boxCorners) {
			viewerTransform.apply(corner, screenCoord);

			double scaleX = Math.abs(cX/(screenCoord.getDoublePosition(0)-viewerWidth/2.0));
			double scaleY = Math.abs(cY/(screenCoord.getDoublePosition(1)-viewerHeight/2.0));

			currentMinScale = Math.min(currentMinScale,scaleX);
			currentMinScale = Math.min(currentMinScale,scaleY);
		}

		viewerTransform.scale( currentMinScale );
		handle.state().setViewerTransform(viewerTransform);

		viewerTransform = BdvHandleHelper.getViewerTransformWithNewCenter(handle, centerGlobal);

		return viewerTransform;
	}

	public static List<RealPoint> getBox(RealInterval ri) {
		ArrayList<RealPoint> rps = new ArrayList<>();
		for (int x=0; x<2; x++) {
			for (int y=0; y<2; y++) {
				for (int z=0; z<2; z++) {
					rps.add(new RealPoint(
							(x==0)?ri.realMin(0):ri.realMax(0),
							(y==0)?ri.realMin(1):ri.realMax(1),
							(z==0)?ri.realMin(2):ri.realMax(2)
					));
				}
			}
		}
		return rps;
	}
}
