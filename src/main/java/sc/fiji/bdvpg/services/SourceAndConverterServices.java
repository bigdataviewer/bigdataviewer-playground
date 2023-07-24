/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2023 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.bdvpg.services;

import sc.fiji.bdvpg.scijava.services.BDVService;

/**
 * Static methods to access BdvSourceAndConverterService and
 * BdvSourceAndConverterDisplayService
 *
 * Should ideally not be used:
 * - try to fetch a {@link sc.fiji.bdvpg.scijava.services.SourceAndConverterService} or a
 * - {@link BDVService}
 * from a scijava {@link org.scijava.Context}
 * instead
 */

public class SourceAndConverterServices {

	private static ISourceAndConverterService sourceAndConverterService;

	private static BDVService BDVService;

	public static ISourceAndConverterService getSourceAndConverterService() {
		return sourceAndConverterService;
	}

	public static void setSourceAndConverterService(
		ISourceAndConverterService sourceAndConverterService)
	{
		SourceAndConverterServices.sourceAndConverterService =
			sourceAndConverterService;
	}

	public static BDVService getBDVService() {
		return BDVService;
	}

	public static void setBDVService(
		BDVService BDVService)
	{
		SourceAndConverterServices.BDVService =
				BDVService;
	}
}
