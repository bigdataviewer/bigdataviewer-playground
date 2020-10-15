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
import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.base.XmlIoEntity;
import org.jdom2.Element;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.base.ViewSetupAttributeIo;
import sc.fiji.bdvpg.bdv.projector.Projection;

@ViewSetupAttributeIo( name = "displaysettings", type = Displaysettings.class )
public class XmlIoDisplaysettings extends XmlIoEntity<Displaysettings>
{

    public static final String DISPLAYSETTINGS_XML_TAG = "Displaysettings";

    public XmlIoDisplaysettings()
    {
        super( DISPLAYSETTINGS_XML_TAG, Displaysettings.class );
    }

    @Override
    public Element toXml( final Displaysettings ds )
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
    public Displaysettings fromXml(final Element elem ) throws SpimDataException
    {
        final Displaysettings ds = super.fromXml( elem );
        ds.isSet = XmlHelpers.getBoolean(elem, "isset");
        ds.color = XmlHelpers.getIntArray(elem, "color");
        ds.min = XmlHelpers.getDouble(elem, "min");
        ds.max = XmlHelpers.getDouble(elem, "max");
        ds.projectionMode = XmlHelpers.getText(elem, Projection.PROJECTION_MODE_XML);
        return ds;
    }
}
