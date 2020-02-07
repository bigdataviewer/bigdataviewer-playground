package sc.fiji.bdvpg.spimdata.exporter;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.ViewSetup;
import net.imglib2.display.ColorConverter;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import spimdata.util.DisplaySettings;

public class XmlFromSpimDataExporter implements Runnable {

    AbstractSpimData spimData;

    String filePath;

    public XmlFromSpimDataExporter ( AbstractSpimData spimData, String filePath) {
        this.spimData = spimData;
        this.filePath = filePath;
    }

    @Override
    public void run() {
        try {
            // Loops through all sources in order to push display settings
            SourceAndConverterServices
                    .getSourceAndConverterService()
                    .getSourceAndConverterFromSpimdata(spimData).forEach(sac -> DisplaySettings.PushDisplaySettings(sac));

            (new XmlIoSpimData()).save((SpimData) spimData, filePath);
        } catch (SpimDataException e) {
            e.printStackTrace();
        }
    }

}
