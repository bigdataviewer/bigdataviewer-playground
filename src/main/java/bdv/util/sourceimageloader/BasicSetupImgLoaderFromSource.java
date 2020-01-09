package bdv.util.sourceimageloader;

import bdv.viewer.Source;
import mpicbg.spim.data.generic.sequence.BasicSetupImgLoader;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.Type;

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
