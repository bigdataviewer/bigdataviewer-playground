package sc.fiji.bdvpg.source.register;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.util.ViewerPanelHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import bigwarp.BigWarpInit;
import mpicbg.spim.data.SpimDataException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Can launch BigWarp with:
 * - Two List of Sources
 * - Two lists of SourceAndConverter
 *
 * Output:
 * - The two BdvHandle
 *
 * Limitation :
 * - when using SourceAndConverter, ConverterSetup are not transfered (but that should be doable hopefully
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

    public BigWarpLauncher(Source movingSource, Source fixedSource, String bigWarpName) {
        this(Arrays.asList(movingSource), Arrays.asList(fixedSource), bigWarpName);
    }

    public BigWarpLauncher(SourceAndConverter movingSource, SourceAndConverter fixedSource, String bigWarpName) {
        this(Arrays.asList(movingSource), Arrays.asList(fixedSource), bigWarpName);
    }

    // Issue with constructor :
    // Making a constructor with lists of SourceAndConverters:
    // public BigWarpLauncher(List<SourceAndConverter> movingSources, List<SourceAndConverter> fixedSources)
    // And a constructor with lists of Source:
    // public BigWarpLauncher(List<Source> movingSources, List<Source> fixedSources)
    // Do not work because constructors have same 'erasure'
    // Option choosen -> a single constructor which checks the types of their inner objects
    // Alternative maybe better option :
    // Use array : Source[] or SourceAndConverter[] (and maybe this issue was the reason for BigWarp choosing this in the beginning)

    public BigWarpLauncher(List movingSources, List fixedSources, String bigWarpName) {

        this.bigWarpName = bigWarpName;

        if (isListMadeOfSource(movingSources)&&(isListMadeOfSource(fixedSources))) {

            // Get names of Sources
            String[] names = new String[movingSources.size()+fixedSources.size()];

            for (int i=0;i<movingSources.size();i++) {
                names[i] = ((Source) movingSources.get(i)).getName();
            }

            for (int i=0;i<fixedSources.size();i++) {
                names[i+movingSources.size()] = ((Source) fixedSources.get(i)).getName();
            }

            // Initializes BigWarpData
            bwData = BigWarpInit.createBigWarpData((Source[]) (movingSources.toArray()), (Source[])(fixedSources.toArray()), names);

        } else if (isListMadeOfSourceAndConverter(movingSources)&&(isListMadeOfSourceAndConverter(movingSources))) {

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

            List<ConverterSetup> allConverterSetups = new ArrayList<>();

            bwData = new BigWarp.BigWarpData(allSources, allConverterSetups, null, mvSrcIndices, fxSrcIndices);

        } else {
            System.err.println("Invalid class for BigWarpLauncher initialization");
        }

    }

    public boolean isListMadeOfSource(List list) {
        for( Object o : list){
            if ((o instanceof Source)==false) return false;
        }
        return true;
    }

    public boolean isListMadeOfSourceAndConverter(List list) {
        for( Object o : list){
            if ((o instanceof SourceAndConverter)==false) return false;
        }
        return true;
    }

    @Override
    public void run() {
        try {
            bigWarp = new BigWarp(bwData, bigWarpName, null);
            // What does P and Q stand for ? Not sure about who's moving and who's fixed
            bdvHandleP = new ViewerPanelHandle(bigWarp.getViewerFrameP().getViewerPanel(), bigWarpName+"_Moving");
            bdvHandleQ = new ViewerPanelHandle(bigWarp.getViewerFrameQ().getViewerPanel(), bigWarpName+"_Fixed");
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
