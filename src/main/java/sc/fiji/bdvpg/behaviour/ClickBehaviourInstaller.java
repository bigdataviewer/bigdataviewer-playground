package sc.fiji.bdvpg.behaviour;

import bdv.util.BdvHandle;
import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

public class ClickBehaviourInstaller
{
	private final BdvHandle bdvHandle;
	private final Behaviour behaviour;

	public ClickBehaviourInstaller( BdvHandle bdvHandle, ClickBehaviour behaviour )
	{
		this.bdvHandle = bdvHandle;
		this.behaviour = behaviour;
	}

	/**
	 * TODO: probably just create one behaviour for each BDV?
	 *
	 * @param name
	 * @param trigger
	 */
	public void install( String name, String trigger )
	{
		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdvHandle.getTriggerbindings(), name );
		behaviours.behaviour( behaviour, name, trigger ) ;
	}
}
