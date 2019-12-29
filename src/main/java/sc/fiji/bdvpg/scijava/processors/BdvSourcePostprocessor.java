package sc.fiji.bdvpg.scijava.processors;

import bdv.viewer.Source;
import org.scijava.module.Module;
import org.scijava.module.process.AbstractPostprocessorPlugin;
import org.scijava.module.process.PostprocessorPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.services.BdvSourceDisplayService;
import sc.fiji.bdvpg.scijava.services.BdvSourceService;

import java.util.function.Consumer;

@Plugin(type = PostprocessorPlugin.class)
public class BdvSourcePostprocessor extends AbstractPostprocessorPlugin {

    @Parameter
    private BdvSourceDisplayService bhs;

    @Parameter
    BdvSourceService bss;

    public static Consumer<String> log = (str) -> System.out.println(BdvSourcePostprocessor.class.getSimpleName()+":"+str);

    @Override
    public void process(Module module) {

       module.getOutputs().forEach((name, object)-> {
           log.accept("input:\t"+name+"\tclass:\t"+object.getClass().getSimpleName());
           if (object instanceof Source) {
               Source src = (Source) object;
               log.accept("Source found.");
               log.accept("Is it registered ? ");
               if (!bss.isRegistered(src)) {
                   log.accept("No.");
                   bss.register(src);
               } else {
                   log.accept("Yes.");
               }
               /**
                * Default behaviour : display it on the active window
                */
               if (bhs!=null) {
                   bhs.show(src);
               }
               module.resolveOutput(name);
           }
       });

    }
}
