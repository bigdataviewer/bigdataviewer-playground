package bdv.util.sourceimageloader;

import bdv.viewer.Source;
import mpicbg.spim.data.generic.sequence.BasicSetupImgLoader;
import mpicbg.spim.data.generic.sequence.TypedBasicImgLoader;
import net.imglib2.type.Type;
import java.util.List;

/**
 * ImageLoader from a list of {@link bdv.viewer.Source}
 * This is convenient in order to save some sources which are not
 * originating from an existing dataset.
 *
 * See its use in {@link sc.fiji.bdvpg.sourceandconverter.exporter.XmlHDF5SpimdataExporter}
 * Associated SetupLoader {@link BasicSetupImgLoaderFromSource}
 *
 * @param <T> : Type of the pixel used, should be {@link net.imglib2.type.numeric.integer.UnsignedShortType} for xml / hdf5 export
 */
public class ImgLoaderFromSources<T extends Type<T>> implements TypedBasicImgLoader< T > {

    List<Source<T>> srcs;

    public ImgLoaderFromSources(List<Source<T>> srcs) {
        this.srcs = srcs;
    }

    @Override
    public BasicSetupImgLoader<T> getSetupImgLoader(int setupId) {
        return new BasicSetupImgLoaderFromSource<>(srcs.get(setupId));
    }
}
