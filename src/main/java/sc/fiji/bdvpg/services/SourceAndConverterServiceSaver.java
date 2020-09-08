package sc.fiji.bdvpg.services;

import bdv.viewer.SourceAndConverter;
import com.google.gson.*;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;

/** Big Objective : save the state of all open sources
 * By Using Gson and specific serialization depending on SourceAndConverter classes
 *
 * TODO : take care of sources not built with SourceAndConverter
 *
 * TODO : BUG do not work if the same spimdata is opened several times!
 *
 */

public class SourceAndConverterServiceSaver extends SourceAndConverterSerializer implements Runnable {

    File f;

    public SourceAndConverterServiceSaver(File f) {
        this.f = f;
        idToSac = new HashMap<>();
        sacToId = new HashMap<>();
        sourceToId = new HashMap<>();
        idToSource = new HashMap<>();
    }

    @Override
    public void run() {
        synchronized (SourceAndConverterServiceSaver.class) {
            List<SourceAndConverter> sacs = SourceAndConverterServices
                    .getSourceAndConverterService()
                    .getSourceAndConverters();

            for (int i = 0; i < sacs.size(); i++) {
                idToSac.put(i, sacs.get(i));
                sacToId.put(sacs.get(i), i);
                idToSource.put(i, sacs.get(i).getSpimSource());
                sourceToId.put(sacs.get(i).getSpimSource(), i);
            }

            Gson gson = getGson();

            try {
                FileWriter writer = new FileWriter(f.getAbsolutePath());
                gson.toJson(sacs, writer);
                writer.flush();
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
