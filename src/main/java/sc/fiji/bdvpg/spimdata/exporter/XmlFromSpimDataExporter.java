/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package sc.fiji.bdvpg.spimdata.exporter;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.spimdata.EntityHandler;
import sc.fiji.bdvpg.spimdata.IEntityHandlerService;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_LOCATION;
import static sc.fiji.bdvpg.services.ISourceAndConverterService.SPIM_DATA_INFO;

public class XmlFromSpimDataExporter implements Runnable {

    protected static Logger logger = LoggerFactory.getLogger(XmlFromSpimDataExporter.class);

    AbstractSpimData spimData;

    String dataLocation;

    Context context;

    public static boolean isPathValid(String path) {
        try {
            Paths.get(path);
        } catch (InvalidPathException ex) {
            return false;
        }
        return true;
    }

    public XmlFromSpimDataExporter (AbstractSpimData spimData, String dataLocation, Context ctx) {
        this.spimData = spimData;
        if (isPathValid(dataLocation)) {
            spimData.setBasePath(new File(dataLocation));
        } else {
            logger.error("Trying to save spimdata into an invalid file Path : "+dataLocation);
        }
        this.dataLocation = dataLocation;
        this.context = ctx;
    }

    @Override
    public void run() {
        try {

            if (context!=null) {
                //System.out.println(" Handling scijava extra attributes");
                // We can handle the extra attributes, see {@link IEntityHandlerService}
                Map<Class<? extends Entity>, EntityHandler> entityClassToHandler = new HashMap<>();

                IEntityHandlerService entityHandlerService = context.getService(IEntityHandlerService.class);

                // For convenience : map setup id with sacs
                Map<Integer, SourceAndConverter<?>> idToSac = new HashMap<>();

                SourceAndConverterService sac_service = context.getService(SourceAndConverterService.class);

                sac_service.getSourceAndConverterFromSpimdata(spimData).forEach(sac -> {
                        SourceAndConverterService.SpimDataInfo sdi = (SourceAndConverterService.SpimDataInfo) sac_service.getMetadata(sac, SPIM_DATA_INFO);
                        idToSac.put(sdi.setupId, sac);
                });

                entityHandlerService.getHandlers(EntityHandler.class).forEach(pi -> {
                    try {
                        EntityHandler handler = pi.createInstance();
                        entityClassToHandler.put(handler.getEntityType(), handler);
                        //log.accept("Plugin found for entity class "+handler.getEntityType().getSimpleName());
                    } catch (InstantiableException e) {
                        e.printStackTrace();
                    }
                });

                final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();

                for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() ) {

                    // Execute {@link EntityHandler}, if a compatible entity is found in the spimdata, compatible with an entity class handler
                    entityClassToHandler.keySet().forEach(entityClass -> {
                        Entity e = setup.getAttribute(entityClass);
                        if (e!=null) {
                            SourceAndConverter<?> sac = idToSac.get(setup);
                            entityClassToHandler.get(entityClass).writeEntity(setup, sac);//.loadEntity(asd, setup);
                        }
                    });
                }
            }

            SourceAndConverterServices.getSourceAndConverterService().setSpimDataName(spimData, dataLocation);
            SourceAndConverterServices.getSourceAndConverterService().setMetadata(spimData, SPIM_DATA_LOCATION, dataLocation);

            if (spimData instanceof SpimData) {
                (new XmlIoSpimData()).save((SpimData) spimData, dataLocation);
            } else if (spimData instanceof SpimDataMinimal) {
                (new XmlIoSpimDataMinimal()).save((SpimDataMinimal) spimData, dataLocation);
            } else {
                logger.error("Cannot save SpimData of class : "+spimData.getClass().getSimpleName());
                return;
            }


        } catch (SpimDataException e) {
            e.printStackTrace();
        }
    }

}
