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

package sc.fiji.bdvpg.scijava.services.ui.inspect;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.services.ISourceAndConverterService;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Set;

/**
 * Interface for inspecting BDV Source objects.
 * <p>
 * This interface allows external repositories to provide custom inspection logic
 * for their own Source implementations. Inspectors can be registered via
 * <p>
 * To create a custom inspector:
 * <pre>
 * public class MySourceInspector implements ISourceInspector&lt;MySource&gt; {
 *     // ...
 * }
 * // Then register it:
 * inspectorService.registerInspector(new MySourceInspector());
 * </pre>
 *
 * @author Nicolas Chiaruttini, EPFL, BIOP
 */
public interface ISourceInspector {

	/**
	 * Inspects the source and adds nodes to the parent tree node.
	 * <p>
	 * Implementations should:
	 * <ul>
	 *   <li>Add a descriptive node to the parent (e.g., "Transformed Source")</li>
	 *   <li>Add any relevant properties or metadata as child nodes</li>
	 *   <li>Return any wrapped/child sources that should be recursively inspected</li>
	 * </ul>
	 *
	 * @param parent the tree node to add inspection results to
	 * @param sac the source and converter to inspect
	 * @param sourceAndConverterService the service for metadata lookup and source registration
	 * @param registerIntermediateSources whether to register intermediate/wrapped sources in the service
	 * @return a set of wrapped/child SourceAndConverters that should be recursively inspected
	 */
	Set<SourceAndConverter<?>> inspect(
		DefaultMutableTreeNode parent,
		SourceAndConverter<?> sac,
		ISourceAndConverterService sourceAndConverterService,
		boolean registerIntermediateSources
	);
}
