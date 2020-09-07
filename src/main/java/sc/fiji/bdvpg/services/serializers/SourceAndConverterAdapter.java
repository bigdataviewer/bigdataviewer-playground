package sc.fiji.bdvpg.services.serializers;

import bdv.SpimSource;
import bdv.img.WarpedSource;
import bdv.tools.transformation.TransformedSource;
import bdv.util.ResampledSource;
import bdv.viewer.SourceAndConverter;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import sc.fiji.bdvpg.services.SourceAndConverterSerializer;

import java.lang.reflect.Type;

public class SourceAndConverterAdapter implements JsonSerializer<SourceAndConverter>,
        JsonDeserializer<SourceAndConverter> {

    SourceAndConverterSerializer sacSerializer;

    public SourceAndConverterAdapter(SourceAndConverterSerializer sacSerializer) {
        this.sacSerializer = sacSerializer;
    }

    @Override
    public JsonElement serialize(SourceAndConverter sourceAndConverter,
                                 Type type,
                                 JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("source name", sourceAndConverter.getSpimSource().getName());
        obj.addProperty("source class", sourceAndConverter.getSpimSource().getClass().getName());
        obj.addProperty("converter class", sourceAndConverter.getConverter().getClass().toString());
        obj.add("converter", jsonSerializationContext.serialize(sourceAndConverter.getConverter()));
        //obj.addProperty("source id", sacToId.get(sourceAndConverter));
        JsonElement element = serializeSubClass(sourceAndConverter, SourceAndConverter.class, jsonSerializationContext);
        obj.add("sac", element);

        return obj;
    }

    JsonElement serializeSubClass (SourceAndConverter sourceAndConverter,
                                          Type type,
                                          JsonSerializationContext jsonSerializationContext) {
        //JsonObject obj = new JsonObject();
        //obj
        if (sourceAndConverter.getSpimSource() instanceof SpimSource) {
            return SpimSourceAndConverterAdapter.serialize(sourceAndConverter, SourceAndConverter.class, jsonSerializationContext);
        }
        if (sourceAndConverter.getSpimSource() instanceof TransformedSource) {
            return new TransformedSourceAndConverterAdapter(sacSerializer).serialize(sourceAndConverter, SourceAndConverter.class, jsonSerializationContext);
        }
        if (sourceAndConverter.getSpimSource() instanceof ResampledSource) {
            throw new UnsupportedOperationException();
        }
        if (sourceAndConverter.getSpimSource() instanceof WarpedSource) {
            throw new UnsupportedOperationException();
        }

        System.out.println("Unsupported serialisation of "+sourceAndConverter.getSpimSource().getClass().getSimpleName());

        throw new UnsupportedOperationException();
        //return null;//jsonSerializationContext.serialize(sourceAndConverter.getSpimSource()):

    }

    @Override
    public SourceAndConverter deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        System.out.println("Bouh!");

        String sourceClass = jsonObject.getAsJsonPrimitive("source class").getAsString();

        System.out.println(sourceClass);
        System.out.println(SpimSource.class.getName());

        if (sourceClass.equals(SpimSource.class.getName())) {
            System.out.println("SpimSource!");
            return SpimSourceAndConverterAdapter.deserialize(jsonObject.get("sac"), SourceAndConverter.class, jsonDeserializationContext);
        }

        return null;
    }
}
