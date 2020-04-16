package sc.fiji.bdvpg.scijava.command.spimdata;

import com.google.gson.stream.JsonReader;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"SpimDataset>SpimDataset [BigDataServer]",
        label = "Command that opens a Spimdata dataset from a BigDataServer. Click on Show to display it.")
public class SpimdataBigDataServerImportCommand implements Command
{
    @Parameter(label = "Big Data Server URL")
    String urlServer = "http://tomancak-srv1.mpi-cbg.de:8081";

    @Parameter(label = "Dataset Name")
    String datasetName = "Drosophila";

    @Override
    public void run()
    {
        try {
            Map<String,String> BDSList = getDatasetList(urlServer);
            final String urlString = BDSList.get(datasetName);
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
        final JsonReader reader = new JsonReader( new InputStreamReader( is, "UTF-8" ) );

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
                if ( name.equals( "id" ) )
                    id = reader.nextString();
                else if ( name.equals( "description" ) )
                    description = reader.nextString();
                else if ( name.equals( "thumbnailUrl" ) )
                    thumbnailUrl = reader.nextString();
                else if ( name.equals( "datasetUrl" ) )
                    datasetUrl = reader.nextString();
                else
                    reader.skipValue();
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
