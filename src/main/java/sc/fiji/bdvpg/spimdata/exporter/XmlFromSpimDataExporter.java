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

public class XmlFromSpimDataExporter implements Runnable {

    AbstractSpimData spimData;

    String filePath;

    public XmlFromSpimDataExporter ( AbstractSpimData spimData, String filePath) {
        this.spimData = spimData;
        spimData.setBasePath(new File(filePath));
        this.filePath = filePath;
    }

    @Override
    public void run() {
        try {
            // Loops through all sources in order to push display settings
            SourceAndConverterServices
                    .getSourceAndConverterService()
                    .getSourceAndConverterFromSpimdata(spimData).forEach(sac -> Displaysettings.PushDisplaySettingsFromCurrentConverter(sac));

            if (spimData instanceof SpimData) {
                (new XmlIoSpimData()).save((SpimData) spimData, filePath);
                SourceAndConverterServices.getSourceAndConverterService().setSpimDataName(spimData, filePath);
            } else if (spimData instanceof SpimDataMinimal) {
                (new XmlIoSpimDataMinimal()).save((SpimDataMinimal) spimData, filePath);
                SourceAndConverterServices.getSourceAndConverterService().setSpimDataName(spimData, filePath);
            } else {
                System.err.println("Cannot save SpimData of class : "+spimData.getClass().getSimpleName());
            }

        } catch (SpimDataException e) {
            e.printStackTrace();
        }
    }

}
