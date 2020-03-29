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
import sc.fiji.bdvpg.scijava.command.bdv.ScreenShotMakerCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.function.Supplier;

public class BdvCreator implements Runnable, Supplier<BdvHandle>
{
	private BdvOptions bdvOptions;
	private boolean interpolate;
	private BdvHandle bdv;
	private int numTimePoints;

	public BdvCreator( )
	{
		this.bdvOptions = BdvOptions.options();
		this.interpolate = false;
		this.numTimePoints = 1;
	}

	public BdvCreator( BdvOptions bdvOptions  )
	{
		this.bdvOptions = bdvOptions;
		this.interpolate = false;
		this.numTimePoints = 1;
	}

	public BdvCreator( BdvOptions bdvOptions, boolean interpolate )
	{
		this.bdvOptions = bdvOptions;
		this.interpolate = interpolate;
		this.numTimePoints = 1;
	}

	public BdvCreator( BdvOptions bdvOptions, boolean interpolate, int numTimePoints )
	{
		this.bdvOptions = bdvOptions;
		this.interpolate = interpolate;
		this.numTimePoints = numTimePoints;
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

		bdv.getViewerPanel().setNumTimepoints(numTimePoints);

		addBehaviours();
	}

	private void addBehaviours()
	{
		addSourceAndConverterContextMenuBehaviour();
	}

	private void addSourceAndConverterContextMenuBehaviour()
	{
		final ClickBehaviourInstaller installerPopup = new ClickBehaviourInstaller( bdv, new SourceAndConverterContextMenuClickBehaviour( bdv ) );

		installerPopup.install( "Sources context menu - C", "C" );
		installerPopup.install( "Sources context menu - Right mouse button", "button3" );

		String actionScreenshotName = SourceAndConverterService.getCommandName(ScreenShotMakerCommand.class);
		final ClickBehaviourInstaller installerScreenshot = new ClickBehaviourInstaller( bdv, (x,y) -> {
			SourceAndConverterServices.getSourceAndConverterService().getAction(actionScreenshotName).accept(null);
		} );

		installerScreenshot.install("Screenshot", "D" );

	}

	public BdvHandle get()
	{
		if ( bdv == null ) run();

		return bdv;
	}
}
