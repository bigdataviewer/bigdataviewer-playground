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
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_INFO;

/**
 * Filter node that selects sources linked to a specific SpimData entity.
 *
 * <p>Entities are attributes of view setups like Channel, Angle, Illumination, etc.
 * This node filters sources by checking if their associated view setup has the
 * specified entity in its attributes.</p>
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL
 */
public class EntityFilterNode extends FilterNode {

    private final Entity entity;
    private final SourceAndConverterService sourceAndConverterService;

    /**
     * Creates a new EntityFilterNode.
     *
     * @param name the display name
     * @param entity the entity to filter for
     * @param sourceAndConverterService the service to query for metadata
     */
    public EntityFilterNode(String name,
                            Entity entity,
                            SourceAndConverterService sourceAndConverterService) {
        super(name, null, false);
        this.entity = entity;
        this.sourceAndConverterService = sourceAndConverterService;
        setFilter(this::filter);
    }

    /**
     * Filters sources that have this entity in their view setup attributes.
     */
    @SuppressWarnings("unchecked")
    private boolean filter(SourceAndConverter<?> sac) {
        if (!sourceAndConverterService.containsMetadata(sac, SPIM_DATA_INFO)) {
            return false;
        }

        SourceAndConverterService.SpimDataInfo info =
                (SourceAndConverterService.SpimDataInfo) sourceAndConverterService.getMetadata(sac, SPIM_DATA_INFO);
        if (info == null) {
            return false;
        }

        AbstractSpimData<AbstractSequenceDescription<BasicViewSetup, ?, ?>> asd =
                (AbstractSpimData<AbstractSequenceDescription<BasicViewSetup, ?, ?>>) info.asd;
        Integer setupId = info.setupId;

        BasicViewSetup setup = asd.getSequenceDescription().getViewSetups().get(setupId);
        if (setup == null) {
            return false;
        }

        return setup.getAttributes().values().contains(entity);
    }

    /**
     * @return the entity this node filters for
     */
    public Entity getEntity() {
        return entity;
    }
}
