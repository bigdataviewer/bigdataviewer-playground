package sc.fiji.serializers;

import com.google.gson.*;

import java.lang.reflect.Type;

public interface IClassRuntimeAdapter<B, T extends B> extends IObjectScijavaAdapter, JsonSerializer<T>,
        JsonDeserializer<T> {

    Class<? extends B> getBaseClass();

    Class<? extends T> getRunTimeClass();

    boolean useCustomAdapter();

    @Override
    default T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        throw new JsonParseException("Default deserializer for class "+getBaseClass()+" ("+getRunTimeClass()+") should not be used, return false in method useCustomAdapter instead");
    }

    @Override
    default JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
        throw new JsonIOException("Default serializer for class "+getBaseClass()+" ("+getRunTimeClass()+") should not be used, should not be used, return false in method useCustomAdapter instead");
    }
}
