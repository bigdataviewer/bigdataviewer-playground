package sc.fiji.bdvpg;

public abstract class PlaygroundPrefs
{
	private static boolean sourceAndConverterUIVisibility = true;

	public static void setSourceAndConverterUIVisibility( boolean visibility )
	{
		sourceAndConverterUIVisibility = visibility;
	}

	public static boolean getSourceAndConverterUIVisibility()
	{
		return sourceAndConverterUIVisibility;
	}
}
