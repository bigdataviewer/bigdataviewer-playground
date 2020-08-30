package sc.fiji.bdvpg.spimdata.importer;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.io.File;
import java.util.function.Function;
import java.util.regex.Pattern;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_LOCATION;

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
            String pattern = Pattern.quote(System.getProperty("file.separator"));
            String[] parts = dataLocation.split(pattern);
            if (parts.length>0) {
                if (parts[parts.length - 1]!=null) {
                    SourceAndConverterServices.getSourceAndConverterService().setSpimDataName(sd, parts[parts.length - 1]);
                } else {
                    System.err.println("Wrong parsing of spimdata name (not enough parts) : "+dataLocation);
                }
            } else {
                System.err.println("Wrong parsing of spimdata name (can't be splitted): "+dataLocation);
            }
            SourceAndConverterServices.getSourceAndConverterService().setMetadata(sd, SPIM_DATA_LOCATION, dataLocation);
        } catch (SpimDataException e) {
            e.printStackTrace();
        }
        return sd;
    }


}
