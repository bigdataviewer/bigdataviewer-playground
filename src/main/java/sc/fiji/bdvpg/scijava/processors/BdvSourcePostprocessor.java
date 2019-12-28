package sc.fiji.bdvpg.scijava.processors;

import bdv.viewer.Source;
import org.scijava.convert.ConvertService;
import org.scijava.module.Module;
import org.scijava.module.process.AbstractPostprocessorPlugin;
import org.scijava.module.process.PostprocessorPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import sc.fiji.bdvpg.scijava.services.BdvSourceDisplayService;
import sc.fiji.bdvpg.scijava.services.BdvSourceService;

import java.util.function.Consumer;

@Plugin(type = PostprocessorPlugin.class)
public class BdvSourcePostprocessor extends AbstractPostprocessorPlugin {

   //@Parameter(required = false)
    //private ModuleService moduleService;

    @Parameter(required = false)
    private ConvertService convertService;

    @Parameter
    private BdvSourceDisplayService bhs;

    @Parameter
    BdvSourceService bss;

    public static Consumer<String> log = (str) -> System.out.println(BdvSourcePostprocessor.class.getSimpleName()+":"+str);

    @Parameter
    UIService uiService;

    @Override
    public void process(Module module) {
       //if (moduleService == null || convertService == null) return;// null;
       /* module.getInputs().forEach((name, object)-> {
            log.accept("input:\t"+name+"\tclass:\t"+object.getClass().getSimpleName());
            if (object instanceof Source) {
                Source src = (Source) object;
                log.accept("Source found.");
                log.accept("Is it registered ? ");

                if (!bss.isRegistered(src)) {
                    bss.registerSource(src);
                }
                module.setInput(name, value);
                module.resolveInput(itemName);
            }
        });*/

       module.getOutputs().forEach((name, object)-> {
           log.accept("input:\t"+name+"\tclass:\t"+object.getClass().getSimpleName());
           if (object instanceof Source) {
               Source src = (Source) object;
               log.accept("Source found.");
               log.accept("Is it registered ? ");

               if (!bss.isRegistered(src)) {
                   log.accept("No.");
                   bss.registerSource(src);
               } else {
                   log.accept("Yes.");
               }
               bhs.show(src);
               module.resolveOutput(name);
           }
       });

        // Check the actual class first
        /*ModuleItem<?> item = moduleService.getSingleInput(module, Source.class);
        if (item == null || !item.isAutoFill()) {
            // No match, so check look for classes that can be converted from the specified type
            final Collection<Class<?>> compatibleClasses = convertService.getCompatibleOutputClasses(Source.class);
            item = moduleService.getSingleInput(module, compatibleClasses);
        }

        // populate the value of the single input
        Object value = getValue();

        //
        if (value == null) return;

        String itemName = singleInput.getName();
        value = convertService.convert(value, singleInput.getType());
        module.setInput(itemName, value);
        module.resolveInput(itemName);*/
    }
}
