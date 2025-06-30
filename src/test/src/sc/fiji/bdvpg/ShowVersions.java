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
package sc.fiji.bdvpg;

import bdv.BigDataViewer;
import bdv.util.BdvHandle;
import bigwarp.BigWarp;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.realtransform.RealTransform;
import org.scijava.util.VersionUtils;

public class ShowVersions {

    static public void main(String... args) {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        TestHelper.startFiji(ij);//ij.ui().showUI();

        // Display versions handled by the parent pom

        System.out.println("imglib2 version = "+VersionUtils.getVersion(RandomAccessibleInterval.class)); // imglib2
        System.out.println("bigwarp version = "+VersionUtils.getVersion(BigWarp.class)); // bigwarp
        System.out.println("bdv vistools version = "+VersionUtils.getVersion(BdvHandle.class)); // bigwarp
        System.out.println("bdv version = "+VersionUtils.getVersion(BigDataViewer.class)); // bdv
        System.out.println("imglib2-cache version = "+VersionUtils.getVersion(CachedCellImg.class)); // bdv
        System.out.println("imglib2-realtransform version = "+VersionUtils.getVersion(RealTransform.class)); // imglib2

    }


}
