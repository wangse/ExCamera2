package com.holenstudio.excamera2.model;

import android.graphics.Bitmap;

import com.holenstudio.excamera2.filter.Filter;

/**
 * Created by hhn6205 on 2016/6/17.
 */
public class ThumbnailItem {
    public Bitmap image;
    public String text;
    public Filter filter;

    public ThumbnailItem() {
        image = null;
        filter = new Filter();
        text = null;
    }
}
