package sc.fiji.bdvpg.services.serializers;

import bdv.viewer.Source;
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
import org.scijava.InstantiableException;
import sc.fiji.bdvpg.services.SourceAndConverterSerializer;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.services.serializers.plugins.BdvPlaygroundObjectAdapterService;
import sc.fiji.bdvpg.services.serializers.plugins.ISourceAdapter;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class SourceAndConverterAdapter implements JsonSerializer<SourceAndConverter>,
        JsonDeserializer<SourceAndConverter> {

    SourceAndConverterSerializer sacSerializer;

    Map<Class<? extends Source>, ISourceAdapter> sourceSerializers = new HashMap<>();
    Map<String, ISourceAdapter> sourceSerializersFromName = new HashMap<>();

    public SourceAndConverterAdapter(SourceAndConverterSerializer sacSerializer) {
        this.sacSerializer = sacSerializer;
        sacSerializer.getScijavaContext().getService(BdvPlaygroundObjectAdapterService.class)
                .getAdapters(ISourceAdapter.class)
                .forEach(pi -> {
                    try {
                        ISourceAdapter adapter = pi.createInstance();
                        adapter.setSacSerializer(sacSerializer);
                        System.out.println("adapter.getSourceClass()= "+adapter.getSourceClass());
                        sourceSerializers.put(adapter.getSourceClass(), adapter);
                        sourceSerializersFromName.put(adapter.getSourceClass().getName(), adapter);
                    } catch (InstantiableException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public JsonElement serialize(SourceAndConverter sourceAndConverter,
                                 Type type,
                                 JsonSerializationContext jsonSerializationContext) {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("source_name", sourceAndConverter.getSpimSource().getName());
            obj.addProperty("source_class", sourceAndConverter.getSpimSource().getClass().getName());
            obj.addProperty("converter_class", sourceAndConverter.getConverter().getClass().toString());
            obj.addProperty("source_id", sacSerializer.getSacToId().get(sourceAndConverter));

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

            JsonElement element = serializeSubClass(sourceAndConverter, SourceAndConverter.class, jsonSerializationContext);
            obj.add("sac", element);

            return obj;
        } catch (UnsupportedOperationException e) {
            System.err.println("Could not serialize source "+ sourceAndConverter.getSpimSource().getName());
            return null;
        }
    }

    JsonElement serializeSubClass (SourceAndConverter sourceAndConverter,
                                          Type type,
                                          JsonSerializationContext jsonSerializationContext) throws UnsupportedOperationException {

        if (!sourceSerializers.containsKey(sourceAndConverter.getSpimSource().getClass())) {
            System.out.println("Unsupported serialisation of "+sourceAndConverter.getSpimSource().getClass());
            throw new UnsupportedOperationException();
        }

        return sourceSerializers.get(sourceAndConverter.getSpimSource().getClass())
                .serialize(sourceAndConverter, SourceAndConverter.class, jsonSerializationContext);
    }

    @Override
    public SourceAndConverter deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        String sourceClass = jsonObject.getAsJsonPrimitive("source_class").getAsString();

        if (!sourceSerializersFromName.containsKey(sourceClass)) {
            System.out.println("Unsupported deserialisation of "+sourceClass);
            throw new UnsupportedOperationException();
        }

        SourceAndConverter sac = sourceSerializersFromName.get(sourceClass)
                .deserialize(jsonObject.get("sac"), SourceAndConverter.class, jsonDeserializationContext);

        if (sac != null) {
            if (jsonObject.getAsJsonPrimitive("color")!=null) {
                // Now the color
                int color = jsonObject.getAsJsonPrimitive("color").getAsInt();
                new ColorChanger(sac,  new ARGBType(color)).run(); // TO deal with volatile and non volatile
                // Min Max display
                SourceAndConverterServices.getSourceAndConverterDisplayService()
                        .getConverterSetup(sac).setDisplayRange(
                                jsonObject.getAsJsonPrimitive("converter_setup_min").getAsDouble(),
                                jsonObject.getAsJsonPrimitive("converter_setup_max").getAsDouble());
            }

            // unique identifier
            int idSource = jsonObject.getAsJsonPrimitive("source_id").getAsInt();
            sacSerializer.getIdToSac().put(idSource, sac);
            sacSerializer.getSacToId().put(sac, idSource);
            sacSerializer.getSourceToId().put(sac.getSpimSource(), idSource);
            sacSerializer.getIdToSource().put(idSource, sac.getSpimSource());
            sacSerializer.alreadyDeSerializedSacs.add(idSource);
            return sac;
        }

        return null;
    }
}
