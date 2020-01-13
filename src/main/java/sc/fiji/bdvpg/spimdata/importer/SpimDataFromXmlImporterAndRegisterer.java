package sc.fiji.bdvpg.spimdata.importer;

import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.services.BdvService;

import java.io.File;
import java.util.function.Function;

public class SpimDataFromXmlImporterAndRegisterer implements Runnable, Function<File, AbstractSpimData> {

    AbstractSpimData spimData;

    File file;

    public SpimDataFromXmlImporterAndRegisterer( File file) {
        this.file = file;
    }

    public SpimDataFromXmlImporterAndRegisterer( String filePath) {
        this.file = new File(filePath);
    }

    @Override
    public void run() {
        apply( file );
    }

    public AbstractSpimData get() {
        return apply( file );
    }

    @Override
    public AbstractSpimData apply(File file) {
        AbstractSpimData sd = null;
        try {
            sd = new XmlIoSpimDataMinimal().load(file.getAbsolutePath());
            BdvService.getSourceAndConverterService().register(sd);
        } catch (SpimDataException e) {
            e.printStackTrace();
        }
        return sd;
    }


}
