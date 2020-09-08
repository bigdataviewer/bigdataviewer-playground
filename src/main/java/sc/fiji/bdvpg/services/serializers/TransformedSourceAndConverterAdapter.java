package sc.fiji.bdvpg.services.serializers;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.SourceAndConverter;
import com.google.gson.*;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.services.SourceAndConverterSerializer;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.lang.reflect.Type;

public class TransformedSourceAndConverterAdapter {

    SourceAndConverterSerializer sacSerializer;

    public TransformedSourceAndConverterAdapter(SourceAndConverterSerializer sacSerializer) {
        this.sacSerializer = sacSerializer;
    }
    public JsonElement serialize(SourceAndConverter sac, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();

        TransformedSource source = (TransformedSource) sac.getSpimSource();
        AffineTransform3D fixedTr = new AffineTransform3D();
        AffineTransform3D incrTr = new AffineTransform3D();
        source.getIncrementalTransform(incrTr);
        source.getFixedTransform(fixedTr);

        obj.add("affinetransform_fixed", jsonSerializationContext.serialize(fixedTr));
        obj.add("affinetransform_incremental", jsonSerializationContext.serialize(incrTr));
        obj.addProperty("wrapped_source_id", sacSerializer.getSourceToId().get(source.getWrappedSource()));
        return obj;
    }

    public SourceAndConverter deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        int wrappedSourceId = obj.getAsJsonPrimitive("wrapped_source_id").getAsInt();
        SourceAndConverter wrappedSac = null;
        if (sacSerializer.getIdToSac().containsKey(wrappedSourceId)) {
            // Already deserialized
            wrappedSac = sacSerializer.getIdToSac().get(wrappedSourceId);
        } else {
            // Should be deserialized first
            JsonElement element = sacSerializer.idToJsonElement.get(wrappedSourceId);
            wrappedSac = sacSerializer.getGson().fromJson(element, SourceAndConverter.class);
        }

        if (wrappedSac == null) {
            System.err.println("Couldn't deserialize wrapped source");
            return null;
        }

        AffineTransform3D at3d = jsonDeserializationContext.deserialize(jsonElement.getAsJsonObject().get("affinetransform_fixed"), AffineTransform3D.class);

        SourceAndConverter sac = new SourceAffineTransformer(wrappedSac, at3d).getSourceOut();
        SourceAndConverterServices.getSourceAndConverterService()
                .register(sac);

        return sac;
    }
}