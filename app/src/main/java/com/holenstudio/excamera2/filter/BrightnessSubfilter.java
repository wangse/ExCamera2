package com.holenstudio.excamera2.filter;

import android.graphics.Bitmap;

import com.holenstudio.excamera2.util.ImageProcessUtil;

/**
 * @author varun
 * subfilter used to tweak brightness of the Bitmap
 */
public class BrightnessSubfilter implements Filter.SubFilter {
    private static String tag = "";
    // Value is in integer
    private int brightness = 0;

    /**
     * Takes Brightness of the image
     *
     * @param brightness Integer brightness value {value 0 has no effect}
     */
    public BrightnessSubfilter(int brightness) {
        this.brightness = brightness;
    }

    @Override
    public Bitmap process(Bitmap inputImage) {
        return ImageProcessUtil.doBrightness(brightness, inputImage);
    }

    @Override
    public String getTag() {
        return tag;
    }

    @Override
    public void setTag(Object tag) {
        BrightnessSubfilter.tag = (String) tag;
    }

    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }

    /**
     * Changes the brightness by the value passed as parameter
     */
    public void changeBrightness(int value) {
        this.brightness += value;
    }

}
