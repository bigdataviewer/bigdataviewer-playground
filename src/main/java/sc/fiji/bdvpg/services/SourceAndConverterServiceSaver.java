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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static sc.fiji.bdvpg.services.ISourceAndConverterService.SPIM_DATA_INFO;
import static sc.fiji.bdvpg.services.ISourceAndConverterService.SPIM_DATA_LOCATION;

/** Big Objective : save the state of all open sources
 * By Using Gson and specific serialization depending on SourceAndConverter classes
 */
public class SourceAndConverterServiceSaver implements Runnable {

    File f;

    public SourceAndConverterServiceSaver(File f) {
        this.f = f;
    }

    static public Gson getGson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                //.registerTypeHierarchyAdapter(Source.class, new SourceAdapter())
                //.registerTypeHierarchyAdapter(SpimSource.class, new SpimSourceAdapter())
                //.registerTypeAdapter(AbstractSpimData.class, new SpimdataAdapter())
                //.registerTypeAdapter(SpimDataMinimal.class, new SpimdataAdapter())
                .registerTypeHierarchyAdapter(ColorConverter.class, new ColorConverterAdapter())
                .registerTypeHierarchyAdapter(SourceAndConverter.class, new SourceAndConverterAdapter())
                .registerTypeHierarchyAdapter(AbstractSpimData.class, new SpimdataAdapter())
                .create();
    }

    // for sources within the service
    static Map<Integer, SourceAndConverter> IdToSac;
    static Map<SourceAndConverter, Integer> SacToId;

    // for sources hidden (partial part of a computation)

    @Override
    public void run() {
        List<SourceAndConverter> sacs = SourceAndConverterServices
                .getSourceAndConverterService()
                .getSourceAndConverters();

        IdToSac = new HashMap<>();
        SacToId = new HashMap<>();

        for (int i=0;i<sacs.size();i++) {
            IdToSac.put(i,sacs.get(i));
            SacToId.put(sacs.get(i),i);
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

        IdToSac.clear();
        SacToId.clear();
        IdToSac = null;
        SacToId = null;

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

    public static class SourceAndConverterAdapter implements JsonSerializer<SourceAndConverter>,
            JsonDeserializer<SourceAndConverter>{

        @Override
        public JsonElement serialize(SourceAndConverter sourceAndConverter,
                                     Type type,
                                     JsonSerializationContext jsonSerializationContext) {
            JsonObject obj = new JsonObject();
            obj.addProperty("source name", sourceAndConverter.getSpimSource().getName());
            obj.addProperty("source class", sourceAndConverter.getSpimSource().getClass().getSimpleName());
            obj.addProperty("converter class", sourceAndConverter.getConverter().getClass().toString());
            obj.add("converter", jsonSerializationContext.serialize(sourceAndConverter.getConverter()));

            /*if ()
            SourceAndConverterServices
                    .getSourceAndConverterService()

                    .getSourceAndConverterDisplayService().getConverterSetup(sourceAndConverter);

            obj.add("convertersetup", );*/

            obj.addProperty("source id", String.valueOf(IdToSac.get(sourceAndConverter)));
            //obj.add("source", jsonSerializationContext.serialize(sourceAndConverter.getSpimSource()));

            if (sourceAndConverter.getSpimSource() instanceof SpimSource) {
                obj.add("sac", SpimSourceAndConverterAdapter.serialize(sourceAndConverter, SourceAndConverter.class, jsonSerializationContext));
            }
            if (sourceAndConverter.getSpimSource() instanceof TransformedSource) {
                throw new UnsupportedOperationException();
            }
            if (sourceAndConverter.getSpimSource() instanceof ResampledSource) {
                throw new UnsupportedOperationException();
            }
            if (sourceAndConverter.getSpimSource() instanceof WarpedSource) {
                throw new UnsupportedOperationException();
            }

            return obj;
        }

        @Override
        public SourceAndConverter deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            String sourceClass = jsonObject.getAsJsonPrimitive("source class").getAsString();

            if (sourceClass.equals("class "+SpimSource.class.getName())) {
                System.out.println("SpimSource!");
                return SpimSourceAndConverterAdapter.deserialize(jsonObject.get("sac"), SourceAndConverter.class, jsonDeserializationContext);
            }


            return null;
        }
    }

    public static class SpimSourceAndConverterAdapter {

        static public JsonElement serialize(SourceAndConverter sac, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject obj = new JsonObject();

            SourceAndConverterService.SpimDataInfo sdi =
                    (SourceAndConverterService.SpimDataInfo) SourceAndConverterServices
                    .getSourceAndConverterService()
                    .getMetadata(sac, SPIM_DATA_INFO);

            obj.add("spimdata", jsonSerializationContext.serialize(sdi.asd));
            obj.addProperty("viewsetup", sdi.setupId);

            return obj;
        }

        static public SourceAndConverter deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject obj = jsonElement.getAsJsonObject();
            System.out.println("Deserialize SpimSourceAndConverterAdapter");

            System.out.println("0");
            AbstractSpimData asd =
                    jsonDeserializationContext.deserialize(obj.get("spimdata"), AbstractSpimData.class);

            System.out.println("1");

            int setupId = obj.getAsJsonPrimitive("viewsetup").getAsInt();

            System.out.println("setupId = "+setupId);

            final ISourceAndConverterService sacservice =  SourceAndConverterServices
                    .getSourceAndConverterService();

            Optional<SourceAndConverter> futureSac = sacservice.getSourceAndConverters()
                    .stream()
                    .filter(sac -> sacservice.containsMetadata(sac, SPIM_DATA_INFO))
                    .filter(sac -> {
                        SourceAndConverterService.SpimDataInfo sdi = (SourceAndConverterService.SpimDataInfo) sacservice.getMetadata(sac,SPIM_DATA_INFO);
                        return sdi.asd.equals(asd)&&sdi.setupId ==setupId;
                    }).findFirst();

            if (futureSac.isPresent()) {
                return futureSac.get();
            } else {
                return null;
            }
        }
    }

    public static class SpimdataAdapter implements JsonSerializer<AbstractSpimData>,
            JsonDeserializer<AbstractSpimData>{

        @Override
        public JsonElement serialize(AbstractSpimData asd,
                                     Type type,
                                     JsonSerializationContext jsonSerializationContext) {
            JsonObject obj = new JsonObject();
            String dataLocation = (String) SourceAndConverterServices
                    .getSourceAndConverterService()
                    .getMetadata(asd, SPIM_DATA_LOCATION );
            obj.addProperty("datalocation", dataLocation);
            return obj;
        }

        @Override
        public AbstractSpimData deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            String datalocation = jsonElement.getAsJsonObject().get("datalocation").getAsString();
            System.out.println("Deserialization of "+datalocation);
            if (datalocation.endsWith(".qpath")) {
                System.err.println("qpath project unhandled in deserialization!");
            }
            List<AbstractSpimData> asds =
            SourceAndConverterServices
                    .getSourceAndConverterService()
                    .getSpimDatasets()
                    .stream()
                    .filter(asd ->
                            SourceAndConverterServices
                                    .getSourceAndConverterService()
                                    .getMetadata(asd, SPIM_DATA_LOCATION)
                            .equals(datalocation)).collect(Collectors.toList());

            // SpimData not found
            if (asds.size()==0) {
                return new SpimDataFromXmlImporter(datalocation).get();
            } else if (asds.size()==1) {
                return asds.get(0);
            } else {
                System.out.println("Warning : multiple spimdata with identical datalocation already in memory");
                return asds.get(0);
            }
        }
    }

    private static class ColorConverterAdapter implements JsonSerializer<ColorConverter>{
        @Override
        public JsonElement serialize(ColorConverter colorConverter, Type type, JsonSerializationContext jsonSerializationContext) {
            System.out.println("Serializing colorconverter");
            JsonObject obj = new JsonObject();
            obj.add("color", jsonSerializationContext.serialize(colorConverter.getColor().get()));
            return obj;
        }
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
