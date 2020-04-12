package sc.fiji.bdvpg.scijava.services.ui;

import bdv.viewer.SourceAndConverter;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.function.Predicate;

/**
 * SourceAndConverter filter node : generic
 */
public class SourceFilterNode extends DefaultMutableTreeNode {
    public Predicate<SourceAndConverter> filter;
    public boolean allowDuplicate;
    public String name;

    public SourceFilterNode(String name, Predicate<SourceAndConverter> filter, boolean allowDuplicate) {
        super(name);
        this.name = name;
        this.filter = filter;
        this.allowDuplicate = allowDuplicate;
    }

    public String toString() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}