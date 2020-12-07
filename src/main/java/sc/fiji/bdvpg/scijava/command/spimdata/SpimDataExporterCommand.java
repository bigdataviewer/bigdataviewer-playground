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
package sc.fiji.bdvpg.scijava.command.spimdata;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.spimdata.exporter.XmlFromSpimDataExporter;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.io.File;

@Plugin( type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDVDataset>Save BDVDataset" )
public class SpimDataExporterCommand implements Command {

    // To get associated spimdata
    @Parameter(label = "Select source(s)")
    SourceAndConverter sac;

    @Parameter(label = "Output File (XML)")
    public File xmlFilePath;

    public void run() {

        if (SourceAndConverterServices.getSourceAndConverterService()
                .getMetadata(sac, SourceAndConverterService.SPIM_DATA_INFO)==null) {
            System.err.println("No BDVDataset associated to the chosen source - Aborting save command");
            return;
        }

        AbstractSpimData asd =
                ((SourceAndConverterService.SpimDataInfo)SourceAndConverterServices.getSourceAndConverterService()
                .getMetadata(sac, SourceAndConverterService.SPIM_DATA_INFO)).asd;

        new XmlFromSpimDataExporter(asd, xmlFilePath.getAbsolutePath()).run();
    }

}
