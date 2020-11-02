/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2020 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
