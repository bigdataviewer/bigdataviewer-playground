/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2026 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

import bdv.img.WarpedSource;
import bdv.viewer.SourceAndConverter;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.service.SourceAdapter;
import sc.fiji.bdvpg.source.transform.SourceRealTransformer;

import java.lang.reflect.Type;

@Plugin(type = ISourceAdapter.class)
public class WarpedSourceAdapter implements ISourceAdapter<WarpedSource> {

	SourceAdapter sourceSerializer;

	@Override
	public void setSourceSerializer(SourceAdapter sourceSerializer) {
		this.sourceSerializer = sourceSerializer;
	}

	@Override
	public Class<WarpedSource> getSourceClass() {
		return WarpedSource.class;
	}

	@Override
	public JsonElement serialize(SourceAndConverter source, Type type,
		JsonSerializationContext jsonSerializationContext)
	{
		JsonObject obj = new JsonObject();
		WarpedSource warpedSource = (WarpedSource) source.getSpimSource();
		obj.add("realtransform", jsonSerializationContext.serialize(warpedSource
			.getTransform(), RealTransform.class));

		/*if (sourceSerializer.isObjectRegistered(Source.class, source.getWrappedSource())) {
		    int idWrapped = sourceSerializer.getObjectIndex()
		} else {
		
		}*/

		Integer idWrapped = sourceSerializer.getSourceToId().get(warpedSource
			.getWrappedSource());

		if (idWrapped == null) {
			System.err.println(warpedSource.getName() +
				" can't be serialized : the wrapped source " + warpedSource.getWrappedSource()
					.getName() + " couldn't be identified. ");
			return null;
		}

		obj.addProperty("wrapped_source_id", idWrapped);
		return obj;
	}

	@Override
	public SourceAndConverter deserialize(JsonElement jsonElement, Type type,
		JsonDeserializationContext jsonDeserializationContext)
		throws JsonParseException
	{
		JsonObject obj = jsonElement.getAsJsonObject();
		int wrappedSourceId = obj.getAsJsonPrimitive("wrapped_source_id")
			.getAsInt();
		SourceAndConverter wrappedSource;
		if (sourceSerializer.getIdToSac().containsKey(wrappedSourceId)) {
			// Already deserialized
			wrappedSource = sourceSerializer.getIdToSac().get(wrappedSourceId);
		}
		else {
			// Should be deserialized first
			JsonElement element = sourceSerializer.idToJsonElement.get(wrappedSourceId);
			wrappedSource = sourceSerializer.getGson().fromJson(element,
				SourceAndConverter.class);
		}

		if (wrappedSource == null) {
			System.err.println(
				"Couldn't deserialize wrapped source of Warped Source");
			return null;
		}
		JsonElement transformElement = jsonElement.getAsJsonObject().get(
			"realtransform");

		RealTransform rt;

		if (transformElement.getAsJsonObject().has("affinetransform3d")) {
			rt = jsonDeserializationContext.deserialize(transformElement,
				AffineTransform3D.class);
		}
		else {
			rt = jsonDeserializationContext.deserialize(jsonElement.getAsJsonObject()
				.get("realtransform"), RealTransform.class);
		}

		SourceRealTransformer srt = new SourceRealTransformer(wrappedSource, rt);
		srt.run();
		return srt.get();
	}
}
