package sc.fiji.bdvpg.bdv.source.append;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.SourceAndConverter;

import java.util.function.Consumer;
import java.util.function.Function;

public class SourceAndConverterBdvAdder implements Runnable, Function<SourceAndConverter, BdvStackSource> {
    private final SourceAndConverter sacIn;
    private final BdvHandle bdvHandle;
    BdvStackSource bdvStackSource;

    public SourceAndConverterBdvAdder(BdvHandle bdvHandle, SourceAndConverter sacIn) {
        this.sacIn=sacIn;
        this.bdvHandle=bdvHandle;
    }

    public void run() {
        bdvStackSource = apply(sacIn);
    }

    public BdvStackSource getBdvStackSource() {
        return bdvStackSource;
    }

    @Override
    public BdvStackSource apply(SourceAndConverter sourceAndConverter) {
        int numTimePoints = 1;
        // IMO BdvFunctions.show should take care of the timepoints...
        final ConverterSetup converterSetup = bdvHandle.getSetupAssignments().getConverterSetups().get( 0 );
        return BdvFunctions.show( sourceAndConverter, numTimePoints, BdvOptions.options().addTo(bdvHandle) );
    }
}
