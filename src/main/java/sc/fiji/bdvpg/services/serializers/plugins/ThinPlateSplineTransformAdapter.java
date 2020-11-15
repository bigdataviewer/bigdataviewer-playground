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
    public ThinplateSplineTransform deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        double[][] srcPts = context.deserialize(obj.get("srcPts"), double[][].class);
        double[][] tgtPts = context.deserialize(obj.get("tgtPts"), double[][].class);
        ThinplateSplineTransform realTransform = new ThinplateSplineTransform(srcPts, tgtPts);
        return realTransform;
    }

    @Override
    public JsonElement serialize(ThinplateSplineTransform thinplateSplineTransform, Type type, JsonSerializationContext jsonSerializationContext) {
            ThinPlateR2LogRSplineKernelTransform kernel = getKernel(thinplateSplineTransform);

            double[][] srcPts = getSrcPts(kernel);
            double[][] tgtPts = getTgtPts(kernel);

            JsonObject obj = new JsonObject();
            obj.addProperty("type", ThinplateSplineTransform.class.getSimpleName());
            obj.add("srcPts", jsonSerializationContext.serialize(srcPts));
            obj.add("tgtPts", jsonSerializationContext.serialize(tgtPts));
            return obj;
    }

    public static ThinPlateR2LogRSplineKernelTransform getKernel(ThinplateSplineTransform thinplateSplineTransform) {
        try {
            Field kernelField = ThinplateSplineTransform.class.getDeclaredField("tps");
            kernelField.setAccessible(true);
            ThinPlateR2LogRSplineKernelTransform kernel = (ThinPlateR2LogRSplineKernelTransform) kernelField.get(thinplateSplineTransform);
            return kernel;
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.err.println("Could not get kernel from ThinplateSplineTransform");
        return null;
    }

    public static double[][] getSrcPts(ThinPlateR2LogRSplineKernelTransform kernel) {
        return kernel.getSourceLandmarks();
    }

    public static double[][] getTgtPts(ThinPlateR2LogRSplineKernelTransform kernel) {
        double[][] srcPts = kernel.getSourceLandmarks(); // srcPts

        int nbLandmarks = kernel.getNumLandmarks();
        int nbDimensions = kernel.getNumDims();

        double[][] tgtPts = new double[nbDimensions][nbLandmarks];

        for (int i = 0;i<nbLandmarks;i++) {
            double[] srcPt = new double[nbDimensions];
            for (int d = 0; d<nbDimensions; d++) {
                srcPt[d] = srcPts[d][i];
            }
            double[] tgtPt = kernel.apply(srcPt);
            for (int d = 0; d<nbDimensions; d++) {
                tgtPts[d][i] = tgtPt[d];
            }
        }
        return tgtPts;
    }
}
