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
import org.scijava.InstantiableException;
import sc.fiji.bdvpg.services.serializers.*;
import sc.fiji.bdvpg.services.serializers.plugins.BdvPlaygroundObjectAdapterService;
import sc.fiji.bdvpg.services.serializers.plugins.IClassAdapter;
import sc.fiji.bdvpg.services.serializers.plugins.IClassRuntimeAdapter;
import sc.fiji.bdvpg.services.serializers.plugins.ISourceAdapter;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class SourceAndConverterSerializer {

    final File basePath;

    public SourceAndConverterSerializer(Context ctx, File basePath) {
        this.ctx = ctx;
        this.basePath = basePath;
    }

    public File getBasePath() {
        return basePath;
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

    GsonBuilder builder;

    public static Consumer<String> log = (str) -> System.out.println(SourceAndConverterSerializer.class+":"+str);

    public Gson getGson() {

        Map<Class, List<Class>> runTimeAdapters = new HashMap<>();

        builder = new GsonBuilder()
                .setPrettyPrinting();

        log.accept("IClassAdapters : ");
        ctx.getService(BdvPlaygroundObjectAdapterService.class)
                .getAdapters(IClassAdapter.class)
                .forEach(pi -> {
                    try {
                        IClassAdapter adapter = pi.createInstance();
                        log.accept("\t "+adapter.getAdapterClass());
                        builder = builder.registerTypeHierarchyAdapter(adapter.getAdapterClass(), adapter);
                    } catch (InstantiableException e) {
                        e.printStackTrace();
                    }
               });

        ctx.getService(BdvPlaygroundObjectAdapterService.class)
                .getAdapters(IClassRuntimeAdapter.class)
                .forEach(pi -> {
                            try {
                                IClassRuntimeAdapter adapter = pi.createInstance();
                                if (runTimeAdapters.containsKey(adapter.getBaseClass())) {
                                    runTimeAdapters.get(adapter.getBaseClass()).add(adapter.getRunTimeClass());
                                } else {
                                    List<Class> subClasses = new ArrayList<>();
                                    subClasses.add(adapter.getRunTimeClass());
                                    runTimeAdapters.put(adapter.getBaseClass(), subClasses);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                );

        ctx.getService(BdvPlaygroundObjectAdapterService.class)
                .getAdapters(IClassRuntimeAdapter.class)
                .forEach(pi -> {
                    try {
                        IClassRuntimeAdapter adapter = pi.createInstance();
                        builder = builder.registerTypeHierarchyAdapter(adapter.getRunTimeClass(), adapter);
                    } catch (InstantiableException e) {
                        e.printStackTrace();
                    }
                });


        log.accept("IRunTimeClassAdapters : ");
        runTimeAdapters.keySet().forEach(baseClass -> {
            log.accept("\t "+baseClass);
            RuntimeTypeAdapterFactory factory = RuntimeTypeAdapterFactory.of(baseClass);
            runTimeAdapters.get(baseClass).forEach(subClass -> {
                factory.registerSubtype(subClass);
                log.accept("\t \t "+subClass);
            });
            builder = builder.registerTypeAdapterFactory(factory);
        });

        builder = builder
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
