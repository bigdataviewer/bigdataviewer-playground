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
package net.imglib2.realtransform;

import com.google.gson.*;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.services.serializers.plugins.IClassRuntimeAdapter;

import java.lang.reflect.Type;

/**
 * Serializes and deserializes {@link InvertibleRealTransformSequence} object
 *
 * As long as each individual {@link RealTransform} object present in the sequence can be
 * serialized, and implements {@link InvertibleRealTransform},
 * the sequence should be serialized successfully
 *
 * This adapter is located in this package in order to access the protected
 * {@link InvertibleRealTransformSequence#transforms} field
 * of an {@link InvertibleRealTransformSequence}
 */
@Plugin(type = IClassRuntimeAdapter.class)
public class InvertibleRealTransformSequenceAdapter implements IClassRuntimeAdapter<RealTransform, InvertibleRealTransformSequence> {
    @Override
    public Class<? extends RealTransform> getBaseClass() {
        return RealTransform.class;
    }

    @Override
    public Class<? extends InvertibleRealTransformSequence> getRunTimeClass() {
        return InvertibleRealTransformSequence.class;
    }

    @Override
    public InvertibleRealTransformSequence deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();

        int nTransform = obj.get("size").getAsInt();

        InvertibleRealTransformSequence irts = new InvertibleRealTransformSequence();

        for (int iTransform = 0; iTransform<nTransform; iTransform++) {
            // Special case in order to deserialize directly
            // affine transforms to AffineTransform3D objects
            JsonObject jsonObj = obj.get("realTransform_"+iTransform).getAsJsonObject();
            if (jsonObj.has("affinetransform3d")) {
                AffineTransform3D at3D = jsonDeserializationContext.deserialize(obj.get("realTransform_"+iTransform), AffineTransform3D.class);
                irts.add(at3D);
            } else {
                RealTransform transform = jsonDeserializationContext.deserialize(obj.get("realTransform_"+iTransform), RealTransform.class);
                if (transform instanceof InvertibleRealTransform) {
                    irts.add((InvertibleRealTransform) transform);
                } else {
                    System.err.println("Deserialization eroor: "+transform+" of class "+transform.getClass().getSimpleName()+" is not invertible!");
                    return null;
                }
            }
        }

        return irts;
    }

    @Override
    public JsonElement serialize(InvertibleRealTransformSequence irts, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();

        obj.addProperty("type", InvertibleRealTransformSequence.class.getSimpleName());

        obj.addProperty("size", irts.transforms.size());

        for (int iTransform = 0; iTransform<irts.transforms.size(); iTransform++) {
            obj.add("realTransform_"+iTransform, jsonSerializationContext.serialize(irts.transforms.get(iTransform)));
        }

        return obj;
    }
}
