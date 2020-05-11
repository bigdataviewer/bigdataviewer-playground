package bdv.util;

import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.*;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import java.util.function.ToIntFunction;
// TODO : replace by FunctionRandomAccessible and FunctionRealRandomAccessible
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
        VoxelDimensions voxdimensions = new VoxelDimensions() {
            @Override
            public String unit() {
                return "undefined";
            }

            @Override
            public void dimensions(double[] dimensions) {
                dimensions[0] = 1;
                dimensions[1] = 1;
                dimensions[2] = 1;
            }

            @Override
            public double dimension(int d) {
                return 1;
            }

            @Override
            public int numDimensions() {
                return 3;
            }
        };
        return getSource(interval, at3D, name, voxdimensions);
    }

    public Source<UnsignedShortType> getSource(final Interval interval, String name) {
        return getSource(interval, new AffineTransform3D(), name );
    }

    public Source<UnsignedShortType> getSource(String name) {
        return getSource(new FinalInterval(new long[]{0,0,0}, new long[]{1,1,1}), name);
    }

    public Source<UnsignedShortType> getSource(final Interval interval, AffineTransform3D at3D, String name, VoxelDimensions voxDimensions) {
        return new RealRandomAccessibleSource(getRRA(), new UnsignedShortType(), name, voxDimensions) {
            @Override
            public Interval getInterval(final int t, final int level) {
                return new FinalInterval(new long[]{0,0,0}, new long[]{1,1,1});
            }

            @Override
            public synchronized void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
            {
                transform.set( at3D );
            }
        };
    }


}
