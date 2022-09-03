/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.bdvpg.scijava.services.ui;

import bdv.viewer.SourceAndConverter;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * {@link SourceAndConverter} filter node : generic node which filters the
 * incoming SourceAndConverter Object Inserting in a node like this a
 * {@link DefaultMutableTreeNode} which contains a user object of class
 * {@link RenamableSourceAndConverter} triggers this procedure: - is the
 * {@link SourceFilterNode#filter} accepting the SourceAndConverter ? - yes :
 * was the SourceAndConverter present in the currentInputSacs ? - yes : nothing
 * to be done - it has already been handled - no : -if the filter keeps do not
 * reject it: - it is inserted in the children filtered node and - if they
 * consume it and duplicates are not allowed : nothing more has to be done - if
 * they do not consume or duplicated is allowed : - SourceAndConverter node is
 * directly added as a child - if the filter rejects it: - Nothing to be done -
 * it's not inserted - no : was the SourceAndConverter present in the
 * currentInputSacs ? - no : nothing has to be done - yes : something has
 * changed, on the source side, or on the filter node side - The children nodes
 * which link to this SourceAndConverter have to be removed - The children
 * filter nodes have have their remove SourceAndConverter method called - the
 * node has to be updated if: - a new SourceAndConverter node is inserted
 * {@link SourceUpdateEvent} - the filter has changed, and source needs to be
 * retested {@link FilterUpdateEvent} - a new children node is inserted
 * {@link NodeAddedUpdateEvent} // TODO : check that if linked nodes are added,
 * the update is properly handled - Implements cloneable : clone is used in copy
 * / paste of nodes + in drag and drop of sourcefilter nodes // TODO : node name
 * change event ? // TODO : Is this functionality implemented in an overly
 * complicated manner ?...
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL
 */

public class SourceFilterNode extends DefaultMutableTreeNode implements
	Cloneable
{

	/**
	 * Filters SourceAndConverter to downstream nodes in the tree
	 */
	public Predicate<SourceAndConverter<?>> filter;

	/**
	 * Name of this node : displayed in the jtree
	 */
	String name;

	/**
	 * Are the filtered sources displayed as direct children of this node ?
	 */
	final boolean displayFilteredSources;

	DefaultTreeModel model;

	public SourceFilterNode(DefaultTreeModel model, String name,
		Predicate<SourceAndConverter<?>> filter, boolean displayFilteredSources)
	{
		super(name);
		this.model = model;
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

	// Holding current state = set of SourceAndConverter contained in the filter
	// node
	final Set<SourceAndConverter<?>> currentInputSacs = ConcurrentHashMap
		.newKeySet();
	final Set<SourceAndConverter<?>> currentOutputSacs = ConcurrentHashMap
		.newKeySet();

	public boolean hasConsumed(SourceAndConverter<?> sac) {
		return currentOutputSacs.contains(sac);
	}

	@Override
	public synchronized void insert(MutableTreeNode newChild, int childIndex) { // is
																																							// synchronized
																																							// useful
																																							// ?

		if (((DefaultMutableTreeNode) newChild)
			.getUserObject() instanceof RenamableSourceAndConverter)
		{
			SourceAndConverter<?> sac = getSacFromNode(newChild);
			if (currentInputSacs.contains(sac)) {
				// Nothing to be done
			}
			else {
				currentInputSacs.add(sac);
				if (filter.test(sac)) {
					currentOutputSacs.add(sac);
					for (int i = 0; i < getChildCount(); i++) {
						DefaultMutableTreeNode n = (DefaultMutableTreeNode) getChildAt(i);
						if (n instanceof SourceFilterNode) {
							n.add(new DefaultMutableTreeNode(
								((DefaultMutableTreeNode) newChild).getUserObject()));
						}
					}
					if (displayFilteredSources) {
						DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(
							((DefaultMutableTreeNode) newChild).getUserObject());
						super.insert(newNode, childIndex);
						safeModelReloadAction(() -> model.reload(this));// .nodesWereInserted(this,
																														// new
																														// int[]{childIndex})
																														// ); // updates
																														// model in EDT
																														// thread
					}
				}
			}
		}
		else {
			// It's not a node containing a SourceAndConverter : standard behaviour
			super.insert(newChild, childIndex);
			safeModelReloadAction(() -> model.nodesWereInserted(this, new int[] {
				childIndex })); // updates model in EDT thread

			// Still : notifying the insertion of a new node, which can be a new
			// filter node and thus needs to be recomputed
			this.update(new NodeAddedUpdateEvent(newChild));
		}
	}

	private static SourceAndConverter<?> getSacFromNode(
		MutableTreeNode newChild)
	{
		return ((RenamableSourceAndConverter) (((DefaultMutableTreeNode) newChild)
			.getUserObject())).sac;
	}

	public void remove(MutableTreeNode aChild) {
		int iChild = this.getIndex(aChild);
		super.remove(aChild);
		safeModelReloadAction(() -> model.nodesWereRemoved(this, new int[] {
			iChild }, new Object[] { aChild }));
	}

	/**
	 * Removes a source and converter from this node and children nodes
	 * 
	 * @param sac source to remove
	 */
	void remove(SourceAndConverter<?> sac) {
		currentInputSacs.remove(sac);
		currentOutputSacs.remove(sac);

		for (int i = 0; i < getChildCount(); i++) {
			DefaultMutableTreeNode n = (DefaultMutableTreeNode) getChildAt(i);
			if (n instanceof SourceFilterNode) {
				((SourceFilterNode) n).remove(sac);
			}
			else {
				if (displayFilteredSources) {
					if (n.getUserObject() instanceof RenamableSourceAndConverter) {
						if (((RenamableSourceAndConverter) (n.getUserObject())).sac.equals(
							sac))
						{
							remove(n);
						}
					}
				}
			}
		}
	}

	/**
	 * Very important method which recomputes the tree based on the
	 * {@link UpdateEvent} notified ensures new and up-to-date computation of the
	 * whole tree
	 * 
	 * @param event cast event
	 */
	public void update(UpdateEvent event) {
		if (event instanceof NodeAddedUpdateEvent) {
			NodeAddedUpdateEvent nodeEvent = (NodeAddedUpdateEvent) event;
			assert this.isNodeChild(nodeEvent.getNode());
			if (nodeEvent.getNode() instanceof SourceFilterNode) {
				for (SourceAndConverter<?> sac : currentOutputSacs) {
					((SourceFilterNode) nodeEvent.getNode()).add(
						new DefaultMutableTreeNode(new RenamableSourceAndConverter(sac)));
				}
			}
		}
		else if (event instanceof FilterUpdateEvent) {
			for (SourceAndConverter<?> sac : currentInputSacs) {
				if (filter.test(sac)) {
					if (!currentOutputSacs.contains(sac)) {
						// a blocked source is now passing
						currentOutputSacs.add(sac);
						RenamableSourceAndConverter rsac = new RenamableSourceAndConverter(
							sac);
						for (int i = 0; i < getChildCount(); i++) {
							DefaultMutableTreeNode n = (DefaultMutableTreeNode) getChildAt(i);
							if (n instanceof SourceFilterNode) {
								n.add(new DefaultMutableTreeNode(rsac));
							}
						}
						if (displayFilteredSources) {
							int iChild = getChildCount() - 1;
							super.insert(new DefaultMutableTreeNode(rsac), iChild);
							safeModelReloadAction(() -> model.nodesWereInserted(this,
								new int[] { iChild }));
						}
					}
				}
				else {
					if (currentOutputSacs.contains(sac)) {
						// a passing source is now blocked
						remove(sac);
						// Restores it because it's this node who filtered it
						currentInputSacs.add(sac);
					}
				}
			}
		}
		else if (event instanceof SourceUpdateEvent) {
			SourceUpdateEvent sourceEvent = (SourceUpdateEvent) event;
			SourceAndConverter<?> sac = sourceEvent.getSource();
			if (filter.test(sac)) {
				if (!currentOutputSacs.contains(sac)) {
					// a blocked source is now passing
					currentOutputSacs.add(sac);
					RenamableSourceAndConverter rsac = new RenamableSourceAndConverter(
						sac);
					for (int i = 0; i < getChildCount(); i++) {
						DefaultMutableTreeNode n = (DefaultMutableTreeNode) getChildAt(i);
						if (n instanceof SourceFilterNode) {
							n.add(new DefaultMutableTreeNode(rsac));
						}
					}
					if (displayFilteredSources) {
						int iChild = getChildCount() - 1;
						super.insert(new DefaultMutableTreeNode(rsac), iChild);
						safeModelReloadAction(() -> model.nodesWereInserted(this,
							new int[] { iChild }));
					}
				}
				else {
					// Still need to update the children
					for (int i = 0; i < getChildCount(); i++) {
						DefaultMutableTreeNode n = (DefaultMutableTreeNode) getChildAt(i);
						if (n instanceof SourceFilterNode) {
							((SourceFilterNode) n).update(event);
						}
					}
				}
			}
			else {
				if (currentOutputSacs.contains(sac)) {
					// a passing source is now blocked
					remove(sac);
					// Restores it because it's this node who filtered it
					currentInputSacs.add(sac);
				}
			}

		}
		else {
			throw new UnsupportedOperationException("Unsupported UpdateEvent class ");
		}

	}

	public String getName() {
		return name;
	}

	/**
	 * Abstract parent method for update events, should not be instantianted
	 * directly
	 */
	abstract static class UpdateEvent {}

	/**
	 * The filter of this node has been modified - recomputation needed There is
	 * no need to notified children nodes as the
	 * {@link SourceFilterNode#update(UpdateEvent)} method will do the job itself
	 */
	public static class FilterUpdateEvent extends UpdateEvent {

		public FilterUpdateEvent() {

		}
	}

	/**
	 * A source has been modified in some ways, all nodes should recompute if the
	 * source filtering result is modified There is no need to notified children
	 * nodes as the {@link SourceFilterNode#update(UpdateEvent)} method will do
	 * the job itself However, this is true for the children only. Normally, such
	 * an event should be triggered from the top node, to ensure a complete update
	 * of the full tree
	 */
	public static class SourceUpdateEvent extends UpdateEvent {

		final SourceAndConverter<?> sac;

		public SourceUpdateEvent(final SourceAndConverter<?> sac) {
			this.sac = sac;
		}

		public SourceAndConverter<?> getSource() {
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

	public Object clone() {
		return new SourceFilterNode(model, name, filter, displayFilteredSources);
	}

	/**
	 * Executes the model reloading actions in the EDT
	 *
	 * @param runnable method to run in the EDT
	 */
	static public void safeModelReloadAction(Runnable runnable) {
		if (SwingUtilities.isEventDispatchThread()) {
			runnable.run();
		}
		else {
			SwingUtilities.invokeLater(runnable);
		}
	}

}
