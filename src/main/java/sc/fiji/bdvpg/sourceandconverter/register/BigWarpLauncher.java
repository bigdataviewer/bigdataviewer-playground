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
package sc.fiji.bdvpg.sourceandconverter.register;

import bdv.gui.BigWarpViewerOptions;
import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.util.ViewerPanelHandle;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import mpicbg.spim.data.SpimDataException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    List<SourceAndConverter> movingSources;
    List<SourceAndConverter> fixedSources;

    //List<SourceAndConverter> allRegisteredSources;

    SourceAndConverter gridSource;
    SourceAndConverter warpMagnitudeSource;

    SourceAndConverter[] warpedSources;


    final Map<ConverterSetup, double[]> displaysettings = new HashMap<>();

    public BigWarpLauncher(List<SourceAndConverter> movingSources, List<SourceAndConverter> fixedSources, String bigWarpName, List<ConverterSetup> allConverterSetups) {

        this.movingSources = movingSources;
        this.fixedSources = fixedSources;

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

            // Stores display settings before BigWarp
            allConverterSetups.forEach(setup -> displaysettings.put(setup, new double[]{setup.getDisplayRangeMin(), setup.getDisplayRangeMax()}));

            bwData = new BigWarp.BigWarpData(allSources, allConverterSetups, null, mvSrcIndices, fxSrcIndices);

    }

    boolean force2d = false;

    public void set2d() {
        force2d = true;
    }

    @Override
    public void run() {
        try {
            if (force2d) {
                bigWarp = new BigWarp(bwData, bigWarpName, BigWarpViewerOptions.options().is2D(true),null);
            } else {
                bigWarp = new BigWarp(bwData, bigWarpName, null);
            }
            // What does P and Q stand for ? Not sure about who's moving and who's fixed
            bdvHandleP = new ViewerPanelHandle(bigWarp.getViewerFrameP().getViewerPanel(), bigWarpName+"_Moving");
            bdvHandleQ = new ViewerPanelHandle(bigWarp.getViewerFrameQ().getViewerPanel(), bigWarpName+"_Fixed");

            warpedSources = new SourceAndConverter[movingSources.size()];

            for (int i=0;i<warpedSources.length;i++) {
                warpedSources[i] = bdvHandleP.getViewerPanel().state().getSources().get(i);
            }

            int nSources = bdvHandleP.getViewerPanel().state().getSources().size();
            gridSource = bdvHandleP.getViewerPanel().state().getSources().get(nSources-1);
            warpMagnitudeSource = bdvHandleP.getViewerPanel().state().getSources().get(nSources-2);

            // Restores display settings
            displaysettings.keySet().forEach(setup ->
                    setup.setDisplayRange(displaysettings.get(setup)[0], displaysettings.get(setup)[1])
            );

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

    public SourceAndConverter getGridSource() {
        return gridSource;
    }

    public SourceAndConverter getWarpMagnitudeSource() {
        return warpMagnitudeSource;
    }

    public SourceAndConverter[] getWarpedSources() {
        return warpedSources;
    }

}
