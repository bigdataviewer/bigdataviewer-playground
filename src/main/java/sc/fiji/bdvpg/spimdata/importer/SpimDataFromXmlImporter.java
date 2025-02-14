/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2025 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.bdvpg.spimdata.importer;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.io.File;
import java.nio.file.Paths;
import java.util.function.Function;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_LOCATION;

public class SpimDataFromXmlImporter implements Runnable,
	Function<String, AbstractSpimData<?>>
{

	protected static final Logger logger = LoggerFactory.getLogger(
		SpimDataFromXmlImporter.class);

	final String dataLocation;

	public SpimDataFromXmlImporter(File file) {
		this.dataLocation = file.getAbsolutePath();
	}

	public SpimDataFromXmlImporter(String dataLocation) {
		this.dataLocation = dataLocation;
	}

	@Override
	public void run() {
		apply(dataLocation);
	}

	public AbstractSpimData<?> get() {
		return apply(dataLocation);
	}

	@Override
	public AbstractSpimData<?> apply(String dataLocation) {
		AbstractSpimData<?> sd = null;
		try {
			sd = new XmlIoSpimData().load(dataLocation);
			SourceAndConverterServices.getSourceAndConverterService().register(sd);
			SourceAndConverterServices.getSourceAndConverterService().setSpimDataName(sd, Paths.get(dataLocation).getFileName().toString());
			SourceAndConverterServices.getSourceAndConverterService().setMetadata(sd, SPIM_DATA_LOCATION, dataLocation);
		}
		catch (SpimDataException e) {
			e.printStackTrace();
		}
		return sd;
	}

}
