package bdv.util.sourceimageloader;

import bdv.viewer.Source;
import mpicbg.spim.data.generic.sequence.BasicSetupImgLoader;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.Type;

/**
 * SetupLoader from a source coming from an {@link ImgLoaderFromSources}
 * This is convenient in order to save some sources which are not
 * originating from an existing dataset.
 *
 * See its use in {@link sc.fiji.bdvpg.sourceandconverter.exporter.XmlHDF5SpimdataExporter}
 * Associated SetupLoader {@link BasicSetupImgLoaderFromSource}
 *
 * @param <T> : Type of the pixel used, should be {@link net.imglib2.type.numeric.integer.UnsignedShortType} for xml / hdf5 export
 */

public class BasicSetupImgLoaderFromSource< T extends Type< T >> implements BasicSetupImgLoader< T > {

    Source<T> src;

    public BasicSetupImgLoaderFromSource(Source<T> src) {
        this.src = src;
    }

    @Override
    public RandomAccessibleInterval<T> getImage(int timepointId, ImgLoaderHint... hints) {
        return src.getSource(timepointId, 0);
    }

    @Override
    public T getImageType() {
        return src.getType();
    }
}
