package com.holenstudio.excamera2.filter;

import android.graphics.Bitmap;

import com.holenstudio.excamera2.util.ImageProcessUtil;

/**
 * @author varun on 28/07/15.
 */
public class SaturationSubfilter implements Filter.SubFilter {
    private static String tag = "";

    // The Level value is float, where level = 1 has no effect on the image
    private float level;

    public SaturationSubfilter(float level) {
        this.level = level;
    }

    @Override
    public Bitmap process(Bitmap inputImage) {
        return ImageProcessUtil.doSaturation(inputImage, level);
    }

    @Override
    public Object getTag() {
        return tag;
    }

    @Override
    public void setTag(Object tag) {
        SaturationSubfilter.tag = (String) tag;
    }

    public void setLevel(float level) {
        this.level = level;
    }
}
