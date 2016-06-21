package com.holenstudio.excamera2.filter;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This Class represents a ImageFilter and includes many subfilters within, we add different subfilters to this class's
 * object and they are then processed in that particular order
 */
public class Filter {
    private List<SubFilter> subFilters = new ArrayList<>();

    public Filter(Filter filter) {
        this.subFilters = filter.subFilters;
    }

    public Filter() {
    }

    /**
     * Adds a Subfilter to the Main Filter
     *
     * @param subFilter Subfilter like contrast, brightness, tone Curve etc. subfilter
     * @see BrightnessSubfilter
     * @see ColorOverlaySubfilter
     * @see ContrastSubfilter
     * @see ToneCurveSubfilter
     * @see VignetteSubfilter
     */
    public void addSubFilter(SubFilter subFilter) {
        subFilters.add(subFilter);
    }

    /**
     * Clears all the subfilters from the Parent Filter
     */
    public void clearSubFilters() {
        subFilters.clear();
    }

    /**
     * Removes the subfilter containing Tag from the Parent Filter
     */
    public void removeSubFilterWithTag(String tag) {
        Iterator<SubFilter> iterator = subFilters.iterator();
        while (iterator.hasNext()) {
            SubFilter subFilter = iterator.next();
            if (subFilter.getTag().equals(tag)) {
                iterator.remove();
            }
        }
    }

    /**
     * Returns The filter containing Tag
     */
    public SubFilter getSubFilterByTag(String tag) {
        for (SubFilter subFilter : subFilters) {
            if (subFilter.getTag().equals(tag)) {
                return subFilter;
            }
        }
        return null;
    }

    /**
     * Give the output Bitmap by applying the defined filter
     *
     * @param inputImage Input Bitmap on which filter is to be applied
     * @return filtered Bitmap
     */
    public Bitmap processFilter(Bitmap inputImage) {
        Bitmap outputImage = inputImage;
        if (outputImage != null) {
            for (SubFilter subFilter : subFilters) {
                try {
                    outputImage = subFilter.process(outputImage);
                } catch (OutOfMemoryError oe) {
                    System.gc();
                    try {
                        outputImage = subFilter.process(outputImage);
                    } catch (OutOfMemoryError ignored) {
                    }
                }
            }
        }

        return outputImage;
    }

    @Override
    public String toString() {
        return "Filter{" +
                "subFilters=" + subFilters +
                '}';
    }

    public interface SubFilter {
        Bitmap process(Bitmap inputImage);

        Object getTag();

        void setTag(Object tag);
    }
}
