package sc.fiji.bdvpg.sourceandconverter;

import bdv.viewer.SourceAndConverter;

import java.util.stream.IntStream;

public class SourceAndConverterAndTimeRange {
    public final SourceAndConverter sac;
    private final int timePointBegin;
    private final int timePointEnd;

    public SourceAndConverterAndTimeRange(SourceAndConverter sac, int timePointBegin, int timePointEnd) {
        this.sac = sac;
        this.timePointBegin = timePointBegin;
        this.timePointEnd = timePointEnd;
    }

    public SourceAndConverterAndTimeRange(SourceAndConverter sac, int timePoint) {
        this.sac = sac;
        this.timePointBegin = timePoint;
        this.timePointEnd = timePoint+1;
    }

    public SourceAndConverter getSourceAndConverter() {
        return sac;
    }

    public IntStream getTimePoints() {
        return IntStream.range(timePointBegin, timePointEnd);
    }

}