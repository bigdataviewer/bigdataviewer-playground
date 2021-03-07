package net.imglib2.realtransform;

import com.google.gson.*;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.services.serializers.plugins.IClassRuntimeAdapter;

import java.lang.reflect.Type;

/**
 * Serializes and deserializes a {@link RealTransformSequence} object
 *
 * As long as each individual {@link RealTransform} object present in the sequence can be
 * serialized, the sequence should be serialized successfully
 *
 * This adapter is located in this package in order to access the protected
 * {@link RealTransformSequence#transforms} field of a {@link RealTransformSequence}
 */

@Plugin(type = IClassRuntimeAdapter.class)
public class RealTransformSequenceAdapter implements IClassRuntimeAdapter<RealTransform, RealTransformSequence> {
    @Override
    public Class<? extends RealTransform> getBaseClass() {
        return RealTransform.class;
    }

    @Override
    public Class<? extends RealTransformSequence> getRunTimeClass() {
        return RealTransformSequence.class;
    }

    @Override
    public RealTransformSequence deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();

        int nTransform = obj.get("size").getAsInt();

        RealTransformSequence rts = new RealTransformSequence();

        for (int iTransform = 0; iTransform<nTransform; iTransform++) {
            // Special case in order to deserialize directly
            // affine transforms to AffineTransform3D objects
            JsonObject jsonObj = obj.get("realTransform_"+iTransform).getAsJsonObject();
            if (jsonObj.has("affinetransform3d")) {
                AffineTransform3D at3D = jsonDeserializationContext.deserialize(obj.get("realTransform_"+iTransform), AffineTransform3D.class);
                rts.add(at3D);
            } else {
                RealTransform transform = jsonDeserializationContext.deserialize(obj.get("realTransform_"+iTransform), RealTransform.class);
                rts.add(transform);
            }
        }

        return rts;
    }

    @Override
    public JsonElement serialize(RealTransformSequence rts, Type type, JsonSerializationContext jsonSerializationContext) {

        JsonObject obj = new JsonObject();

        obj.addProperty("type", RealTransformSequence.class.getSimpleName());

        obj.addProperty("size", rts.transforms.size());

        for (int iTransform = 0; iTransform<rts.transforms.size(); iTransform++) {
            obj.add("realTransform_"+iTransform, jsonSerializationContext.serialize(rts.transforms.get(iTransform)));
        }

        return obj;
    }
}