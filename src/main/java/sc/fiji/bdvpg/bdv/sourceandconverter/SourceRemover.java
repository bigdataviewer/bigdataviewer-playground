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

public class SourceRemover implements Runnable, Consumer<SourceAndConverter>
{
	SourceAndConverter srcIn;
	BdvHandle bdvh;

	public SourceRemover(BdvHandle bdvh, SourceAndConverter srcIn) {
		this.srcIn=srcIn;
		this.bdvh=bdvh;
	}

	public SourceRemover(SourceAndConverter srcIn) {
		this.srcIn=srcIn;
		this.bdvh=null;
	}

	public SourceRemover() {
		this.srcIn=null;
		this.bdvh=null;
	}

	public void run() {
		accept(srcIn);
	}

	@Override
	public void accept(SourceAndConverter source) {
		if (bdvh==null) {
			// Remove from all displays
			SourceAndConverterServices.getSourceAndConverterDisplayService().removeFromAllBdvs(source);
		} else {
			// Remove from a specific bdvHandle
			SourceAndConverterServices.getSourceAndConverterDisplayService().remove(bdvh, source);
		}
	}
}
