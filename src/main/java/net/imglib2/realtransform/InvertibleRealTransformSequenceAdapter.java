package net.imglib2.realtransform;

import com.google.gson.*;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.services.serializers.plugins.IClassRuntimeAdapter;

import java.lang.reflect.Type;

/**
 * Serializes and deserializes {@link InvertibleRealTransformSequence} object
 *
 * As long as each individual {@link RealTransform} object present in the sequence can be
 * serialized, and implements {@link InvertibleRealTransform},
 * the sequence should be serialized successfully
 *
 * This adapter is located in this package in order to access the protected
 * {@link InvertibleRealTransformSequence#transforms} field
 * of an {@link InvertibleRealTransformSequence}
 */
@Plugin(type = IClassRuntimeAdapter.class)
public class InvertibleRealTransformSequenceAdapter implements IClassRuntimeAdapter<RealTransform, InvertibleRealTransformSequence> {
    @Override
    public Class<? extends RealTransform> getBaseClass() {
        return RealTransform.class;
    }

    @Override
    public Class<? extends InvertibleRealTransformSequence> getRunTimeClass() {
        return InvertibleRealTransformSequence.class;
    }

    @Override
    public InvertibleRealTransformSequence deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();

        int nTransform = obj.get("size").getAsInt();

        InvertibleRealTransformSequence irts = new InvertibleRealTransformSequence();

        for (int iTransform = 0; iTransform<nTransform; iTransform++) {
            // Special case in order to deserialize directly
            // affine transforms to AffineTransform3D objects
            JsonObject jsonObj = obj.get("realTransform_"+iTransform).getAsJsonObject();
            if (jsonObj.has("affinetransform3d")) {
                AffineTransform3D at3D = jsonDeserializationContext.deserialize(obj.get("realTransform_"+iTransform), AffineTransform3D.class);
                irts.add(at3D);
            } else {
                RealTransform transform = jsonDeserializationContext.deserialize(obj.get("realTransform_"+iTransform), RealTransform.class);
                if (transform instanceof InvertibleRealTransform) {
                    irts.add((InvertibleRealTransform) transform);
                } else {
                    System.err.println("Deserialization eroor: "+transform+" of class "+transform.getClass().getSimpleName()+" is not invertible!");
                    return null;
                }
            }
        }

        return irts;
    }

    @Override
    public JsonElement serialize(InvertibleRealTransformSequence irts, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();

        obj.addProperty("type", InvertibleRealTransformSequence.class.getSimpleName());

        obj.addProperty("size", irts.transforms.size());

        for (int iTransform = 0; iTransform<irts.transforms.size(); iTransform++) {
            obj.add("realTransform_"+iTransform, jsonSerializationContext.serialize(irts.transforms.get(iTransform)));
        }

        return obj;
    }
}
