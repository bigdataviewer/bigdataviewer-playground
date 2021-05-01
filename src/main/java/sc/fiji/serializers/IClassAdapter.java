package sc.fiji.serializers;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

public interface IClassAdapter<T> extends IObjectScijavaAdapter, JsonSerializer<T>,
        JsonDeserializer<T> {

    Class<? extends T> getAdapterClass();

}
