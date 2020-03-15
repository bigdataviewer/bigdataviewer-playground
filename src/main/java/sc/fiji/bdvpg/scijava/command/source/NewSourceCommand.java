package sc.fiji.bdvpg.scijava.command.source;

import bdv.viewer.SourceAndConverter;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.sourceandconverter.importer.NewSourceAndConverterGetter;

import static net.imglib2.cache.img.DiskCachedCellImgOptions.options;

/**
 * Command which creates an empty Source based on a model Source
 * The created source will cover the same portion of space as the model source,
 * but with the specified voxel size, and at a specific timepoint
 */

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>New Source Based on Model Source")
public class NewSourceCommand implements Command {
    
    @Parameter(label = "Model Source", description = "Defines the portion of space covered by the new source")
    SourceAndConverter model;

    @Parameter(label = "Source name")
    String name;

    @Parameter(type = ItemIO.OUTPUT)
    SourceAndConverter newsource;
    
    @Parameter(label = "Voxel Size X")
    double voxSizeX;

    @Parameter(label = "Voxel Size Y")
    double voxSizeY;

    @Parameter(label = "Voxel Size Z")
    double voxSizeZ;

    @Parameter(label = "Timepoint (0 based index)")
    int timePoint;

    @Override
    public void run() {

        // Make edge display on demand
        final int[] cellDimensions = new int[] { 32, 32, 32 };

        // Cached Image Factory Options
        final DiskCachedCellImgOptions factoryOptions = options()
                .cellDimensions( cellDimensions )
                .cacheType( DiskCachedCellImgOptions.CacheType.BOUNDED )
                .maxCacheSize( 1 );

        // Creates cached image factory of Type UnsignedShort
        final DiskCachedCellImgFactory<UnsignedShortType> factory = new DiskCachedCellImgFactory<>( new UnsignedShortType(), factoryOptions );

        newsource = new NewSourceAndConverterGetter(name, model, timePoint, voxSizeX, voxSizeY, voxSizeZ, factory).get();
    }
}
