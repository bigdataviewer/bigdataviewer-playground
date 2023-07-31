/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2023 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.bdvpg.scijava.services;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import com.google.gson.Gson;
import ij.Prefs;
import net.imglib2.util.Pair;
import org.scijava.Context;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptService;
import org.scijava.service.AbstractService;
import org.scijava.service.SciJavaService;
import org.scijava.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.services.ui.BdvHandleFilterNode;
import sc.fiji.bdvpg.scijava.services.ui.SourceFilterNode;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.bdv.supplier.DefaultBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.IBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.SerializableBdvOptions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import sc.fiji.bdvpg.services.IViewerService;
import sc.fiji.bdvpg.viewer.ViewerHelper;
import sc.fiji.persist.ScijavaGsonHelper;

import javax.swing.tree.DefaultTreeModel;

import static sc.fiji.bdvpg.bdv.BdvHandleHelper.LAST_ACTIVE_BDVH_KEY;

/**
 * SciJava Service which handles the Display of SourceAndConverters in one
 * or multiple BDV Windows Pairs with SourceAndConverterService, but this
 * service is optional Handling multiple Sources displayed in potentially
 * multiple BDV Windows Make its best to keep in synchronizations all of this,
 * without creating errors nor memory leaks
 */

@SuppressWarnings("unused") // Because SciJava parameters are filled through
														// reflection
