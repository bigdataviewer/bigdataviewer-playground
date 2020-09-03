package sc.fiji.bdvpg.services;

import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Map;

public class SourceAndConverterServiceLoader implements Runnable{

    File f;

    public SourceAndConverterServiceLoader(File f) {
        this.f = f;
    }


    static Map<Integer, SourceAndConverter> sacsId;

    @Override
    public void run() {

        // Empty service
        SourceAndConverter[] sacs =
                SourceAndConverterServices
                        .getSourceAndConverterService()
                        .getSourceAndConverters().toArray(new SourceAndConverter[0]);

        SourceAndConverterServices
                .getSourceAndConverterService()
                .remove(sacs);

        try {

            SourceAndConverter[] sacs_loaded = SourceAndConverterServiceSaver.getGson().fromJson(new FileReader("src/test/resources/bdvplaygroundstate.json"), SourceAndConverter[].class);

            System.out.println(sacs_loaded.length);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
