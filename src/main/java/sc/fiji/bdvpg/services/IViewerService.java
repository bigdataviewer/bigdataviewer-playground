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

package sc.fiji.bdvpg.services;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * SciJava Service which handles the viewers (BDV or BVV or ImagePlus) of SourceAndConverters in one
 * or multiple viewers
 * Such services are used in conjunction a SourceAndConverterService, but this
 * service is optional
 *
 * Handling multiple Sources displayed in potentially
 * multiple BDV Windows Make its best to keep in synchronizations all of this,
 * without creating errors nor memory leaks
 */


public interface IViewerService<T>
{

	void setDefaultViewerSupplier(Supplier<T> viewerSupplier);

	T getNewViewer();

	/**
	 * @return the last active BDV or create a new one
	 */
	T getActiveViewer();

	/**
	 * Displays a Source, the last active bdvh is chosen since none is specified
	 * in this method
	 * 
	 * @param sacs sources to display
	 */
	void show(SourceAndConverter<?>... sacs);

	/**
	 * Makes visible or invisible a source, applies this to all bdvs according to
	 * BdvhReferences
	 * 
	 * @param sac source
	 * @param visible whether to set it visible
	 */
	void setVisible(SourceAndConverter<?> sac, boolean visible);

	/**
	 * @param sac source to display
	 * @param viewer bdv window where to check this property
	 * @return true if the source is visible
	 */
	boolean isVisible(SourceAndConverter<?> sac, T viewer);
	/**
	 * Displays a BDV SourceAndConverter into the specified BdvHandle This
	 * function really is the core of this service It mimicks or copies the
	 * functions of BdvVisTools because it is responsible to create converter,
	 * volatiles, convertersetups and so on
	 * 
	 * @param sacs sources to display
	 * @param viewer bdvhandle to append the sources
	 */
	 void show(T viewer, SourceAndConverter<?>... sacs);

	/**
	 * Displays a BDV SourceAndConverter into the specified BdvHandle This
	 * function really is the core of this service It mimics or copies the
	 * functions of BdvVisTools because it is responsible to create converter,
	 * volatiles, converter setups and so on
	 * 
	 * @param sacs sources to display
	 * @param visible whether to make the source active (=visible)
	 * @param viewer viewer to append the sources
	 */
	void show(T viewer, boolean visible, SourceAndConverter<?>... sacs);

	/**
	 * Removes a SourceAndConverter from all BdvHandle displaying this
	 * SourceAndConverter Updates all references of other Sources present
	 * 
	 * @param sacs sources to remove
	 */
	void removeFromAllViewers(SourceAndConverter<?>... sacs);

	/**
	 * Removes a SourceAndConverter from the active Bdv Updates all references of
	 * other Sources present
	 * 
	 * @param sacs sources to remove from active bdv
	 */
	void removeFromActiveViewer(SourceAndConverter<?>... sacs);

	/**
	 * Removes a SourceAndConverter from a BdvHandle Updates all references of
	 * other Sources present
	 * 
	 * @param viewer viewer
	 * @param sacs Array of SourceAndConverter
	 */
	void remove(T viewer, SourceAndConverter<?>... sacs);


	/**
	 * Closes appropriately a BdvHandle which means that it updates the callbacks
	 * for ConverterSetups and updates the ObjectService
	 * 
	 * @param viewer that needs to be closed
	 */
	void closeViewer(T viewer);

	/**
	 * Enables proper closing of Big Warp paired BdvHandles
	 */
	final List<Pair<BdvHandle, BdvHandle>> pairedBdvs = new ArrayList<>();

	void bindClosing(T viewer1, T viewer2);

	/**
	 * Registers a SourceAndConverter which has originated from a BdvHandle Useful
	 * for BigWarp where the grid and the deformation magnitude SourceAndConverter
	 * are created into BigWarp
	 * 
	 * @param viewer viewer fetched for registration
	 */
	void registerSourcesFromViewer(T viewer);

	/**
	 * Updates bdvHandles which are displaying at least one of these sacs
	 * Potentially improvement is to check whether the timepoint need an update ?
	 * 
	 * @param sacs sources to update
	 */
	void updateViewersOf(SourceAndConverter<?>... sacs);

	/**
	 * Returns the list of sacs held within a BdvHandle ( whether they are visible
	 * or not ) List is ordered by index in the BdvHandle - complexification to
	 * implement the mixed projector TODO : Avoid duplicates by returning a Set
	 * 
	 * @param viewer the viewer
	 * @return all sources present in a bdvhandle
	 */
	List<SourceAndConverter<?>> getSourceAndConverterOf(T viewer);

	/**
	 * Returns a List of BdvHandle which are currently displaying a sac Returns an
	 * empty set in case the sac is not displayed
	 * 
	 * @param sacs the sources queried
	 * @return all bdvhandle which contain the source
	 */
	Set<T> getViewersOf(SourceAndConverter<?>... sacs);

	List<T> getViewers();

	void setViewerMetadata(T viewer, String key, Object data);

	Object getViewerMetadata(T viewer, String key);

	void registerViewer(T viewer);

}
