/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2021 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_LOCATION;

public class XmlFromSpimDataExporter implements Runnable {

    AbstractSpimData spimData;

    String dataLocation;

    public static boolean isPathValid(String path) {
        try {
            Paths.get(path);
        } catch (InvalidPathException ex) {
            return false;
        }
        return true;
    }

    public XmlFromSpimDataExporter ( AbstractSpimData spimData, String dataLocation) {
        this.spimData = spimData;
        if (isPathValid(dataLocation)) {
            spimData.setBasePath(new File(dataLocation));
        } else {
            System.out.println("Trying to save spimdata into an invalid file Path : "+dataLocation);
        }
        this.dataLocation = dataLocation;
    }

    @Override
    public void run() {
        try {

            if (spimData instanceof SpimData) {
                (new XmlIoSpimData()).save((SpimData) spimData, dataLocation);
            } else if (spimData instanceof SpimDataMinimal) {
                (new XmlIoSpimDataMinimal()).save((SpimDataMinimal) spimData, dataLocation);
            } else {
                System.err.println("Cannot save SpimData of class : "+spimData.getClass().getSimpleName());
                return;
            }

            SourceAndConverterServices.getSourceAndConverterService().setSpimDataName(spimData, dataLocation);
            SourceAndConverterServices.getSourceAndConverterService().setMetadata(spimData, SPIM_DATA_LOCATION, dataLocation);

        } catch (SpimDataException e) {
            e.printStackTrace();
        }
    }

}
