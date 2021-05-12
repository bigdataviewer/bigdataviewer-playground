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
package sc.fiji.persist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterServiceUI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Get serializers that registers all scijava registered adapters and runtime adapters
 *
 * Note : some "simple" objects do not require specific adapters with json.
 *
 * See {@link RuntimeTypeAdapterFactory}
 *
 */

public class ScijavaGsonHelper {

    protected static Logger logger = LoggerFactory.getLogger(ScijavaGsonHelper.class);

    public static Gson getGson(Context ctx) {
        return getGson(ctx, false);
    }

    public static Gson getGson(Context ctx, boolean verbose) {
        return getGsonBuilder(ctx, new GsonBuilder(), verbose).create();
    }

    public static GsonBuilder getGsonBuilder(Context ctx, boolean verbose) {
        return getGsonBuilder(ctx, new GsonBuilder().setPrettyPrinting(), verbose);
    }

    public static GsonBuilder getGsonBuilder(Context ctx, GsonBuilder builder, boolean verbose) {
        Consumer<String> log;
        if (verbose) {
            log = logger::info;
        } else {
            log = logger::debug;
        }

        // We need to get all serializers which require custom adapters. This typically happens
        // when serialiazing interfaces or abstract classes (typical scenario : imglib2 RealTransform objects)
        // The interface or abstract class is the Base class, and runtime classes are
        // implementing the base interface class or extending the abstract base class
        // typical scenario : AffineTransform3D, ThinPlateSplineTransform, all implementing imglib2 RealTransform interface
        Map<Class<?>, ClassTypesAndSubTypes<?>> runTimeAdapters = new HashMap<>();

        ctx.getService(IObjectScijavaAdapterService.class)
                .getAdapters(IClassRuntimeAdapter.class) // Gets all Runtime adapters
                .forEach(pi -> {
                            try {
                                IClassRuntimeAdapter adapter = pi.createInstance(); // Creates runtime adapter TODO : how to fiw raw type here ?
                                if (runTimeAdapters.containsKey(adapter.getBaseClass())) {
                                    ClassTypesAndSubTypes<?> typesAndSubTypes = runTimeAdapters.get(adapter.getBaseClass());
                                    if (typesAndSubTypes.subClasses.contains(adapter.getRunTimeClass())) { // Presence of two runtime adapters for the same class!
                                        throw new RuntimeException("Presence of conflicting adapters for class "+adapter.getRunTimeClass());
                                    } else {
                                        runTimeAdapters.get(adapter.getBaseClass()).subClasses.add(adapter.getRunTimeClass());
                                        //builder.registerTypeHierarchyAdapter(adapter.getRunTimeClass(), adapter); // Register gson adapter
                                    }
                                } else {
                                    ClassTypesAndSubTypes<?> element = new ClassTypesAndSubTypes<>(adapter.getBaseClass());
                                    element.subClasses.add(adapter.getRunTimeClass());
                                    runTimeAdapters.put(adapter.getBaseClass(), element);
                                    //builder.registerTypeHierarchyAdapter(adapter.getRunTimeClass(), adapter); // Register gson adapter
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                );

        log.accept("IRunTimeClassAdapters : ");
        runTimeAdapters.values().forEach(typesAndSubTypes -> builder.registerTypeAdapterFactory(typesAndSubTypes.getRunTimeAdapterFactory(log)));

        ctx.getService(IObjectScijavaAdapterService.class)
                .getAdapters(IClassRuntimeAdapter.class)
                .forEach(pi -> {
                    try {
                        IClassRuntimeAdapter<?,?> adapter = pi.createInstance();
                        if (adapter.useCustomAdapter()) { // Overrides default adapter only if needed
                            builder.registerTypeHierarchyAdapter(adapter.getRunTimeClass(), adapter);
                        }
                    } catch (InstantiableException e) {
                        e.printStackTrace();
                    }
                });

        // First, we register all adapters which are directly serializing/deserializing classes, without the need
        // of runtime class serialization customisation
        log.accept("IClassAdapters : ");
        ctx.getService(IObjectScijavaAdapterService.class)
                .getAdapters(IClassAdapter.class) // Gets all scijava class adapters (no runtime)
                .forEach(pi -> {
                    try {
                        IClassAdapter<?> adapter = pi.createInstance(); // Instanciate the adapter (no argument should be present in the constructor, but auto filled scijava parameters are allowed)
                        log.accept("\t "+adapter.getAdapterClass());
                        builder.registerTypeHierarchyAdapter(adapter.getAdapterClass(), adapter); // Register gson adapter
                    } catch (InstantiableException e) {
                        e.printStackTrace();
                    }
                });

        return builder;
    }

    // Inner static class needed for type safety
    public static class ClassTypesAndSubTypes<T> {

        Class<T> baseClass;

        public ClassTypesAndSubTypes(Class<T> clazz) {
            this.baseClass = clazz;
        }

        List<Class<? extends T>> subClasses = new ArrayList<>();

        public RuntimeTypeAdapterFactory<T> getRunTimeAdapterFactory(Consumer<String> log) {
            RuntimeTypeAdapterFactory<T> factory = RuntimeTypeAdapterFactory.of(baseClass);
            log.accept("\t "+baseClass);
            subClasses.forEach(subClass -> {
                log.accept("\t \t "+subClass);
                factory.registerSubtype( subClass );
            });
            return factory;
        }
    }
}
