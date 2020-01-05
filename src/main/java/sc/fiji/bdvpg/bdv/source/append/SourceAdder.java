package sc.fiji.bdvpg.bdv.source.append;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import sc.fiji.bdvpg.services.BdvService;

import java.util.function.Consumer;

public class SourceAdder implements Runnable, Consumer<Source>
{
	Source srcIn;
	BdvHandle bdvh;

	public SourceAdder(BdvHandle bdvh, Source srcIn) {
		this.srcIn=srcIn;
		this.bdvh=bdvh;
	}

	public void run() {
		accept(srcIn);
	}

	@Override
	public void accept(Source source) {
		BdvService.getSourceDisplayService().show(bdvh, source);
	}
}
