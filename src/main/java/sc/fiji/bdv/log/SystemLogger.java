package sc.fiji.bdv.log;

import sc.fiji.bdv.log.Logger;

public class SystemLogger implements Logger
{
	public void out( String msg )
	{
		System.out.println( msg );
	}

	public void err( String msg )
	{
		System.err.println( msg );
	}
}
