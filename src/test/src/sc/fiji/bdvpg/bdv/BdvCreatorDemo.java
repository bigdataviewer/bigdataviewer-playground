package sc.fiji.bdvpg.bdv;

import sc.fiji.bdvpg.services.SacServices;

public class BdvCreatorDemo
{
	public static void main( String[] args )
	{
		// Initializes static SourceService and Display Service
		SacServices.InitScijavaServices();

		// Creates a Bdv since none exists yet
		SacServices.getSourceAndConverterDisplayService().getActiveBdv();
	}
}
