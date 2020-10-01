package sc.fiji.bdvpg.services.serializers;

import bdv.viewer.SourceAndConverter;
import com.google.gson.*;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterSerializer;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.lang.reflect.Type;
import java.util.Optional;

import static sc.fiji.bdvpg.services.ISourceAndConverterService.SPIM_DATA_INFO;

class SpimSourceAndConverterAdapter {

    SourceAndConverterSerializer sacSerializer;

    public SpimSourceAndConverterAdapter(SourceAndConverterSerializer sacSerializer) {
        this.sacSerializer = sacSerializer;
    }

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
        AbstractSpimData asd = jsonDeserializationContext.deserialize(obj.get("spimdata"), AbstractSpimData.class);
        int setupId = obj.getAsJsonPrimitive("viewsetup").getAsInt();
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
            System.err.println("Couldn't deserialize spim source from json element "+jsonElement.getAsString());
            return null;
        }
    }
}
