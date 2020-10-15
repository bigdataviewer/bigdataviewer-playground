/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2020 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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

/**
 * Helper function to cache a {@link RandomAccessibleInterval}
 *
 * TODO : replace by an helper function moved to imglib2 when available
 *
 */

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
