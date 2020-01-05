package sc.fiji.bdvpg.scijava.processors;

import bdv.SpimSource;
import bdv.ViewerImgLoader;
import bdv.VolatileSpimSource;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import org.scijava.module.Module;
import org.scijava.module.process.AbstractPostprocessorPlugin;
import org.scijava.module.process.PostprocessorPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.services.BdvSourceDisplayService;
import sc.fiji.bdvpg.scijava.services.BdvSourceService;

import java.util.function.Consumer;

@Plugin(type = PostprocessorPlugin.class)
public class BdvSpimdataPostprocessor extends AbstractPostprocessorPlugin {

    @Parameter
    BdvSourceService bss;

    public static Consumer<String> log = (str) -> System.out.println(BdvSpimdataPostprocessor.class.getSimpleName()+":"+str);

    public static Consumer<String> errlog = (str) -> System.err.println(BdvSpimdataPostprocessor.class.getSimpleName()+":"+str);

    @Override
    public void process(Module module) {

       module.getOutputs().forEach((name, object)-> {
           log.accept("input:\t"+name+"\tclass:\t"+object.getClass().getSimpleName());
           if (object instanceof AbstractSpimData) {
               AbstractSpimData asd = (AbstractSpimData) object;

               log.accept("Spimdata found.");

               bss.register(asd);
               /*final AbstractSequenceDescription< ?, ?, ? > seq = asd.getSequenceDescription();
               final ViewerImgLoader imgLoader = ( ViewerImgLoader ) seq.getImgLoader();
               for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
               {
                   final int setupId = setup.getId();
                   final Object type = imgLoader.getSetupImgLoader( setupId ).getImageType();
                   if ( RealType.class.isInstance( type ) ) {
                       final VolatileSpimSource vs = new VolatileSpimSource<>( asd, setupId, "" );
                       final SpimSource s = vs.nonVolatile();
                       bss.register(s,vs);
                       bss.linkToSpimData(s,asd);
                       //if (bhs!=null) {
                       //    bhs.show(s);
                       //}
                   } else if ( ARGBType.class.isInstance( type ) ) {
                       //TODO
                       errlog.accept("Cannot open Spimdata with Source of Type ARGBType");
                   } else {
                       errlog.accept("Cannot open Spimdata with Source of type "+type.getClass().getSimpleName());
                   }
               }*/

               module.resolveOutput(name);
           }
       });

    }
}
