package sc.fiji.bdvpg.spimdata.importer;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.io.File;
import java.util.function.Function;

public class SpimDataFromXmlImporter implements Runnable, Function<String, AbstractSpimData> {

    String dataLocation;

    public SpimDataFromXmlImporter( File file ) {
        this.dataLocation = file.getAbsolutePath();
    }

    public SpimDataFromXmlImporter( String dataLocation) {
        this.dataLocation = dataLocation;
    }

    @Override
    public void run() {
        apply(dataLocation);
    }

    public AbstractSpimData get() {
        return apply(dataLocation);
    }

    @Override
    public AbstractSpimData apply(String dataLocation) {
        AbstractSpimData sd = null;
        try {
            sd = new XmlIoSpimData().load(dataLocation);
            SourceAndConverterServices.getSourceAndConverterService().register(sd);
        } catch (SpimDataException e) {
            e.printStackTrace();
        }
        return sd;
    }


}
