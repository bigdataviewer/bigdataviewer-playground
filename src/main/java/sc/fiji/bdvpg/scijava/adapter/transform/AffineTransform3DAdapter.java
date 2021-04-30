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
package sc.fiji.bdvpg.scijava.adapter.transform;

import com.google.gson.*;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.plugin.Plugin;
import sc.fiji.serializers.IClassAdapter;

import java.lang.reflect.Type;

@Plugin(type = IClassAdapter.class)
public class AffineTransform3DAdapter implements IClassAdapter<AffineTransform3D> {

    @Override
    public AffineTransform3D deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        double[] rowPackedCopy =
        jsonDeserializationContext.deserialize(jsonElement.getAsJsonObject().get("affinetransform3d"), double[].class);
        AffineTransform3D at3d = new AffineTransform3D();
        at3d.set(rowPackedCopy);
        return at3d;
    }

    @Override
    public JsonElement serialize(AffineTransform3D affineTransform3D, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.add("affinetransform3d", jsonSerializationContext.serialize(affineTransform3D.getRowPackedCopy()));
        return obj;
    }

    @Override
    public Class<? extends AffineTransform3D> getAdapterClass() {
        return AffineTransform3D.class;
    }
}
