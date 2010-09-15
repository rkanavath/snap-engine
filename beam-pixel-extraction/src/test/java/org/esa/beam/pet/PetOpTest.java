package org.esa.beam.pet;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.After;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class PetOpTest {

    @Test
    public void testSingleProduct() throws Exception {

        HashMap<String, Object> parameterMap = new HashMap<String, Object>();

        Coordinate[] coordinates = {
                new Coordinate("coord1", new GeoPos(10.0f, 10.0f)),
                new Coordinate("coord2", new GeoPos(20.0f, 20.0f))
        };
        int windowSize = 3;

        parameterMap.put("exportTiePoints", false);
        parameterMap.put("exportMasks", false);
        parameterMap.put("coordinates", coordinates);
        parameterMap.put("windowSize", windowSize);

        String[] bandNames = {"rad_1", "rad_2"};
        Product[] sourceProduct = {createTestProduct("andi", "level1", bandNames)};
        String[] lines = computeData(parameterMap, sourceProduct);

        checkData(lines, sourceProduct, coordinates, windowSize);
    }

    @Test
    public void testTwoProductsSameType() throws Exception {

        HashMap<String, Object> parameterMap = new HashMap<String, Object>();

        Coordinate[] coordinates = {
                new Coordinate("coord1", new GeoPos(10.0f, 10.0f)),
                new Coordinate("coord2", new GeoPos(20.0f, 20.0f)),
                new Coordinate("coord3", new GeoPos(0.5f, 0.5f))
        };
        int windowSize = 5;

        parameterMap.put("exportTiePoints", false);
        parameterMap.put("exportMasks", false);
        parameterMap.put("coordinates", coordinates);
        parameterMap.put("windowSize", windowSize);

        String[] bandNames = {"rad_1", "rad_2"};

        Product[] products = {
                createTestProduct("kallegrabowski", "level1", bandNames),
                createTestProduct("keek", "level1", bandNames)
        };

        String[] lines = computeData(parameterMap, products);
        checkData(lines, products, coordinates, windowSize);
    }

    @Test
    public void testTwentyProductsSameType() throws Exception {

        HashMap<String, Object> parameterMap = new HashMap<String, Object>();

        Coordinate[] coordinates = {
                new Coordinate("coord1", new GeoPos(10.0f, 10.0f)),
                new Coordinate("coord3", new GeoPos(0.5f, 0.5f))
        };
        int windowSize = 1;

        parameterMap.put("exportTiePoints", false);
        parameterMap.put("exportMasks", false);
        parameterMap.put("coordinates", coordinates);
        parameterMap.put("windowSize", windowSize);

        String[] bandNames = {"rad_1", "rad_2, radiance_3"};

        List<Product> productList = new ArrayList<Product>();
        for (int i = 0; i < 20; i++) {
            productList.add(createTestProduct("prod_" + i, "type", bandNames));
        }

        Product[] products = productList.toArray(new Product[productList.size()]);

        String[] lines = computeData(parameterMap, products);
        checkData(lines, products, coordinates, windowSize);
    }

    @Test
    public void testTwoProductsTwoDifferentTypes() throws Exception {

        HashMap<String, Object> parameterMap = new HashMap<String, Object>();

        Coordinate[] coordinates = {
                new Coordinate("coord1", new GeoPos(10.0f, 10.0f)),
                new Coordinate("coord2", new GeoPos(20.0f, 20.0f)),
                new Coordinate("coord3", new GeoPos(0.5f, 0.5f))
        };
        int windowSize = 5;

        parameterMap.put("exportTiePoints", false);
        parameterMap.put("exportMasks", false);
        parameterMap.put("coordinates", coordinates);
        parameterMap.put("windowSize", windowSize);

        String[] bandNames = {"rad_1", "rad_2"};
        String[] bandNames2 = {"refl_1", "refl_2"};

        Product[] products = {
                createTestProduct("kallegrabowski", "level1", bandNames),
                createTestProduct("keek", "level2", bandNames2)
        };

        String[] lines = computeData(parameterMap, products);
        checkData(lines, products, coordinates, windowSize);
    }

    @Test
    public void testTwentyProductsWithDifferentTypes() throws Exception {

        HashMap<String, Object> parameterMap = new HashMap<String, Object>();

        Coordinate[] coordinates = {
                new Coordinate("coord1", new GeoPos(10.0f, 10.0f)),
                new Coordinate("coord2", new GeoPos(8.0f, 8.0f)),
                new Coordinate("coord3", new GeoPos(2.5f, 1.0f)),
                new Coordinate("coord4", new GeoPos(0.5f, 0.5f))
        };
        int windowSize = 13;

        parameterMap.put("exportTiePoints", false);
        parameterMap.put("exportMasks", false);
        parameterMap.put("coordinates", coordinates);
        parameterMap.put("windowSize", windowSize);

        List<Product> productList = new ArrayList<Product>();
        for (int i = 0; i < 20; i++) {
            productList.add(createTestProduct("prod_" + i, "type" + i, new String[]{"band" + i}));
        }

        Product[] products = productList.toArray(new Product[productList.size()]);

        String[] lines = computeData(parameterMap, products);
        checkData(lines, products, coordinates, windowSize);
    }

    @Test(expected = OperatorException.class)
    public void testFailForEvenWindowSize() throws Exception {

        HashMap<String, Object> parameterMap = new HashMap<String, Object>();

        Coordinate[] coordinates = {
                new Coordinate("coord1", new GeoPos(10.0f, 10.0f))
        };
        int windowSize = 2; // not allowed !!

        parameterMap.put("coordinates", coordinates);
        parameterMap.put("windowSize", windowSize);

        String[] bandNames = {"rad_1", "rad_2"};
        Product[] sourceProduct = {createTestProduct("werner", "level1", bandNames)};
        String[] lines = computeData(parameterMap, sourceProduct);

        checkData(lines, sourceProduct, coordinates, windowSize);
    }

    @Test
    public void testReadMeasurement() throws TransformException, FactoryException, IOException {
        PetOp op = new PetOp();
        String[] bandNames = {"band_1", "band_2", "band_3"};
        Product product = createTestProduct("horst", "horse", bandNames);
        String productType = product.getProductType();
        HashMap<String, String[]> bandNamesMap = new HashMap<String, String[]>();
        bandNamesMap.put(productType, bandNames);
        op.setRasterNamesMap(bandNamesMap);
        op.setWindowSize(3);
        Map<String, List<Measurement>> measurements = new HashMap<String, List<Measurement>>();
        GeoPos geoPos = new GeoPos(20, 10);
        op.readMeasurement(product, new Coordinate("Coord_1", geoPos), 1, measurements);
        geoPos = new GeoPos(21, 9);

        List<Measurement> measurementList = measurements.get(productType);
        assertNotNull(measurementList);
        assertTrue(!measurementList.isEmpty());

        for (int i = 0; i < measurementList.size(); i++) {
            assertEquals(3 * 3, measurementList.size());
            Measurement measurement = measurementList.get(i);
            assertEquals(1, measurement.getCoordinateID());
            assertEquals(geoPos.lat - i / 3, measurement.getLat(), 1.0e-4);
            assertEquals(geoPos.lon + i % 3, measurement.getLon(), 1.0e-4);
            assertEquals("Coord_1", measurement.getCoordinateName());
            assertNull(measurement.getStartTime());
            double[] values = measurement.getValues();
            assertEquals(bandNames.length, values.length);
            assertEquals(0, values[0], 1.0e-4);
            assertEquals(1, values[1], 1.0e-4);
            assertEquals(2, values[2], 1.0e-4);
        }
    }

    private static String[] computeData(Map<String, Object> parameterMap, Product[] sourceProducts) throws
                                                                                                    UnsupportedFlavorException,
                                                                                                    IOException {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new PetOp.Spi());
        GPF.createProduct("Pet", parameterMap, sourceProducts);

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        String data = String.valueOf(clipboard.getData(clipboard.getAvailableDataFlavors()[0]));
        return data.split("\n");
    }

    private Product createTestProduct(String name, String type, String[] bandNames) throws FactoryException,
                                                                                           TransformException {
        Rectangle bounds = new Rectangle(360, 180);
        Product product = new Product(name, type, bounds.width, bounds.height);
        AffineTransform i2mTransform = new AffineTransform();
        final int northing = 90;
        final int easting = -180;
        i2mTransform.translate(easting, northing);
        final double scaleX = 360 / bounds.width;
        final double scaleY = 180 / bounds.height;
        i2mTransform.scale(scaleX, -scaleY);
        CrsGeoCoding geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84, bounds, i2mTransform);
        product.setGeoCoding(geoCoding);
        for (int i = 0; i < bandNames.length; i++) {
            Band band = product.addBand(bandNames[i], ProductData.TYPE_FLOAT32);
            band.setDataElems(generateData(bounds, i));
        }
        return product;
    }

    private float[] generateData(Rectangle bounds, int val) {
        float[] floats = new float[bounds.width * bounds.height];
        Arrays.fill(floats, val);
        return floats;
    }

    private void checkData(String[] lines, Product[] products, Coordinate[] coordinates, int windowSize) {

        List<String> productTypes = new ArrayList<String>();
        for (Product product : products) {
            String productType = product.getProductType();
            if (!productTypes.contains(productType)) {
                productTypes.add(productType);
            }
        }

        int lineCount = windowSize * windowSize * coordinates.length * products.length;
        lineCount += productTypes.size(); // add a line for each header
        lineCount += productTypes.size() > 1 ? productTypes.size() : 0; // if more than one product type is present, add a line for each
        lineCount -= productTypes.size() > 1 ? 1 : 0; // if more than one product type is present, the last productType has no line break

        assertEquals(lineCount, lines.length);

        String header = "ID\tName\tLatitude\tLongitude\tDate(yyyy-MM-dd)\tTime(HH:mm:ss)\t";

        List<Integer> headerLines = new ArrayList<Integer>();
        for (int i = 0; i < productTypes.size(); i++) {
            headerLines.add(i * (windowSize * windowSize * coordinates.length + 2));
        }

        for (int headerLine : headerLines) {
            boolean containsBandNames = false;

            for (Product product : products) {
                String[] bandNames = product.getBandNames();
                String nameString = "";
                for (String bandName : bandNames) {
                    nameString += bandName + "\t";
                }
                if (lines[headerLine].contains(nameString)) {
                    containsBandNames = true;
                    break;
                }
            }

            assertTrue(lines[headerLine].startsWith(header));
            assertTrue(containsBandNames);
        }
    }

    @After
    public void tearDown() {
        StringSelection contents = new StringSelection("");
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(contents, contents);
    }

}
