/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Service which centralizes BDV Sources, independently of their display
 * BDV Sources can be registered to this Service.
 * This service stores Sources but on top of it,
 * It contains a Map which contains any object which can be linked to the sourceandconverter.
 *
 * Objects needed for display should be created by a  BdvSourceAndConverterDisplayService
 * - Converter to ARGBType, ConverterSetup, and Volatile view
 *
 * TODO : Think more carefully : maybe the volatile sourceandconverter should be done here...
 * Because when multiply wrapped sourceandconverter end up here, it maybe isn't possible to make the volatile view
 */

public interface ISourceAndConverterService
{

    /**
     * Reserved key for the data map. data.get(sourceandconverter).get(SPIM_DATA)
     * is expected to return a List of Spimdata Objects which refer to this sourceandconverter
     * whether a list of necessary is not obvious at the moment
     * TODO : make an example
     */
    String SPIM_DATA_INFO = "SPIMDATA";

    String SPIM_DATA_LOCATION = "SPIM_DATA_LOCATION";

    /**
     * Test if a Source is already registered in the Service
     * @param src source
     * @return if the source is already registered
     */
    boolean isRegistered(SourceAndConverter src);

    /**
     * Register a BDV Source in this Service.
     * Called in the BdvSourcePostProcessor
     * @param src source
     */
    void register(SourceAndConverter src);

    /**
     * Registers all sources contained with a SpimData Object in this Service.
     * Called in the BdvSourcePostProcessor
     * @param asd spimdata to register
     */
    void register(AbstractSpimData asd);

    /**
     * @return list of all registered sources
     */
    List<SourceAndConverter> getSourceAndConverters();

    /**
     * Return sources assigned to a SpimDataObject
     * @param asd spimdata to check
     * @return a list of all sources contained in this spimdata object, provided it has been registered
     */
    List<SourceAndConverter> getSourceAndConverterFromSpimdata(AbstractSpimData asd);

    /**
     * Removes a BDV Source in this Service.
     * Called in the BdvSourcePostProcessor
     * @param sac source
     */
    void remove(SourceAndConverter... sac);


    void linkToSpimData(SourceAndConverter sac, AbstractSpimData asd, int idSetup);

    /**
     * Adds metadata for a sac
     * @param sac source
     * @param key of the metadata
     * @param data object to keep / store
     */
    void setMetadata(SourceAndConverter sac, String key, Object data);

    /**
     * Adds metadata for a sac
     * @param sac source
     * @param key of the metadata
     * @return metadata object
     */
    Object getMetadata(SourceAndConverter sac, String key);

    /**
     * Removes metadata for a sac
     * @param sac source
     * @param key of the metadata
     */
    void removeMetadata(SourceAndConverter sac, String key);

    /**
     * @param sac source to check
     * @return keys of all metadata linked to the source
     */
    Collection<String> getMetadataKeys(SourceAndConverter sac);

    /**
     * Convenient method to know if a metadata for a sac exists
     * @param sac source to check
     * @param key of the metadata to check
     * @return true if a metadata for such key is present
     */
    boolean containsMetadata(SourceAndConverter sac, String key);

    /**
     * Finds the list of corresponding registered sac for a source. This
     * is convenient to know because a single source can be associated with
     * multiple sourceandconverters
     * @param source to check ( not a sourceandconverter )
     * @return a list of all sacs which wraps the same underlying source
     */
    List<SourceAndConverter> getSourceAndConvertersFromSource( Source source );

    /**
     * Register an action ( a consumer of sourceandconverter array)
     * @param actionName action name
     * @param action the action itself
     * TODO : link a description ?
     */
    void registerAction(String actionName, Consumer<SourceAndConverter[]> action);

    /**
     * Removes an action from the registration
     * @param actionName action to remove
     */
    void removeAction(String actionName);

    /**
     * @return a list of action name / keys / identifiers
     */
    Set<String> getActionsKeys();

    /**
     * Gets an action from its identifier
     * @param actionName identifier of the action
     * @return the action
     */
    Consumer<SourceAndConverter[]> getAction(String actionName);

    /**
     * @return all SpimDatas present in the service
     */
    Set<AbstractSpimData> getSpimDatasets();

    /**
     * Attach a name to a SpimDataObject
     * @param asd spimdata
     * @param name name of the spimdata
     */
    void setSpimDataName(AbstractSpimData asd, String name);


    /**
     * Adds metadata for a sac
     * @param key key of metadata object
     * @param data metadata object
     * @param asd the spimdata to add some metadata (a file or a name)
     */
    void setMetadata(AbstractSpimData asd, String key, Object data);

    /**
     * Adds metadata for a sac
     * TODO : tell what's happening is the spimdata is not registered
     * @param asd the spimdata where to attach this metadata
     * @param key the key
     * @return the metadata object
     */
    Object getMetadata(AbstractSpimData asd, String key);

    /**
     * Gets source and converter of the source
     * @param sac the source
     * @return the convertersetup associated to the source
     */
    ConverterSetup getConverterSetup(SourceAndConverter sac);

    /**
     * Gets metadata of a spimdata
     * @param asd dataset
     * @return the metadata of a spimdata
     */
    Collection<String> getMetadataKeys(AbstractSpimData asd);

    /**
     * Convenient method to know if a metadata for a spimdata exists
     * @param key  metadata key to check
     * @param asd spimdata object
     * @return is the spimdata contains this metadata
     */
    boolean containsMetadata(AbstractSpimData asd, String key);

}
