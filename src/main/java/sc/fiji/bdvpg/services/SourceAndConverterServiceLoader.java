package sc.fiji.bdvpg.services;

import bdv.viewer.SourceAndConverter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import org.scijava.Context;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;

public class SourceAndConverterServiceLoader extends SourceAndConverterSerializer implements Runnable{

    String filePath;
    Context ctx;

    public SourceAndConverterServiceLoader(String filePath, Context ctx) {
        super(ctx);
        this.filePath = filePath;
        idToSac = new HashMap<>();
        sacToId = new HashMap<>();
        sourceToId = new HashMap<>();
        idToSource = new HashMap<>();
        this.ctx = ctx;
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
                if (rawSacsArray.get(i).isJsonObject()) {
                    idToJsonElement.put(rawSacsArray.get(i).getAsJsonObject().get("source_id").getAsInt(), rawSacsArray.get(i));
                } else {
                    // Source couldn't be serialized
                }
            }

            SourceAndConverter[] sacs_loaded = getGson().fromJson(rawSacsArray, SourceAndConverter[].class);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
