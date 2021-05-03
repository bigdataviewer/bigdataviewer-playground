package sc.fiji.bdvpg.bdv.projector;

import bdv.viewer.render.AccumulateProjectorFactory;
import com.google.gson.*;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.projector.DefaultAccumulatorFactory;
import sc.fiji.serializers.IClassRuntimeAdapter;

import java.lang.reflect.Type;

/**
 * For serialization of {@link DefaultAccumulatorFactory} objects
 * Because the standard factory is not serializable
 */

@Plugin(type = IClassRuntimeAdapter.class)
public class DefaultAccumulatorFactoryAdapter implements IClassRuntimeAdapter<AccumulateProjectorFactory, DefaultAccumulatorFactory> {
    @Override
    public Class<? extends AccumulateProjectorFactory> getBaseClass() {
        return AccumulateProjectorFactory.class;
    }

    @Override
    public Class<? extends DefaultAccumulatorFactory> getRunTimeClass() {
        return DefaultAccumulatorFactory.class;
    }

    @Override
    public boolean useCustomAdapter() {
        return false;
    }

    /*@Override
    public DefaultAccumulatorFactory deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return new DefaultAccumulatorFactory();
    }

    @Override
    public JsonElement serialize(DefaultAccumulatorFactory src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        //obj.addProperty("type", DefaultAccumulatorFactory.class.getSimpleName());
        return obj;
    }*/
}
