package sc.fiji.bdvpg.scijava.services.ui;

import bdv.AbstractSpimSource;
import bdv.img.WarpedSource;
import bdv.tools.transformation.TransformedSource;
import bdv.util.ResampledSource;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_INFO;

/**
 * Swing UI for Scijava BdvSourceAndConverterService
 *
 * All the SourceAndConverter are inserted into the tree potentially multiple times,
 * in order to allow for multiple useful hierarchy to appear. The most basic example is a
 * SourceAndConverter which is part of a SpimData object. This source will be caught by
 * the appropriate Spimdata Filter Node in which it will appear again multiple times, sorted
 * by Entity class found in the spimdata object (Illumination, Angle, Channel ...)
 *
 *
 * Nodes are DefaultTreeMutableNode containing potentially:
 * - RenamableSourceAndConverter (SourceAndConverter with an overriden toString method)
 * - Filtering nodes : nodes that can filter SourceAndConverters,
 * they contain a Predicate<SourceAndConverter> that decides whether a source and converter
 * object should be included in the node; they also contain a boolean flag which sets whether the
 * SourceAndConverter should be passed to the current remaining branch in the tree
 * - Getter Node for Source properties. For instance in the inspect method, a Transformed Source will
 * create a Node for the wrapped source and another node which holds a getter for the fixedAffineTransform
 *
 */
public class BdvSourceServiceUI {

    /**
     * Linked SourceAndConverter Scijava service
     */
    SourceAndConverterService sourceAndConverterService;

    /**
     * JFrame container
     */
    JFrame frame;

    /**
     * JPanel container
     */
    JPanel panel;

    /**
     * Swing JTree used for displaying Sources object
     */
    JTree tree;

    /**
     * Tree root note
     */
    SourceFilterNode top;

    /**
     * Scrollpane to display the JTree, if too big
     */
    JScrollPane treeView;

    /**
     * Tree model
     */
    DefaultTreeModel model;

    /**
     * SourceAndConverter currently displayed in the JTree
     */
    Set<SourceAndConverter> displayedSource = new HashSet<>();

    /**
     * Node holding all Sources just below the root node,
     * Should be kept as last index in this branch. Nothing below except
     * results of inspection should go there.
     */
    SourceFilterNode allSourcesNode;

    /**
     * Spimdata Filter nodes currently present in the tree
     */
    List<SpimDataFilterNode> spimdataFilterNodes = new ArrayList<>();

