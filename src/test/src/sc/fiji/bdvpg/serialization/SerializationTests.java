package sc.fiji.bdvpg.serialization;

import com.google.gson.Gson;
import net.imagej.ImageJ;
import net.imglib2.realtransform.AffineTransform3D;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sc.fiji.bdvpg.TestHelper;
import sc.fiji.bdvpg.scijava.adapter.transform.AffineTransform3DAdapter;
import sc.fiji.bdvpg.scijava.adapter.bdv.DefaultBdvSupplierAdapter;
import sc.fiji.bdvpg.bdv.supplier.DefaultBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.SerializableBdvOptions;
import sc.fiji.serializers.ScijavaGsonHelper;

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
     * {@link AffineTransform3DAdapter}
     */
    @Test
    public void testAffineTransformSerialization() {
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.scale(2);
        testSerialization(gson, at3D, AffineTransform3D.class);
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
        String json = gson.toJson(o);
        System.out.println(json);
        Object oRestored = gson.fromJson(json, c);
        String json2 = gson.toJson(oRestored);
        System.out.println(json2);
        Assert.assertEquals(json, json2);
    }
}
