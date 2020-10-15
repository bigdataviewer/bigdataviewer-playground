package sc.fiji.bdvpg.services.serializers.plugins;

import org.scijava.Context;
import org.scijava.Priority;
import org.scijava.plugin.*;
import org.scijava.service.Service;
import java.util.List;

@Plugin(type = Service.class)
public class DefaultBdvPlaygroundSerializerService extends AbstractPTService<IBdvPlaygroundObjectAdapter> implements BdvPlaygroundObjectAdapterService {

    @Override
    public Class<IBdvPlaygroundObjectAdapter> getPluginType() {
        return IBdvPlaygroundObjectAdapter.class;
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
    public <PT extends IBdvPlaygroundObjectAdapter> List<PluginInfo<PT>> getAdapters(Class<PT> adapterClass) {
        return pluginService().getPluginsOfType(adapterClass);
    }
}
