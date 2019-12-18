package sc.fiji.bdvpg.spimdata.importer;

import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;

import java.io.File;
import java.util.function.Function;

public class SpimDataImporterXML implements Runnable, Function<File, SpimData> {

    SpimData spimData;

    File f;

    public SpimDataImporterXML(File f) {
        this.f = f;
    }

    @Override
    public void run() {
        spimData = apply(f);
    }

    public SpimData get() {
        return spimData;
    }

    @Override
    public SpimData apply(File file) {
        SpimData sd = null;
        try {
            sd = new XmlIoSpimData().load(file.getAbsolutePath());
        } catch (SpimDataException e) {
            e.printStackTrace();
        }
        return sd;
    }


}
