package sc.fiji.bdvpg.spimdata.exporter;

import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.io.File;
import java.util.function.Function;

public class XmlFromSpimDataExporter implements Runnable, Function<AbstractSpimData, File> {

    AbstractSpimData spimData;

    File file;

    public XmlFromSpimDataExporter ( AbstractSpimData spimData) {
        this.spimData = spimData;
    }

    @Override
    public void run() {
    }

    public File get() {
        return apply(spimData);
    }

    @Override
    public File apply(AbstractSpimData spimData) {
        /*AbstractSpimData sd = null;
        try {
            sd = new XmlIoSpimDataMinimal().load(file.getAbsolutePath());
            SourceAndConverterServices.getSourceAndConverterService().register(sd);
        } catch (SpimDataException e) {
            e.printStackTrace();
        }*/
        return null; //sd;
    }


}
