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
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import org.scijava.plugin.Plugin;

import java.lang.reflect.Type;

@Plugin(type = IClassRuntimeAdapter.class)
public class WrappedIterativeInvertibleRealTransformAdapter implements IClassRuntimeAdapter<RealTransform, WrappedIterativeInvertibleRealTransform> {
    @Override
    public Class<? extends RealTransform> getBaseClass() {
        return RealTransform.class;
    }

    @Override
    public Class<? extends WrappedIterativeInvertibleRealTransform> getRunTimeClass() {
        return WrappedIterativeInvertibleRealTransform.class;
    }

    @Override
    public WrappedIterativeInvertibleRealTransform deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        RealTransform rt = jsonDeserializationContext.deserialize(obj.get("wrappedTransform"), RealTransform.class);
        WrappedIterativeInvertibleRealTransform invTransform =
                new WrappedIterativeInvertibleRealTransform<>(rt);
        return invTransform;
    }

    @Override
    public JsonElement serialize(WrappedIterativeInvertibleRealTransform wrappedIterativeInvertibleRealTransform, Type type, JsonSerializationContext jsonSerializationContext) {
        WrappedIterativeInvertibleRealTransform rt = wrappedIterativeInvertibleRealTransform;
        JsonObject obj = new JsonObject();

        obj.addProperty("type", WrappedIterativeInvertibleRealTransform.class.getSimpleName());
        obj.add("wrappedTransform", jsonSerializationContext.serialize(rt.getTransform()));
        return obj;
    }
}
