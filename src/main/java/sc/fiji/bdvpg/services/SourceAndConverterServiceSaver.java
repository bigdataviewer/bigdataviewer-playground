package sc.fiji.bdvpg.services;

import bdv.SpimSource;
import bdv.img.WarpedSource;
import bdv.tools.transformation.TransformedSource;
import bdv.util.ResampledSource;
import bdv.viewer.SourceAndConverter;
import com.google.gson.*;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.display.ColorConverter;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static sc.fiji.bdvpg.services.ISourceAndConverterService.SPIM_DATA_INFO;
import static sc.fiji.bdvpg.services.ISourceAndConverterService.SPIM_DATA_LOCATION;

/** Big Objective : save the state of all open sources
 * By Using Gson and specific serialization depending on SourceAndConverter classes
 *
 * TODO : take care of sources not built with SourceAndConverter
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

            idToSac.clear();
            sacToId.clear();
            sourceToId.clear();
            idToSource.clear();
            sourceToId = null;
            idToSource = null;
            idToSac = null;
            sacToId = null;
        }
        //} catch (IOException e) {
        //    e.printStackTrace();
        //}

        /*System.out.println(
            gson.toJson(SourceAndConverterServices.getSourceAndConverterService().getSpimDatasets())
        );*/

        //gson.toJson()

        /*sacs.forEach(sac -> {
            System.out.println("Export of sac "+sac.getSpimSource().getName());
            // Analysis

            SourceAndConverter rootSac = SourceAndConverterInspector.getRootSourceAndConverter(sac);

            if (rootSac==sac) {
                System.out.println("Sac is NOT derived from another source");
            } else {
                System.out.println("Sac IS derived from another source");
            }

            System.out.println(gson.toJson(sac));
            //new Gson().toJson(sac);

        });*/


    }


    public static void main(String... args) {
        /* this fails
        Gson gson = new Gson();
        TestObject to1 = new TestObject("My Test Object");
        TestObject to2 = new TestObject("The child");
        to1.child = to2;
        to2.child = to1;
        System.out.println(gson.toJson(to1));
         */
    }

    public static class TestObject {
        public String name;
        public TestObject child;

        public TestObject(String name){
            this.name = name;
        }
    }
}
