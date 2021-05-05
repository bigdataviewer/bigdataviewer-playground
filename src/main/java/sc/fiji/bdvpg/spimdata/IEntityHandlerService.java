package sc.fiji.bdvpg.spimdata;

import org.scijava.plugin.PTService;
import org.scijava.plugin.PluginInfo;
import org.scijava.service.SciJavaService;

import java.util.List;

public interface IEntityHandlerService extends PTService<EntityHandler>, SciJavaService {
    <PT extends EntityHandler> List<PluginInfo<PT>> getHandlers(Class<PT> handlerClass);
}
