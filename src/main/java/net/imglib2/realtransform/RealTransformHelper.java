package net.imglib2.realtransform;

import com.google.gson.stream.JsonReader;
import net.imagej.DefaultDataset;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.scijava.Context;
import sc.fiji.persist.ScijavaGsonHelper;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

// TODO : move to bigdataviewer-playground
public class RealTransformHelper {

    public static List<RealTransform> getTransformSequence(RealTransformSequence rts) {
        return rts.transforms;
    }

    public static List<InvertibleRealTransform> getTransformSequence(InvertibleRealTransformSequence irts) {
        return irts.transforms;
    }

    public static RealTransform fromJson(Context ctx, File f) throws IOException {
        try (JsonReader reader = new JsonReader(new FileReader(f))) {
            return ScijavaGsonHelper.getGson(ctx).fromJson(reader, RealTransform.class);
        }
    }

    public static RealTransform fromJson(Context ctx, String filePath) throws IOException {
        return fromJson(ctx, new File(filePath));
    }

    public static void apply(DefaultDataset dataset, RealTransform transform) {

        Cursor<RealType<?>> xPosCursor = Views.hyperSlice(dataset, 0, 0).cursor();
        Cursor<RealType<?>> yPosCursor = Views.hyperSlice(dataset, 0, 1).cursor();
        Cursor<RealType<?>> zPosCursor = Views.hyperSlice(dataset, 0, 2).cursor();

        while (xPosCursor.hasNext()) {
            RealType<?> xPos = xPosCursor.next();
            RealType<?> yPos = yPosCursor.next();
            RealType<?> zPos = zPosCursor.next();

            double[] posSource = new double[]{xPos.getRealDouble(), yPos.getRealDouble(), zPos.getRealDouble()};
            double[] posDest = new double[]{0,0,0};

            transform.apply(posSource, posDest);

            xPos.setReal(posDest[0]);
            yPos.setReal(posDest[1]);
            zPos.setReal(posDest[2]);


        }

    }



}
