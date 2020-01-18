package sc.fiji.bdvpg.scijava.processors;

import bdv.viewer.SourceAndConverter;
import org.scijava.module.Module;
import org.scijava.module.process.AbstractPostprocessorPlugin;
import org.scijava.module.process.PostprocessorPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

import java.util.function.Consumer;

@Plugin(type = PostprocessorPlugin.class)
public class BdvSourceAndConverterPostprocessor extends AbstractPostprocessorPlugin {

    @Parameter
    SourceAndConverterService bss;

    public static Consumer<String> log = (str) -> System.out.println(BdvSourceAndConverterPostprocessor.class.getSimpleName()+":"+str);

    @Override
    public void process(Module module) {

       module.getOutputs().forEach((name, object)-> {
           //log.accept("input:\t"+name+"\tclass:\t"+object.getClass().getSimpleName());
           if (object instanceof SourceAndConverter) {
               SourceAndConverter sac = (SourceAndConverter) object;
               log.accept("Source found.");
               log.accept("Is it registered ? ");
               if (!bss.isRegistered(sac)) {
                   log.accept("No.");
                   bss.register(sac);
               } else {
                   log.accept("Yes.");
               }
               module.resolveOutput(name);
           }
           if (object instanceof SourceAndConverter[]) {
               SourceAndConverter[] sacs = (SourceAndConverter[]) object;
               for (SourceAndConverter sac:sacs) {
                   log.accept("Source found.");
                   log.accept("Is it registered ? ");
                   if (!bss.isRegistered(sac)) {
                       log.accept("No.");
                       bss.register(sac);
                   } else {
                       log.accept("Yes.");
                   }
               }
               module.resolveOutput(name);
           }
       });
    }
}
