package sc.fiji.bdvpg.bdv;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.*;
import mpicbg.spim.data.SpimData;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;

import java.util.List;


public class BDVSingleton
{
	private static BdvHandle bdvHandle = null;

	public static BdvHandle getInstance() {
		final ArrayImg source = ArrayImgs.shorts(100,100,100);

		if (bdvHandle == null) {
			BdvStackSource stackSource = BdvFunctions.show(source, "empty");
			bdvHandle = stackSource.getBdvHandle();
		} else {
			BdvFunctions.show(source, "empty", BdvOptions.options().addTo( bdvHandle ));
		}
		return bdvHandle;
	}

	@Deprecated
	public static BdvHandle getInstance( SpimData source ) {
		if (bdvHandle == null) {
			final List< BdvStackSource< ? > > bdvStackSources = BdvFunctions.show( source );
			bdvHandle = bdvStackSources.get( 0 ).getBdvHandle();
		} else {
			BdvFunctions.show(source, BdvOptions.options().addTo( bdvHandle ));
		}
		return bdvHandle;
	}

	@Deprecated
	public static BdvHandle getInstance( Source source ) {
		if (bdvHandle == null) {
			BdvStackSource stacksource = BdvFunctions.show(source);
			bdvHandle = stacksource.getBdvHandle();
		} else {
			BdvFunctions.show(source, BdvOptions.options().addTo( bdvHandle ));
		}
		return bdvHandle;
	}

	@Deprecated
	public static BdvHandle getInstance( RandomAccessibleInterval source, String name) {
		if (bdvHandle == null) {
			BdvStackSource stacksource = BdvFunctions.show(source, name);
			bdvHandle = stacksource.getBdvHandle();
		} else {
			BdvFunctions.show(source, name, BdvOptions.options().addTo( bdvHandle ));
		}
		return bdvHandle;
	}

	public static void main(String... args ) {
		BDVSingleton.getInstance();
	}
}
