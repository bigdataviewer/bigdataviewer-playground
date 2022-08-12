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
package sc.fiji.bdvpg.scijava.adapter.source;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.SourceAndConverter;
import com.google.gson.*;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.services.SourceAndConverterAdapter;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.lang.reflect.Type;

@Plugin(type = ISourceAdapter.class)
public class TransformedSourceAdapter implements ISourceAdapter<TransformedSource> {

    SourceAndConverterAdapter sacSerializer;

    @Override
    public void setSacSerializer(SourceAndConverterAdapter sacSerializer) {
        this.sacSerializer = sacSerializer;
    }

    @Override
    public Class<TransformedSource> getSourceClass() {
        return TransformedSource.class;
    }

    @Override
    public JsonElement serialize(SourceAndConverter sac, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();

        TransformedSource source = (TransformedSource) sac.getSpimSource();
        AffineTransform3D fixedTr = new AffineTransform3D();
        AffineTransform3D incrTr = new AffineTransform3D();
        source.getIncrementalTransform(incrTr);
        source.getFixedTransform(fixedTr);

        obj.add("affinetransform_fixed", jsonSerializationContext.serialize(fixedTr, RealTransform.class));
        Integer idWrapped = sacSerializer.getSourceToId().get(source.getWrappedSource());

        if (idWrapped==null) {
            System.err.println(source.getName()+" can't be serialized : the wrapped source "+source.getWrappedSource().getName()+" couldn't be identified. ");
            return null;
        }

        obj.addProperty("wrapped_source_id", idWrapped);
        return obj;
    }

    @Override
    public SourceAndConverter deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        int wrappedSourceId = obj.getAsJsonPrimitive("wrapped_source_id").getAsInt();
        SourceAndConverter wrappedSac;
        if (sacSerializer.getIdToSac().containsKey(wrappedSourceId)) {
            // Already deserialized
            wrappedSac = sacSerializer.getIdToSac().get(wrappedSourceId);
        } else {
            // Should be deserialized first
            JsonElement element = sacSerializer.idToJsonElement.get(wrappedSourceId);
            wrappedSac = sacSerializer.getGson().fromJson(element, SourceAndConverter.class);
        }

        if (wrappedSac == null) {
            System.err.println("Couldn't deserialize wrapped source");
            return null;
        }

        AffineTransform3D at3d = jsonDeserializationContext.deserialize(jsonElement.getAsJsonObject().get("affinetransform_fixed"), RealTransform.class);

        SourceAndConverter sac = new SourceAffineTransformer(wrappedSac, at3d).getSourceOut();

        return sac;
    }
}
