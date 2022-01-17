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
package sc.fiji.bdvpg.scijava.command.source;

import bdv.viewer.SourceAndConverter;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAdjuster;

import java.text.DecimalFormat;

import static org.scijava.ItemVisibility.MESSAGE;

/**
 *
 * @author Nicolas Chiaruttini, EPFL 2020
 */

@Plugin(type = BdvPlaygroundActionCommand.class, initializer = "init",  menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Display>Set Sources Brightness (Interactive)")
public class InteractiveBrightnessAdjusterCommand extends InteractiveCommand implements BdvPlaygroundActionCommand {

    @Parameter(label = "Sources :", required = false, description = "Label the sources controlled by this window", persist = false)
    String customsourcelabel = "Label your sources here";

    @Parameter(label = "Select Source(s)")
    SourceAndConverter[] sacs;

    @Parameter(visibility=MESSAGE, required=false, style = "text field")
    String message = "Display Range [ NaN - NaN ]";

    @Parameter(callback = "updateMessage")
    double min;

    @Parameter(callback = "updateMessage")
    double max;

    @Parameter(label = "relative Minimum", style = "slider", min = "0", max = "1000", callback = "updateMessage")
    double minslider;

    @Parameter(label = "relative Maximum", style = "slider", min = "0", max = "1000", callback = "updateMessage")
    double maxslider;

    boolean firstTimeCalled = true;

    boolean secondTimeCalled = true;

    public void run() {
        if ((!firstTimeCalled)&&(!secondTimeCalled)) {
            double minValue = min + minslider /1000.0*(max-min);
            double maxValue = min + maxslider /1000.0*(max-min);
            for (SourceAndConverter source:sacs) {
                new BrightnessAdjuster(source, minValue, maxValue).run();
            }
        } else {
            init();
            if (firstTimeCalled) {
                firstTimeCalled = false;
            } else if (secondTimeCalled) {
                secondTimeCalled = false;
            }
        }
    }

    DecimalFormat formatter = new DecimalFormat("#.###");

    public void updateMessage() {
        formatter.setMinimumFractionDigits(3);
        double minValue = min + minslider /1000.0*(max-min);
        double maxValue = min + maxslider /1000.0*(max-min);
        message = "Display Range ["+ formatter.format(minValue) +" - "+ formatter.format(maxValue) +"]";
    }


    public void init() {
        if (sacs!=null)
        if (sacs.length>0) {
            double minSource = SourceAndConverterServices.getSourceAndConverterService().getConverterSetup(sacs[0]).getDisplayRangeMin();
            double maxSource = SourceAndConverterServices.getSourceAndConverterService().getConverterSetup(sacs[0]).getDisplayRangeMax();

            if (minSource>=0) {
                min = 0;
            } else {
                min = minSource;
            }
            if (maxSource>65535) {
                max = maxSource;
            } else if (maxSource>255) {
                max = 65535;
            } else if (maxSource>1){
                max = 255;
            } else {
                max = 1;
            }
            minslider = (minSource-min)/(max-min)*1000;
            maxslider = (maxSource-min)/(max-min)*1000;
            message = "Display Range [ NaN - NaN ]";
        }
    }
}
