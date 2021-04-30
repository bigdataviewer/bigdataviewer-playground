package sc.fiji.bdvpg.bdv.supplier;

import bdv.util.BdvHandle;
import org.scijava.plugin.SciJavaPlugin;
import sc.fiji.bdvpg.scijava.adapter.bdv.DefaultBdvSupplierAdapter;

import java.util.function.Supplier;

/**
 * Top level interface that should be implemented by BdvSuppliers
 *
 * See {@link DefaultBdvSupplier} for an example and {@link DefaultBdvSupplierAdapter} for a way to serialize
 * these objects
 */
public interface IBdvSupplier extends Supplier<BdvHandle>, SciJavaPlugin {
}
