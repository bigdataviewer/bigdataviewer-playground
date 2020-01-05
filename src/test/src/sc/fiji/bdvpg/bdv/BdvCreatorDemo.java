package sc.fiji.bdvpg.bdv;

import bdv.util.BdvHandle;
import sc.fiji.bdvpg.services.BdvService;

public class BdvCreatorDemo
{
	public static void main( String[] args )
	{
		final BdvCreator bdvCreator = new BdvCreator( false, "Hello World!" );
		bdvCreator.run();
		final BdvHandle bdvHandle = bdvCreator.getBdvHandle();

		// This should be equivalent to
		// final BdvHandle bdvHandle = BdvService.getSourceDisplayService().getActiveBdv();
	}
}
