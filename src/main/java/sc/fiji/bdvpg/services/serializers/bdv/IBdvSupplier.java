package sc.fiji.bdvpg.services.serializers.bdv;

import bdv.util.BdvHandle;
import org.scijava.plugin.SciJavaPlugin;

import java.util.function.Supplier;

/**
 * Top level interface that should be implemented by BdvSuppliers
 *
 * See {@link DefaultBdvSupplier} for an example and {@link DefaultBdvSupplierAdapter} for a way to serialize
 * these objects
 */
public interface IBdvSupplier extends Supplier<BdvHandle>, SciJavaPlugin {
}
