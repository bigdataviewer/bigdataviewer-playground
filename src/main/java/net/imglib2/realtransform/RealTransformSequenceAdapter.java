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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.scijava.plugin.Plugin;
import sc.fiji.persist.IClassAdapter;

import java.lang.reflect.Type;

/**
 * Serializes and deserializes a {@link RealTransformSequence} object As long as
 * each individual {@link RealTransform} object present in the sequence can be
 * serialized, the sequence should be serialized successfully This adapter is
 * located in this package in order to access the protected
 * {@link RealTransformSequence#transforms} field of a
 * {@link RealTransformSequence}
 */

@Plugin(type = IClassAdapter.class)
public class RealTransformSequenceAdapter implements
	IClassAdapter<RealTransformSequence>
{

	@Override
	public Class<? extends RealTransformSequence> getAdapterClass() {
		return RealTransformSequence.class;
	}

	@Override
	public RealTransformSequence deserialize(JsonElement jsonElement, Type type,
		JsonDeserializationContext jsonDeserializationContext)
	{
		JsonObject obj = jsonElement.getAsJsonObject();
		int nTransform = obj.get("size").getAsInt();
		RealTransformSequence rts = new RealTransformSequence();
		for (int iTransform = 0; iTransform < nTransform; iTransform++) {
			RealTransform transform = jsonDeserializationContext.deserialize(obj.get(
				"realTransform_" + iTransform), RealTransform.class);
			rts.add(transform);
		}
		return rts;
	}

	@Override
	public JsonElement serialize(RealTransformSequence rts, Type type,
		JsonSerializationContext jsonSerializationContext)
	{
		JsonObject obj = new JsonObject();
		obj.addProperty("size", rts.transforms.size());
		for (int iTransform = 0; iTransform < rts.transforms.size(); iTransform++) {
			obj.add("realTransform_" + iTransform, jsonSerializationContext.serialize(
				rts.transforms.get(iTransform), RealTransform.class));
		}
		return obj;
	}

}
