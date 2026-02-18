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

import bdv.util.ResampledSource;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.services.SourceAndConverterAdapter;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;

import java.lang.reflect.Type;

@Plugin(type = ISourceAdapter.class)
public class ResampledSourceAdapter implements ISourceAdapter<ResampledSource> {

	protected static final Logger logger = LoggerFactory.getLogger(
		ResampledSourceAdapter.class);

	SourceAndConverterAdapter sourceSerializer;

	@Override
	public void setSourceSerializer(SourceAndConverterAdapter sourceSerializer) {
		this.sourceSerializer = sourceSerializer;
	}

	@Override
	public Class<ResampledSource> getSourceClass() {
		return ResampledSource.class;
	}

	@Override
	public JsonElement serialize(SourceAndConverter source, Type type,
		JsonSerializationContext jsonSerializationContext)
	{
		JsonObject obj = new JsonObject();

		ResampledSource resampledSource = (ResampledSource) source.getSpimSource();

		obj.addProperty("type", ResampledSource.class.getSimpleName());

		obj.add("interpolate", jsonSerializationContext.serialize(resampledSource
			.originInterpolation()));
		obj.addProperty("cache", resampledSource.isCached());
		obj.addProperty("name", resampledSource.getName());
		obj.addProperty("mipmaps_reused", resampledSource.areMipmapsReused());
		obj.addProperty("defaultMipmapLevel", resampledSource.getDefaultMipMapLevel());

		Integer idOrigin = sourceSerializer.getSourceToId().get(resampledSource
			.getOriginalSource());
		Integer idModel = sourceSerializer.getSourceToId().get(resampledSource
			.getModelResamplerSource());

		if (idOrigin == null) {
			logger.error("The resampled source " + resampledSource.getOriginalSource()
				.getName() + " couldn't be serialized : origin source not identified.");
			return null;
		}

		if (idModel == null) {
			logger.error("The resampled source " + resampledSource.getOriginalSource()
				.getName() + " couldn't be serialized : model source not identified.");
			return null;
		}

		obj.addProperty("origin_source_id", idOrigin);
		obj.addProperty("model_source_id", idModel);

		return obj;
	}

	@Override
	public SourceAndConverter<?> deserialize(JsonElement jsonElement, Type type,
		JsonDeserializationContext jsonDeserializationContext)
		throws JsonParseException
	{
		JsonObject obj = jsonElement.getAsJsonObject();
		int origin_source_id = obj.getAsJsonPrimitive("origin_source_id")
			.getAsInt();
		int model_source_id = obj.getAsJsonPrimitive("model_source_id").getAsInt();

		Interpolation interpolation = jsonDeserializationContext.deserialize(obj
			.get("interpolate"), Interpolation.class);
		boolean cache = obj.getAsJsonPrimitive("cache").getAsBoolean();
		String name = obj.getAsJsonPrimitive("name").getAsString();
		boolean reuseMipMaps = obj.getAsJsonPrimitive("mipmaps_reused")
			.getAsBoolean();
		int defaultMipMapLevel = obj.getAsJsonPrimitive("defaultMipmapLevel")
			.getAsInt();

		SourceAndConverter<?> originSource;
		SourceAndConverter<?> modelSource;

		if (sourceSerializer.getIdToSac().containsKey(origin_source_id)) {
			// Already deserialized
			originSource = sourceSerializer.getIdToSac().get(origin_source_id);
		}
		else {
			// Should be deserialized first
			JsonElement element = sourceSerializer.idToJsonElement.get(origin_source_id);
			originSource = sourceSerializer.getGson().fromJson(element,
				SourceAndConverter.class);
		}

		if (sourceSerializer.getIdToSac().containsKey(model_source_id)) {
			// Already deserialized
			modelSource = sourceSerializer.getIdToSac().get(model_source_id);
		}
		else {
			// Should be deserialized first
			JsonElement element = sourceSerializer.idToJsonElement.get(model_source_id);
			modelSource = sourceSerializer.getGson().fromJson(element,
				SourceAndConverter.class);
		}

		if (originSource == null) {
			System.err.println(
				"Couldn't deserialize origin source in Resampled Source");
			return null;
		}

		if (modelSource == null) {
			System.err.println(
				"Couldn't deserialize model source in Resampled Source");
			return null;
		}

		return new SourceResampler(originSource, modelSource, name, reuseMipMaps, cache,
			interpolation.equals(Interpolation.NLINEAR), defaultMipMapLevel).get();
	}
}
