package sc.fiji.bdvpg.services.serializers;

import com.google.gson.*;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.services.SourceAndConverterSerializer;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

import static sc.fiji.bdvpg.services.ISourceAndConverterService.SPIM_DATA_LOCATION;

public class AbstractSpimdataAdapter implements JsonSerializer<AbstractSpimData>,
        JsonDeserializer<AbstractSpimData> {

    SourceAndConverterSerializer sacSerializer;

    public AbstractSpimdataAdapter(SourceAndConverterSerializer sacSerializer) {
        this.sacSerializer = sacSerializer;
    }

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
