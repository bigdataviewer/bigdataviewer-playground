package sc.fiji.serializers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.scijava.Context;
import org.scijava.InstantiableException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Get serializers that registers all scijava registered adapters and runtime adapters
 */
public class ScijavaGsonHelper {

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
            log = (str) -> System.out.println(str);
        } else {
            log = (str) -> {};
        }

        Map<Class, List<Class>> runTimeAdapters = new HashMap<>();

        log.accept("IClassAdapters : ");
        ctx.getService(IObjectScijavaAdapterService.class)
                .getAdapters(IClassAdapter.class)
                .forEach(pi -> {
                    try {
                        IClassAdapter adapter = pi.createInstance();
                        log.accept("\t "+adapter.getAdapterClass());
                        builder.registerTypeHierarchyAdapter(adapter.getAdapterClass(), adapter);
                    } catch (InstantiableException e) {
                        e.printStackTrace();
                    }
                });

        ctx.getService(IObjectScijavaAdapterService.class)
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

        ctx.getService(IObjectScijavaAdapterService.class)
                .getAdapters(IClassRuntimeAdapter.class)
                .forEach(pi -> {
                    try {
                        IClassRuntimeAdapter adapter = pi.createInstance();
                        builder.registerTypeHierarchyAdapter(adapter.getRunTimeClass(), adapter);
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
            builder.registerTypeAdapterFactory(factory);
        });

        return builder;
    }
}
