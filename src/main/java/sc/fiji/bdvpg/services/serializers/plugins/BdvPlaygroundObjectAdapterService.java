package sc.fiji.bdvpg.services.serializers.plugins;

import org.scijava.plugin.PTService;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.service.SciJavaService;

import java.util.List;

public interface BdvPlaygroundObjectAdapterService extends PTService<IBdvPlaygroundObjectAdapter>, SciJavaService {

    //List<PluginInfo<IBdvPlaygroundObjectAdapter>> getAdapters(Class<? extends IBdvPlaygroundObjectAdapter> adapterClass);

    <PT extends IBdvPlaygroundObjectAdapter> List<PluginInfo<PT>> getAdapters(Class<PT> adapterClass);
}
