package sc.fiji.bdvpg;

import bdv.BigDataViewer;
import bdv.util.BdvHandle;
import bigwarp.BigWarp;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import org.scijava.util.VersionUtils;

public class ShowVersions {

    static public void main(String... args) {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // Display versions handled by the parent pom

        System.out.println("imglib2 version = "+VersionUtils.getVersion(RandomAccessibleInterval.class)); // imglib2
        System.out.println("bigwarp version = "+VersionUtils.getVersion(BigWarp.class)); // bigwarp
        System.out.println("bdv vistools version = "+VersionUtils.getVersion(BdvHandle.class)); // bigwarp
        System.out.println("bdv version = "+VersionUtils.getVersion(BigDataViewer.class)); // bdv
        System.out.println("imglib2-cache version = "+VersionUtils.getVersion(CachedCellImg.class)); // bdv

    }


}
