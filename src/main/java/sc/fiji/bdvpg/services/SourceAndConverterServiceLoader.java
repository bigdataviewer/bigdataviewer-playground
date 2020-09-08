package sc.fiji.bdvpg.services;

import bdv.viewer.SourceAndConverter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;

public class SourceAndConverterServiceLoader extends SourceAndConverterSerializer implements Runnable{

    String filePath;

    public SourceAndConverterServiceLoader(String filePath) {
        this.filePath = filePath;
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
            FileReader fileReader = new FileReader(filePath);

            Gson gson = new Gson();
            JsonArray rawSacsArray = gson.fromJson(fileReader, JsonArray.class);
            System.out.println(rawSacsArray.size());

            for (int i = 0;i<rawSacsArray.size();i++) {
                idToJsonElement.put(rawSacsArray.get(i).getAsJsonObject().get("source_id").getAsInt(),rawSacsArray.get(i));
            }

            SourceAndConverter[] sacs_loaded = getGson().fromJson(rawSacsArray, SourceAndConverter[].class);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
