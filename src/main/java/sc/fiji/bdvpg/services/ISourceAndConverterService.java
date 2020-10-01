package sc.fiji.bdvpg.services;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
     * @param src
     * @return
     */
    boolean isRegistered(SourceAndConverter src);

    /**
     * Register a BDV Source in this Service.
     * Called in the BdvSourcePostProcessor
     * @param src
     */
    void register(SourceAndConverter src);

    /**
     * Registers all sources contained with a SpimData Object in this Service.
     * Called in the BdvSourcePostProcessor
     */
    void register(AbstractSpimData asd);

    /**
     * @return list of all registered sources
     */
    List<SourceAndConverter> getSourceAndConverters();

    /**
     * Return sources assigned to a SpimDataObject
     */
    List<SourceAndConverter> getSourceAndConverterFromSpimdata(AbstractSpimData asd);

    /**
     * Removes a BDV Source in this Service.
     * Called in the BdvSourcePostProcessor
     * @param sac
     */
    void remove(SourceAndConverter... sac);


    void linkToSpimData(SourceAndConverter sac, AbstractSpimData asd, int idSetup);

    /**
     * Adds metadata for a sac
     * @return
     */
    void setMetadata(SourceAndConverter sac, String key, Object data);

    /**
     * Adds metadata for a sac
     *
     * @return
     */
    Object getMetadata(SourceAndConverter sac, String key);

    /**
     * Adds metadata for a sac
     *
     * @return
     */
    Collection<String> getMetadataKeys(SourceAndConverter sac);

    /**
     * Convenient method to know if a metadata for a sac exists
     *
     * @return
     */
    boolean containsMetadata(SourceAndConverter sac, String key);

    /**
     * Finds the list of corresponding registered sac for a source.
     */
    List<SourceAndConverter> getSourceAndConvertersFromSource( Source source );

    /**
     * Register an action ( a consumer of sourceandconverter array)
     * @param actionName
     * @param action
     * TODO : link a description ?
     */
    void registerAction(String actionName, Consumer<SourceAndConverter[]> action);

    /**
     * Removes an action from the registration
     * @param actionName
     */
    void removeAction(String actionName);

    /**
     *
     * @return a list of of action name / keys / identifiers
     */
    Set<String> getActionsKeys();

    /**
     * Gets an action from its identifier
     * @param actionName
     * @return
     */
    Consumer<SourceAndConverter[]> getAction(String actionName);

    /**
     * Gets All SpimDatas present in the service
     */
    Set<AbstractSpimData> getSpimDatasets();

    /**
     * Attach a name to a SpimDataObject
     * @param asd
     * @param name
     */
    void setSpimDataName(AbstractSpimData asd, String name);


    /**
     * Adds metadata for a sac
     * @return
     */
    void setMetadata(AbstractSpimData asd, String key, Object data);

    /**
     * Adds metadata for a sac
     *
     * @return
     */
    Object getMetadata(AbstractSpimData asd, String key);

    /**
     * Adds metadata for a sac
     *
     * @return
     */
    Collection<String> getMetadataKeys(AbstractSpimData asd);

    /**
     * Convenient method to know if a metadata for a sac exists
     *
     * @return
     */
    boolean containsMetadata(AbstractSpimData asd, String key);

}
