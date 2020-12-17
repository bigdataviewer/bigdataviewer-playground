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
package sc.fiji.bdvpg.services.serializers.plugins;

import bdv.SpimSource;
import bdv.viewer.SourceAndConverter;
import com.google.gson.*;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterSerializer;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.lang.reflect.Type;
import java.util.Optional;

import static sc.fiji.bdvpg.services.ISourceAndConverterService.SPIM_DATA_INFO;

@Plugin(type = ISourceAdapter.class)
public class SpimSourceAdapter implements ISourceAdapter<SpimSource> {

    SourceAndConverterSerializer sacSerializer;

    @Override
    public void setSacSerializer(SourceAndConverterSerializer sacSerializer) {
        this.sacSerializer = sacSerializer;
    }

    @Override
    public Class<SpimSource> getSourceClass() {
        return SpimSource.class;
    }

    @Override
    public JsonElement serialize(SourceAndConverter sac, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();

        SourceAndConverterService.SpimDataInfo sdi =
                (SourceAndConverterService.SpimDataInfo) SourceAndConverterServices
                        .getSourceAndConverterService()
                        .getMetadata(sac, SPIM_DATA_INFO);

        if (sdi == null) {
            System.err.println("Spim Source "+sac.getSpimSource().getName()+"  has no associated spimdata. Deserialization will fail.");
        } else {
            obj.add("spimdata", jsonSerializationContext.serialize(sdi.asd));
            obj.addProperty("viewsetup", sdi.setupId);
        }
        return obj;
    }

    @Override
    public SourceAndConverter deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        AbstractSpimData asd = jsonDeserializationContext.deserialize(obj.get("spimdata"), AbstractSpimData.class);
        if (asd == null) {
            System.err.println("A BDV Dataset could not be serialized!");
            return null;
        } else {
            int setupId = obj.getAsJsonPrimitive("viewsetup").getAsInt();
            final ISourceAndConverterService sacservice = SourceAndConverterServices
                    .getSourceAndConverterService();
            Optional<SourceAndConverter> futureSac = sacservice.getSourceAndConverters()
                    .stream()
                    .filter(sac -> sacservice.containsMetadata(sac, SPIM_DATA_INFO))
                    .filter(sac -> {
                        SourceAndConverterService.SpimDataInfo sdi = (SourceAndConverterService.SpimDataInfo) sacservice.getMetadata(sac, SPIM_DATA_INFO);
                        return sdi.asd.equals(asd) && sdi.setupId == setupId;
                    }).findFirst();
            if (futureSac.isPresent()) {
                return futureSac.get();
            } else {
                System.err.println("Couldn't deserialize spim source from json element " + jsonElement.getAsString());
                return null;
            }
        }
    }
}
