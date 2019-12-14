package sc.fiji.util;

import java.io.File;

public class Util
{
	public static String[] fileArrayToStringArray( File[] files )
	{
		final String[] filePaths = new String[ files.length ];

		for ( int i = 0; i < filePaths.length; i++ )
		{
			filePaths[ i ] = files[ i ].getAbsolutePath();
		}
		return filePaths;
	}
}
