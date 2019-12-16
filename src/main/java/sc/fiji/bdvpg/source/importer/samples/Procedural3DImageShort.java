package sc.fiji.bdvpg.source.importer.samples;

import bdv.util.RealRandomAccessibleIntervalSource;
import bdv.viewer.Source;
import net.imglib2.*;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import java.util.function.ToIntFunction;

public class Procedural3DImageShort extends RealPoint implements RealRandomAccess<UnsignedShortType> {
    final UnsignedShortType t;

    ToIntFunction<double[]> evalFunction;

    public Procedural3DImageShort(ToIntFunction<double[]> evalFunction)
    {
        super( 3 ); // number of dimensions is 3
        t = new UnsignedShortType();
        this.evalFunction=evalFunction;
    }

    public Procedural3DImageShort(UnsignedShortType t) {
        this.t = t;
    }

    @Override
    public RealRandomAccess<UnsignedShortType> copyRealRandomAccess() {
        return copy();
    }

    @Override
    public UnsignedShortType get() {
        t.set(
                evalFunction.applyAsInt(position)
        );
        return t;
    }

    @Override
    public Procedural3DImageShort copy() {
        Procedural3DImageShort a = new Procedural3DImageShort(evalFunction);
        a.setPosition( this );
        return a;
    }

    public RealRandomAccessible<UnsignedShortType> getRRA() {

        RealRandomAccessible<UnsignedShortType> rra = new RealRandomAccessible<UnsignedShortType>() {
            @Override
            public RealRandomAccess<UnsignedShortType> realRandomAccess() {
                return copy();
            }

            @Override
            public RealRandomAccess<UnsignedShortType> realRandomAccess(RealInterval realInterval) {
                return copy();
            }

            @Override
            public int numDimensions() {
                return 3;
            }
        };

        return rra;
    }

    public Source<UnsignedShortType> getSource(final Interval interval, AffineTransform3D at3D, String name) {
        return new RealRandomAccessibleIntervalSource<>( getRRA(), interval, new UnsignedShortType(),
                new AffineTransform3D(), name );
    }

    public Source<UnsignedShortType> getSource(final Interval interval, String name) {
        return new RealRandomAccessibleIntervalSource<>( getRRA(), interval, new UnsignedShortType(),
                new AffineTransform3D(), name );
    }

    public Source<UnsignedShortType> getSource(String name) {
        return new RealRandomAccessibleIntervalSource<>( getRRA(), new FinalInterval(new long[]{0,0,0}, new long[]{1,1,1}), new UnsignedShortType(),
                new AffineTransform3D(), name );
    }


}
