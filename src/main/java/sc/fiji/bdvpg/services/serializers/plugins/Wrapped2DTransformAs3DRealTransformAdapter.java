package sc.fiji.bdvpg.services.serializers.plugins;

import com.google.gson.*;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.Wrapped2DTransformAs3D;
import org.scijava.plugin.Plugin;

import java.lang.reflect.Type;

@Plugin(type = IClassRuntimeAdapter.class)
public class Wrapped2DTransformAs3DRealTransformAdapter implements IClassRuntimeAdapter<RealTransform, Wrapped2DTransformAs3D> {
    @Override
    public Class<? extends RealTransform> getBaseClass() {
        return RealTransform.class;
    }

    @Override
    public Class<? extends Wrapped2DTransformAs3D> getRunTimeClass() {
        return Wrapped2DTransformAs3D.class;
    }

    @Override
    public Wrapped2DTransformAs3D deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        RealTransform rt = jsonDeserializationContext.deserialize(obj.get("wrappedTransform"), RealTransform.class);

        if (!(rt instanceof InvertibleRealTransform)) {
            System.err.println("Wrapped transform not invertible -> deserialization impossible...");
            // TODO : see if autowrapping works ?
            return null;
        }

        Wrapped2DTransformAs3D wrapped2DTransformAs3D =
                new Wrapped2DTransformAs3D((InvertibleRealTransform) rt);
        return wrapped2DTransformAs3D;
    }

    @Override
    public JsonElement serialize(Wrapped2DTransformAs3D wrapped2DTransformAs3D, Type type, JsonSerializationContext jsonSerializationContext) {
        Wrapped2DTransformAs3D rt = wrapped2DTransformAs3D;
        JsonObject obj = new JsonObject();

        obj.addProperty("type", Wrapped2DTransformAs3D.class.getSimpleName());
        obj.add("wrappedTransform", jsonSerializationContext.serialize(rt.getTransform()));
        return obj;
    }
}
