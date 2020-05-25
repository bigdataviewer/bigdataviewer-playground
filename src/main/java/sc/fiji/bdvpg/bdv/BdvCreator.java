package sc.fiji.bdvpg.bdv;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.Interpolation;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.InputTriggerConfigHelper;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.bdv.config.BdvSettingsGUISetter;
import sc.fiji.bdvpg.behaviour.SourceAndConverterContextMenuClickBehaviour;
import sc.fiji.bdvpg.scijava.command.bdv.ScreenShotMakerCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.swingdnd.BdvTransferHandler;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.io.File;
import java.util.function.Supplier;

/**
 * BigDataViewer Playground Action -->
 *
 * Creates a new {@link bdv.BigDataViewer} instance accessible through the {@link BdvHandle} interface
 *
 */

public class BdvCreator implements Runnable, Supplier<BdvHandle>
{

	private BdvOptions bdvOptions;
	private boolean interpolate;
	//private BdvHandle bdv;
	private int numTimePoints;
	private String pathToBindings;

	/**
	 * @param bdvOptions holds a list of settings for creating options see {@link BdvOptions}
	 *
	 * @param interpolate should the window use linear interpolation or nearest neighbor interpolation ?
	 *
	 * @param numTimePoints Number of timepoints contained in the creating bdv window
	 *
	 * @param pathToBindings This String should hold a reference to a path which could contains:
	 * 	 * a bdvkeyconfig.yaml file
	 * 	 * a contextmenu.txt file
	 */
	public BdvCreator( BdvOptions bdvOptions, boolean interpolate, int numTimePoints, String pathToBindings )
	{
		this.bdvOptions = bdvOptions;
		this.interpolate = interpolate;
		this.numTimePoints = numTimePoints;
		this.pathToBindings = pathToBindings;
	}

	public BdvCreator( )
	{
		this.bdvOptions = BdvOptions.options();
		this.interpolate = false;
		this.numTimePoints = 1;
		this.pathToBindings = BdvSettingsGUISetter.defaultBdvPgSettingsRootPath;
	}

	public BdvCreator( BdvOptions bdvOptions  )
	{
		this.bdvOptions = bdvOptions;
		this.interpolate = false;
		this.numTimePoints = 1;
		this.pathToBindings = BdvSettingsGUISetter.defaultBdvPgSettingsRootPath;
	}

	public BdvCreator( BdvOptions bdvOptions, boolean interpolate )
	{
		this.bdvOptions = bdvOptions;
		this.interpolate = interpolate;
		this.numTimePoints = 1;
		this.pathToBindings = BdvSettingsGUISetter.defaultBdvPgSettingsRootPath;
	}

	public BdvCreator( BdvOptions bdvOptions, boolean interpolate, int numTimePoints )
	{
		this.bdvOptions = bdvOptions;
		this.interpolate = interpolate;
		this.numTimePoints = numTimePoints;
		this.pathToBindings = BdvSettingsGUISetter.defaultBdvPgSettingsRootPath;
	}

	@Override
	public void run()
	{
		// Do nothing -> bdvhandle created with get() method
	}

	/**
	 * Hack: adds an image and remove it after the
	 * bdvHandle has been created.
	 */
	public BdvHandle get()
	{
		ArrayImg dummyImg = ArrayImgs.bytes(2, 2, 2);

		bdvOptions = bdvOptions.sourceTransform( new AffineTransform3D() );

		BdvStackSource bss = BdvFunctions.show( dummyImg, "dummy", bdvOptions );

		BdvHandle bdv = bss.getBdvHandle();

		/*if (pathToBindings!=null) {
			if (new File(pathToBindings).exists()) {
				install(bdv,pathToBindings);
			} else {
				System.err.println("Bindings path "+pathToBindings+" do not exist.");
			}
		}*/

		if ( interpolate ) bdv.getViewerPanel().setInterpolation( Interpolation.NLINEAR );

		bdv.getViewerPanel().state().removeSource(bdv.getViewerPanel().state().getCurrentSource());

		bdv.getViewerPanel().setNumTimepoints(numTimePoints);

		addBdvPlaygroundActions(bdv);

		// For drag and drop
		addCustomTransferHandler(bdv);

		return bdv;
	}

	/**
	 * Adds Bdv Playground specific actions :
	 * For now:
	 * - Screenshot
	 * - Show context menu
	 * TODO : improve this
	 */
	private void addBdvPlaygroundActions(BdvHandle bdv)
	{
		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		String actionScreenshotName = SourceAndConverterService.getCommandName(ScreenShotMakerCommand.class);
		behaviours.behaviour((ClickBehaviour) (x, y) -> SourceAndConverterServices.getSourceAndConverterService().getAction(actionScreenshotName).accept(null),
				actionScreenshotName, "D");
		behaviours.behaviour(new SourceAndConverterContextMenuClickBehaviour( bdv ), "Sources Context Menu", "button3");
		behaviours.install(bdv.getTriggerbindings(), "bdvpgactions");

	}

	/**
	 * Install trigger bindings according to the path specified
	 * See {@link BdvSettingsGUISetter}
	 * Key bindings can not be overriden yet
	 * @param bdv
	 * @param pathToBindings
	 */
	void install(BdvHandle bdv, String pathToBindings) {
		String yamlDataLocation = pathToBindings + File.separator + BdvSettingsGUISetter.defaultYamlFileName;

		InputTriggerConfig yamlConf = null;

		try {
			yamlConf = new InputTriggerConfig( YamlConfigIO.read( yamlDataLocation ) );
		} catch (final Exception e) {
			System.err.println("Could not create "+yamlDataLocation+" file. Using defaults instead.");
		}

		if (yamlConf!=null) {

			bdv.getTriggerbindings().addInputTriggerMap(pathToBindings, InputTriggerConfigHelper.getInputTriggerMap(yamlConf), "transform");

			// TODO : support replacement of key bindings bdv.getKeybindings().addInputMap("bdvpg", new InputMap(), "bdv", "navigation");
		}

	}

	/**
	 * For debugging:
	 * - print actions and triggers of a bdv
	 * @param bdv
	 */
	public static void printBindings(BdvHandle bdv) {
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
		System.out.println("--------------------- Key Action");
		for (Object o : bdv.getKeybindings().getConcatenatedActionMap().allKeys()) {
			System.out.println("\t"+o);
		}
		System.out.println("--------------------- Key Triggers");
		for (KeyStroke ks : bdv.getKeybindings().getConcatenatedInputMap().allKeys()) {
			System.out.println("\t"+ks+":"+bdv.getKeybindings().getConcatenatedInputMap().get(ks));
		}
	}

	void addCustomTransferHandler(BdvHandle bdv) {
		bdv.getViewerPanel().setTransferHandler(new BdvTransferHandler());
	}

}
