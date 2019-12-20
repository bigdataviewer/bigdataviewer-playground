package sc.fiji.bdvpg.bdv.source.display;

import bdv.tools.brightness.MinMaxGroup;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import sc.fiji.bdvpg.bdv.BdvUtils;

import java.util.function.Consumer;

public class BrightnessAdjuster implements Runnable, Consumer<Source> {

    BdvHandle bdvHandle;
    Source< ? > source;
    double minDisplayValue;
    double maxDisplayValue;

    public BrightnessAdjuster(final BdvHandle bdvHandle, final Source< ? > source, double min, double max )
    {
        this.bdvHandle = bdvHandle;
        this.source = source;
        minDisplayValue = min;
        maxDisplayValue = max;
    }

    @Override
    public void run() {
        accept(source);
    }

    @Override
    public void accept(Source src) {
        final MinMaxGroup minMaxGroup = BdvUtils.getMinMaxGroup( bdvHandle, src );
        minMaxGroup.getMinBoundedValue().setCurrentValue( minDisplayValue );
        minMaxGroup.getMaxBoundedValue().setCurrentValue( maxDisplayValue );
    }
}
