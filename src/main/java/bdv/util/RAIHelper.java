package bdv.util;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.Cache;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.LoadedCellCacheLoader;
import net.imglib2.cache.ref.SoftRefLoaderCache;
import net.imglib2.img.basictypeaccess.AccessFlags;
import net.imglib2.img.basictypeaccess.ArrayDataAccessFactory;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.GenericIntType;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.type.numeric.integer.GenericShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.janelia.saalfeldlab.n5.imglib2.RandomAccessibleLoader;

import static net.imglib2.img.basictypeaccess.AccessFlags.VOLATILE;
import static net.imglib2.type.PrimitiveType.*;
import static net.imglib2.type.PrimitiveType.DOUBLE;

public class RAIHelper {

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public static final <T extends NativeType<T>> RandomAccessibleInterval<T> wrapAsVolatileCachedCellImg(
            final RandomAccessibleInterval<T> source,
            final int[] blockSize) {

        final long[] dimensions = Intervals.dimensionsAsLongArray(source);
        final CellGrid grid = new CellGrid(dimensions, blockSize);

        final RandomAccessibleLoader<T> loader = new RandomAccessibleLoader<T>(Views.zeroMin(source));

        final T type = Util.getTypeFromInterval(source);

        final CachedCellImg<T, ?> img;
        final Cache<Long, Cell<?>> cache =
                new SoftRefLoaderCache().withLoader(LoadedCellCacheLoader.get(grid, loader, type, AccessFlags.setOf(VOLATILE)));

        if (GenericByteType.class.isInstance(type)) {
            img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(BYTE, AccessFlags.setOf(VOLATILE)));
        } else if (GenericShortType.class.isInstance(type)) {
            img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(SHORT, AccessFlags.setOf(VOLATILE)));
        } else if (GenericIntType.class.isInstance(type)) {
            img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(INT, AccessFlags.setOf(VOLATILE)));
        } else if (GenericLongType.class.isInstance(type)) {
            img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(LONG, AccessFlags.setOf(VOLATILE)));
        } else if (FloatType.class.isInstance(type)) {
            img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(FLOAT, AccessFlags.setOf(VOLATILE)));
        } else if (DoubleType.class.isInstance(type)) {
            img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(DOUBLE, AccessFlags.setOf(VOLATILE)));
        } else {
            img = null;
        }

        return img;
    }
}
