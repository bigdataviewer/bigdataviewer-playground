package sc.fiji.bdvpg.spimdata;


import org.scijava.plugin.AbstractPTService;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.service.Service;
import sc.fiji.persist.IObjectScijavaAdapter;

import java.util.List;

@Plugin(type = Service.class)
public class DefaultEntityHandlerService extends AbstractPTService<EntityHandler> implements IEntityHandlerService {

    @Override
    public <PT extends EntityHandler> List<PluginInfo<PT>> getHandlers(Class<PT> handlerClass) {
        return pluginService().getPluginsOfType(handlerClass);
    }

    @Override
    public Class<EntityHandler> getPluginType() {
        return EntityHandler.class;
    }

}