    public BdvSourceServiceUI( SourceAndConverterService sourceAndConverterService ) {
        this.sourceAndConverterService = sourceAndConverterService;

        frame = new JFrame("Bdv Sources");
        panel = new JPanel(new BorderLayout());

        // Tree view of Spimdata
        top = new SourceFilterNode("Sources", (sac) -> true, true);
        tree = new JTree(top);
        //tree.setRootVisible(false);
        allSourcesNode = new SourceFilterNode("All Sources", (sac) -> true, false);
        top.add(allSourcesNode);

        SourceFilterNode spimDataSources = new SourceFilterNode("In SpimData", (sac) -> sourceAndConverterService.getSacToMetadata().get(sac).containsKey( SPIM_DATA_INFO ), false);
        allSourcesNode.add(spimDataSources);

        model = (DefaultTreeModel)tree.getModel();
        treeView = new JScrollPane(tree);

        panel.add(treeView, BorderLayout.CENTER);

        // JTree of SpimData
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                // Right Click -> popup
                if (SwingUtilities.isRightMouseButton(e)) {
                    new SourceAndConverterPopupMenu(getSelectedSourceAndConverters())
                            .getPopup()
                            .show(e.getComponent(), e.getX(), e.getY());
                }
                // Double Click : display source, if possible
                /*if (e.getClickCount()==2 && !e.isConsumed()) {
                    if (SacServies.getSourceDisplayService()!=null) {
                        for (SourceAndConverter sac: getSelectedSourceAndConverters()) {
                            SacServies.getSourceDisplayService().show(sac);
                        }
                    }
                }*/
            }
        });

        this.sourceAndConverterService.registerAction("Inspect Sources", this::inspectSources);

        // Delete node for inspection result only
        JMenuItem menuItem = new JMenuItem("Delete Inspect Node");
        menuItem.addActionListener(e -> {
                    for (TreePath tp : tree.getSelectionModel().getSelectionPaths()) {
                        if ((tp.getLastPathComponent()).toString().startsWith("Inspect Results [")) {
                            model.removeNodeFromParent((DefaultMutableTreeNode)tp.getLastPathComponent());
                        }
                    }
                }
        );
        frame.add(panel);
        frame.pack();
        frame.setVisible(false);
    }

    public void inspectSources(SourceAndConverter[] sacs) {
        for (SourceAndConverter sac:sacs) {
            inspectSource(sac);
        }
    }

    public void inspectSource(SourceAndConverter sac) {
        DefaultMutableTreeNode parentNodeInspect = new DefaultMutableTreeNode("Inspect Results ["+sac.getSpimSource().getName()+"]");
        appendInspectorResult(parentNodeInspect, sac);
        top.add(parentNodeInspect);
        model.reload(top);
    }

    public void appendMetadata(DefaultMutableTreeNode parent, SourceAndConverter sac) {
        Map<String, Object> metadata = SourceAndConverterServices.getSourceAndConverterService().getSacToMetadata().get(sac);
        metadata.keySet().forEach(k -> {
            DefaultMutableTreeNode nodeMetaKey = new DefaultMutableTreeNode(k);
            parent.add(nodeMetaKey);
            DefaultMutableTreeNode nodeMetaValue = new DefaultMutableTreeNode(metadata.get(k));
            nodeMetaKey.add(nodeMetaValue);
        });
    }

    public void appendInspectorResult(DefaultMutableTreeNode parent, SourceAndConverter sac) {
        if (sac.getSpimSource() instanceof TransformedSource) {
            DefaultMutableTreeNode nodeTransformedSource = new DefaultMutableTreeNode("Transformed Source");
            parent.add(nodeTransformedSource);
            TransformedSource source = (TransformedSource) sac.getSpimSource();
            DefaultMutableTreeNode nodeAffineTransformGetter = new DefaultMutableTreeNode(new Supplier<AffineTransform3D>(){
                public AffineTransform3D get() {
                    AffineTransform3D at3D = new AffineTransform3D();
                    source.getFixedTransform(at3D);
                    return at3D;
                }
                public String toString() {
                    return "AffineTransform["+source.getName()+"]";
                }
            });
            nodeTransformedSource.add(nodeAffineTransformGetter);
            if (sourceAndConverterService.getSourceAndConvertersFromSource(source.getWrappedSource()).size()>0) {
                // at least A sourceandconverteralready exists for this source
                sourceAndConverterService.getSourceAndConvertersFromSource(source.getWrappedSource()).forEach((src) -> {
                            DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                            nodeTransformedSource.add(wrappedSourceNode);
                            appendInspectorResult(wrappedSourceNode, src);
                        }
                );
            } else {
                // no source and converter exist for this source : creates it
                SourceAndConverter src = SourceAndConverterUtils.createSourceAndConverter(source.getWrappedSource());
                sourceAndConverterService.register(src);
                DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                nodeTransformedSource.add(wrappedSourceNode);
                appendInspectorResult(wrappedSourceNode, src);
            }
            appendMetadata(nodeTransformedSource,sac);
        }

        if (sac.getSpimSource() instanceof WarpedSource) {
            DefaultMutableTreeNode nodeWarpedSource = new DefaultMutableTreeNode("Warped Source");
            parent.add(nodeWarpedSource);
            WarpedSource source = (WarpedSource) sac.getSpimSource();
            DefaultMutableTreeNode nodeRealTransformGetter = new DefaultMutableTreeNode(new Supplier<RealTransform>(){
                public RealTransform get() {
                    return source.getTransform();
                }
                public String toString() {
                    return "RealTransform["+source.getName()+"]";
                }
            });
            nodeWarpedSource.add(nodeRealTransformGetter);
            if (sourceAndConverterService.getSourceAndConvertersFromSource(source.getWrappedSource()).size()>0) {
                // at least A sourceandconverteralready exists for this source
                sourceAndConverterService.getSourceAndConvertersFromSource(source.getWrappedSource()).forEach((src) -> {
                            DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                            nodeWarpedSource.add(wrappedSourceNode);
                            appendInspectorResult(wrappedSourceNode, src);
                        }
                );
            } else {
                // no source and converter exist for this source : creates it
                SourceAndConverter src = SourceAndConverterUtils.createSourceAndConverter(source.getWrappedSource());
                sourceAndConverterService.register(src);
                DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                nodeWarpedSource.add(wrappedSourceNode);
                appendInspectorResult(wrappedSourceNode, src);
            }
            appendMetadata(nodeWarpedSource,sac);
        }

        if (sac.getSpimSource() instanceof ResampledSource) {
            DefaultMutableTreeNode nodeResampledSource = new DefaultMutableTreeNode("Resampled Source");
            parent.add(nodeResampledSource);
            ResampledSource source = (ResampledSource) sac.getSpimSource();

            DefaultMutableTreeNode nodeOrigin = new DefaultMutableTreeNode("Origin");
            nodeResampledSource.add(nodeOrigin);

            if (sourceAndConverterService.getSourceAndConvertersFromSource(source.getOriginalSource()).size()>0) {
                // at least A sourceandconverteralready exists for this source
                sourceAndConverterService.getSourceAndConvertersFromSource(source.getOriginalSource()).forEach((src) -> {
                            DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                            nodeOrigin.add(wrappedSourceNode);
                            appendInspectorResult(wrappedSourceNode, src);
                        }
                );
            } else {
                // no source and converter exist for this source : creates it
                SourceAndConverter src = SourceAndConverterUtils.createSourceAndConverter(source.getOriginalSource());
                sourceAndConverterService.register(src);
                DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                nodeOrigin.add(wrappedSourceNode);
                appendInspectorResult(wrappedSourceNode, src);
            }

            DefaultMutableTreeNode nodeResampler = new DefaultMutableTreeNode("Sampler Model");
            nodeResampledSource.add(nodeResampler);

            if (sourceAndConverterService.getSourceAndConvertersFromSource(source.getModelResamplerSource()).size()>0) {
                // at least A sourceandconverteralready exists for this source
                sourceAndConverterService.getSourceAndConvertersFromSource(source.getModelResamplerSource()).forEach((src) -> {
                            DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                            nodeResampler.add(wrappedSourceNode);
                            appendInspectorResult(wrappedSourceNode, src);
                        }
                );
            } else {
                // no source and converter exist for this source : creates it
                SourceAndConverter src = SourceAndConverterUtils.createSourceAndConverter(source.getModelResamplerSource());
                sourceAndConverterService.register(src);
                DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                nodeResampler.add(wrappedSourceNode);
                appendInspectorResult(wrappedSourceNode, src);
            }
            appendMetadata(nodeResampledSource,sac);
        }

        if (sac.getSpimSource() instanceof AbstractSpimSource) {
            DefaultMutableTreeNode nodeSpimSource = new DefaultMutableTreeNode("Spim Source");
            parent.add(nodeSpimSource);
            appendMetadata(nodeSpimSource,sac);
        }
    }

    public void update(SourceAndConverter sac) {
        if (displayedSource.contains(sac)) {
            // No Need to update
            visitAllNodesAndDelete(top, sac);
            updateSpimDataFilterNodes();
            insertIntoTree(sac);
            //model.reload();
        } else {
            //System.out.println("Adding "+sac.getSpimSource().getName());
            displayedSource.add(sac);

            updateSpimDataFilterNodes();
            insertIntoTree(sac);
            panel.revalidate();
            frame.setVisible( true );
        }
    }

    private void updateSpimDataFilterNodes() {
        // Fetch All Spimdatas from all Sources
        Set<AbstractSpimData> currentSpimdatas = new HashSet<>();

        displayedSource.forEach(sac -> {
            if (sourceAndConverterService.getSacToMetadata().get(sac)!=null) {
                if (sourceAndConverterService.getSacToMetadata().get(sac).containsKey(SPIM_DATA_INFO)) {
                    currentSpimdatas.add(((SourceAndConverterService.SpimDataInfo) sourceAndConverterService.getSacToMetadata().get(sac).get(SPIM_DATA_INFO)).asd);
                }
            };
        });

        // Check for obsolete spimdatafilternodes
        spimdataFilterNodes.forEach(fnode -> {
            if (!currentSpimdatas.contains(fnode.asd)) {
                if (fnode.getParent()!=null) {
                    model.removeNodeFromParent(fnode);
                }
            }
        });

        // Check for new spimdata
        currentSpimdatas.forEach(asd -> {
                    //System.out.println("Test "+sdi.toString());
                    if ((spimdataFilterNodes.size()==0)||(spimdataFilterNodes.stream().noneMatch(fnode -> fnode.asd.equals(asd)))) {
                        SpimDataFilterNode newNode = new SpimDataFilterNode("SpimData "+spimdataFilterNodes.size(), asd);
                        spimdataFilterNodes.add(newNode);
                        addEntityFilterNodes(newNode, asd);
                        top.insert(newNode, 0);
                        model.reload(top);
                        //System.out.println("Adding");
                    }
                }
        );
    }

    private void addEntityFilterNodes(SpimDataFilterNode nodeSpimData, AbstractSpimData<AbstractSequenceDescription<BasicViewSetup,?,?>> asd) {
        // Gets all entities by class
        Map<Class, List<Entity>> entitiesByClass = asd.getSequenceDescription()
                .getViewDescriptions()
                // Streams viewSetups
                .values().stream()
                // Filters if view is present
                .filter(v -> v.isPresent())
                // Gets Entities associated to ViewSetup
                .map(v -> v.getViewSetup().getAttributes().values())
                // Reduce into a single list and stream
                .reduce(new ArrayList<>(), (a, b) -> {
                    a.addAll(b);
                    return a;
                }).stream()
                // Collected and sorted by class
                .collect(Collectors.groupingBy(e -> e.getClass(), Collectors.toList()));

        Map<Class, SourceFilterNode> classNodes = new HashMap<>();
        entitiesByClass.keySet().forEach((c)-> {
            classNodes.put(c, new SourceFilterNode(c.getSimpleName(),(sac)-> true, true));
        });

        classNodes.values().forEach((f) -> nodeSpimData.add(f));

        nodeSpimData.add(new SourceFilterNode("All Sources", (sac)->true, false));

        Set<Entity> entitiesAlreadyRegistered = new HashSet<>();
        entitiesByClass.forEach((c,el) -> {
            el.forEach(entity -> {
                if (!entitiesAlreadyRegistered.contains(entity)) {
                    classNodes.get(c).add(new SpimDataElementFilter(c.getSimpleName()+" "+entity.getId(),entity));
                    entitiesAlreadyRegistered.add(entity);
                }
            });
        });

    }

    void insertIntoTree(SourceAndConverter sac) {
        RenamableSourceAndConverter rsac = new RenamableSourceAndConverter(sac);
        insertIntoTree(top, rsac);
    }

    void insertIntoTree(SourceFilterNode parent, RenamableSourceAndConverter rsac) {
        boolean consumed = false;
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) parent.getChildAt(i);
            if (n instanceof SourceFilterNode) {
                SourceFilterNode f = (SourceFilterNode) n;
                if (f.filter.test(rsac.sac)) {
                    insertIntoTree(f, rsac);
                    if (!f.allowDuplicate) {
                        consumed = true;
                    }
                }
            }
        }
        if (!consumed) {
            //System.out.println("Adding "+rsac.sac.getSpimSource().getName());
            parent.add(new DefaultMutableTreeNode(rsac));
            model.reload(parent);
        }
    }

    public void remove(SourceAndConverter sac) {
        if (displayedSource.contains(sac)) {
            // No Need to update
            displayedSource.remove(sac);
            visitAllNodesAndDelete(top, sac);
            updateSpimDataFilterNodes();
        }
    }

    void visitAllNodesAndDelete(TreeNode node, SourceAndConverter sac) {
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements();) {
                TreeNode n = (TreeNode) e.nextElement();
                //n.isLeaf() &&
                if (((DefaultMutableTreeNode) n).getUserObject() instanceof RenamableSourceAndConverter) {
                    if (((RenamableSourceAndConverter)((DefaultMutableTreeNode) n).getUserObject()).sac.equals(sac)) {
                        model.removeNodeFromParent(((DefaultMutableTreeNode) n));
                    }
                } else {
                    visitAllNodesAndDelete(n, sac);
                }
            }
        }
    }

    public SourceAndConverter[] getSelectedSourceAndConverters() {
        Set<SourceAndConverter> sacList = new HashSet<>(); // A set avoids duplicate SourceAndConverter
        for (TreePath tp : tree.getSelectionModel().getSelectionPaths()) {
            if (((DefaultMutableTreeNode) tp.getLastPathComponent()).getUserObject() instanceof RenamableSourceAndConverter) {
                Object userObj = ((RenamableSourceAndConverter) ((DefaultMutableTreeNode) tp.getLastPathComponent()).getUserObject()).sac;
                sacList.add((SourceAndConverter) userObj);
            } else {
                sacList.addAll(getSourceAndConvertersFromChildrenOf((DefaultMutableTreeNode) tp.getLastPathComponent()));
            }
        }
        return sacList.toArray(new SourceAndConverter[sacList.size()]);
    }

    private Set<SourceAndConverter> getSourceAndConvertersFromChildrenOf(DefaultMutableTreeNode node) {
        Set<SourceAndConverter> sacs = new HashSet<>();
        for (int i=0;i<node.getChildCount();i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            if (child.getUserObject() instanceof RenamableSourceAndConverter) {
                Object userObj = ((RenamableSourceAndConverter) (child.getUserObject())).sac;
                sacs.add((SourceAndConverter) userObj);
            } else {
                sacs.addAll(getSourceAndConvertersFromChildrenOf(child));
            }
        }
        return sacs;
    }

    public TreeModel getTreeModel() {
        return model;
    }

    // --------------------- INNER CLASSES

    /**
     * Wraps a SourceAndConverter and allow to change its name
     */
    public class RenamableSourceAndConverter {
        public SourceAndConverter sac;
        public RenamableSourceAndConverter(SourceAndConverter sac) {
            this.sac = sac;
        }
        public String toString() {
            return sac.getSpimSource().getName();
        }
    }

    /**
     * SourceAndConverter filter node : generic
     */
    public class SourceFilterNode extends DefaultMutableTreeNode {
        Predicate<SourceAndConverter> filter;
        boolean allowDuplicate;
        String name;

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

    /**
     * SourceAndConverter filter node : Selects SpimData and allow for duplicate
     */
    public class SpimDataFilterNode extends SourceFilterNode {

        public AbstractSpimData asd;

        public boolean filter(SourceAndConverter sac) {
            Map<String, Object> props = sourceAndConverterService.getSacToMetadata().get(sac);
            assert props!=null;
            //System.out.println("Testing "+sac.getSpimSource().getName()+" vs "+asd.toString());
            //assert props.get(SPIM_DATA) instanceof Set<BdvSourceAndConverterService.SpimDataInfo>;
            return (props.containsKey( SPIM_DATA_INFO ))&&(( SourceAndConverterService.SpimDataInfo)props.get( SPIM_DATA_INFO )).asd.equals(asd);
        }

        public SpimDataFilterNode(String name, AbstractSpimData spimdata) {
            super(name,null, true);
            this.filter = this::filter;
            asd = spimdata;
        }

        public String toString() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * SourceAndConverter filter node : Selected a SourceAndConverter which is linked
     * to a particular Entity
     */
    public class SpimDataElementFilter extends SourceFilterNode {

        Entity e;

        public SpimDataElementFilter(String name, Entity e) {
            super(name, null, false);
            this.filter = this::filter;
            this.e = e;
        }

        public boolean filter(SourceAndConverter sac) {
            Map<String, Object> props = sourceAndConverterService.getSacToMetadata().get(sac);
            assert props!=null;
            assert props.containsKey( SPIM_DATA_INFO );

            AbstractSpimData<AbstractSequenceDescription<BasicViewSetup,?,?>> asd = ( AbstractSpimData<AbstractSequenceDescription<BasicViewSetup,?,?>>) (( SourceAndConverterService.SpimDataInfo)props.get( SPIM_DATA_INFO )).asd;
            Integer idx = (( SourceAndConverterService.SpimDataInfo)props.get( SPIM_DATA_INFO )).setupId;

            return asd.getSequenceDescription().getViewSetups().get(idx).getAttributes().values().contains(e);
        }

    }

}
