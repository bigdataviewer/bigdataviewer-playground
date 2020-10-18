package sc.fiji.bdvpg.services.serializers.plugins;

import com.google.gson.*;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import org.scijava.plugin.Plugin;

import java.lang.reflect.Type;

@Plugin(type = IClassRuntimeAdapter.class)
public class WrappedIterativeInvertibleRealTransformAdapter implements IClassRuntimeAdapter<RealTransform, WrappedIterativeInvertibleRealTransform> {
    @Override
    public Class<? extends RealTransform> getBaseClass() {
        return RealTransform.class;
    }

    @Override
    public Class<? extends WrappedIterativeInvertibleRealTransform> getRunTimeClass() {
        return WrappedIterativeInvertibleRealTransform.class;
    }

    @Override
    public WrappedIterativeInvertibleRealTransform deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        RealTransform rt = jsonDeserializationContext.deserialize(obj.get("wrappedTransform"), RealTransform.class);
        WrappedIterativeInvertibleRealTransform invTransform =
                new WrappedIterativeInvertibleRealTransform<>(rt);
        return invTransform;
    }

    @Override
    public JsonElement serialize(WrappedIterativeInvertibleRealTransform wrappedIterativeInvertibleRealTransform, Type type, JsonSerializationContext jsonSerializationContext) {
        WrappedIterativeInvertibleRealTransform rt = wrappedIterativeInvertibleRealTransform;
        JsonObject obj = new JsonObject();

        obj.addProperty("type", WrappedIterativeInvertibleRealTransform.class.getSimpleName());
        obj.add("wrappedTransform", jsonSerializationContext.serialize(rt.getTransform()));
        return obj;
    }
}
