package bdv.util.sourceimageloader;

import bdv.viewer.Source;
import mpicbg.spim.data.generic.sequence.BasicSetupImgLoader;
import mpicbg.spim.data.generic.sequence.TypedBasicImgLoader;
import net.imglib2.type.Type;

import java.util.List;

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
