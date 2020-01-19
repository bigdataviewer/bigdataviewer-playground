package sc.fiji.bdvpg.bdv;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.Interpolation;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.behaviour.ClickBehaviourInstaller;
import sc.fiji.bdvpg.behaviour.SourceAndConverterContextMenuClickBehaviour;

import java.util.function.Supplier;

public class BdvCreator implements Runnable, Supplier<BdvHandle>
{
	private BdvOptions bdvOptions;
	private boolean interpolate;
	private BdvHandle bdv;

	public BdvCreator( )
	{
		this.bdvOptions = BdvOptions.options();
		this.interpolate = false;
	}

	public BdvCreator( BdvOptions bdvOptions  )
	{
		this.bdvOptions = bdvOptions;
		this.interpolate = false;
	}

	public BdvCreator( BdvOptions bdvOptions, boolean interpolate )
	{
		this.bdvOptions = bdvOptions;
		this.interpolate = interpolate;
	}

	@Override
	public void run()
	{
		createEmptyBdv();
	}

	/**
	 * Hack: add an image and remove it after the
	 * bdvHandle has been created.
	 */
	private void createEmptyBdv()
	{
		ArrayImg dummyImg = ArrayImgs.bytes(2, 2, 2);

		bdvOptions = bdvOptions.sourceTransform( new AffineTransform3D() );

		BdvStackSource bss = BdvFunctions.show( dummyImg, "dummy", bdvOptions );

		bdv = bss.getBdvHandle();

		if ( interpolate ) bdv.getViewerPanel().setInterpolation( Interpolation.NLINEAR );

		bss.removeFromBdv();

		addBehaviours();
	}

	private void addBehaviours()
	{
		addSourceAndConverterContextMenuBehaviour();
	}

	private void addSourceAndConverterContextMenuBehaviour()
	{
		final ClickBehaviourInstaller installer = new ClickBehaviourInstaller( bdv, new SourceAndConverterContextMenuClickBehaviour( bdv ) );

		installer.install( "Sources context menu - C", "C" );
		installer.install( "Sources context menu - Right mouse button", "button3" );
	}

	public BdvHandle get()
	{
		if ( bdv == null ) run();

		return bdv;
	}
}
