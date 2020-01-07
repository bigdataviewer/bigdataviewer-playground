package sc.fiji.bdvpg.sourceandconverter.register;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.util.ViewerPanelHandle;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import mpicbg.spim.data.SpimDataException;

import java.util.ArrayList;
import java.util.List;

/**
 * Can launch BigWarp with:
 * - Two lists of SourceAndConverter
 *
 * Output:
 * - The two BdvHandle
 *
 * Limitation :
 * - Cache is null in BigWarpInit
 *
 * In order to retrieve the transform, TODO
 */

public class BigWarpLauncher implements Runnable {

    BigWarp.BigWarpData<?> bwData;

    BigWarp<?> bigWarp;

    String bigWarpName;

    BdvHandle bdvHandleP;

    BdvHandle bdvHandleQ;

    // Issue with constructor :
    // Making a constructor with lists of SourceAndConverters:
    // public BigWarpLauncher(List<SourceAndConverter> movingSources, List<SourceAndConverter> fixedSources)
    // And a constructor with lists of Source:
    // public BigWarpLauncher(List<Source> movingSources, List<Source> fixedSources)
    // Do not work because constructors have same 'erasure'
    // Option choosen -> a single constructor which checks the types of their inner objects
    // Alternative maybe better option :
    // Use array : Source[] or SourceAndConverter[] (and maybe this issue was the reason for BigWarp choosing this in the beginning)

    public BigWarpLauncher(List<SourceAndConverter> movingSources, List<SourceAndConverter> fixedSources, String bigWarpName, List<ConverterSetup> allConverterSetups) {

        this.bigWarpName = bigWarpName;

            List<SourceAndConverter> allSources = new ArrayList<>();
            allSources.addAll(movingSources);
            allSources.addAll(fixedSources);

            int[] mvSrcIndices = new int[movingSources.size()];
            for (int i = 0; i < movingSources.size(); i++) {
                mvSrcIndices[i] = i;
            }

            int[] fxSrcIndices = new int[fixedSources.size()];
            for (int i = 0; i < fixedSources.size(); i++) {
                fxSrcIndices[i] = i+movingSources.size();
            }

            if (allConverterSetups==null) {
                allConverterSetups = new ArrayList<>();
            }

            bwData = new BigWarp.BigWarpData(allSources, allConverterSetups, null, mvSrcIndices, fxSrcIndices);

    }

    @Override
    public void run() {
        try {
            bigWarp = new BigWarp(bwData, bigWarpName, null);
            // What does P and Q stand for ? Not sure about who's moving and who's fixed
            bdvHandleP = new ViewerPanelHandle(bigWarp.getViewerFrameP().getViewerPanel(), bigWarp.getSetupAssignments(), bigWarpName+"_Moving");
            bdvHandleQ = new ViewerPanelHandle(bigWarp.getViewerFrameQ().getViewerPanel(), bigWarp.getSetupAssignments(), bigWarpName+"_Fixed");
        } catch (SpimDataException e) {
            e.printStackTrace();
        }
    }

    // Outputs

    public BdvHandle getBdvHandleP() {
        return bdvHandleP;
    }

    public BdvHandle getBdvHandleQ() {
        return bdvHandleQ;
    }

    public BigWarp getBigWarp() {
        return bigWarp;
    }

}
