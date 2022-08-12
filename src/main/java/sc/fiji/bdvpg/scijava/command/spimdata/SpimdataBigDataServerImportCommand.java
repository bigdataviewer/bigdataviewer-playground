/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package sc.fiji.bdvpg.scijava.command.spimdata;

import com.google.gson.stream.JsonReader;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"CanBeFinal", "unused"}) // Because SciJava command fields are set by SciJava pre-processors

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDVDataset>BDVDataset [BigDataServer]",
        label = "Command that opens a BDV dataset from a BigDataServer. Click on Show to display it.")
public class SpimdataBigDataServerImportCommand implements BdvPlaygroundActionCommand
{
    @Parameter(label = "Big Data Server URL")
    String urlserver = "http://tomancak-srv1.mpi-cbg.de:8081";

    @Parameter(label = "Dataset Name")
    String datasetname = "Drosophila";

    @Override
    public void run()
    {
        try {
            Map<String,String> BDSList = getDatasetList(urlserver);
            final String urlString = BDSList.get(datasetname);
            new SpimDataFromXmlImporter(urlString).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String,String> getDatasetList( final String remoteUrl ) throws IOException {

        Map< String, String > datasetUrlMap = new HashMap<>();

        // Get JSON string from the server
        final URL url = new URL( remoteUrl + "/json/" );
        final InputStream is = url.openStream();
        final JsonReader reader = new JsonReader( new InputStreamReader( is, StandardCharsets.UTF_8) );

        reader.beginObject();
        while ( reader.hasNext() )
        {
            // skipping id
            reader.nextName();
            reader.beginObject();
            String id = null, description = null, thumbnailUrl = null, datasetUrl = null;
            while ( reader.hasNext() )
            {
                final String name = reader.nextName();
                switch (name) {
                    case "id":
                        id = reader.nextString();
                        break;
                    case "description":
                        description = reader.nextString();
                        break;
                    case "thumbnailUrl":
                        thumbnailUrl = reader.nextString();
                        break;
                    case "datasetUrl":
                        datasetUrl = reader.nextString();
                        break;
                    default:
                        reader.skipValue();
                        break;
                }
            }
            if ( id != null )
            {
                datasetUrlMap.put( id, datasetUrl );
            }
            reader.endObject();
        }
        reader.endObject();
        reader.close();
        return datasetUrlMap;
    }

}
