package sc.fiji.bdv;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.*;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;


public class BDVSingleton
{
	private static BdvHandle bdvHandle = null;

	public static BdvHandle getInstance() {
		final ArrayImg source = ArrayImgs.shorts(1,1,1);

		if (bdvHandle == null) {
			BdvStackSource stacksource = BdvFunctions.show(source, "empty");
			bdvHandle = stacksource.getBdvHandle();
		} else {
			BdvFunctions.show(source, "empty", BdvOptions.options().addTo( bdvHandle ));
		}
		return bdvHandle;
	}

	@Deprecated
	public static BdvHandle getInstance( Source source) {
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
