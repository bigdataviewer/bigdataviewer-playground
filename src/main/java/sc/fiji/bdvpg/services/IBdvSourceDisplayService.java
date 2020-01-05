package sc.fiji.bdvpg.services;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;

public interface IBdvSourceDisplayService {

    /**
     * String keys for data stored in the BdvSourceService
     **/
    String VOLATILESOURCE = "VOLATILESOURCE";
    String CONVERTER = "CONVERTER";
    String CONVERTERSETUP = "CONVERTERSETUP";

    /**
     * Displays a Source, the last active bdv is chosen since none is specified in this method
     * The service should handle internally volatile views and converters
     * @param src
     */
    void show(Source src);

    /**
     * Returns the last active Bdv or create a new one
     */
    BdvHandle getActiveBdv();

    /**
     * Displays a Bdv source into the specified BdvHandle
     * This function really is the core of this service
     * It mimicks or copies the functions of BdvVisTools because it is responsible to
     * create converter, volatiles, convertersetups and so on
     * @param src
     * @param bdvh
     */
    void show(BdvHandle bdvh, Source src);

    SourceAndConverter getSourceAndConverter(Source src);

    /**
     * Closes appropriately a BdvHandle which means that it updates
     * the callbacks for ConverterSetups and updates the ObjectService
     * @param bdvh
     */
    void closeBdv(BdvHandle bdvh);

    /**
     * Registers into the BdvSourceService a SourceAndConverter object
     * @param sac
     */
    void registerSourceAndConverter(SourceAndConverter sac);

    /**
     * Registers a source which has originated from a BdvHandle
     * Useful for BigWarp where the grid and the deformation magnitude source are created
     * into bigwarp
     * @param bdvh_in
     * @param index
     */
    void registerBdvSource(BdvHandle bdvh_in, int index);


}
