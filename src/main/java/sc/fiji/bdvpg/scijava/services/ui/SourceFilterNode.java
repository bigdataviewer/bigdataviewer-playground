package sc.fiji.bdvpg.scijava.services.ui;

import bdv.viewer.SourceAndConverter;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * {@link SourceAndConverter} filter node : generic node which filters the incoming SourceAndConverter Object
 *
 * Inserting in a node like this a {@link DefaultMutableTreeNode} which contains a user object of
 * class {@link RenamableSourceAndConverter} triggers this procedure:
 * - is the {@link SourceFilterNode#filter} accepting the SourceAndConverter ?
 *     - yes : was the SourceAndConverter present in the currentInputSacs ?
 *         - yes : nothing to be done - it has already been handled
 *         - no :
 *              -if the filter keeps do not reject it:
 *                  - it is inserted in the children filtered node and
 *                       - if they consume it and duplicates are not allowed : nothing more has to be done
 *                       - if they do not consume or duplicated is allowed :
 *                            - SourceAndConverter node is directly added as a child
 *              - if the filter rejects it:
 *                   - Nothing to be done - it's not inserted
 *
 *     - no : was the SourceAndConverter present in the currentInputSacs ?
 *         - no : nothing has to be done
 *         - yes : something has changed, on the source side, or on the filter node side
 *             - The children nodes which link to this SourceAndConverter have to be removed
 *             - The children filter nodes have have their remove SourceAndConverter method called
 *
 * - the node has to be updated if:
 *      - a new SourceAndConverter node is inserted {@link SourceUpdateEvent}
 *      - the filter has changed, and source needs to be retested {@link FilterUpdateEvent}
 *      - a new children node is inserted {@link NodeAddedUpdateEvent} // TODO : check that is linked nodes are added, the update is properly handled
 *
 */

public class SourceFilterNode extends DefaultMutableTreeNode {

    /**
     * Filters SourceAndConverter to downstream nodes in the tree
     */
    public Predicate<SourceAndConverter> filter;

    /**
     * Name of this node : displayed in the jtree
     */
    String name;

    /**
     * Are the filtered sources displayed as direct children of this node ?
     */
    boolean displayFilteredSources;

    public SourceFilterNode(String name, Predicate<SourceAndConverter> filter, boolean displayFilteredSources) {
        super(name);
        this.name = name;
        this.filter = filter;
        this.displayFilteredSources = displayFilteredSources;
    }

    public String toString() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Holding current state = set of SourceAndConverter contained in the filter node
    Set<SourceAndConverter> currentInputSacs = ConcurrentHashMap.newKeySet();
    Set<SourceAndConverter> currentOutputSacs = ConcurrentHashMap.newKeySet();

    public boolean hasConsumed(SourceAndConverter sac) {
        return currentOutputSacs.contains(sac);
    }

    @Override
    public synchronized void insert(MutableTreeNode newChild, int childIndex) { // is synchronized useful ?

        if (((DefaultMutableTreeNode)newChild).getUserObject() instanceof RenamableSourceAndConverter) {
            SourceAndConverter sac = getSacFromNode(newChild);
            if (currentInputSacs.contains(sac)) {
                // Nothing to be done
            } else {
                currentInputSacs.add(sac);
                if (filter.test(sac)) {
                    currentOutputSacs.add(sac);
                    for (int i = 0; i < getChildCount(); i++) {
                        DefaultMutableTreeNode n = (DefaultMutableTreeNode) getChildAt(i);
                        if (n instanceof SourceFilterNode) {
                            n.add(new DefaultMutableTreeNode(((DefaultMutableTreeNode)newChild).getUserObject()));
                        }
                    }
                    if (displayFilteredSources) {
                        super.insert(new DefaultMutableTreeNode(((DefaultMutableTreeNode)newChild).getUserObject()), childIndex);
                    }
                }
            }
        } else {
            // It's not a node containing a SourceAndConverter : standard behaviour
            super.insert(newChild, childIndex);
            // Still : notifying the insertion of a new node, which can be a nw filter node and thus needs recomputation
            this.update(new NodeAddedUpdateEvent(newChild));
        }
    }

    public static SourceAndConverter getSacFromNode(MutableTreeNode newChild) {
        return ((RenamableSourceAndConverter)(((DefaultMutableTreeNode)newChild).getUserObject())).sac;
    }

    /**
     * Removes a source and converter from this node and children nodes
     * @param sac
     */
    void remove(SourceAndConverter sac) {
        currentInputSacs.remove(sac);
        currentOutputSacs.remove(sac);
        for (int i = 0; i < getChildCount(); i++) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) getChildAt(i);
            if (n instanceof SourceFilterNode) {
                // System.out.println("updating child "+i+" named "+n.toString());
                ((SourceFilterNode) n).remove(sac);
            } else {
                if (displayFilteredSources) {
                    if (n.getUserObject() instanceof RenamableSourceAndConverter) {
                        if (((RenamableSourceAndConverter)(n.getUserObject())).sac.equals(sac)) {
                            super.remove(n);
                        }
                    }
                }
            }
        }
    }

    /**
     * Very important method which recomputes the tree based on the {@link UpdateEvent} notified
     * ensures new and up to date recomputation of the whole tree
     * @param event
     */
    public synchronized void update(UpdateEvent event) {
        if (event instanceof NodeAddedUpdateEvent) {
            NodeAddedUpdateEvent nodeEvent = (NodeAddedUpdateEvent) event;
            assert this.isNodeChild(nodeEvent.getNode());
            if (nodeEvent.getNode() instanceof SourceFilterNode) {
                for (SourceAndConverter sac : currentOutputSacs) {
                    ((SourceFilterNode) nodeEvent.getNode()).add(new DefaultMutableTreeNode(new RenamableSourceAndConverter(sac)));
                }
            }
        } else if (event instanceof FilterUpdateEvent) {
            for (SourceAndConverter sac : currentInputSacs) {
                if (filter.test(sac)) {
                    if (!currentOutputSacs.contains(sac)) {
                        // a blocked source is now passing
                        currentOutputSacs.add(sac);
                        RenamableSourceAndConverter rsac = new RenamableSourceAndConverter(sac);
                        for (int i = 0; i < getChildCount(); i++) {
                            DefaultMutableTreeNode n = (DefaultMutableTreeNode) getChildAt(i);
                            if (n instanceof SourceFilterNode) {
                                n.add(new DefaultMutableTreeNode(rsac));
                            }
                        }
                        if (displayFilteredSources) {
                            super.insert(new DefaultMutableTreeNode(rsac), getChildCount());
                        }
                    }
                } else {
                    if (currentOutputSacs.contains(sac)) {
                        // a passing source is now blocked
                        remove(sac);
                        // Restores it because it's this node who filtered it
                        currentInputSacs.add(sac);
                    }
                }
            }
        } else if (event instanceof SourceUpdateEvent){
            SourceUpdateEvent sourceEvent = (SourceUpdateEvent) event;
            SourceAndConverter sac = sourceEvent.getSource();
            if (filter.test(sac)) {
                if (!currentOutputSacs.contains(sac)) {
                    // a blocked source is now passing
                    currentOutputSacs.add(sac);
                    RenamableSourceAndConverter rsac = new RenamableSourceAndConverter(sac);
                    for (int i = 0; i < getChildCount(); i++) {
                        DefaultMutableTreeNode n = (DefaultMutableTreeNode) getChildAt(i);
                        if (n instanceof SourceFilterNode) {
                            n.add(new DefaultMutableTreeNode(rsac));
                        }
                    }
                    if (displayFilteredSources) {
                        super.insert(new DefaultMutableTreeNode(rsac), getChildCount());
                    }
                } else {
                    // Still need to update the children
                    for (int i = 0; i < getChildCount(); i++) {
                        DefaultMutableTreeNode n = (DefaultMutableTreeNode) getChildAt(i);
                        if (n instanceof SourceFilterNode) {
                            ((SourceFilterNode)n).update(event);
                        }
                    }
                }
            } else {
                if (currentOutputSacs.contains(sac)) {
                    // a passing source is now blocked
                    remove(sac);
                    // Restores it because it's this node who filtered it
                    currentInputSacs.add(sac);
                }
            }

        } else {
            throw new UnsupportedOperationException("Unsupported UpdateEvent class ");
        }

    }

    public String getName() {
        return name;
    }

    /**
     * Abstract parent method for update events, should not be instantianted directly
     */
    abstract static class UpdateEvent {
    }

    /**
     * The filter of this node has been modified - recomputation needed
     * There is no need to notified children nodes as the {@link SourceFilterNode#update(UpdateEvent)}
     * method will do the job itself
     */
    public static class FilterUpdateEvent extends UpdateEvent {
        public FilterUpdateEvent() {

        }
    }

    /**
     * A source has been modified in some ways, all nodes should recompute if
     * the source filtering result is modified
     * There is no need to notified children nodes as the {@link SourceFilterNode#update(UpdateEvent)}
     * method will do the job itself
     *
     * However this is true for the children only.
     *
     * Normally, such an event should be triggered from the top node, to ensure a complete update of
     * the full tree
     */
    public static class SourceUpdateEvent extends UpdateEvent {
        final SourceAndConverter sac;

        public SourceUpdateEvent(final SourceAndConverter sac) {
            this.sac = sac;
        }

        public SourceAndConverter getSource() {
            return sac;
        }

    }

    public static class NodeAddedUpdateEvent extends UpdateEvent {

        final TreeNode o;

        public NodeAddedUpdateEvent(final TreeNode o) {
            this.o = o;
        }

        public TreeNode getNode() {
            return o;
        }
    }

}