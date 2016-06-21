package com.holenstudio.excamera2.filter;

import android.graphics.Bitmap;

import com.holenstudio.excamera2.util.ImageProcessUtil;

/**
 * @author varun
 * Class to add Contrast Subfilter
 */
public class ContrastSubfilter implements Filter.SubFilter {

    private static String tag = "";

    // The value is in fraction, value 1 has no effect
    private float contrast = 0;

    /**
     * Initialise contrast subfilter
     *
     * @param contrast The contrast value ranges in fraction, value 1 has no effect
     */
    public ContrastSubfilter(float contrast) {
        this.contrast = contrast;
    }

    @Override
    public Bitmap process(Bitmap inputImage) {
        return ImageProcessUtil.doContrast(contrast, inputImage);
    }

    @Override
    public String getTag() {
        return tag;
    }

    @Override
    public void setTag(Object tag) {
        ContrastSubfilter.tag = (String) tag;
    }

    /**
     * Sets the contrast value by the value passed in as parameter
     */
    public void setContrast(float contrast) {
        this.contrast = contrast;
    }

    /**
     * Changes contrast value by the value passed in as a parameter
     */
    public void changeContrast(float value) {
        this.contrast += value;
    }
}
