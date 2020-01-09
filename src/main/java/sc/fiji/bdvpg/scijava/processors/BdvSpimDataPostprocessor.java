package sc.fiji.bdvpg.scijava.processors;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.scijava.module.Module;
import org.scijava.module.process.AbstractPostprocessorPlugin;
import org.scijava.module.process.PostprocessorPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.services.BdvSourceAndConverterService;

import java.util.function.Consumer;

@Plugin(type = PostprocessorPlugin.class)
public class BdvSpimDataPostprocessor extends AbstractPostprocessorPlugin {

    @Parameter
    BdvSourceAndConverterService bss;

    public static Consumer<String> log = (str) -> System.out.println( BdvSpimDataPostprocessor.class.getSimpleName()+":"+str);

    public static Consumer<String> errlog = (str) -> System.err.println( BdvSpimDataPostprocessor.class.getSimpleName()+":"+str);

    @Override
    public void process(Module module) {

       module.getOutputs().forEach((name, object)-> {
           //log.accept("input:\t"+name+"\tclass:\t"+object.getClass().getSimpleName());
           if (object instanceof AbstractSpimData) {
               AbstractSpimData asd = (AbstractSpimData) object;
               log.accept("SpimData found.");
               bss.register(asd);
               module.resolveOutput(name);
           }
           if (object instanceof AbstractSpimData[]) {
               AbstractSpimData[] asds = (AbstractSpimData[]) object;
               for (AbstractSpimData asd:asds) {
                   log.accept( "SpimData found." );
                   bss.register( asd );
                   module.resolveOutput( name );
               }
            }
       });
    }
}
