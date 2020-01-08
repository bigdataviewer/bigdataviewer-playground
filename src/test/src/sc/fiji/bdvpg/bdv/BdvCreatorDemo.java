package sc.fiji.bdvpg.bdv;

import bdv.util.BdvHandle;
import sc.fiji.bdvpg.services.BdvService;

public class BdvCreatorDemo
{
	public static void main( String[] args )
	{
		// Initializes static SourceService and Display Service
		BdvService.InitScijavaServices();

		final BdvHandle bdvHandle = BdvService.getSourceDisplayService().getActiveBdv();
	}
}
