package sc.fiji.bdvpg.bdv;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.Source;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;

public class BdvCreator implements Runnable
{
	private final boolean is2D;
	private final String name;
	private BdvHandle bdvHandle;

	public BdvCreator( boolean is2D )
	{
		this( is2D, "BigDataViewer" );
	}

	public BdvCreator( boolean is2D, String name )
	{
		this.is2D = is2D;
		this.name = name;
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
		BdvOptions bdvOptions = BdvOptions.options();
		if (is2D)
			bdvOptions = bdvOptions.is2D();

		ArrayImg dummyImg = ArrayImgs.bytes(2, 2, 2);

		BdvStackSource bss = BdvFunctions.show(
				dummyImg, "dummy",
				bdvOptions.frameTitle(name)
						.sourceTransform(new AffineTransform3D()));

		bdvHandle = bss.getBdvHandle();

		bss.removeFromBdv();
	}

	public BdvHandle getBdvHandle()
	{
		if ( bdvHandle == null ) run();

		return bdvHandle;
	}
}
