package sc.fiji.bdvpg.bdv;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.Interpolation;
import bdv.viewer.ViewerOptions;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.InputTriggerConfigHelper;
import org.scijava.ui.behaviour.io.InputTriggerDescriptionsBuilder;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.behaviour.ClickBehaviourInstaller;
import sc.fiji.bdvpg.behaviour.SourceAndConverterContextMenuClickBehaviour;
import sc.fiji.bdvpg.scijava.command.bdv.ScreenShotMakerCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.io.IOException;
import java.util.function.Supplier;

public class BdvCreator implements Runnable, Supplier<BdvHandle>
{
	private BdvOptions bdvOptions;
	private boolean interpolate;
	private BdvHandle bdv;
	private int numTimePoints;

	public BdvCreator( )
	{
		this.bdvOptions = BdvOptions.options();
		this.interpolate = false;
		this.numTimePoints = 1;
	}

	public BdvCreator( BdvOptions bdvOptions  )
	{
		this.bdvOptions = bdvOptions;
		this.interpolate = false;
		this.numTimePoints = 1;
	}

	public BdvCreator( BdvOptions bdvOptions, boolean interpolate )
	{
		this.bdvOptions = bdvOptions;
		this.interpolate = interpolate;
		this.numTimePoints = 1;
	}

	public BdvCreator( BdvOptions bdvOptions, boolean interpolate, int numTimePoints )
	{
		this.bdvOptions = bdvOptions;
		this.interpolate = interpolate;
		this.numTimePoints = numTimePoints;
	}

	@Override
	public void run()
	{
		createEmptyBdv();
	}

	/**
	 * Hack: add an image and remove it after the
	 * bdvHandle has been created.
	 */
	private void createEmptyBdv()
	{
		ArrayImg dummyImg = ArrayImgs.bytes(2, 2, 2);

		bdvOptions = bdvOptions.sourceTransform( new AffineTransform3D() );

		String yamlDataLocation = "bdvkeyconfig.yaml";
		try {
			yamlConf = new InputTriggerConfig( YamlConfigIO.read( yamlDataLocation ) );
		} catch (final Exception e) {
			System.err.println("Could not find "+yamlDataLocation+" file. Create it.");
			try {
				YamlConfigIO.write(null, yamlDataLocation);
				yamlConf = new InputTriggerConfig( YamlConfigIO.read( yamlDataLocation ) );
			} catch (IOException ex) {
				ex.printStackTrace();
				System.err.println("Could not create yaml file : settings will not be saved.");
				yamlConf = new InputTriggerConfig();
			}
		}
		//bdvOptions.inputTriggerConfig(yamlConf);
		// "transform" ou "bdv"

		BdvStackSource bss = BdvFunctions.show( dummyImg, "dummy", bdvOptions );

		bdv = bss.getBdvHandle();

		/*Behaviours behaviour_bdvpg = new Behaviours( yamlConf, "bdvpg" );
		behaviour_bdvpg.updateKeyConfig(yamlConf);
		InputTriggerConfigHelper.getInputTriggerMap(yamlConf);*/

		bdv.getTriggerbindings().addInputTriggerMap("bdvpg", InputTriggerConfigHelper.getInputTriggerMap(yamlConf), "transform");

		bdv.getKeybindings().addInputMap("bdvpg", new InputMap(), "bdv", "navigation");
		// bdv a des trucs

		if ( interpolate ) bdv.getViewerPanel().setInterpolation( Interpolation.NLINEAR );

		bdv.getViewerPanel().state().removeSource(bdv.getViewerPanel().state().getCurrentSource());

		bdv.getViewerPanel().setNumTimepoints(numTimePoints);

		//addBehaviours();

		System.out.println("--------------------- Behaviours");
		bdv.getTriggerbindings().getConcatenatedBehaviourMap().getAllBindings().forEach((label,behaviour) -> {
			System.out.println(label);
			System.out.println("\t"+behaviour.getClass().getSimpleName());
		});
		System.out.println("--------------------- Triggers");
		bdv.getTriggerbindings().getConcatenatedInputTriggerMap().getAllBindings().forEach((trigger, actions) -> {
			System.out.println(trigger);
			for (String action : actions)
				System.out.println("\t"+action);
		});
		System.out.println("--------------------- Mine");
		InputTriggerConfigHelper.getInputTriggerMap(yamlConf).getAllBindings().forEach((trigger, actions) -> {
			System.out.println(trigger);
			for (String action : actions)
				System.out.println("\t"+action);
		});

		System.out.println("--------------------- Key Action");
		for (Object o : bdv.getKeybindings().getConcatenatedActionMap().allKeys()) {
			System.out.println("\t"+o);
		}
		System.out.println("--------------------- Key Triggers");
/*		for (KeyStroke ks : bdv.getKeybindings().getConcatenatedInputMap().allKeys()) {
			System.out.println("\t"+ks+":"+bdv.getKeybindings().getConcatenatedInputMap().get(ks));
		}*/
	}
	InputTriggerConfig yamlConf;
	private void addBehaviours()
	{

		//addSourceAndConverterContextMenuBehaviour();
		//new InputTriggerDescriptionsBuilder(yamlConf).
		//bdv.getTriggerbindings().addInputTriggerMap("bdv playground", yamlConf.);
		//bdv.getTriggerbindings().getConcatenatedInputTriggerMap();
		//bdv.getTriggerbindings().addInputTriggerMap();

	}

	private void addSourceAndConverterContextMenuBehaviour()
	{
		final ClickBehaviourInstaller installerPopup = new ClickBehaviourInstaller( bdv, new SourceAndConverterContextMenuClickBehaviour( bdv ) );

		installerPopup.install( "Sources context menu - C", "C" );
		installerPopup.install( "Sources context menu - Right mouse button", "button3" );

		String actionScreenshotName = SourceAndConverterService.getCommandName(ScreenShotMakerCommand.class);
		final ClickBehaviourInstaller installerScreenshot = new ClickBehaviourInstaller( bdv, (x,y) -> {
			SourceAndConverterServices.getSourceAndConverterService().getAction(actionScreenshotName).accept(null);
		} );

		installerScreenshot.install("Screenshot", "D" );

	}

	public BdvHandle get()
	{
		if ( bdv == null ) run();

		return bdv;
	}
}