@Plugin(type = Service.class, headless = false)
public class BdvService extends AbstractService
	implements SciJavaService, IViewerService<BdvHandle>
{

	protected static final Logger logger = LoggerFactory.getLogger(
		BdvService.class);

	public static final String CONVERTER_SETUP = "ConverterSetup";

	/**
	 * Used to add Aliases for BdvHandle objects
	 **/
	@Parameter
	ScriptService scriptService;

	/**
	 * Service containing all registered BDV Sources
	 **/
	@Parameter
	SourceAndConverterService sourceAndConverterService;

	/**
	 * Used to retrieve the last active BDV Windows (if the activated callback has
	 * been set right)
	 **/
	@Parameter
	GuavaWeakCacheService cacheService;

	@Parameter
	ObjectService os;

	@Parameter
	Context ctx;

	Supplier<BdvHandle> bdvSupplier; // = new DefaultBdvSupplier(new
																		// SerializableBdvOptions());

	@Override
	public void setDefaultViewerSupplier(Supplier<BdvHandle> bdvSupplier) {
		if (bdvSupplier instanceof IBdvSupplier) {
			setDefaultViewerSupplier((IBdvSupplier) bdvSupplier);
		} else {
			throw new UnsupportedOperationException(bdvSupplier+" is not a "+IBdvSupplier.class.getSimpleName()+" object");
		}
	}

	@Override
	public Class<BdvHandle> getViewerType() {
		return BdvHandle.class;
	}

	/**
	 * Can be used to change how Bdv Windows are created
	 * 
	 * @param bdvSupplier supplier of bdv window
	 */
	public void setDefaultViewerSupplier(IBdvSupplier bdvSupplier) {
		this.bdvSupplier = bdvSupplier;

		logger.info(" --- Serializing to save default bdv window of class " +
			bdvSupplier.getClass().getSimpleName());
		Gson gson = ScijavaGsonHelper.getGson(ctx, true);
		String bdvSupplierSerialized = gson.toJson(bdvSupplier, IBdvSupplier.class);
		logger.info("Bdv Supplier serialized into : " + bdvSupplierSerialized);
		// Saved in prefs for next session
		Prefs.set("bigdataviewer.playground.supplier", bdvSupplierSerialized);
	}

	@Override
	public BdvHandle getNewViewer() {

		if (bdvSupplier == null) {
			logger.debug(" --- Fetching or generating default bdv window");
			Gson gson = ScijavaGsonHelper.getGson(ctx);
			String defaultBdvViewer = gson.toJson(new DefaultBdvSupplier(
				new SerializableBdvOptions()), IBdvSupplier.class);
			String bdvSupplierJson = Prefs.get("bigdataviewer.playground.supplier",
				defaultBdvViewer);
			try {
				bdvSupplier = gson.fromJson(bdvSupplierJson, IBdvSupplier.class);
			}
			catch (Exception e) {
				e.printStackTrace();
				logger.info("Restoring default bdv supplier");
				bdvSupplier = new DefaultBdvSupplier(new SerializableBdvOptions());
				String bdvSupplierSerialized = gson.toJson(bdvSupplier,
					IBdvSupplier.class);
				logger.debug("Bdv Supplier serialized into : " + bdvSupplierSerialized);
				// Saved in prefs for next session
				Prefs.set("bigdataviewer.playground.supplier", bdvSupplierSerialized);
			}
		}

		BdvHandle bdvh = bdvSupplier.get();
		this.registerViewer(bdvh); // We always want it to be registered
		return bdvh;
	}

	/**
	 * @return the last active BDV or create a new one
	 */
	@Override
	public BdvHandle getActiveViewer() {
		List<BdvHandle> bdvhs = os.getObjects(BdvHandle.class);
		if ((bdvhs == null) || (bdvhs.size() == 0)) {
			return getNewViewer();
		}

		if (bdvhs.size() == 1) {
			return bdvhs.get(0);
		}
		else {
			// Get the one with the most recent focus ?
			Optional<BdvHandle> bdvh = bdvhs.stream().filter(b -> b.getViewerPanel()
				.hasFocus()).findFirst();
			if (bdvh.isPresent()) {
				return bdvh.get();
			}
			else {
				if (cacheService.get(LAST_ACTIVE_BDVH_KEY) != null) {
					WeakReference<BdvHandle> wr_bdv_h =
						(WeakReference<BdvHandle>) cacheService.get(LAST_ACTIVE_BDVH_KEY);
					return wr_bdv_h.get();
				}
				else {
					return null;
				}
			}
		}
	}

	/**
	 * Displays a Source, the last active bdvh is chosen since none is specified
	 * in this method
	 * 
	 * @param sacs sources to display
	 */
	@Override
	public void show(SourceAndConverter<?>... sacs) {
		show(getActiveViewer(), sacs);
	}

	/**
	 * Makes visible or invisible a source, applies this to all bdvs according to
	 * BdvhReferences
	 * 
	 * @param sac source
	 * @param visible whether to set it visible
	 */
	@Override
	public void setVisible(SourceAndConverter<?> sac, boolean visible) {
		getViewersOf(sac).forEach(bdvhr -> bdvhr.getViewerPanel().state()
				.setSourceActive(sac, visible));
	}

	/**
	 * @param sac source to display
	 * @param bdvh bdv window where to check this property
	 * @return true if the source is visible
	 */
	@Override
	public boolean isVisible(SourceAndConverter<?> sac, BdvHandle bdvh) {
		return bdvh.getViewerPanel().state().isSourceActive(sac);
	}

	/**
	 * Displays a BDV SourceAndConverter into the specified BdvHandle This
	 * function really is the core of this service It mimicks or copies the
	 * functions of BdvVisTools because it is responsible to create converter,
	 * volatiles, convertersetups and so on
	 * 
	 * @param sacs sources to display
	 * @param bdvh bdvhandle to append the sources
	 */
	@Override
	public void show(BdvHandle bdvh, SourceAndConverter<?>... sacs) {
		show(bdvh, true, sacs);
	}

	/**
	 * Displays a BDV SourceAndConverter into the specified BdvHandle This
	 * function really is the core of this service It mimics or copies the
	 * functions of BdvVisTools because it is responsible to create converter,
	 * volatiles, converter setups and so on
	 * 
	 * @param sacs sources to display
	 * @param visible whether to make the source active (=visible)
	 * @param bdvh bdvhandle to append the sources
	 */
	@Override
	public void show(BdvHandle bdvh, boolean visible,
		SourceAndConverter<?>... sacs)
	{

		List<SourceAndConverter<?>> sacsToDisplay = new ArrayList<>();

		for (SourceAndConverter<?> sac : sacs) {
			if (!sourceAndConverterService.isRegistered(sac)) {
				sourceAndConverterService.register(sac);
			}

			boolean escape = false;

			if (bdvh.getViewerPanel().state().getSources().contains(sac)) {
				escape = true;
			}

			// Do not display 2 times the same source and converter
			if (sacsToDisplay.contains(sac)) {
				escape = true;
			}

			if (!escape) {
				sacsToDisplay.add(sac);
				bdvh.getConverterSetups().put(sac, sourceAndConverterService
					.getConverterSetup(sac));
			}
		}

		// Actually display the sources -> repaint called only once!
		bdvh.getViewerPanel().state().addSources(sacsToDisplay);
		// And make them active
		bdvh.getViewerPanel().state().setSourcesActive(sacsToDisplay, visible);
	}

	/**
	 * Removes a SourceAndConverter from all BdvHandle displaying this
	 * SourceAndConverter Updates all references of other Sources present
	 * 
	 * @param sacs sources to remove
	 */
	@Override
	public void removeFromAllViewers(SourceAndConverter<?>... sacs) {
		getViewersOf(sacs).forEach(bdv -> bdv.getViewerPanel().state()
			.removeSources(Arrays.asList(sacs)));
	}

	/**
	 * Removes a SourceAndConverter from the active Bdv Updates all references of
	 * other Sources present
	 * 
	 * @param sacs sources to remove from active bdv
	 */
	@Override
	public void removeFromActiveViewer(SourceAndConverter<?>... sacs) {
		// This condition avoids creating a window for nothing
		if (os.getObjects(BdvHandle.class).size() > 0) {
			remove(getActiveViewer(), sacs);
		}
	}

	/**
	 * Removes a SourceAndConverter from a BdvHandle Updates all references of
	 * other Sources present
	 * 
	 * @param bdvh bdvhandle
	 * @param sacs Array of SourceAndConverter
	 */
	@Override
	public void remove(BdvHandle bdvh, SourceAndConverter<?>... sacs) {
		bdvh.getViewerPanel().state().removeSources(Arrays.asList(sacs));
		bdvh.getViewerPanel().requestRepaint();
	}

	/**
	 * Service initialization
	 */
	@Override
	public void initialize() {
		scriptService.addAlias(BdvHandle.class);
		displayToMetadata = CacheBuilder.newBuilder().weakKeys().build();// new
																																			// HashMap<>();
		sourceAndConverterService.addViewerService(this);
		SourceAndConverterServices.setBDVService(this);
		// Catching bdv supplier from Prefs
		logger.debug("Bdv Playground Display Service initialized.");
	}

	/**
	 * Closes appropriately a BdvHandle which means that it updates the callbacks
	 * for ConverterSetups and updates the ObjectService
	 * 
	 * @param bdvh bdvhandle to close
	 */
	@Override
	public void closeViewer(BdvHandle bdvh) {
		os.removeObject(bdvh);
		displayToMetadata.invalidate(bdvh); // enables memory release on GC - even
																				// if it bdv was weekly referenced

		// Fix BigWarp closing issue
		boolean isPaired = pairedBdvs.stream().anyMatch(p -> (p.getA() == bdvh) ||
			(p.getB() == bdvh));
		if (isPaired) {
			Pair<BdvHandle, BdvHandle> pair = pairedBdvs.stream().filter(p -> (p
				.getA() == bdvh) || (p.getB() == bdvh)).findFirst().get();
			pairedBdvs.remove(pair);
			if (pair.getA() == bdvh) {
				closeViewer(pair.getB());
			}
			else {
				closeViewer(pair.getA());
			}
		}
	}

	/**
	 * Enables proper closing of Big Warp paired BdvHandles
	 */
	final List<Pair<BdvHandle, BdvHandle>> pairedBdvs = new ArrayList<>();

	@Override
	public void bindClosing(BdvHandle bdv1, BdvHandle bdv2) {
		pairedBdvs.add(new Pair<BdvHandle, BdvHandle>() {

			@Override
			public BdvHandle getA() {
				return bdv1;
			}

			@Override
			public BdvHandle getB() {
				return bdv2;
			}
		});
	}

	/**
	 * Registers a SourceAndConverter which has originated from a BdvHandle Useful
	 * for BigWarp where the grid and the deformation magnitude SourceAndConverter
	 * are created into BigWarp
	 * 
	 * @param bdvh_in bdvhandle fetched for registration
	 */
	@Override
	public void registerSourcesFromViewer(BdvHandle bdvh_in) {
		bdvh_in.getViewerPanel().state().getSources().forEach(sac -> {
			if (!sourceAndConverterService.isRegistered(sac)) {
				sourceAndConverterService.register(sac);
			}
		});
	}

	/**
	 * Updates bdvHandles which are displaying at least one of these sacs
	 * Potentially improvement is to check whether the timepoint need an update ?
	 * 
	 * @param sacs sources to update
	 */
	public void updateViewersOf(SourceAndConverter<?>... sacs) {
		getViewersOf(sacs).forEach(bdvHandle -> bdvHandle.getViewerPanel()
			.requestRepaint());
	}

	/**
	 * Returns the list of sacs held within a BdvHandle ( whether they are visible
	 * or not ) List is ordered by index in the BdvHandle - complexification to
	 * implement the mixed projector TODO : Avoid duplicates by returning a Set
	 * 
	 * @param bdvHandle the bdvhandle
	 * @return all sources present in a bdvhandle
	 */
	public List<SourceAndConverter<?>> getSourceAndConverterOf(
		BdvHandle bdvHandle)
	{
		return bdvHandle.getViewerPanel().state().getSources();
	}

	/**
	 * Returns a List of BdvHandle which are currently displaying a sac Returns an
	 * empty set in case the sac is not displayed
	 * 
	 * @param sacs the sources queried
	 * @return all bdvhandle which contain the source
	 */
	@Override
	public Set<BdvHandle> getViewersOf(SourceAndConverter<?>... sacs) {
		if (sacs == null) {
			return new HashSet<>();
		}

		List<SourceAndConverter<?>> sacList = Arrays.asList(sacs);

		return os.getObjects(BdvHandle.class).stream().filter(bdv -> {
			synchronized (bdv.getViewerPanel().state()) {
				return bdv.getViewerPanel().state().getSources().stream().anyMatch(
					sacList::contains);
			}
		}).collect(Collectors.toSet());

	}

	@Override
	public List<BdvHandle> getViewers() {
		return os.getObjects(BdvHandle.class);
	}

	/**
	 * Map containing objects that are 1 to 1 linked to a Display ( a BdvHandle
	 * object ) Keys are Weakly referenced -> Metadata should be GCed if
	 * referenced only here
	 */
	Cache<BdvHandle, Map<String, Object>> displayToMetadata;

	@Override
	@SuppressWarnings("ConstantConditions")
	public void setViewerMetadata(BdvHandle bdvh, String key, Object data) {
		if (bdvh == null) {
			logger.error("Error : bdvh is null in setMetadata function! ");
			return;
		}
		if (displayToMetadata.getIfPresent(bdvh) == null) {
			// Create Metadata
			displayToMetadata.put(bdvh, new HashMap<>());
		}
		displayToMetadata.getIfPresent(bdvh).put(key, data);
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public Object getViewerMetadata(BdvHandle bdvh, String key) {
		if (displayToMetadata.getIfPresent(bdvh) != null) {
			return displayToMetadata.getIfPresent(bdvh).get(key);
		}
		else {
			return null;
		}
	}

	@Override
	public void registerViewer(BdvHandle bdvh) {
		// ------------ Register BdvHandle in ObjectService
		if (!os.getObjects(BdvHandle.class).contains(bdvh)) { // adds it only if not
																													// already present in
																													// ObjectService
			os.addObject(bdvh);

			// ------------ Renames window to ensure unicity
			String windowTitle = ViewerHelper.getViewerTitle(bdvh.getViewerPanel());
			windowTitle = BdvHandleHelper.getUniqueWindowTitle(os, windowTitle);
			ViewerHelper.setViewerTitle(bdvh.getViewerPanel(), windowTitle);

			// ------------ Event handling in bdv sourceandconverterserviceui
			final SourceAndConverterService sacService =
				(SourceAndConverterService) SourceAndConverterServices
					.getSourceAndConverterService();
			DefaultTreeModel model = sacService.getUI().getTreeModel();
			BdvHandleFilterNode node = new BdvHandleFilterNode(model, windowTitle,
				bdvh);
			node.add(new SourceFilterNode(model, "All Sources", (sac) -> true, true));

			// ------------ Allows to remove the BdvHandle from the objectService when
			// closed by the user
			BdvHandleHelper.setBdvHandleCloseOperation(bdvh, cacheService, this, true,
				() -> sacService.getUI().removeBdvHandleNodes(bdvh));

			((SourceFilterNode) sacService.getUI().getTreeModel().getRoot()).insert(
				node, 0);
		}
	}

}
