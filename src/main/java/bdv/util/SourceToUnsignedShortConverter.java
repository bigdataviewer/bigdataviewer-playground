package bdv.util;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

/**
 * Helper function to transform RealTyped {@link bdv.viewer.Source} to {@link UnsignedShortType} source
 *
 * TODO : improved conversion or retire... the conversion is not modular it's a direct casting to int
 *
 */

public class SourceToUnsignedShortConverter {

    static public Source<UnsignedShortType> convertRealSource(Source<RealType> iniSrc) {
        Converter<RealType, UnsignedShortType> cvt = (i, o) -> o.set((int) i.getRealDouble());
        Source<UnsignedShortType> cvtSrc = new Source<UnsignedShortType>() {
            @Override
            public boolean isPresent(int t) {
                return iniSrc.isPresent(t);
            }

            @Override
            public RandomAccessibleInterval<UnsignedShortType> getSource(int t, int level) {
                return Converters.convert(iniSrc.getSource(t,level),cvt,new UnsignedShortType());
            }

            @Override
            public RealRandomAccessible<UnsignedShortType> getInterpolatedSource(int t, int level, Interpolation method) {
                return Converters.convert(iniSrc.getInterpolatedSource(t,level,method),cvt,new UnsignedShortType());
            }

            @Override
            public void getSourceTransform(int t, int level, AffineTransform3D transform) {
                iniSrc.getSourceTransform(t,level,transform);
            }

            @Override
            public UnsignedShortType getType() {
                return new UnsignedShortType();
            }

            @Override
            public String getName() {
                return iniSrc.getName();
            }

            @Override
            public VoxelDimensions getVoxelDimensions() {
                return iniSrc.getVoxelDimensions();
            }

            @Override
            public int getNumMipmapLevels() {
                return iniSrc.getNumMipmapLevels();
            }
        };

        return cvtSrc;
    }

    static public <T> Source<UnsignedShortType> convertSource(Source<T> iniSrc) {
        if (iniSrc.getType() instanceof UnsignedShortType) return (Source<UnsignedShortType>) iniSrc;
        if (iniSrc.getType() instanceof RealType) return convertRealSource((Source<RealType>) iniSrc);
        System.err.println("Cannot convert source to Unsigned Short Type, "+iniSrc.getType().getClass().getSimpleName()+" cannot be converted to RealType");
        return null;
    }
}
