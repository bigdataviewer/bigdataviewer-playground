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

import bdv.util.EmptySource;
import bdv.viewer.SourceAndConverter;
import com.google.gson.*;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.services.SourceAndConverterAdapter;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.lang.reflect.Type;

@Plugin(type = ISourceAdapter.class)
public class EmptySourceAdapter implements ISourceAdapter<EmptySource> {

	SourceAndConverterAdapter sacSerializer;

	@Override
	public void setSacSerializer(SourceAndConverterAdapter sacSerializer) {
		this.sacSerializer = sacSerializer;
	}

	@Override
	public Class<EmptySource> getSourceClass() {
		return EmptySource.class;
	}

	@Override
	public JsonElement serialize(SourceAndConverter<?> sac, Type type,
		JsonSerializationContext jsonSerializationContext)
	{
		JsonObject obj = new JsonObject();
		EmptySource source = (EmptySource) sac.getSpimSource();
		obj.add("empty_source_parameters", jsonSerializationContext.serialize(source
			.getParameters()));
		return obj;
	}

	@Override
	public SourceAndConverter<?> deserialize(JsonElement jsonElement, Type type,
		JsonDeserializationContext jsonDeserializationContext)
		throws JsonParseException
	{
		JsonObject obj = jsonElement.getAsJsonObject();
		EmptySource.EmptySourceParams sourceParams = jsonDeserializationContext
			.deserialize(obj.get("empty_source_parameters"),
				EmptySource.EmptySourceParams.class);

		return SourceAndConverterHelper.createSourceAndConverter(new EmptySource(
			sourceParams));
	}
}
