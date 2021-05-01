package sc.fiji.serializers;

import org.scijava.plugin.PTService;
import org.scijava.plugin.PluginInfo;
import org.scijava.service.SciJavaService;

import java.util.List;

public interface IObjectScijavaAdapterService extends PTService<IObjectScijavaAdapter>, SciJavaService {
    <PT extends IObjectScijavaAdapter> List<PluginInfo<PT>> getAdapters(Class<PT> adapterClass);
}
