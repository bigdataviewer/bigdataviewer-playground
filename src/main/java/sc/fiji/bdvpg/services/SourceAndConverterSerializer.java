package sc.fiji.bdvpg.services;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.display.ColorConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import org.scijava.Context;
import sc.fiji.bdvpg.services.serializers.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SourceAndConverterSerializer {

    public SourceAndConverterSerializer(Context ctx) {
        this.ctx = ctx;
    }

    Map<Integer, SourceAndConverter> idToSac;
    Map<SourceAndConverter, Integer> sacToId;
    Map<Integer, Source> idToSource;
    Map<Source, Integer> sourceToId;

    public Set<Integer> alreadyDeSerializedSacs = new HashSet<>();
    public Map<Integer, JsonElement> idToJsonElement = new HashMap<>();

    Context ctx;

    public Context getScijavaContext() {
        return ctx;
    }

    public Gson getGson() {
        GsonBuilder builder = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeHierarchyAdapter(RealTransform.class, new RealTransformAdapter())
                .registerTypeHierarchyAdapter(AffineTransform3D.class, new AffineTransform3DAdapter())
                .registerTypeHierarchyAdapter(ColorConverter.class, new ColorConverterAdapter(this))
                .registerTypeHierarchyAdapter(SourceAndConverter.class, new SourceAndConverterAdapter(this))
                .registerTypeHierarchyAdapter(AbstractSpimData.class, new AbstractSpimdataAdapter(this));

        return builder.create();
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
