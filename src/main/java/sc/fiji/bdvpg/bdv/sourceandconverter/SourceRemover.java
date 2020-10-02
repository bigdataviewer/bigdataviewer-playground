package sc.fiji.bdvpg.bdv.sourceandconverter;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.function.Consumer;

/**
 * BigDataViewer Playground Action -->
 * Removes a {@link SourceAndConverter} from a {@link BdvHandle}
 *
 * Note : - the functional interface allows to use this action in a functional way,
 * in this case, the constructor without SourceAndConverter can be used
 *
 * TODO : think if this action is useful ? It looks unused because the direct call to SourceAndConverterServices.getSourceAndConverterDisplayService().remove is more convenient
 *
 */

public class SourceRemover implements Runnable, Consumer<SourceAndConverter[]>
{
	SourceAndConverter sacsIn;
	BdvHandle bdvh;

	public SourceRemover(BdvHandle bdvh, SourceAndConverter sacsIn) {
		this.sacsIn=sacsIn;
		this.bdvh=bdvh;
	}

	public SourceRemover(SourceAndConverter sacsIn) {
		this.sacsIn=sacsIn;
		this.bdvh=null;
	}

	public SourceRemover() {
		this.sacsIn=null;
		this.bdvh=null;
	}

	public void run() {
		accept(sacsIn);
	}

	@Override
	public void accept(SourceAndConverter... sacs) {
		if (bdvh==null) {
			// Remove from all displays
			SourceAndConverterServices.getSourceAndConverterDisplayService().removeFromAllBdvs(sacs);
		} else {
			// Remove from a specific bdvHandle
			SourceAndConverterServices.getSourceAndConverterDisplayService().remove(bdvh, sacs);
		}
	}
}
