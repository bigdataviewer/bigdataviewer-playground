package sc.fiji.bdvpg.services.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.imglib2.display.ColorConverter;
import sc.fiji.bdvpg.services.SourceAndConverterSerializer;

import java.lang.reflect.Type;

public class ColorConverterAdapter implements JsonSerializer<ColorConverter> {

    SourceAndConverterSerializer sacSerializer;

    public ColorConverterAdapter(SourceAndConverterSerializer sacSerializer) {
        this.sacSerializer = sacSerializer;
    }

    @Override
    public JsonElement serialize(ColorConverter colorConverter, Type type, JsonSerializationContext jsonSerializationContext) {
        System.out.println("Serializing colorconverter");
        JsonObject obj = new JsonObject();
        obj.add("color", jsonSerializationContext.serialize(colorConverter.getColor().get()));
        return obj;
    }
}