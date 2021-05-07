/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2021 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package sc.fiji.bdvpg.scijava.adapter;

import com.google.gson.*;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.services.SourceAndConverterAdapter;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.spimdata.exporter.XmlFromSpimDataExporter;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.io.File;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

import static sc.fiji.bdvpg.services.ISourceAndConverterService.SPIM_DATA_LOCATION;

public class AbstractSpimdataAdapter implements JsonSerializer<AbstractSpimData>,
        JsonDeserializer<AbstractSpimData> {

    SourceAndConverterAdapter sacSerializer;

    int spimdataCounter = 0;

    public AbstractSpimdataAdapter(SourceAndConverterAdapter sacSerializer) {
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
        if ((dataLocation == null)||(dataLocation.equals(""))) {
            dataLocation = new File(sacSerializer.getBasePath(), "_bdvdataset_"+spimdataCounter+".xml").getAbsolutePath();
            while (new File(dataLocation).exists()) {
                spimdataCounter++;
                dataLocation = new File(sacSerializer.getBasePath(), "_bdvdataset_"+spimdataCounter+".xml").getAbsolutePath();
            }
            spimdataCounter++;
            System.out.println("Previously unsaved bdv dataset, saving it to "+dataLocation);
            new XmlFromSpimDataExporter(asd, dataLocation, sacSerializer.getScijavaContext() ).run();
        }
        obj.addProperty("datalocation", dataLocation);
        return obj;
    }

    @Override
    public AbstractSpimData deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        String datalocation = jsonElement.getAsJsonObject().get("datalocation").getAsString();
        //System.out.println("Deserialization of "+datalocation);
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
