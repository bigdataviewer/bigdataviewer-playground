package sc.fiji.bdvpg.spimdata.importer;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.io.File;
import java.util.function.Function;

public class SpimDataFromXmlImporter implements Runnable, Function<File, AbstractSpimData> {

    File file;

    public SpimDataFromXmlImporter( File file) {
        this.file = file;
    }

    public SpimDataFromXmlImporter( String filePath) {
        this.file = new File(filePath);
    }

    @Override
    public void run() {
        apply(file);
    }

    public AbstractSpimData get() {
        return apply(file);
    }

    @Override
    public AbstractSpimData apply(File file) {
        AbstractSpimData sd = null;
        try {
            sd = new XmlIoSpimData().load(file.getAbsolutePath());
            SourceAndConverterServices.getSourceAndConverterService().register(sd);
        } catch (SpimDataException e) {
            e.printStackTrace();
        }
        return sd;
    }


}
