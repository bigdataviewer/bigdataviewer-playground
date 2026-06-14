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

package sc.fiji.bdvpg.scijava.converter;

import bdv.viewer.SourceAndConverter;
import org.scijava.convert.AbstractConverter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.service.SourceService;

import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

/**
 * Array-form of {@link StringToSourceArray}: each entry is a {@code ">"}-delimited
 * tree path resolved against the {@link SourceService} tree, and the resulting
 * sources are flattened (in input order) into a single {@code SourceAndConverter[]}.
 * <p>
 * Like {@link StringToSourceArray}, paths are not trimmed — segment matching
 * is exact on {@code DefaultMutableTreeNode.toString()}. Entries that don't
 * resolve to a tree path are skipped silently.
 */

@SuppressWarnings("unused")
@Plugin(type = org.scijava.convert.Converter.class)
public class StringArrayToSourceArray extends
	AbstractConverter<String[], SourceAndConverter<?>[]>
{

	@Parameter
	SourceService source_service;

	@Override
	public <T> T convert(Object src, Class<T> dest) {
		String[] paths = (String[]) src;
		List<SourceAndConverter<?>> all = new ArrayList<>();
		for (String str : paths) {
			TreePath tp = source_service.tree().getTreePathFromString(str);
			if (tp != null) {
				all.addAll(source_service.tree().getSourcesFromTreePath(tp));
			}
		}
		if (all.isEmpty()) {
			return null;
		}
		return (T) all.toArray(new SourceAndConverter[0]);
	}

	@Override
	public Class getOutputType() {
		return SourceAndConverter[].class;
	}

	@Override
	public Class<String[]> getInputType() {
		return String[].class;
	}
}
