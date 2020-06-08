package sc.fiji.bdvpg.bvv;

import bvv.util.BvvFunctions;
import bvv.util.BvvHandle;
import bvv.util.BvvOptions;
import bvv.util.BvvStackSource;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import java.util.function.Supplier;

/**
 * BigDataViewer Playground Action -->
 *
 * Creates a BigVolumeViewer - should be replaced by Sciview
 */

public class BvvCreator implements Runnable, Supplier<BvvHandle>
{
	private BvvOptions bvvOptions;
	private int numTimePoints;

	public BvvCreator( ) {
		this.bvvOptions = BvvOptions.options();
		this.numTimePoints = 1;
	}

	public BvvCreator(BvvOptions bvvOptions) {
		this.bvvOptions = bvvOptions;
		this.numTimePoints = 1;
	}

	public BvvCreator(BvvOptions bvvOptions, int numTimePoints ) {
		this.bvvOptions = bvvOptions;
		this.numTimePoints = numTimePoints;
	}

	public void run() {
		// Do nothing -> see get() method
	}

	/**
	 * Hack: add an image and remove it after the
	 * bvvHandle has been created.
	 */
	public BvvHandle get() {
		ArrayImg< UnsignedShortType, ShortArray> dummyImg = ArrayImgs.unsignedShorts(2, 2, 2);

		bvvOptions = bvvOptions.sourceTransform( new AffineTransform3D() );

		BvvStackSource bss = BvvFunctions.show( dummyImg, "dummy", bvvOptions );

		BvvHandle bvv = bss.getBvvHandle();

		bvv.getViewerPanel().setNumTimepoints(numTimePoints);

		return bvv;
	}

}
