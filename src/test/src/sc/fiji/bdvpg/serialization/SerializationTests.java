package sc.fiji.bdvpg.serialization;

import com.google.gson.Gson;
import net.imagej.ImageJ;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sc.fiji.bdvpg.TestHelper;
import net.imglib2.realtransform.AffineTransform3DRunTimeAdapter;
import sc.fiji.bdvpg.bdv.supplier.DefaultBdvSupplierAdapter;
import sc.fiji.bdvpg.bdv.supplier.DefaultBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.SerializableBdvOptions;
import sc.fiji.persist.ScijavaGsonHelper;

public class SerializationTests {

    static ImageJ ij;
    static Gson gson;

    @Before
    public void openFiji() {
        // Initializes static SourceService and Display Service and plugins for serialization
        ij = new ImageJ();
        ij.ui().showUI();
        gson = ScijavaGsonHelper.getGson(ij.context());
    }

    @After
    public void closeFiji() {
        TestHelper.closeFijiAndBdvs(ij);
    }

    /**
     * Test:
     * {@link DefaultBdvSupplierAdapter}
     */
    @Test
    public void testBdvSupplierSerialization() {
        DefaultBdvSupplier bdvSupplier = new DefaultBdvSupplier(new SerializableBdvOptions());
        testSerialization(gson, bdvSupplier, DefaultBdvSupplier.class);
    }

    /**
     * Test:
     * {@link AffineTransform3DRunTimeAdapter}
     */
    @Test
    public void testAffineTransformSerialization() {
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.scale(2);
        testSerialization(gson, at3D, AffineTransform3D.class); // This needs to work
        testSerialization(gson, at3D, RealTransform.class); // This needs to work
    }

    // TODO : more unit serialization tests!

    /**
     * Just makes a loop serialize / deserialize / reserialize and checks
     * whether the string representation is identical
     *
     * @param gson serializer/deserializer
     * @param o object to serialize and deserialize
     * @param c class of the object
     */
    public static void testSerialization(Gson gson, Object o, Class c) {
        String json = gson.toJson(o, c);
        System.out.println(json);
        Object oRestored = gson.fromJson(json, c);
        String json2 = gson.toJson(oRestored, c);
        System.out.println(json2);
        Assert.assertEquals(json, json2);
    }

}
