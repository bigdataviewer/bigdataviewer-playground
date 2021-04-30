package sc.fiji.bdvpg.services.serializers.bdv;

import bdv.util.AxisOrder;
import bdv.util.BdvOptions;
import bdv.viewer.render.AccumulateProjectorFactory;
import net.imglib2.type.numeric.ARGBType;

/**
 * Because BdvOptions is not directly serializable
 */
public class SerializableBdvOptions {

    int width = -1;

    int height = -1;

    double[] screenScales = new double[] { 1, 0.75, 0.5, 0.25, 0.125 };

    long targetRenderNanos = 30 * 1000000l;

    int numRenderingThreads = 3;

    int numSourceGroups = 10;

    String frameTitle = "BigDataViewer";

    boolean is2D = false;

    AxisOrder axisOrder = AxisOrder.DEFAULT;

    // Extra for the playground
    boolean interpolate = false;

    int numTimePoints = 1;

    AccumulateProjectorFactory<ARGBType> accumulateProjectorFactory = new DefaultAccumulatorFactory();

    // Not serialized
    //private TransformEventHandlerFactory transformEventHandlerFactory = TransformEventHandler3D::new;
    //private InputTriggerConfig inputTriggerConfig = null;
    //private final AffineTransform3D sourceTransform = new AffineTransform3D();

    public SerializableBdvOptions() {
    }

    public BdvOptions getBdvOptions() {
        BdvOptions o =
                BdvOptions.options()
                .screenScales(this.screenScales)
                .targetRenderNanos(this.targetRenderNanos)
                .numRenderingThreads(this.numRenderingThreads)
                .numSourceGroups(this.numSourceGroups)
                .axisOrder(this.axisOrder)
                .preferredSize(this.width, this.height)
                .frameTitle(this.frameTitle);
        if (this.accumulateProjectorFactory!=null) {
            o = o.accumulateProjectorFactory(this.accumulateProjectorFactory);
        }
        if (this.is2D) o = o.is2D();

        return o;
    }

}
