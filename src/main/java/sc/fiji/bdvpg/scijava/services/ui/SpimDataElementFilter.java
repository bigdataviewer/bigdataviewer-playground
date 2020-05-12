package sc.fiji.bdvpg.scijava.services.ui;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

import java.util.Map;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_INFO;

/**
 * SourceAndConverter filter node : Selected a SourceAndConverter which is linked
 * to a particular Entity
 */
public class SpimDataElementFilter extends SourceFilterNode {

    final Entity e;
    final SourceAndConverterService sourceAndConverterService;

    public SpimDataElementFilter(String name, Entity e, SourceAndConverterService sourceAndConverterService) {
        super(name, null, true);
        this.filter = this::filter;
        this.e = e;
        this.sourceAndConverterService = sourceAndConverterService;
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
