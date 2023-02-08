/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2023 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.persist;

import org.scijava.Context;
import org.scijava.Priority;
import org.scijava.plugin.AbstractPTService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.service.Service;

import java.util.List;

/**
 * Scijava service which provides the different Scijava Adapters available in
 * the current context. {@link IObjectScijavaAdapter} plugins are automatically
 * discovered and accessible in this service. In practice, serializer /
 * deserializers are obtained via {@link ScijavaGsonHelper} helper class
 *
 * @author Nicolas Chiaruttini, EPFL, 2021
 */
@Plugin(type = Service.class)
public class DefaultScijavaAdapterService extends
	AbstractPTService<IObjectScijavaAdapter> implements
	IObjectScijavaAdapterService
{

	@Override
	public Class<IObjectScijavaAdapter> getPluginType() {
		return IObjectScijavaAdapter.class;
	}

	@SuppressWarnings("unused")
	@Parameter
	Context ctx;

	@Override
	public Context context() {
		return ctx;
	}

	@Override
	public Context getContext() {
		return ctx;
	}

	double priority = Priority.NORMAL;

	@Override
	public double getPriority() {
		return priority;
	}

	@Override
	public void setPriority(double priority) {
		this.priority = priority;
	}

	@Override
	public <PT extends IObjectScijavaAdapter> List<PluginInfo<PT>> getAdapters(
		Class<PT> adapterClass)
	{
		return pluginService().getPluginsOfType(adapterClass);
	}
}
