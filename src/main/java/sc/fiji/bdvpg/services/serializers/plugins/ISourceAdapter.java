package sc.fiji.bdvpg.services.serializers.plugins;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import com.google.gson.*;
import sc.fiji.bdvpg.services.SourceAndConverterSerializer;

import java.lang.reflect.Type;

public interface ISourceAdapter<S extends Source> extends IBdvPlaygroundObjectAdapter {

    void setSacSerializer(SourceAndConverterSerializer sacSerializer);

    Class<S> getSourceClass();

    JsonElement serialize(SourceAndConverter sac, Type type, JsonSerializationContext jsonSerializationContext);

    SourceAndConverter deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException;
}
