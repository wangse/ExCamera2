package com.holenstudio.excamera2.model;

import android.graphics.Bitmap;

/**
 * Created by hhn6205 on 2016/6/17.
 */
public class ThumbnailItem {
    public Bitmap image;
    public String text;
    public int filter;

    public ThumbnailItem() {
        image = null;
        filter = 0;
        text = null;
    }
}
