package sc.fiji.persist;

import org.scijava.plugin.SciJavaPlugin;

/**
 * Top level class for plugins which can serialize object using gson and the scijava context.
 *
 * The scijava context may provide custom adapters {@link IClassAdapter} and also
 * runtime adapters, see {@link IClassRuntimeAdapter}) auto-discovered via scijava plugin
 * extensibility mechanism.
 *
 * @author Nicolas Chiaruttini, EPFL, 2021
 *
 */

public interface IObjectScijavaAdapter extends SciJavaPlugin {
}
