package sc.fiji.bdvpg.spimdata.importer;

import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;

import java.io.File;
import java.util.function.Function;

public class SpimDataImporterXML implements Runnable, Function<File, AbstractSpimData> {

    AbstractSpimData spimData;

    File f;

    public SpimDataImporterXML(File f) {
        this.f = f;
    }

    public SpimDataImporterXML(String filePath) {
        this.f = new File(filePath);
    }

    @Override
    public void run() {
        // Unnecessary
    }

    public AbstractSpimData get() {
        return apply(f);
    }

    @Override
    public AbstractSpimData apply(File file) {
        AbstractSpimData sd = null;
        try {
            sd = new XmlIoSpimDataMinimal().load(file.getAbsolutePath());
        } catch (SpimDataException e) {
            e.printStackTrace();
        }
        return sd;
    }


}
