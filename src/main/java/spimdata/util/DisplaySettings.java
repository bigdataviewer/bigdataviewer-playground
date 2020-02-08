package spimdata.util;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.base.NamedEntity;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

public class DisplaySettings extends NamedEntity implements Comparable< DisplaySettings >
{
    public int color;

    public double min;

    public double max;

    public boolean isSet = false;

    public DisplaySettings( final int id, final String name)
    {
        super( id, name );
    }

    public DisplaySettings( final int id )
    {
        this( id, Integer.toString( id ) );
    }

    /**
     * Get the unique id of this location.
     */
    @Override
    public int getId()
    {
        return super.getId();
    }

    /**
     * Get the name of this tile.
     *
     * The name is used for example to replace it in filenames when
     * opening individual 3d-stacks (e.g. SPIM_TL20_Tile1_Angle45.tif)
     */
    @Override
    public String getName()
    {
        return super.getName();
    }

    /**
     * Set the name of this tile.
     */
    @Override
    public void setName( final String name )
    {
        super.setName( name );
    }

    /**
     * Compares the {@link #getId() ids}.
     */
    @Override
    public int compareTo( final DisplaySettings o )
    {
        return getId() - o.getId();
    }

    protected DisplaySettings()
    {}

    /**
     * Stores display settings into the link SpimData object
     * @param sac
     */
    public static void PushDisplaySettings(SourceAndConverter sac) {
        if (SourceAndConverterServices
                .getSourceAndConverterService()
                .getMetadata(sac, SourceAndConverterService.SPIM_DATA_INFO)==null) {
            System.err.println("No Linked SpimData Object -> Display settings cannot be stored.");
            return;
        }

        int viewSetup =
                ((SourceAndConverterService.SpimDataInfo) SourceAndConverterServices
                        .getSourceAndConverterService()
                        .getMetadata(sac, SourceAndConverterService.SPIM_DATA_INFO)).setupId;

        SourceAndConverterService.SpimDataInfo sdi = (SourceAndConverterService.SpimDataInfo) SourceAndConverterServices
                .getSourceAndConverterService()
                .getMetadata(sac, SourceAndConverterService.SPIM_DATA_INFO);


        DisplaySettings ds = new DisplaySettings(viewSetup);

        // Color + min max
        if (sac.getConverter() instanceof ColorConverter) {
            ColorConverter cc = (ColorConverter) sac.getConverter();
            ds.setName("vs:" + viewSetup);
            ds.color = cc.getColor().get();
            ds.min = cc.getMin();
            ds.max = cc.getMax();
            ds.isSet = true;

        } else {
            System.err.println("Converter is of class :"+sac.getConverter().getClass().getSimpleName()+" -> Display settings cannot be stored.");
        }

        ((BasicViewSetup)sdi.asd.getSequenceDescription().getViewSetups().get(viewSetup)).setAttribute(ds);


    }

    public static void PullDisplaySettings(SourceAndConverter sac, DisplaySettings ds) {
        if (ds.isSet) {
            if (sac.getConverter() instanceof ColorConverter) {
                ColorConverter cc = (ColorConverter) sac.getConverter();
                cc.setColor(new ARGBType(ds.color));
                cc.setMin(ds.min);
                cc.setMax(ds.max);
                if (sac.asVolatile() != null) {
                    cc = (ColorConverter) sac.asVolatile().getConverter();
                    cc.setColor(new ARGBType(ds.color));
                    cc.setMin(ds.min);
                    cc.setMax(ds.max);
                }
            } else {
                System.err.println("Converter is of class :" + sac.getConverter().getClass().getSimpleName() + " -> Display settings cannot be reapplied.");
            }
        }
    }

}
