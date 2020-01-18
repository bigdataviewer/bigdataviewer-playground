package sc.fiji.bdvpg.bdv;

import sc.fiji.bdvpg.services.SourceAndConverterServices;

public class BdvCreatorDemo
{
	public static void main( String[] args )
	{
		// Initializes static SourceService and Display Service
		SourceAndConverterServices.InitScijavaServices();

		// Creates a Bdv since none exists yet
		SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();
	}
}
