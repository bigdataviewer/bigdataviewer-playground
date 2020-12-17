/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2020 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package sc.fiji.bdvpg.services.serializers;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import com.google.gson.*;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.InstantiableException;
import sc.fiji.bdvpg.services.SourceAndConverterSerializer;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.services.serializers.plugins.BdvPlaygroundObjectAdapterService;
import sc.fiji.bdvpg.services.serializers.plugins.ISourceAdapter;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class SourceAndConverterAdapter implements JsonSerializer<SourceAndConverter>,
        JsonDeserializer<SourceAndConverter> {

    SourceAndConverterSerializer sacSerializer;

    Map<Class<? extends Source>, ISourceAdapter> sourceSerializers = new HashMap<>();
    Map<String, ISourceAdapter> sourceSerializersFromName = new HashMap<>();

    public SourceAndConverterAdapter(SourceAndConverterSerializer sacSerializer) {
        this.sacSerializer = sacSerializer;
        sacSerializer.getScijavaContext().getService(BdvPlaygroundObjectAdapterService.class)
                .getAdapters(ISourceAdapter.class)
                .forEach(pi -> {
                    try {
                        ISourceAdapter adapter = pi.createInstance();
                        adapter.setSacSerializer(sacSerializer);
                        System.out.println("adapter.getSourceClass()= "+adapter.getSourceClass());
                        sourceSerializers.put(adapter.getSourceClass(), adapter);
                        sourceSerializersFromName.put(adapter.getSourceClass().getName(), adapter);
                    } catch (InstantiableException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public JsonElement serialize(SourceAndConverter sourceAndConverter,
                                 Type type,
                                 JsonSerializationContext jsonSerializationContext) {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("source_name", sourceAndConverter.getSpimSource().getName());
            obj.addProperty("source_class", sourceAndConverter.getSpimSource().getClass().getName());
            obj.addProperty("converter_class", sourceAndConverter.getConverter().getClass().toString());
            obj.addProperty("source_id", sacSerializer.getSacToId().get(sourceAndConverter));

            if (sourceAndConverter.getConverter() instanceof ColorConverter) {
                ColorConverter colorConverter = (ColorConverter) sourceAndConverter.getConverter();
                obj.add("color", jsonSerializationContext.serialize(colorConverter.getColor().get()));
                double min = SourceAndConverterServices.getSourceAndConverterDisplayService()
                        .getConverterSetup(sourceAndConverter).getDisplayRangeMin();
                double max = SourceAndConverterServices.getSourceAndConverterDisplayService()
                        .getConverterSetup(sourceAndConverter).getDisplayRangeMax();
                obj.addProperty("converter_setup_min", min);
                obj.addProperty("converter_setup_max", max);
            }

            JsonElement element = serializeSubClass(sourceAndConverter, SourceAndConverter.class, jsonSerializationContext);
            obj.add("sac", element);

            return obj;
        } catch (UnsupportedOperationException e) {
            System.err.println("Could not serialize source "+ sourceAndConverter.getSpimSource().getName() + " of class "+ sourceAndConverter.getSpimSource().getClass().getName());
            return null;
        }
    }

    JsonElement serializeSubClass (SourceAndConverter sourceAndConverter,
                                          Type type,
                                          JsonSerializationContext jsonSerializationContext) throws UnsupportedOperationException {

        if (!sourceSerializers.containsKey(sourceAndConverter.getSpimSource().getClass())) {
            System.out.println("Unsupported serialisation of "+sourceAndConverter.getSpimSource().getClass());
            throw new UnsupportedOperationException();
        }

        return sourceSerializers.get(sourceAndConverter.getSpimSource().getClass())
                .serialize(sourceAndConverter, SourceAndConverter.class, jsonSerializationContext);
    }

    @Override
    public SourceAndConverter deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        String sourceClass = jsonObject.getAsJsonPrimitive("source_class").getAsString();

        if (!sourceSerializersFromName.containsKey(sourceClass)) {
            System.out.println("Unsupported deserialisation of "+sourceClass);
            throw new UnsupportedOperationException();
        }

        SourceAndConverter sac = sourceSerializersFromName.get(sourceClass)
                .deserialize(jsonObject.get("sac"), SourceAndConverter.class, jsonDeserializationContext);

        if (sac != null) {
            if (jsonObject.getAsJsonPrimitive("color")!=null) {
                // Now the color
                int color = jsonObject.getAsJsonPrimitive("color").getAsInt();
                new ColorChanger(sac,  new ARGBType(color)).run(); // TO deal with volatile and non volatile
                // Min Max display
                SourceAndConverterServices.getSourceAndConverterDisplayService()
                        .getConverterSetup(sac).setDisplayRange(
                                jsonObject.getAsJsonPrimitive("converter_setup_min").getAsDouble(),
                                jsonObject.getAsJsonPrimitive("converter_setup_max").getAsDouble());
            }

            // unique identifier
            int idSource = jsonObject.getAsJsonPrimitive("source_id").getAsInt();
            sacSerializer.getIdToSac().put(idSource, sac);
            sacSerializer.getSacToId().put(sac, idSource);
            sacSerializer.getSourceToId().put(sac.getSpimSource(), idSource);
            sacSerializer.getIdToSource().put(idSource, sac.getSpimSource());
            sacSerializer.alreadyDeSerializedSacs.add(idSource);
            return sac;
        }

        return null;
    }
}
