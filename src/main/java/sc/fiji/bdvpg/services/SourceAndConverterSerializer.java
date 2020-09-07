package sc.fiji.bdvpg.services;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.display.ColorConverter;
import sc.fiji.bdvpg.services.serializers.AbstractSpimdataAdapter;
import sc.fiji.bdvpg.services.serializers.ColorConverterAdapter;
import sc.fiji.bdvpg.services.serializers.SourceAndConverterAdapter;

import java.util.Map;

public class SourceAndConverterSerializer {

    Map<Integer, SourceAndConverter> idToSac;
    Map<SourceAndConverter, Integer> sacToId;
    Map<Integer, Source> idToSource;
    Map<Source, Integer> sourceToId;

    public Gson getGson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                //.registerTypeHierarchyAdapter(Source.class, new SourceAdapter())
                //.registerTypeHierarchyAdapter(SpimSource.class, new SpimSourceAdapter())
                //.registerTypeAdapter(AbstractSpimData.class, new SpimdataAdapter())
                //.registerTypeAdapter(SpimDataMinimal.class, new SpimdataAdapter())
                .registerTypeHierarchyAdapter(ColorConverter.class, new ColorConverterAdapter(this))
                .registerTypeHierarchyAdapter(SourceAndConverter.class, new SourceAndConverterAdapter(this))
                .registerTypeHierarchyAdapter(AbstractSpimData.class, new AbstractSpimdataAdapter(this))
                .create();
    }

    public synchronized Map<Integer, SourceAndConverter> getIdToSac() {
        return idToSac;
    }

    public synchronized Map<SourceAndConverter, Integer> getSacToId() {
        return sacToId;
    }

    public synchronized Map<Integer, Source> getIdToSource() {
        return idToSource;
    }

    public synchronized Map<Source, Integer> getSourceToId() {
        return sourceToId;
    }
}
