package sc.fiji.bdvpg.bdv.source.append;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.viewer.Source;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.generic.AbstractSpimData;

import java.util.function.Consumer;

public class AddSpimdataToBdv implements Runnable, Consumer<AbstractSpimData> {

    AbstractSpimData spimDataIn;
    BdvHandle bdvh;

    public AddSpimdataToBdv(BdvHandle bdvh, AbstractSpimData spimData) {
        this.spimDataIn=spimData;
        this.bdvh=bdvh;
    }

    public void run() {
        accept(spimDataIn);
    }

    @Override
    public void accept(AbstractSpimData spimData) {
        BdvFunctions.show(spimData, BdvOptions.options().addTo(bdvh));
    }
}
