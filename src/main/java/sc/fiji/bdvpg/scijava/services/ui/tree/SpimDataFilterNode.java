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

package sc.fiji.bdvpg.scijava.services.ui.tree;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_INFO;

/**
 * Filter node that selects sources belonging to a specific SpimData dataset.
 *
 * <p>This node filters sources by checking if they are linked to a specific
 * {@link AbstractSpimData} instance via the SPIM_DATA_INFO metadata.</p>
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL
 */
public class SpimDataFilterNode extends FilterNode {

    private final AbstractSpimData<?> spimData;
    private final SourceAndConverterService sourceAndConverterService;

    /**
     * Creates a new SpimDataFilterNode.
     *
     * @param name the display name
     * @param spimData the SpimData dataset to filter for
     * @param sourceAndConverterService the service to query for metadata
     */
    public SpimDataFilterNode(String name,
                               AbstractSpimData<?> spimData,
                               SourceAndConverterService sourceAndConverterService) {
        super(name, null, false);
        this.spimData = spimData;
        this.sourceAndConverterService = sourceAndConverterService;
        setFilter(this::filter);
    }

    /**
     * Filters sources that belong to this SpimData.
     */
    private boolean filter(SourceAndConverter<?> sac) {
        if (!sourceAndConverterService.containsMetadata(sac, SPIM_DATA_INFO)) {
            return false;
        }
        SourceAndConverterService.SpimDataInfo info =
                (SourceAndConverterService.SpimDataInfo) sourceAndConverterService.getMetadata(sac, SPIM_DATA_INFO);
        return info != null && info.asd.equals(spimData);
    }

    /**
     * @return the SpimData dataset this node filters for
     */
    public AbstractSpimData<?> getSpimData() {
        return spimData;
    }
}
