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
package spimdata.util;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.base.NamedEntity;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import sc.fiji.bdvpg.bdv.projector.Projection;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

/**
 * Entity which stores the display settings of a view setup
 *
 * limited to simple colored LUT + min max display
 *
 * also stores the projection mode
 *
 */

public class Displaysettings extends NamedEntity implements Comparable<Displaysettings>
{
    // RGBA value
    public int[] color = new int[] {255,255,255,0}; // Initialization avoids null pointer exception

    // min display value
    public double min = 0;

    // max display value
    public double max = 255;

    // if isset is false, the display value is discarded
    public boolean isSet = false;

    // stores projection mode
    public String projectionMode = Projection.PROJECTION_MODE_SUM; // Default projection mode

    public Displaysettings(final int id, final String name)
    {
        super( id, name );
    }

    public Displaysettings(final int id )
    {
        this( id, Integer.toString( id ) );
    }

    /**
     * Get the unique id of this displaysettings
     */
    @Override
    public int getId()
    {
        return super.getId();
    }

    /**
     * Get the name of this Display Settings Entity.
     */
    @Override
    public String getName()
    {
        return super.getName();
    }

    /**
     * Set the name of this displaysettings (probably useless).
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
    public int compareTo( final Displaysettings o )
    {
        return getId() - o.getId();
    }

    protected Displaysettings()
    {}

    /**
     * Stores display settings currently in use by the SourceAndConverter into the link SpimData object
     * @param sac
     */
    public static void GetDisplaySettingsFromCurrentConverter(SourceAndConverter sac, Displaysettings ds) {

        // Color + min max
        if (sac.getConverter() instanceof ColorConverter) {
            ColorConverter cc = (ColorConverter) sac.getConverter();
            ds.setName("vs:" + ds.getId());
            int colorCode = cc.getColor().get();
            ds.color = new int[]{
                    ARGBType.red(colorCode),
                    ARGBType.green(colorCode),
                    ARGBType.blue(colorCode),
                    ARGBType.alpha(colorCode)};
            ds.min = cc.getMin();
            ds.max = cc.getMax();
            ds.isSet = true;
        } else {
            System.err.println("Converter is of class :"+sac.getConverter().getClass().getSimpleName()+" -> Display settings cannot be stored.");
        }

        if (SourceAndConverterServices
                .getSourceAndConverterService()
                .getMetadata(sac, Projection.PROJECTION_MODE)!=null) {
            // A projection mode is set
            ds.projectionMode = (String) (SourceAndConverterServices
                    .getSourceAndConverterService()
                    .getMetadata(sac, Projection.PROJECTION_MODE));
        }
    }

    /**
     * Stores display settings currently in use by the SourceAndConverter into the link SpimData object
     * @param sac
     */
    public static void PushDisplaySettingsFromCurrentConverter(SourceAndConverter sac) {
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

        Displaysettings ds = new Displaysettings(viewSetup);

        GetDisplaySettingsFromCurrentConverter(sac, ds);

        ((BasicViewSetup)sdi.asd.getSequenceDescription().getViewSetups().get(viewSetup)).setAttribute(ds);

    }

    /**
     * Apply the display settings to the SourceAndConverter object
     * @param sac
     */
    public static void PullDisplaySettings(SourceAndConverter sac, Displaysettings ds) {

        if (ds.isSet) {
            if (sac.getConverter() instanceof ColorConverter) {
                ColorConverter cc = (ColorConverter) sac.getConverter();
                cc.setColor(new ARGBType(ARGBType.rgba(ds.color[0], ds.color[1], ds.color[2], ds.color[3])));
                cc.setMin(ds.min);
                cc.setMax(ds.max);
                if (sac.asVolatile() != null) {
                    cc = (ColorConverter) sac.asVolatile().getConverter();
                    cc.setColor(new ARGBType(ARGBType.rgba(ds.color[0], ds.color[1], ds.color[2], ds.color[3])));
                    cc.setMin(ds.min);
                    cc.setMax(ds.max);
                }
            } else {
                System.err.println("Converter is of class :" + sac.getConverter().getClass().getSimpleName() + " -> Display settings cannot be reapplied.");
            }

            SourceAndConverterServices
                    .getSourceAndConverterService()
                    .setMetadata(sac, Projection.PROJECTION_MODE, ds.projectionMode);

        }
    }

    /**
     * More meaningful String representation of DisplaySettings
     * @return
     */
    public String toString() {
        String str = "";
        str+="set = "+this.isSet+", ";

        if (this.projectionMode!=null)
            str+="set = "+this.projectionMode+", ";

        if (this.color!=null) {
            str += "color = ";
            for (int i = 0; i < this.color.length;i++) {
                str += this.color[i] + ", ";
            }
        }

        str+="min = "+this.min+", ";

        str+="max = "+this.max;

        return str;
    }

}
