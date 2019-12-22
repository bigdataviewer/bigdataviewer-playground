package sc.fiji.bdvpg.bdv;

import bdv.util.BdvHandle;

public class BdvCreatorDemo
{
	public static void main( String[] args )
	{
		final BdvCreator bdvCreator = new BdvCreator( false, "Hello World!" );
		bdvCreator.run();
		final BdvHandle bdvHandle = bdvCreator.get();
	}
}
