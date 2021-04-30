package sc.fiji.bdvpg.scijava.adapter.bdv;

import com.google.gson.*;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.supplier.DefaultBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.IBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.SerializableBdvOptions;
import sc.fiji.serializers.IClassRuntimeAdapter;

import java.lang.reflect.Type;

/**
 * For serialization of {@link DefaultBdvSupplier} objects
 */

@Plugin(type = IClassRuntimeAdapter.class)
public class DefaultBdvSupplierAdapter implements IClassRuntimeAdapter<IBdvSupplier, DefaultBdvSupplier> {

    @Override
    public Class<? extends IBdvSupplier> getBaseClass() {
        return IBdvSupplier.class;
    }

    @Override
    public Class<? extends DefaultBdvSupplier> getRunTimeClass() {
        return DefaultBdvSupplier.class;
    }

    @Override
    public DefaultBdvSupplier deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        SerializableBdvOptions options = context.deserialize(obj.get("options"), SerializableBdvOptions.class);
        return new DefaultBdvSupplier(options);
    }

    @Override
    public JsonElement serialize(DefaultBdvSupplier bdvSupplier, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", DefaultBdvSupplier.class.getSimpleName());
        obj.add("options", context.serialize(bdvSupplier.sOptions));
        return obj;
    }
}
