package sc.fiji.bdvpg.services;

import bdv.viewer.SourceAndConverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;

public class SourceAndConverterServiceLoader extends SourceAndConverterSerializer implements Runnable{

    File f;

    public SourceAndConverterServiceLoader(File f) {
        this.f = f;
        idToSac = new HashMap<>();
        sacToId = new HashMap<>();
        sourceToId = new HashMap<>();
        idToSource = new HashMap<>();
    }

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

            SourceAndConverter[] sacs_loaded = getGson().fromJson(new FileReader("src/test/resources/bdvplaygroundstate.json"), SourceAndConverter[].class);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
