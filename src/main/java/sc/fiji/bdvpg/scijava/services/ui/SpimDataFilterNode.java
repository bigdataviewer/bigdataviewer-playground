package sc.fiji.bdvpg.scijava.services.ui;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

import java.util.Map;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_INFO;

/**
 * SourceAndConverter filter node : Selects SpimData and allow for duplicate
 */
public class SpimDataFilterNode extends SourceFilterNode {

    final AbstractSpimData asd;
    final SourceAndConverterService sourceAndConverterService;

    public boolean filter(SourceAndConverter sac) {
        //Map<String, Object> props = sourceAndConverterService.getSacToMetadata().get(sac);
        //assert props!=null;
        return (sourceAndConverterService.containsMetadata(sac, SPIM_DATA_INFO ))&&(( SourceAndConverterService.SpimDataInfo)sourceAndConverterService.getMetadata(sac, SPIM_DATA_INFO)).asd.equals(asd);
    }

    public SpimDataFilterNode(String defaultName, AbstractSpimData spimdata, SourceAndConverterService sourceAndConverterService) {
        super(defaultName,null, false);
        this.sourceAndConverterService = sourceAndConverterService;
        this.filter = this::filter;
        asd = spimdata;
    }

    String getName(AbstractSpimData spimdata, String defaultName) {
        return defaultName;
    }

    public String toString() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}