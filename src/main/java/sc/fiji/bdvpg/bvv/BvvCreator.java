package sc.fiji.bdvpg.bvv;

import bdv.util.BdvHandle;
import bdv.viewer.Interpolation;
import bvv.util.BvvFunctions;
import bvv.util.BvvHandle;
import bvv.util.BvvOptions;
import bvv.util.BvvStackSource;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import sc.fiji.bdvpg.behaviour.ClickBehaviourInstaller;
import sc.fiji.bdvpg.behaviour.SourceAndConverterContextMenuClickBehaviour;
import sc.fiji.bdvpg.scijava.command.bdv.ScreenShotMakerCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.function.Supplier;

public class BvvCreator implements Runnable, Supplier<BvvHandle>
{
	private BvvOptions bvvOptions;
	//private boolean interpolate;
	private BvvHandle bvv;
	private int numTimePoints;

	public BvvCreator( )
	{
		this.bvvOptions = BvvOptions.options();
		//this.interpolate = false;
		this.numTimePoints = 1;
	}

	public BvvCreator(BvvOptions bvvOptions)
	{
		this.bvvOptions = bvvOptions;
		//this.interpolate = false;
		this.numTimePoints = 1;
	}

	public BvvCreator(BvvOptions bvvOptions, int numTimePoints )
	{
		this.bvvOptions = bvvOptions;
		//this.interpolate = interpolate;
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
		ArrayImg< UnsignedShortType, ShortArray> dummyImg = ArrayImgs.unsignedShorts(2, 2, 2);

		//dummyImg.forEach(t -> t.set((int) (Math.random()*65500)));

		bvvOptions = bvvOptions.sourceTransform( new AffineTransform3D() );

		BvvStackSource bss = BvvFunctions.show( dummyImg, "dummy", bvvOptions );

		bvv = bss.getBvvHandle();

		//if ( interpolate ) bvv.getViewerPanel().setInterpolation( Interpolation.NLINEAR );

		//bvv.getViewerPanel().getState().removeSource(bvv.getViewerPanel().getState().getCurrentSource());

		bvv.getViewerPanel().setNumTimepoints(numTimePoints);

		addBehaviours();
	}

	private void addBehaviours()
	{
		addSourceAndConverterContextMenuBehaviour();
	}

	private void addSourceAndConverterContextMenuBehaviour()
	{
		//final ClickBehaviourInstaller installerPopup = new ClickBehaviourInstaller(bvv, new SourceAndConverterContextMenuClickBehaviour(bvv) );

		//installerPopup.install( "Sources context menu - C", "C" );
		//installerPopup.install( "Sources context menu - Right mouse button", "button3" );

		//String actionScreenshotName = SourceAndConverterService.getCommandName(ScreenShotMakerCommand.class);
		/*final ClickBehaviourInstaller installerScreenshot = new ClickBehaviourInstaller(bvv, (x, y) -> {
			SourceAndConverterServices.getSourceAndConverterService().getAction(actionScreenshotName).accept(null);
		} );*/

		//installerScreenshot.install("Screenshot", "D" );

	}

	public BvvHandle get()
	{
		if ( bvv == null ) run();

		return bvv;
	}
}
