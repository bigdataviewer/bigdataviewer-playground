package sc.fiji.bdvpg.services;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;

public interface IBdvSourceAndConverterDisplayService {

    /**
     * String keys for data stored in the BdvSourceAndConverterService
     **/
    String CONVERTERSETUP = "CONVERTERSETUP";

    /**
     * Displays a Source, the last active bdv is chosen since none is specified in this method
     * The service should handle internally volatile views and converters
     * @param src
     */
    void show(SourceAndConverter src);

    /**
     * Returns the last active Bdv or create a new one
     */
    BdvHandle getActiveBdv();

    /**
     * Displays a Bdv sourceandconverter into the specified BdvHandle
     * This function really is the core of this service
     * It mimicks or copies the functions of BdvVisTools because it is responsible to
     * create converter, volatiles, convertersetups and so on
     * @param src
     * @param bdvh
     */
    void show(BdvHandle bdvh, SourceAndConverter src);

    /**
     * Returns SourceAndConverter object
     * @param src
     * @return
     */
    ConverterSetup getConverterSetup(SourceAndConverter src);

    /**
     * Closes appropriately a BdvHandle which means that it updates
     * the callbacks for ConverterSetups and updates the ObjectService
     * @param bdvh
     */
    void closeBdv(BdvHandle bdvh);

    /**
     * Registers a sourceandconverter which has originated from a BdvHandle
     * Useful for BigWarp where the grid and the deformation magnitude sourceandconverter are created
     * into bigwarp
     * @param bdvh_in
     * @param index
     */
    void registerBdvSource(BdvHandle bdvh_in, int index);


}
