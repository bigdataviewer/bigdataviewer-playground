package sc.fiji.bdvpg.services;

import bdv.viewer.Source;
import mpicbg.spim.data.generic.AbstractSpimData;

import java.util.List;
import java.util.Map;

/**
 * Service which centralizes Bdv Sources, independently of their display
 * Bdv Sources can be registered to this Service.
 * This service stores Sources but on top of it,
 * It contains a Map which contains any object which can be linked to the source.
 *
 * Objects needed for display should be created by a  IBdvSourceDisplayService
 * - Converter to ARGBType, ConverterSetup, and Volatile view
 *
 * TODO : Think more carefully : maybe the volatile source should be done here...
 * Because when multiply wrapped source end up here, it maybe isn't possible to make the volatile view
 */

public interface IBdvSourceService {

    /**
     * Test if a Source is already registered in the Service
     * @param src
     * @return
     */
    boolean isRegistered(Source src);

    /**
     * Register a Bdv Source in this Service.
     * Called in the BdvSourcePostProcessor
     * @param src
     */
    void register(Source src);

    /**
     * Registers all sources contained with a SpimData Object in this Service.
     * Called in the BdvSourcePostProcessor
     */
    void register(AbstractSpimData asd);

    /**
     * Register a Bdv Source in this Service.
     * Called in the BdvSourcePostProcessor
     * @param src
     */
    void register(Source src, Source vsrc);

    /**
     * @return list of all registered sources
     */
    List<Source> getSources();

    /**
     * Return sources assigned to a SpimDataObject
     */
    List<Source> getSourcesFromSpimdata(AbstractSpimData asd);

    /**
     * Removes a Bdv Source in this Service.
     * Called in the BdvSourcePostProcessor
     * @param src
     */
    void remove(Source src);


    void linkToSpimData(Source src, AbstractSpimData asd);

    /**
     * Gets lists of associated objects and data attached to a Bdv Source
     * @return
     */
    Map<Source, Map<String, Object>> getAttachedSourceData();
}
