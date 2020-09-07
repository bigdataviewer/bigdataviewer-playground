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
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import sc.fiji.bdvpg.services.SourceAndConverterSerializer;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;

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
        //obj.add("converter", jsonSerializationContext.serialize(sourceAndConverter.getConverter()));

        if (sourceAndConverter.getConverter() instanceof ColorConverter) {
            ColorConverter colorConverter = (ColorConverter) sourceAndConverter.getConverter();
            obj.add("color", jsonSerializationContext.serialize(colorConverter.getColor().get()));
            double min = SourceAndConverterServices.getSourceAndConverterDisplayService()
                    .getConverterSetup(sourceAndConverter).getDisplayRangeMin();
            double max = SourceAndConverterServices.getSourceAndConverterDisplayService()
                    .getConverterSetup(sourceAndConverter).getDisplayRangeMax();
            obj.addProperty("converter_setup_min", min);
            obj.addProperty("converter_setup_max", max);
        }



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

        String sourceClass = jsonObject.getAsJsonPrimitive("source class").getAsString();

        SourceAndConverter sac = null;

        if (sourceClass.equals(SpimSource.class.getName())) {
            sac = SpimSourceAndConverterAdapter.deserialize(jsonObject.get("sac"), SourceAndConverter.class, jsonDeserializationContext);
        }

        if (sac != null) {
            // Now the color
            if (jsonObject.getAsJsonPrimitive("color")!=null) {
                int color = jsonObject.getAsJsonPrimitive("color").getAsInt();
                new ColorChanger(sac,  new ARGBType(color)).run(); // TO deal with volatile and non volatile

                SourceAndConverterServices.getSourceAndConverterDisplayService()
                        .getConverterSetup(sac).setDisplayRange(
                                jsonObject.getAsJsonPrimitive("converter_setup_min").getAsDouble(),
                                jsonObject.getAsJsonPrimitive("converter_setup_max").getAsDouble());

            }
            // Min Max display
            return sac;
        }

        return null;
    }
}
