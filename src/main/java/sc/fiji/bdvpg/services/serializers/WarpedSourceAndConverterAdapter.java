package sc.fiji.bdvpg.services.serializers;

import bdv.img.WarpedSource;
import bdv.tools.transformation.TransformedSource;
import bdv.viewer.SourceAndConverter;
import com.google.gson.*;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import sc.fiji.bdvpg.services.SourceAndConverterSerializer;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceRealTransformer;

import java.lang.reflect.Type;

public class WarpedSourceAndConverterAdapter {

    SourceAndConverterSerializer sacSerializer;

    public WarpedSourceAndConverterAdapter(SourceAndConverterSerializer sacSerializer) {
        this.sacSerializer = sacSerializer;
    }

    public JsonElement serialize(SourceAndConverter sac, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        WarpedSource source = (WarpedSource) sac.getSpimSource();
        obj.add("realtransform", jsonSerializationContext.serialize(source.getTransform()));
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

        RealTransform rt = jsonDeserializationContext.deserialize(jsonElement.getAsJsonObject().get("realtransform"), RealTransform.class);

        SourceRealTransformer srt = new SourceRealTransformer(wrappedSac, rt);
        srt.run();
        SourceAndConverter sac = srt.getSourceOut();

        SourceAndConverterServices.getSourceAndConverterService()
                .register(sac);

        return sac;
    }
}
