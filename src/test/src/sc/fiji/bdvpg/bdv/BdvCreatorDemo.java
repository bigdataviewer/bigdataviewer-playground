package sc.fiji.bdvpg.bdv;

import bdv.util.BdvHandle;

public class BdvCreatorDemo
{
	public static void main( String[] args )
	{
		final BdvCreator bdvCreator = new BdvCreator( );
		bdvCreator.run();

		// This should be equivalent to
		// final BdvHandle bdvHandle = BdvService.getSourceDisplayService().getActiveBdv();
		final BdvHandle bdvHandle = bdvCreator.get();
	}
}
