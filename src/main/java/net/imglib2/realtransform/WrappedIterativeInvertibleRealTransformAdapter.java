/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2024 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import org.scijava.plugin.Plugin;
import sc.fiji.persist.IClassAdapter;

import java.lang.reflect.Type;

@Plugin(type = IClassAdapter.class)
public class WrappedIterativeInvertibleRealTransformAdapter implements
	IClassAdapter<WrappedIterativeInvertibleRealTransform>
{

	@Override
	public WrappedIterativeInvertibleRealTransform deserialize(
		JsonElement jsonElement, Type type,
		JsonDeserializationContext jsonDeserializationContext)
		throws JsonParseException
	{
		JsonObject obj = jsonElement.getAsJsonObject();
		RealTransform rt = jsonDeserializationContext.deserialize(obj.get(
			"wrappedTransform"), RealTransform.class);
		return new WrappedIterativeInvertibleRealTransform<>(rt);
	}

	@Override
	public JsonElement serialize(
		WrappedIterativeInvertibleRealTransform wrappedIterativeInvertibleRealTransform,
		Type type, JsonSerializationContext jsonSerializationContext)
	{
		JsonObject obj = new JsonObject();
		obj.add("wrappedTransform", jsonSerializationContext.serialize(
			wrappedIterativeInvertibleRealTransform.getTransform(),
			RealTransform.class));
		// TODO : get tolerance and maxiter
		// wrappedIterativeInvertibleRealTransform.getOptimzer().getError().setTolerance();
		// wrappedIterativeInvertibleRealTransform.getOptimzer().setTolerance().setTolerance(
		// 0.000001 ); // keeps running until error is < 0.000001
		// ixfm.getOptimzer().setMaxIters( 1000 ); // or 1000 iterations
		return obj;
	}

	@Override
	public Class<? extends WrappedIterativeInvertibleRealTransform>
		getAdapterClass()
	{
		return WrappedIterativeInvertibleRealTransform.class;
	}
}
