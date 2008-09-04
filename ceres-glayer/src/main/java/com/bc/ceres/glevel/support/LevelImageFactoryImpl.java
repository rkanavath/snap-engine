package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.IMultiLevelImage;
import com.bc.ceres.glevel.LevelImageFactory;

import javax.media.jai.Interpolation;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.image.RenderedImage;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class LevelImageFactoryImpl implements LevelImageFactory {
    private final RenderedImage frSourceImage;
    private Interpolation interpolation;

    public LevelImageFactoryImpl(RenderedImage frSourceImage) {
        this(frSourceImage, Interpolation.getInstance(Interpolation.INTERP_NEAREST));
    }

    public LevelImageFactoryImpl(RenderedImage frSourceImage, Interpolation interpolation) {
        this.frSourceImage = frSourceImage;
        this.interpolation = interpolation;
    }

    public RenderedImage getFRSourceImage() {
        return frSourceImage;
    }

    @Override
    public final RenderedImage createLevelImage(int level) {
        final RenderedImage lrSource;
        if (level > 0) {
            if (getFRSourceImage() instanceof IMultiLevelImage) {
                lrSource = ((IMultiLevelImage) getFRSourceImage()).getLevelImage(level);
            } else {
                lrSource = createLRSourceImage(convertLevelToScale(level));
            }
        } else {
            lrSource = getFRSourceImage();
        }
        return createLRTargetImage(lrSource);
    }

    protected double convertLevelToScale(int level) {
        return Math.pow(2, -level);
    }

    protected RenderedImage createLRSourceImage(double scale) {
        return ScaleDescriptor.create(getFRSourceImage(),
                                      (float)scale, (float)scale,
                                      0.0f, 0.0f,
                                      interpolation,
                                      null);
    }

    protected RenderedImage createLRTargetImage(RenderedImage lrSource) {
        return lrSource;
    }
}