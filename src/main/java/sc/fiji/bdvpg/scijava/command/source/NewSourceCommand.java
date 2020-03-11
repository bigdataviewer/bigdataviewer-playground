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

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>New Source Based on Model Source")
public class NewSourceCommand implements Command {
    
    @Parameter
    SourceAndConverter model;

    @Parameter
    String name;

    @Parameter(type = ItemIO.OUTPUT)
    SourceAndConverter newsource;
    
    @Parameter
    double voxSizeX, voxSizeY, voxSizeZ;

    @Parameter
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

        /*RandomAccessibleInterval rai = model.getSpimSource().getSource(timePoint,0);

        long nPixModelX = rai.dimension(0);
        long nPixModelY = rai.dimension(1);
        long nPixModelZ = rai.dimension(2);

        AffineTransform3D at3Dorigin = new AffineTransform3D();

        model.getSpimSource().getSourceTransform(timePoint,0,at3Dorigin);

        // Origin
        double[] x0 = new double[3];
        at3Dorigin.apply(new double[]{0,0,0}, x0);

        // xMax
        double[] pt = new double[3];
        double dist;

        at3Dorigin.apply(new double[]{1,0,0},pt);

        dist = (pt[0]-x0[0])*(pt[0]-x0[0]) +
                      (pt[1]-x0[1])*(pt[1]-x0[1]) +
                      (pt[2]-x0[2])*(pt[2]-x0[2]);

        double distx =  Math.sqrt(dist);

        dist = Math.sqrt(dist)*nPixModelX;

        double nPixX = dist/voxSizeX;

        at3Dorigin.apply(new double[]{0,1,0},pt);

        dist = (pt[0]-x0[0])*(pt[0]-x0[0]) +
                (pt[1]-x0[1])*(pt[1]-x0[1]) +
                (pt[2]-x0[2])*(pt[2]-x0[2]);

        double disty =  Math.sqrt(dist);

        dist = Math.sqrt(dist)*nPixModelY;
        double nPixY = dist/voxSizeY;

        at3Dorigin.apply(new double[]{0,0,1},pt);

        dist = (pt[0]-x0[0])*(pt[0]-x0[0]) +
                (pt[1]-x0[1])*(pt[1]-x0[1]) +
                (pt[2]-x0[2])*(pt[2]-x0[2]);

        double distz =  Math.sqrt(dist);

        dist = Math.sqrt(dist)*nPixModelZ;
        double nPixZ = dist/voxSizeZ;

        double[] m = at3Dorigin.getRowPackedCopy();

        m[0] = m[0]/distx * voxSizeX;
        m[4] = m[4]/distx * voxSizeX;
        m[8] = m[8]/distx * voxSizeX;

        m[1] = m[1]/disty * voxSizeY;
        m[5] = m[5]/disty * voxSizeY;
        m[9] = m[9]/disty * voxSizeY;

        m[2] = m[2]/distz * voxSizeZ;
        m[6] = m[6]/distz * voxSizeZ;
        m[10] = m[10]/distz * voxSizeZ;

        at3Dorigin.set(m);*/

        newsource = new NewSourceAndConverterGetter(name, model, timePoint, voxSizeX, voxSizeY, voxSizeZ, factory).get();
    }
}
