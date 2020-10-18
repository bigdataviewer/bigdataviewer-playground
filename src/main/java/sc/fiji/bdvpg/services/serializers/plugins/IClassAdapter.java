package sc.fiji.bdvpg.services.serializers.plugins;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

public interface IClassAdapter<T> extends IBdvPlaygroundObjectAdapter, JsonSerializer<T>,
        JsonDeserializer<T> {

    Class<? extends T> getAdapterClass();

}
