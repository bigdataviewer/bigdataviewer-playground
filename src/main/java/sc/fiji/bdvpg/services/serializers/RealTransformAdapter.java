package sc.fiji.bdvpg.services.serializers;

import com.google.gson.*;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.ThinplateSplineTransform;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class RealTransformAdapter implements JsonSerializer<RealTransform>,
        JsonDeserializer<RealTransform> {

    public static final String REALTRANSFORM_CLASS_KEY = "realtransform_class";

    @Override
    public RealTransform deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        String realtransformClassName = obj.get(REALTRANSFORM_CLASS_KEY).getAsString();
        if (realtransformClassName.equals(ThinplateSplineTransform.class.getName())) {
            ThinPlateR2LogRSplineKernelTransform kernel = jsonDeserializationContext.deserialize(obj.get("kernel"), ThinPlateR2LogRSplineKernelTransform.class);
            ThinplateSplineTransform realTransform = new ThinplateSplineTransform(kernel);
            return realTransform;
        } else {
            System.err.println("Could not deserialise RealTransform of class : "+realtransformClassName);
        }

        return null;
    }

    @Override
    public JsonElement serialize(RealTransform realTransform, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();

        if (realTransform instanceof WrappedIterativeInvertibleRealTransform) {
            // Give up inversibility
            RealTransform wrappedTransform =
                    ((WrappedIterativeInvertibleRealTransform) realTransform).getTransform();
            return jsonSerializationContext.serialize(wrappedTransform);
        }

        if (realTransform instanceof ThinplateSplineTransform) {
            ThinplateSplineTransform plateTransform = (ThinplateSplineTransform) realTransform;
            try {
                Field kernelField = ThinplateSplineTransform.class.getDeclaredField("tps");
                kernelField.setAccessible(true);
                ThinPlateR2LogRSplineKernelTransform kernel = (ThinPlateR2LogRSplineKernelTransform) kernelField.get(plateTransform);

                obj.addProperty(REALTRANSFORM_CLASS_KEY, ThinplateSplineTransform.class.getName());
                obj.add("kernel", jsonSerializationContext.serialize(kernel));
                return obj;
            } catch (Exception e) {
                e.printStackTrace();
            }
            //return jsonSerializationContext.serialize(plateTransform);
        }

        // Raw attempt
        return null; //jsonSerializationContext.serialize(realTransform);
    }
}
