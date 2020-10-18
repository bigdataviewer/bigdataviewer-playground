package sc.fiji.bdvpg.services.serializers.plugins;

import com.google.gson.*;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.ThinplateSplineTransform;
import org.scijava.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

@Plugin(type = IClassRuntimeAdapter.class)
public class ThinPlateSplineTransformAdapter implements IClassRuntimeAdapter<RealTransform, ThinplateSplineTransform> {

    @Override
    public Class<? extends RealTransform> getBaseClass() {
        return RealTransform.class;
    }

    @Override
    public Class<? extends ThinplateSplineTransform> getRunTimeClass() {
        return ThinplateSplineTransform.class;
    }

    @Override
    public ThinplateSplineTransform deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        ThinPlateR2LogRSplineKernelTransform kernel = jsonDeserializationContext.deserialize(obj.get("kernel"), ThinPlateR2LogRSplineKernelTransform.class);
        ThinplateSplineTransform realTransform = new ThinplateSplineTransform(kernel);
        return realTransform;
    }

    @Override
    public JsonElement serialize(ThinplateSplineTransform thinplateSplineTransform, Type type, JsonSerializationContext jsonSerializationContext) {
        try {
            Field kernelField = ThinplateSplineTransform.class.getDeclaredField("tps");
            kernelField.setAccessible(true);
            ThinPlateR2LogRSplineKernelTransform kernel = (ThinPlateR2LogRSplineKernelTransform) kernelField.get(thinplateSplineTransform);
            JsonObject obj = new JsonObject();
            obj.addProperty("type", ThinplateSplineTransform.class.getSimpleName());
            obj.add("kernel", jsonSerializationContext.serialize(kernel));
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.err.println("Could not serilalize ThinplateSplineTransform");
        return null;
    }
}
