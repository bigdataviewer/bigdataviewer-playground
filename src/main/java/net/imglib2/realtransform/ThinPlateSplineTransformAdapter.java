/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package net.imglib2.realtransform;

import com.google.gson.*;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.persist.IClassAdapter;
import sc.fiji.persist.IClassRuntimeAdapter;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * Adapter for ThinPlateSplineTransform Objects
 */
@Plugin(type = IClassAdapter.class)
public class ThinPlateSplineTransformAdapter implements IClassAdapter<ThinplateSplineTransform> {

    protected static Logger logger = LoggerFactory.getLogger(ThinPlateSplineTransformAdapter.class);

    @Override
    public ThinplateSplineTransform deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        double[][] srcPts = context.deserialize(obj.get("srcPts"), double[][].class);
        double[][] tgtPts = context.deserialize(obj.get("tgtPts"), double[][].class);
        return new ThinplateSplineTransform(srcPts, tgtPts);
    }

    @Override
    public JsonElement serialize(ThinplateSplineTransform thinplateSplineTransform, Type type, JsonSerializationContext jsonSerializationContext) {
        ThinPlateR2LogRSplineKernelTransform kernel = getKernel(thinplateSplineTransform);
        assert kernel != null;
        double[][] srcPts = getSrcPts(kernel);
        double[][] tgtPts = getTgtPts(kernel);

        JsonObject obj = new JsonObject();
        obj.add("srcPts", jsonSerializationContext.serialize(srcPts));
        obj.add("tgtPts", jsonSerializationContext.serialize(tgtPts));
        return obj;
    }

    public static ThinPlateR2LogRSplineKernelTransform getKernel(ThinplateSplineTransform thinplateSplineTransform) {
        try {
            Field kernelField = ThinplateSplineTransform.class.getDeclaredField("tps");
            kernelField.setAccessible(true);
            return (ThinPlateR2LogRSplineKernelTransform) kernelField.get(thinplateSplineTransform);
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.error("Could not get kernel from ThinplateSplineTransform");
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

    @Override
    public Class<? extends ThinplateSplineTransform> getAdapterClass() {
        return ThinplateSplineTransform.class;
    }
}
