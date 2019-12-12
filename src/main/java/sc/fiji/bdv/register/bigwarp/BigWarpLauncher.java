package sc.fiji.bdv.register.bigwarp;

import bdv.gui.TransformTypeSelectDialog;
import bdv.ij.util.ProgressWriterIJ;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.MinMaxGroup;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bigwarp.BigWarp;
import bigwarp.BigWarpInit;
import ij.gui.GenericDialog;
import mpicbg.spim.data.SpimDataException;
import net.imagej.ops.Ops;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.ui.TransformListener;
import sc.fiji.bdv.BdvUtils;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class BigWarpLauncher implements Runnable
{
	private final BdvHandle bdvHandle;
	private Source< ? > movingSource;
	private Source< ? > fixedSource;
	private double[] displayRangeMovingSource;
	private double[] displayRangeFixedSource;

	private List< Integer > sourceIndices;

	private AffineTransform3D bigWarpTransform;

	public BigWarpLauncher( BdvHandle bdvHandle )
	{
		this.bdvHandle = bdvHandle;
	}

	public BigWarpLauncher( BdvHandle bdvHandle, Source< ? > movingSource, Source< ? > fixedSource, double[] displayRangeMovingSource, double[] displayRangeFixedSource )
	{
		this.bdvHandle = bdvHandle;
		this.movingSource = movingSource;
		this.fixedSource = fixedSource;
		this.displayRangeMovingSource = displayRangeMovingSource;
		this.displayRangeFixedSource = displayRangeFixedSource;
	}

	/**
	 *
	 * @param bdvHandle
	 * @param sourceIndices the source selection dialog will only show sources that are present in this list.
	 */
	public BigWarpLauncher( BdvHandle bdvHandle, List< Integer > sourceIndices )
	{
		this.bdvHandle = bdvHandle;
		this.sourceIndices = sourceIndices;
	}

	@Override
	public void run()
	{
		if ( movingSource == null )
		{
			if ( ! fetchParametersFromDialog() ) return;
		}

		runBigWarp();
	}

	public void runBigWarp()
	{
		final String[] sourceNames = { movingSource.getName(), fixedSource.getName() };
		final Source< ? >[] movingSources = { movingSource };
		final Source< ? >[] fixedSources = { fixedSource };

		final BigWarp.BigWarpData< ? > bigWarpData = BigWarpInit.createBigWarpData( movingSources, fixedSources, sourceNames );
		final BigWarp bigWarp = tryGetBigWarp( bigWarpData );

		setDisplayRange( bigWarp, displayRangeMovingSource, 0 );
		setDisplayRange( bigWarp, displayRangeFixedSource, 1 );

		bigWarp.getViewerFrameP().getViewerPanel().requestRepaint();
		bigWarp.getViewerFrameQ().getViewerPanel().requestRepaint();
		bigWarp.getLandmarkFrame().repaint();
		bigWarp.setTransformType( TransformTypeSelectDialog.SIMILARITY );

		addListeners( bdvHandle, bigWarp, ( TransformedSource ) movingSource );
	}

	public boolean fetchParametersFromDialog()
	{
		BigWarpLauncherDialog dialog = createDialog();
		if ( ! dialog.showDialog( ) ) return false;

		this.movingSource =	dialog.getMovingVolatileSource();
		this.fixedSource = dialog.getFixedVolatileSource();
		this.displayRangeMovingSource = dialog.getDisplayRangeMovingSource();
		this.displayRangeFixedSource = dialog.getDisplayRangeFixedSource();
		return true;
	}

	public BigWarpLauncherDialog createDialog()
	{
		BigWarpLauncherDialog dialog= null;
		if ( sourceIndices != null )
		{
			dialog = new BigWarpLauncherDialog( bdvHandle );
		}
		else
		{
			dialog = new BigWarpLauncherDialog( bdvHandle, sourceIndices );
		}
		return dialog;
	}

	private void addListeners( BdvHandle bdvHandle, BigWarp bigWarp, TransformedSource movingSource )
	{
		bigWarp.addTransformListener( new TransformListener< InvertibleRealTransform >()
		{
			@Override
			public void transformChanged( InvertibleRealTransform invertibleRealTransform )
			{
				bigWarpTransform = bigWarp.getMovingToFixedTransformAsAffineTransform3D();
			}
		} );

		bigWarp.getViewerFrameP().addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				applyBigWarpTransform( bdvHandle, movingSource );
			}
		} );

		bigWarp.getViewerFrameQ().addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				applyBigWarpTransform( bdvHandle, movingSource );
			}
		} );

		bigWarp.getLandmarkFrame().addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				applyBigWarpTransform( bdvHandle, movingSource );
			}
		} );
	}

	private void applyBigWarpTransform( BdvHandle bdvHandle, TransformedSource movingSource )
	{
		if ( bigWarpTransform == null ) return;

		final TransformedSource source = movingSource;
		final AffineTransform3D tmp = new AffineTransform3D();
		tmp.identity();
		source.setIncrementalTransform( tmp );
		source.getFixedTransform( tmp );
		tmp.preConcatenate( bigWarpTransform );
		source.setFixedTransform( tmp );
		BdvUtils.repaint( bdvHandle );
	}

	public void setDisplayRange( BigWarp bigWarp, double[] displayRangeMovingSource, int sourceIndexBigWarp )
	{
		final ConverterSetup converterSetup = bigWarp.getSetupAssignments().getConverterSetups().get( sourceIndexBigWarp );
		final double min = displayRangeMovingSource[ 0 ];
		final double max = displayRangeMovingSource[ 1 ];
		converterSetup.setDisplayRange( min, max );
		final MinMaxGroup minMaxGroup = bigWarp.getSetupAssignments().getMinMaxGroup( converterSetup );
		minMaxGroup.getMinBoundedValue().setCurrentValue( min );
		minMaxGroup.getMaxBoundedValue().setCurrentValue( max );
	}

	public BigWarp tryGetBigWarp( BigWarp.BigWarpData< ? > bigWarpData )
	{
		try
		{
			// TODO: what to do about the ProgressWriter??
			return new BigWarp( bigWarpData, "Big Warp", new ProgressWriterIJ() );
		} catch ( SpimDataException e )
		{
			e.printStackTrace();
		}
		return null;
	}

}
