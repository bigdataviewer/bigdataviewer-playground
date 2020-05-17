package sc.fiji.bdvpg.scijava.command.source;

import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformEventHandler3D;
import net.imglib2.util.LinAlgHelpers;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

/**
 * Not clever but consistent : always append transform which acts as if it was inserted at first position
 * Maybe not good numerically speaking - but at least it's consistent and there's
 * no special case depending on the type of the SourceAndConverter
 */

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Transform>Basic Transformation")
public class BasicTransformerCommand implements Command {
    @Parameter
    SourceAndConverter[] sources_in;

    @Parameter(choices = {"Flip", "Rot90", "Rot180", "Rot270"})
    String type;

    @Parameter(choices = {"X", "Y", "Z"})
    String axis;

    @Parameter
    int timepoint;

    @Parameter
    boolean globalChange;

    @Override
    public void run() {
        for (SourceAndConverter sac : sources_in) {
            if (!globalChange) {
                throw new UnsupportedOperationException("TODO - contact the authors of bigdataviewer-playground in github!");
            } else {
                AffineTransform3D at3D = new AffineTransform3D();
                at3D.identity();
                switch (type) {
                    case "Flip": flip(at3D );
                    break;
                    case "Rot90": rot(1, at3D );
                    break;
                    case "Rot180": rot(2, at3D );
                    break;
                    case "Rot270": rot(3, at3D );
                    break;
                }
                SourceAndConverterUtils.append(at3D, sac);
            }
            /*
            // Get affine transform
            AffineTransform3D at3D = new AffineTransform3D();
            sac.getSpimSource().getSourceTransform(timepoint,0,at3D);
            // Get dimension
            long[] dims = new long[3];
            sac.getSpimSource().getSource(timepoint,0).dimensions(dims);
            AffineTransform3D at3DInverse = at3D.inverse().copy();

            AffineTransform3D transform3D = new AffineTransform3D();



            // Apply : append
            AffineTransform3D at3DOut = new AffineTransform3D();
            SourceAndConverterUtils.append(at3DOut, sac);
            */
        }
        SourceAndConverterServices.getSourceAndConverterDisplayService()
                .updateDisplays(sources_in);
    }

    private void flip(AffineTransform3D at3D) {
        switch (axis) {
            case "X":
                at3D.set(-1,0,0);
                break;
            case "Y":
                at3D.set(-1,1,1);
                break;
            case "Z":
                at3D.set(-1,2,2);
                break;
        }
    }

    private void rot(int quarterTurn, AffineTransform3D at3D) {
        switch (axis) {
            case "X":
                at3D.rotate(0,((double)quarterTurn)*Math.PI/180.0);
                break;
            case "Y":
                at3D.rotate(1,((double)quarterTurn)*Math.PI/180.0);
                break;
            case "Z":
                at3D.rotate(2,((double)quarterTurn)*Math.PI/180.0);
                break;
        }
    }
}
