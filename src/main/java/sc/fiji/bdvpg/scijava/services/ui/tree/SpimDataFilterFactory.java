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

import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.base.NamedEntity;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Factory for creating SpimData filter node hierarchies.
 *
 * <p>When a SpimData is registered, this factory creates a hierarchical structure
 * of filter nodes that organize sources by their entities (Channel, Angle, etc.).</p>
 *
 * <p>The hierarchy structure is:</p>
 * <pre>
 * SpimDataFilterNode (filters sources belonging to this SpimData)
 *   ├── All Sources (shows all sources in this SpimData)
 *   ├── Channel (class-level filter node)
 *   │   ├── Channel 0 (EntityFilterNode)
 *   │   │   └── All Sources
 *   │   └── Channel 1 (EntityFilterNode)
 *   │       └── All Sources
 *   ├── Angle (class-level filter node)
 *   │   ├── Angle 0 (EntityFilterNode)
 *   │   │   └── All Sources
 *   │   └── ...
 *   └── ... (other entity classes)
 * </pre>
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL
 */
public class SpimDataFilterFactory {

    private final SourceAndConverterService sourceAndConverterService;

    /**
     * Creates a new SpimDataFilterFactory.
     *
     * @param sourceAndConverterService the service to use for metadata queries
     */
    public SpimDataFilterFactory(SourceAndConverterService sourceAndConverterService) {
        this.sourceAndConverterService = sourceAndConverterService;
    }

    /**
     * Creates the complete filter hierarchy for a SpimData.
     *
     * @param spimData the SpimData to create the hierarchy for
     * @param name the display name for the SpimData node
     * @return the root SpimDataFilterNode with all children populated
     */
    @SuppressWarnings("unchecked")
    public SpimDataFilterNode createHierarchy(AbstractSpimData<?> spimData, String name) {
        SpimDataFilterNode spimDataNode = new SpimDataFilterNode(name, spimData, sourceAndConverterService);

        // Add "All Sources" node
        FilterNode allSourcesNode = new FilterNode("All Sources", sac -> true, true);
        spimDataNode.addChild(allSourcesNode);

        // Add entity-based filter nodes
        addEntityFilterNodes(spimDataNode,
                (AbstractSpimData<AbstractSequenceDescription<BasicViewSetup, ?, ?>>) spimData);

        return spimDataNode;
    }

    /**
     * Adds entity filter nodes to a SpimData node.
     * Entities are organized by class (Channel, Angle, etc.) and then by individual entity.
     */
    private void addEntityFilterNodes(SpimDataFilterNode nodeSpimData,
                                       AbstractSpimData<AbstractSequenceDescription<BasicViewSetup, ?, ?>> asd) {
        // Get all entities grouped by class
        Map<Class<?>, List<Entity>> entitiesByClass = asd.getSequenceDescription()
                .getViewDescriptions()
                .values().stream()
                .filter(BasicViewDescription::isPresent)
                .map(v -> v.getViewSetup().getAttributes().values())
                .reduce(new ArrayList<>(), (a, b) -> {
                    a.addAll(b);
                    return a;
                }).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Entity::getClass, Collectors.toList()));

        // Create class-level filter nodes
        Map<Class<?>, FilterNode> classNodes = new HashMap<>();
        for (Class<?> entityClass : entitiesByClass.keySet()) {
            FilterNode classNode = new FilterNode(entityClass.getSimpleName(), sac -> true, false);
            classNodes.put(entityClass, classNode);
        }

        // Sort and add class nodes to SpimData node
        List<FilterNode> orderedNodes = new ArrayList<>(classNodes.values());
        orderedNodes.sort(Comparator.comparing(FilterNode::getName));
        for (FilterNode classNode : orderedNodes) {
            nodeSpimData.addChild(classNode);
        }

        // Add individual entity filter nodes under each class node
        Set<Entity> entitiesAlreadyRegistered = new HashSet<>();
        for (Map.Entry<Class<?>, List<Entity>> entry : entitiesByClass.entrySet()) {
            Class<?> entityClass = entry.getKey();
            List<Entity> entities = entry.getValue();

            for (Entity entity : entities) {
                if (entitiesAlreadyRegistered.contains(entity)) {
                    continue;
                }
                entitiesAlreadyRegistered.add(entity);

                // Determine entity name
                String entityName = null;
                if (entity instanceof NamedEntity) {
                    entityName = ((NamedEntity) entity).getName();
                }
                if (entityName == null || entityName.isEmpty()) {
                    entityName = entityClass.getSimpleName() + " " + entity.getId();
                }

                // Create entity filter node
                EntityFilterNode entityNode = new EntityFilterNode(entityName, entity, sourceAndConverterService);

                // Add "All Sources" child to entity node
                FilterNode allSourcesNode = new FilterNode("All Sources", sac -> true, true);
                entityNode.addChild(allSourcesNode);

                // Add to class node
                classNodes.get(entityClass).addChild(entityNode);
            }
        }
    }
}
