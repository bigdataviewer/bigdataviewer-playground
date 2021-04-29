/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2021 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
            return new ThinplateSplineTransform(kernel);
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
