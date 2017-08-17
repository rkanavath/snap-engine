package org.esa.snap.cluster;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.pointop.PixelOperator;
import org.esa.snap.core.gpf.pointop.ProductConfigurer;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;

/**
 * @author Tonio Fincke
 */
@OperatorMetadata(alias = "KMeansClusterAnalysis",
        internal = true,
        version = "1.0",
        authors = "Ralf Quast, Marco Zuehlke, Tonio Fincke",
        copyright = "(c) 2008 by Brockmann Consult",
        description = "Performs a K-Means cluster analysis.")
public class KMeansClusterIterOp extends PixelOperator {

    @SourceProduct
    Product sourceProduct;

    @TargetProduct
    Product targetProduct;

    @Parameter
    double[][] means;

    @Parameter(label = "Number of clusters", description = "Number of clusters", defaultValue = "14", interval = "(0,100]")
    private int clusterCount;

    @Parameter(label = "Source band names",
            description = "The names of the bands being used for the cluster analysis.",
            rasterDataNodeType = Band.class)
    private String[] sourceBandNames;

    double[][] sums;
    private int[] memberCounts;
    private int dimensionCount;

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();
        dimensionCount = sourceBandNames.length;
        sums = new double[clusterCount][dimensionCount];
        memberCounts = new int[clusterCount];
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        //todo consider ROI
        final double[] point = new double[dimensionCount];
        for (int i = 0; i < dimensionCount; i++) {
            point[i] = sourceSamples[i].getDouble();
        }
        final int closestCluster = getClosestCluster(means, point);
        for (int d = 0; d < dimensionCount; ++d) {
            sums[closestCluster][d] += point[d];
        }
        memberCounts[closestCluster]++;
    }

    private static int getClosestCluster(double[][] mean, final double[] point) {
        double minDistance = Double.MAX_VALUE;
        int closestCluster = 0;
        for (int c = 0; c < mean.length; ++c) {
            final double distance = squaredDistance(mean[c], point);
            if (distance < minDistance) {
                closestCluster = c;
                minDistance = distance;
            }
        }
        return closestCluster;
    }

    /**
     * Distance measure used by the k-means method.
     *
     * @param x a point.
     * @param y a point.
     * @return squared Euclidean distance between x and y.
     */
    private static double squaredDistance(double[] x, double[] y) {
        double distance = 0.0;
        for (int d = 0; d < x.length; ++d) {
            final double difference = y[d] - x[d];
            distance += difference * difference;
        }

        return distance;
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
        for (int i = 0; i < sourceBandNames.length; i++) {
            sampleConfigurer.defineSample(i, sourceBandNames[i]);
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        productConfigurer.copyBands(sourceBandNames);
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
        // do nothing
    }
}
