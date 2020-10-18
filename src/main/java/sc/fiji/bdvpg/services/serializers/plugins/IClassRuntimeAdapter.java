package sc.fiji.bdvpg.services.serializers.plugins;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

public interface IClassRuntimeAdapter<B, T extends B> extends IBdvPlaygroundObjectAdapter, JsonSerializer<T>,
        JsonDeserializer<T> {

    Class<? extends B> getBaseClass();

    Class<? extends T> getRunTimeClass();

}
