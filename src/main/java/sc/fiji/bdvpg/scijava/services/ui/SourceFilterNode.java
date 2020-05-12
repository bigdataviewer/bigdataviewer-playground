package sc.fiji.bdvpg.scijava.services.ui;

import bdv.viewer.SourceAndConverter;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * SourceAndConverter filter node : generic node which filters the incoming SourceAndConverter Object
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
 *      - a new SourceAndConverter node is inserted
 *      - the filter has changed
 *      - one of the input SourceAndConverter PROPERTY has changed
 *      - a SourceAndConverter node has to be removed
 *
 */

public class SourceFilterNode extends DefaultMutableTreeNode {

    public Predicate<SourceAndConverter> filter;
    String name;
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
    volatile Set<SourceAndConverter> currentInputSacs = ConcurrentHashMap.newKeySet();
    volatile Set<SourceAndConverter> currentOutputSacs = ConcurrentHashMap.newKeySet();

    public boolean hasConsumed(SourceAndConverter sac) {
        return currentOutputSacs.contains(sac);
    }

    @Override
    public void insert(MutableTreeNode newChild, int childIndex) { // is synchronized useful ?
        System.out.println("insert called");
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
                        //super.insert(newChild, childIndex);
                    }
                }
            }
        } else {
            // It's not a node containing a SourceAndConverter : standard behaviour
            super.insert(newChild, childIndex);
            this.update(new NodeAddedUpdateEvent(newChild));
        }
    }

    public static SourceAndConverter getSacFromNode(MutableTreeNode newChild) {
        return ((RenamableSourceAndConverter)(((DefaultMutableTreeNode)newChild).getUserObject())).sac;
    }

    public void remove(SourceAndConverter sac) {
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

    /*public final static String SOURCES_UPDATED = "SOURCES_UPDATED";

    public final static String FILTER_UPDATED = "FILTER_UPDATED";

    public final static String NODE_ADDED = "NODE_ADDED";*/

    public synchronized void update(UpdateEvent event) {
        if (event instanceof NodeAddedUpdateEvent) {
            System.out.println("Node added");
            NodeAddedUpdateEvent nodeEvent = (NodeAddedUpdateEvent) event;
            assert this.isNodeChild(nodeEvent.getNode());
            if (nodeEvent.getNode() instanceof SourceFilterNode) {
                for (SourceAndConverter sac : currentOutputSacs) {
                    ((SourceFilterNode) nodeEvent.getNode()).add(new DefaultMutableTreeNode(new RenamableSourceAndConverter(sac)));
                }
            }
        } else if (event instanceof FilterUpdateEvent) {
            for (SourceAndConverter sac : currentInputSacs) {
                System.out.println("currentInputSacs length = "+currentInputSacs.size());
                System.out.println("testing " + sac.getSpimSource().getName());
                if (filter.test(sac)) {
                    System.out.println("test pass ");
                    if (!currentOutputSacs.contains(sac)) {
                        // a blocked source is now passing
                        System.out.println("1111111111111 a blocked source is now passing");
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

                    System.out.println("test do not pass ");
                    if (currentOutputSacs.contains(sac)) {
                        // a passing source is now blocked
                        //System.out.println(toString());
                        System.out.println("0000000000000 a passing source is now blocked");
                        remove(sac);
                        // Restores it because it's this node who filtered it
                        currentInputSacs.add(sac);
                    }
                }
            }
        } else if (event instanceof SourceUpdateEvent){
            SourceUpdateEvent sourceEvent = (SourceUpdateEvent) event;
            SourceAndConverter sac = sourceEvent.getSource();
            System.out.println("currentInputSacs length = "+currentInputSacs.size());
            System.out.println("testing " + sac.getSpimSource().getName());
            if (filter.test(sac)) {
                System.out.println("test pass ");
                if (!currentOutputSacs.contains(sac)) {
                    // a blocked source is now passing
                    System.out.println("1111111111111 a blocked source is now passing");
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

                System.out.println("test do not pass ");
                if (currentOutputSacs.contains(sac)) {
                    // a passing source is now blocked
                    //System.out.println(toString());
                    System.out.println("0000000000000 a passing source is now blocked");
                    remove(sac);
                    // Restores it because it's this node who filtered it
                    currentInputSacs.add(sac);
                }
            }

        } else {
            throw new UnsupportedOperationException("Unknown UpdateEvent class ");
        }


        //System.out.println("Update node "+toString()+" event = "+event);
        /*
        switch (event) {
            case SOURCES_UPDATED:
            case FILTER_UPDATED:
                // Actually it's the same behaviour
                for (SourceAndConverter sac : currentInputSacs) {
                    System.out.println("currentInputSacs length = "+currentInputSacs.size());
                    System.out.println("testing " + sac.getSpimSource().getName());
                    if (filter.test(sac)) {
                        System.out.println("test pass ");
                        if (!currentOutputSacs.contains(sac)) {
                            // a blocked source is now passing
                            System.out.println("1111111111111 a blocked source is now passing");
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

                            System.out.println("a passing source is still passing, updating children");
                            if (event.equals(SOURCES_UPDATED)) {

                                // System.out.println("there are "+getChildCount()+" children");
                                for (int i = 0; i < getChildCount(); i++) {
                                    DefaultMutableTreeNode n = (DefaultMutableTreeNode) getChildAt(i);
                                    if (n instanceof SourceFilterNode) {

                                        System.out.println("updating child "+i+" named "+n.toString());
                                        ((SourceFilterNode) n).update(event);
                                    }
                                }
                            }
                        }
                    } else {

                        System.out.println("test do not pass ");
                        if (currentOutputSacs.contains(sac)) {
                            // a passing source is now blocked
                            //System.out.println(toString());
                            System.out.println("0000000000000 a passing source is now blocked");
                            currentOutputSacs.remove(sac);
                            // System.out.println("there are "+getChildCount()+" children");
                            for (int i = 0; i < getChildCount(); i++) {

                                System.out.println("Getting node i");
                                DefaultMutableTreeNode n = (DefaultMutableTreeNode) getChildAt(i);

                                System.out.println("Got it!");
                                if (n instanceof SourceFilterNode) {
                                    System.out.println("updating child "+i+" named "+n.toString());
                                    ((SourceFilterNode) n).remove(sac);
                                } else {
                                    if (displayFilteredSources) {
                                        if (n.getUserObject() instanceof RenamableSourceAndConverter) {
                                            if (((RenamableSourceAndConverter)(n.getUserObject())).sac.equals(sac)) {

                                                System.out.println("removing child "+i+" named "+n.toString()+" because it contains an obsolete leaf");
                                                this.remove(i);
                                                i--;
                                                System.out.println("done removing");
                                                //n.removeFromParent();//.removefrompaent();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                break;

            case NODE_ADDED:
                System.out.println("Node added");
                for (SourceAndConverter sac : currentOutputSacs) {
                    for (int i = 0; i < getChildCount(); i++) {
                        DefaultMutableTreeNode n = (DefaultMutableTreeNode) getChildAt(i);
                        if (n instanceof SourceFilterNode) {
                            n.add(new DefaultMutableTreeNode(new RenamableSourceAndConverter(sac)));
                        }
                    }
                }
                break;
        }*/
    }

    public String getName() {
        return name;
    }

    public static class UpdateEvent {
    }

    public static class FilterUpdateEvent extends UpdateEvent {
        public FilterUpdateEvent() {

        }
    }

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