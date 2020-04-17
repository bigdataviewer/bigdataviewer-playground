package spimdata.util;
import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.base.XmlIoEntity;
import mpicbg.spim.data.generic.base.XmlIoNamedEntity;
import org.jdom2.Element;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.base.ViewSetupAttributeIo;
import sc.fiji.bdvpg.bdv.projector.Projection;

@ViewSetupAttributeIo( name = "displaysettings", type = DisplaySettings.class )
public class XmlIoDisplaySettings extends XmlIoEntity< DisplaySettings >
{
    public XmlIoDisplaySettings()
    {
        super( "displaysettings", DisplaySettings.class );
    }

    @Override
    public Element toXml( final DisplaySettings ds )
    {
        final Element elem = super.toXml( ds );

        elem.addContent(XmlHelpers.booleanElement("isset", ds.isSet));
        elem.addContent(XmlHelpers.intArrayElement("color", ds.color));
        elem.addContent(XmlHelpers.doubleElement("min", ds.min));
        elem.addContent(XmlHelpers.doubleElement("max", ds.max));
        elem.addContent(XmlHelpers.textElement(Projection.PROJECTION_MODE_XML, ds.projectionMode));
        return elem;
    }

    @Override
    public DisplaySettings fromXml( final Element elem ) throws SpimDataException
    {
        final DisplaySettings ds = super.fromXml( elem );
        ds.isSet = XmlHelpers.getBoolean(elem, "isset");
        ds.color = XmlHelpers.getIntArray(elem, "color");
        ds.min = XmlHelpers.getDouble(elem, "min");
        ds.max = XmlHelpers.getDouble(elem, "max");
        ds.projectionMode = XmlHelpers.getText(elem, Projection.PROJECTION_MODE_XML);
        return ds;
    }
}