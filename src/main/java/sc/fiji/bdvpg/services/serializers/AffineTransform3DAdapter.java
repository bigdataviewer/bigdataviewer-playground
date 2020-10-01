package sc.fiji.bdvpg.services.serializers;

import com.google.gson.*;
import net.imglib2.realtransform.AffineTransform3D;

import java.lang.reflect.Type;

public class AffineTransform3DAdapter implements JsonSerializer<AffineTransform3D>,
        JsonDeserializer<AffineTransform3D> {

    @Override
    public AffineTransform3D deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        double[] rowPackedCopy =
        jsonDeserializationContext.deserialize(jsonElement.getAsJsonObject().get("affinetransform3d"), double[].class);
        AffineTransform3D at3d = new AffineTransform3D();
        at3d.set(rowPackedCopy);
        return at3d;
    }

    @Override
    public JsonElement serialize(AffineTransform3D affineTransform3D, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.add("affinetransform3d", jsonSerializationContext.serialize(affineTransform3D.getRowPackedCopy()));
        return obj;
    }
}
