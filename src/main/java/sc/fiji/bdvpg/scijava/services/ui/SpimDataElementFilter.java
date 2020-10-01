package sc.fiji.bdvpg.scijava.services.ui;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

import javax.swing.tree.DefaultTreeModel;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_INFO;

/**
 * SourceAndConverter filter node : Selected a SourceAndConverter which is linked
 * to a particular Entity
 */
public class SpimDataElementFilter extends SourceFilterNode {

    final Entity e;
    final SourceAndConverterService sourceAndConverterService;

    public SpimDataElementFilter(DefaultTreeModel model, String name, Entity e, SourceAndConverterService sourceAndConverterService) {
        super(model, name, null, false);
        this.filter = this::filter;
        this.e = e;
        this.sourceAndConverterService = sourceAndConverterService;
    }

    public boolean filter(SourceAndConverter sac) {
        if (sourceAndConverterService.containsMetadata(sac, SPIM_DATA_INFO)) {
            AbstractSpimData<AbstractSequenceDescription<BasicViewSetup,?,?>> asd = ( AbstractSpimData<AbstractSequenceDescription<BasicViewSetup,?,?>>) (( SourceAndConverterService.SpimDataInfo)sourceAndConverterService.getMetadata(sac, SPIM_DATA_INFO)).asd;
            Integer idx = (( SourceAndConverterService.SpimDataInfo)sourceAndConverterService.getMetadata(sac, SPIM_DATA_INFO)).setupId;
            return asd.getSequenceDescription().getViewSetups().get(idx).getAttributes().values().contains(e);
        } else {
            return false;
        }
    }

    @Override
    public Object clone() {
        return new SpimDataElementFilter(model, name, e, sourceAndConverterService);
    }

}
