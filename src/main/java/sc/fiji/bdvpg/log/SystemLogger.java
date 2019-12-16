package sc.fiji.bdvpg.log;

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
