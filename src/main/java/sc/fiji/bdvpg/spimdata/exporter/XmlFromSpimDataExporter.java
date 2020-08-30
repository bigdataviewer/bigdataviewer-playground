package sc.fiji.bdvpg.spimdata.exporter;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import spimdata.util.Displaysettings;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_LOCATION;

public class XmlFromSpimDataExporter implements Runnable {

    AbstractSpimData spimData;

    String dataLocation;

    public static boolean isPathValid(String path) {
        try {
            Paths.get(path);
        } catch (InvalidPathException ex) {
            return false;
        }
        return true;
    }

    public XmlFromSpimDataExporter ( AbstractSpimData spimData, String dataLocation) {
        this.spimData = spimData;
        if (isPathValid(dataLocation)) {
            spimData.setBasePath(new File(dataLocation));
        } else {
            System.out.println("Trying to save spimdata into an invalid file Path : "+dataLocation);
        }
        this.dataLocation = dataLocation;
    }

    @Override
    public void run() {
        try {
            // Loops through all sources in order to push display settings
            SourceAndConverterServices
                    .getSourceAndConverterService()
                    .getSourceAndConverterFromSpimdata(spimData).forEach(sac -> Displaysettings.PushDisplaySettingsFromCurrentConverter(sac));

            if (spimData instanceof SpimData) {
                (new XmlIoSpimData()).save((SpimData) spimData, dataLocation);
            } else if (spimData instanceof SpimDataMinimal) {
                (new XmlIoSpimDataMinimal()).save((SpimDataMinimal) spimData, dataLocation);
            } else {
                System.err.println("Cannot save SpimData of class : "+spimData.getClass().getSimpleName());
                return;
            }

            SourceAndConverterServices.getSourceAndConverterService().setSpimDataName(spimData, dataLocation);
            SourceAndConverterServices.getSourceAndConverterService().setMetadata(spimData, SPIM_DATA_LOCATION, dataLocation);

        } catch (SpimDataException e) {
            e.printStackTrace();
        }
    }

}
