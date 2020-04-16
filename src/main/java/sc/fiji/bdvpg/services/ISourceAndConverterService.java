package sc.fiji.bdvpg.services;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Service which centralizes Bdv Sources, independently of their display
 * Bdv Sources can be registered to this Service.
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
     * Test if a Source is already registered in the Service
     * @param src
     * @return
     */
    boolean isRegistered(SourceAndConverter src);

    /**
     * Register a Bdv Source in this Service.
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
     * Removes a Bdv Source in this Service.
     * Called in the BdvSourcePostProcessor
     * @param sac
     */
    void remove(SourceAndConverter... sac);


    void linkToSpimData(SourceAndConverter sac, AbstractSpimData asd, int idSetup);

    /**
     * TODO: maybe remove this?
     * Gets lists of associated objects and data attached to a Bdv Source
     * @return
     */
    Map<SourceAndConverter, Map<String, Object>> getSacToMetadata();

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

}
