package sc.fiji.bdvpg.serialization;

import com.google.gson.Gson;
import net.imagej.ImageJ;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sc.fiji.bdvpg.TestHelper;
import sc.fiji.bdvpg.services.serializers.bdv.DefaultBdvSupplier;
import sc.fiji.bdvpg.services.serializers.bdv.SerializableBdvOptions;
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

    @Test
    public void testBdvSupplierSerialization() {
        DefaultBdvSupplier bdvSupplier = new DefaultBdvSupplier(new SerializableBdvOptions());
        testSerialization(gson, bdvSupplier, DefaultBdvSupplier.class);
    }

    public static void testSerialization(Gson gson, Object o, Class c) {
        String json = gson.toJson(o);
        System.out.println(json);
        Object oRestored = gson.fromJson(json, c);
        String json2 = gson.toJson(oRestored);
        System.out.println(json2);
        Assert.assertEquals(json, json2);
    }
}
