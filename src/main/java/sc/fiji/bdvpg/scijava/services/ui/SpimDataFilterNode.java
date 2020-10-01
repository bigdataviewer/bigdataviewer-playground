package sc.fiji.bdvpg.scijava.services.ui;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

import javax.swing.tree.DefaultTreeModel;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_INFO;

/**
 * SourceAndConverter filter node : Selects SpimData and allow for duplicate
 */
public class SpimDataFilterNode extends SourceFilterNode {

    final AbstractSpimData asd;
    final SourceAndConverterService sourceAndConverterService;

    public boolean filter(SourceAndConverter sac) {
        return (sourceAndConverterService.containsMetadata(sac, SPIM_DATA_INFO ))&&(( SourceAndConverterService.SpimDataInfo)sourceAndConverterService.getMetadata(sac, SPIM_DATA_INFO)).asd.equals(asd);
    }

    public SpimDataFilterNode(DefaultTreeModel model, String defaultName, AbstractSpimData spimdata, SourceAndConverterService sourceAndConverterService) {
        super(model, defaultName,null, false);
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

    @Override
    public Object clone() {
        return new SpimDataFilterNode(model, name, asd, sourceAndConverterService);
    }
}