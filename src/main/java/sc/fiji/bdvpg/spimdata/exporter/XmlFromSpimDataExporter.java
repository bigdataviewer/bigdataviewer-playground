package sc.fiji.bdvpg.spimdata.exporter;

import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import spimdata.util.DisplaySettings;

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
                    .getSourceAndConverterFromSpimdata(spimData).forEach(sac -> DisplaySettings.PushDisplaySettingsFromCurrentConverter(sac));

            (new XmlIoSpimData()).save((SpimData) spimData, filePath);
        } catch (SpimDataException e) {
            e.printStackTrace();
        }
    }

}
