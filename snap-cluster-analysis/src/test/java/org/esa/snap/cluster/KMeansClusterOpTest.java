package org.esa.snap.cluster;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.junit.Test;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertArrayEquals;

/**
 * @author Tonio Fincke
 */
public class KMeansClusterOpTest {

    @Test
    public void testKMeansClusterOp() {
        int width = 24;
        int height = 48;
        final Product product = createProduct(width, height);

        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new KMeansClusterOp.Spi());

        final Map<String, Object> parameters = new HashMap<>();
        final Map<String, Product> sourceProducts = new HashMap<>();
        parameters.put("roiMaskName", "Mask_1");
        sourceProducts.put("sourceProduct", product);
        final Product targetProduct = GPF.getDefaultInstance().createProductNS("KMeansClusterAnalysis", parameters, sourceProducts, null);

        assertNotNull(targetProduct);

        assertEquals(1, targetProduct.getBandGroup().getNodeCount());
        final Band classIndicesBand = targetProduct.getBand("class_indices");
        assertNotNull(classIndicesBand);
        int[] classIndices = new int[width * height];
        classIndices = classIndicesBand.getSourceImage().getData(new Rectangle(0, 0, width, height)).
                getSamples(0, 0, width, height, 0, classIndices);

        int[] expectedIndices = new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
                2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 2, 2, 2, 2,
                2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 2, 2,
                1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 3, 3, 3, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 3, 3, 3,
                3, 3, 3, 3, 3, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3,
                3, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 5, 5, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 5, 5, 5, 5, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3,
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 5, 5, 5, 5, 5, 5, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 5,
                5, 5, 5, 5, 5, 5, 5, 5, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 7, 7, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 7, 7, 7, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
                7, 7, 7, 7, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 7, 7, 7, 7, 7, 7, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 10, 10, 5, 5, 5, 5, 5, 5, 7, 7, 7, 7, 7, 7, 7, 4, 4, 4, 4, 4, 4, 4, 4, 4, 10, 10, 10, 10, 10,
                5, 5, 7, 7, 7, 7, 7, 7, 7, 7, 7, 4, 4, 4, 4, 4, 4, 4, 4, 10, 10, 10, 10, 10, 10, 10, 7, 7, 7, 7, 7, 7, 7,
                7, 7, 7, 4, 4, 4, 4, 4, 4, 4, 10, 10, 10, 10, 10, 10, 10, 10, 7, 7, 7, 7, 7, 7, 7, 7, 7, 4, 4, 4, 4, 4,
                4, 8, 10, 10, 10, 10, 10, 10, 10, 10, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 8, 8, 10, 10, 10, 10,
                10, 10, 10, 10, 10, 7, 7, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 8, 8, 10, 10, 10, 10, 10, 10, 10, 10, 10, 7,
                7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 8, 8, 8, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 7, 7, 7, 7, 7, 8, 8,
                8, 8, 8, 8, 8, 8, 8, 10, 10, 10, 10, 10, 10, 10, 10, 10, 9, 9, 9, 9, 9, 9, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                11, 11, 11, 10, 10, 10, 10, 10, 9, 9, 9, 9, 9, 9, 9, 9, 8, 8, 8, 8, 8, 8, 8, 8, 11, 11, 11, 11, 11, 11,
                11, 9, 9, 9, 9, 9, 9, 9, 9, 9, 8, 8, 8, 8, 8, 8, 8, 8, 11, 11, 11, 11, 11, 11, 11, 11, 9, 9, 9, 9, 9, 9,
                9, 9, 9, 8, 8, 8, 8, 8, 8, 8, 11, 11, 11, 11, 11, 11, 11, 11, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 8, 8, 8, 8,
                8, 8, 11, 11, 11, 11, 11, 11, 11, 11, 11, 9, 9, 9, 9, 9, 9, 9, 9, 9, 8, 8, 8, 8, 8, 8, 11, 11, 11, 11,
                11, 11, 11, 11, 11, 9, 9, 9, 9, 9, 9, 9, 9, 9, 6, 6, 6, 6, 6, 6, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11,
                9, 9, 9, 9, 9, 9, 9, 6, 6, 6, 6, 6, 6, 6, 13, 11, 11, 11, 11, 11, 11, 11, 11, 11, 12, 9, 9, 9, 9, 9, 6,
                6, 6, 6, 6, 6, 6, 6, 13, 13, 13, 11, 11, 11, 11, 11, 11, 12, 12, 12, 12, 12, 12, 6, 6, 6, 6, 6, 6, 6, 6,
                6, 13, 13, 13, 13, 13, 13, 13, 11, 12, 12, 12, 12, 12, 12, 12, 12, 6, 6, 6, 6, 6, 6, 6, 6, 13, 13, 13,
                13, 13, 13, 13, 13, 12, 12, 12, 12, 12, 12, 12, 12, 6, 6, 6, 6, 6, 6, 6, 6, 13, 13, 13, 13, 13, 13, 13,
                13, 12, 12, 12, 12, 12, 12, 12, 12, 12, 6, 6, 6, 6, 6, 6, 6, 13, 13, 13, 13, 13, 13, 13, 13, 13, 12, 12,
                12, 12, 12, 12, 12, 12, 6, 6, 6, 6, 6, 6, 6, 13, 13, 13, 13, 13, 13, 13, 13, 13, 12, 12, 12, 12, 12, 12,
                12, 12, 12, 6, 6, 6, 6, 6, 6, 13, 13, 13, 13, 13, 13, 13, 13, 13, 12, 12, 12, 12, 12, 12, 12, 12, 12, 6,
                6, 6, 6, 6, 6, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 12, 12, 12, 12, 12, 12, 12, 12, 12, 6, 6, 6, 6, 6};

        assertArrayEquals(expectedIndices, classIndices);
    }

    public static Product createProduct(int width, int height) {
        Product product = new Product("Test_Product_2", "Test_Type_2", width, height);
        product.addBand("Band_1", "cos(X/100)-sin(Y/100)");
        product.addBand("Band_2", "sin(X/100)+cos(Y/100)");
        product.addBand("Band_3", "cos(X/100)*cos(Y/100)");
        product.addMask("Mask_1", "Band_1 > 0.5", "I am Mask 1", Color.GREEN, 0.5);
        product.setModified(false);
        return product;
    }

}