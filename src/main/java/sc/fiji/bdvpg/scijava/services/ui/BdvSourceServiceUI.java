package sc.fiji.bdvpg.scijava.services.ui;

import bdv.img.WarpedSource;
import bdv.tools.transformation.TransformedSource;
import bdv.util.ResampledSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import sc.fiji.bdvpg.scijava.services.BdvSourceAndConverterService;
import sc.fiji.bdvpg.services.BdvService;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static sc.fiji.bdvpg.scijava.services.BdvSourceAndConverterService.SPIMDATAINFO;

public class BdvSourceServiceUI {

    BdvSourceAndConverterService bss;
    JFrame frame;
    JPanel panel;
    /**
     * Swing JTree used for displaying Sources object
     */
    JTree tree;
    SourceFilterNode top;
    JScrollPane treeView;
    DefaultTreeModel model;
    Set<SourceAndConverter> displayedSource = new HashSet<>();

    JPopupMenu popup = new JPopupMenu();
    SourceFilterNode allSourcesNode;

    public BdvSourceServiceUI(BdvSourceAndConverterService bss) {
        this.bss = bss;

        frame = new JFrame("Bdv Sources");
        panel = new JPanel(new BorderLayout());

        // Tree view of Spimdata
        top = new SourceFilterNode("Sources", (sac) -> true, true);
        tree = new JTree(top);
        //tree.setRootVisible(false);
        allSourcesNode = new SourceFilterNode("All Sources", (sac) -> true, false);
        top.add(allSourcesNode);

        SourceFilterNode spimDataSources = new SourceFilterNode("In SpimData", (sac) -> bss.getAttachedSourceAndConverterData().get(sac).containsKey(SPIMDATAINFO), false);
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
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
                // Double Click : display source, if possible
                /*if (e.getClickCount()==2 && !e.isConsumed()) {
                    if (BdvService.getSourceDisplayService()!=null) {
                        for (SourceAndConverter sac: getSelectedSourceAndConverters()) {
                            BdvService.getSourceDisplayService().show(sac);
                        }
                    }
                }*/
            }
        });

        this.addPopupAction(this::inspectSources, "Inspect Sources");

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
        popup.add(menuItem);

        frame.add(panel);
        frame.pack();
        frame.setVisible( false );
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
            if (getSourceAndConvertersOfSource(source.getWrappedSource()).size()>0) {
                // at least A sourceandconverteralready exists for this source
                getSourceAndConvertersOfSource(source.getWrappedSource()).forEach((src) -> {
                            DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                            nodeTransformedSource.add(wrappedSourceNode);
                            appendInspectorResult(wrappedSourceNode, src);
                        }
                );
            } else {
                // no source and converter exist for this source : creates it
                SourceAndConverter src = SourceAndConverterUtils.createSourceAndConverter(source.getWrappedSource());
                BdvService.getSourceService().register(src);
                DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                nodeTransformedSource.add(wrappedSourceNode);
                appendInspectorResult(wrappedSourceNode, src);
            }
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
            if (getSourceAndConvertersOfSource(source.getWrappedSource()).size()>0) {
                // at least A sourceandconverteralready exists for this source
                getSourceAndConvertersOfSource(source.getWrappedSource()).forEach((src) -> {
                            DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                            nodeWarpedSource.add(wrappedSourceNode);
                            appendInspectorResult(wrappedSourceNode, src);
                        }
                );
            } else {
                // no source and converter exist for this source : creates it
                SourceAndConverter src = SourceAndConverterUtils.createSourceAndConverter(source.getWrappedSource());
                BdvService.getSourceService().register(src);
                DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                nodeWarpedSource.add(wrappedSourceNode);
                appendInspectorResult(wrappedSourceNode, src);
            }
        }

        if (sac.getSpimSource() instanceof ResampledSource) {
            DefaultMutableTreeNode nodeResampledSource = new DefaultMutableTreeNode("Resampled Source");
            parent.add(nodeResampledSource);
            ResampledSource source = (ResampledSource) sac.getSpimSource();

            DefaultMutableTreeNode nodeOrigin = new DefaultMutableTreeNode("Origin");
            nodeResampledSource.add(nodeOrigin);

            if (getSourceAndConvertersOfSource(source.getOriginalSource()).size()>0) {
                // at least A sourceandconverteralready exists for this source
                getSourceAndConvertersOfSource(source.getOriginalSource()).forEach((src) -> {
                            DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                            nodeOrigin.add(wrappedSourceNode);
                            appendInspectorResult(wrappedSourceNode, src);
                        }
                );
            } else {
                // no source and converter exist for this source : creates it
                SourceAndConverter src = SourceAndConverterUtils.createSourceAndConverter(source.getOriginalSource());
                BdvService.getSourceService().register(src);
                DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                nodeOrigin.add(wrappedSourceNode);
                appendInspectorResult(wrappedSourceNode, src);
            }

            DefaultMutableTreeNode nodeResampler = new DefaultMutableTreeNode("Sampler Model");
            nodeResampledSource.add(nodeResampler);

            if (getSourceAndConvertersOfSource(source.getModelResamplerSource()).size()>0) {
                // at least A sourceandconverteralready exists for this source
                getSourceAndConvertersOfSource(source.getModelResamplerSource()).forEach((src) -> {
                            DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                            nodeResampler.add(wrappedSourceNode);
                            appendInspectorResult(wrappedSourceNode, src);
                        }
                );
            } else {
                // no source and converter exist for this source : creates it
                SourceAndConverter src = SourceAndConverterUtils.createSourceAndConverter(source.getModelResamplerSource());
                BdvService.getSourceService().register(src);
                DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                nodeResampler.add(wrappedSourceNode);
                appendInspectorResult(wrappedSourceNode, src);
            }

        }
    }

    public List<SourceAndConverter> getSourceAndConvertersOfSource(Source src) {
        return BdvService.getSourceService().getSources().stream().filter(sac -> sac.getSpimSource().equals(src)).collect(Collectors.toList());
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

    List<SpimDataFilterNode> spimdataFilterNodes = new ArrayList<>();

    private void updateSpimDataFilterNodes() {
        // Fetch All Spimdatas from all Sources
        Set<AbstractSpimData> currentSpimdatas = new HashSet<>();
        displayedSource.forEach(sac -> {
            if (bss.getAttachedSourceAndConverterData().get(sac).containsKey(SPIMDATAINFO)) {
                currentSpimdatas.add(((BdvSourceAndConverterService.SpimDataInfo)bss.getAttachedSourceAndConverterData().get(sac).get(SPIMDATAINFO)).asd);
            }
        });

        // Check for obsolete spimdatafilternodes
        spimdataFilterNodes.forEach(fnode -> {
            if (!currentSpimdatas.contains(fnode.asd)) {
                 model.removeNodeFromParent(fnode);
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
        //if (asd instanceof SpimDataMinimal) {
        //    SpimDataMinimal sdm = (SpimDataMinimal) asd;
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


        //} else {
        //    System.out.println("Cannot sort by entities with spimdata of class "+asd.getClass().getSimpleName());
        //}

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

    public void addPopupAction(Consumer<SourceAndConverter[]> action, String actionName) {
        // Show
        JMenuItem menuItem = new JMenuItem(actionName);
        menuItem.addActionListener(e -> action.accept(getSelectedSourceAndConverters()));
        popup.add(menuItem);
    }

    public void addPopupLine() {
        popup.addSeparator();
    }

    public class RenamableSourceAndConverter {
        public SourceAndConverter sac;
        public RenamableSourceAndConverter(SourceAndConverter sac) {
            this.sac = sac;
        }
        public String toString() {
            return sac.getSpimSource().getName();
        }
    }

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

    public class SpimDataFilterNode extends SourceFilterNode {

        public AbstractSpimData asd;

        public boolean filter(SourceAndConverter sac) {
            Map<String, Object> props = bss.getAttachedSourceAndConverterData().get(sac);
            assert props!=null;
            //System.out.println("Testing "+sac.getSpimSource().getName()+" vs "+asd.toString());
            //assert props.get(SPIMDATAINFO) instanceof Set<BdvSourceAndConverterService.SpimDataInfo>;
            return (props.containsKey(SPIMDATAINFO))&&((BdvSourceAndConverterService.SpimDataInfo)props.get(SPIMDATAINFO)).asd.equals(asd);
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

    public class SpimDataElementFilter extends SourceFilterNode {

        Entity e;

        public SpimDataElementFilter(String name, Entity e) {
            super(name, null, false);
            this.filter = this::filter;
            this.e = e;
        }

        public boolean filter(SourceAndConverter sac) {
            Map<String, Object> props = bss.getAttachedSourceAndConverterData().get(sac);
            assert props!=null;
            assert props.containsKey(SPIMDATAINFO);
            //System.out.println("Testing "+sac.getSpimSource().getName()+" vs "+asd.toString());
            //assert props.get(SPIMDATAINFO) instanceof Set<BdvSourceAndConverterService.SpimDataInfo>;

            AbstractSpimData<AbstractSequenceDescription<BasicViewSetup,?,?>> asd = ( AbstractSpimData<AbstractSequenceDescription<BasicViewSetup,?,?>>) ((BdvSourceAndConverterService.SpimDataInfo)props.get(SPIMDATAINFO)).asd;
            Integer idx = ((BdvSourceAndConverterService.SpimDataInfo)props.get(SPIMDATAINFO)).setupId;

            return asd.getSequenceDescription().getViewSetups().get(idx).getAttributes().values().contains(e);
        }

    }

}
