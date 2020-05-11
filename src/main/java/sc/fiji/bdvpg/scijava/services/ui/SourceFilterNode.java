package sc.fiji.bdvpg.scijava.services.ui;

import bdv.viewer.SourceAndConverter;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
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
 * - Properties:
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
    final Set<SourceAndConverter> currentInputSacs = ConcurrentHashMap.newKeySet();
    final Set<SourceAndConverter> currentOutputSacs = ConcurrentHashMap.newKeySet();

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
                        //super.insert(newChild, childIndex);
                    }
                }
            }
        } else {
            // It's not a node containing a SourceAndConverter : standard behaviour
            super.insert(newChild, childIndex);
            this.update(NODE_ADDED);
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

    public final static String SOURCES_UPDATED = "SOURCES_UPDATED";
    public final static String FILTER_UPDATED = "FILTER_UPDATED";
    public final static String NODE_ADDED = "NODE_ADDED";

    public synchronized void update(String event) {
        //System.out.println("Update node "+toString()+" event = "+event);
        switch (event) {
            case SOURCES_UPDATED:
            case FILTER_UPDATED:
                // Actually it's the same behaviour
                for (SourceAndConverter sac : currentInputSacs) {

                    //System.out.println("testing " + sac.getSpimSource().getName());
                    if (filter.test(sac)) {
                        if (!currentOutputSacs.contains(sac)) {
                            // a blocked source is now passing
                            // System.out.println("a blocked source is now passing");
                            currentOutputSacs.add(sac);
                            for (int i = 0; i < getChildCount(); i++) {
                                DefaultMutableTreeNode n = (DefaultMutableTreeNode) getChildAt(i);
                                if (n instanceof SourceFilterNode) {
                                    n.add(new DefaultMutableTreeNode(new RenamableSourceAndConverter(sac)));
                                }
                            }
                            if (displayFilteredSources) {
                               super.insert(new DefaultMutableTreeNode(new RenamableSourceAndConverter(sac)), getChildCount());
                            }
                        } else {
                            // System.out.println("a passing source is still passing, updating children");
                            if (event.equals(SOURCES_UPDATED)) {

                                // System.out.println("there are "+getChildCount()+" children");
                                for (int i = 0; i < getChildCount(); i++) {
                                    DefaultMutableTreeNode n = (DefaultMutableTreeNode) getChildAt(i);
                                    if (n instanceof SourceFilterNode) {

                                        // System.out.println("updating child "+i+" named "+n.toString());
                                        ((SourceFilterNode) n).update(event);
                                    }
                                }
                            }
                        }
                    } else {
                        if (currentOutputSacs.contains(sac)) {
                            // a passing source is now blocked
                            System.out.println(toString());
                            System.out.println("a passing source is now blocked");
                            currentOutputSacs.remove(sac);
                            // System.out.println("there are "+getChildCount()+" children");
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
                    }
                }

                break;

            case NODE_ADDED:

                for (SourceAndConverter sac : currentOutputSacs) {
                    for (int i = 0; i < getChildCount(); i++) {
                        DefaultMutableTreeNode n = (DefaultMutableTreeNode) getChildAt(i);
                        if (n instanceof SourceFilterNode) {
                            n.add(new DefaultMutableTreeNode(new RenamableSourceAndConverter(sac)));
                        }
                    }
                }
                break;
        }
    }

    public String getName() {
        return name;
    }

}