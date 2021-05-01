package sc.fiji.serializers;

import org.scijava.Context;
import org.scijava.Priority;
import org.scijava.plugin.AbstractPTService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.service.Service;

import java.util.List;

/**
 * Scijava service which provides the different Scijava Adapters available in the current context.
 *
 * {@link IObjectScijavaAdapter} plugins are automatically discovered and accessible in this service.
 *
 * In practice, serializer / deserializers are obtained via {@link ScijavaGsonHelper} helper class
 *
 * @author Nicolas Chiaruttini, EPFL, 2021
 *
 */
@Plugin(type = Service.class)
public class DefaultScijavaAdapterService extends AbstractPTService<IObjectScijavaAdapter> implements IObjectScijavaAdapterService {

    @Override
    public Class<IObjectScijavaAdapter> getPluginType() {
        return IObjectScijavaAdapter.class;
    }

    @Parameter
    Context ctx;

    @Override
    public Context context() {
        return ctx;
    }

    @Override
    public Context getContext() {
        return ctx;
    }

    double priority = Priority.NORMAL;

    @Override
    public double getPriority() {
        return priority;
    }

    @Override
    public void setPriority(double priority) {
        this.priority = priority;
    }

    @Override
    public <PT extends IObjectScijavaAdapter> List<PluginInfo<PT>> getAdapters(Class<PT> adapterClass) {
        return pluginService().getPluginsOfType(adapterClass);
    }
}
