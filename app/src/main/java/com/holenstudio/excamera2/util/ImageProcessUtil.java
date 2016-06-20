package com.holenstudio.excamera2.util;

import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.os.Build;
import android.view.animation.PathInterpolator;

import com.holenstudio.excamera2.util.BezierSpline.Point;

/**
 * Created by hhn6205 on 2016/6/20.
 */
public class ImageProcessUtil {
    static {
        System.loadLibrary("NativeImageProcessor");
    }

    public static Bitmap applyCurves(int[] rgb, int[] red, int[] green, int[] blue, Bitmap inputImage) {
        // create output bitmap
        Bitmap outputImage = inputImage;

        // get image size
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();

        int[] pixels = new int[width * height];
        outputImage.getPixels(pixels, 0, width, 0, 0, width, height);

        if (rgb != null) {
            pixels = applyRGBCurve(pixels, rgb, width, height);
        }

        if (!(red == null && green == null && blue == null)) {
            pixels = applyChannelCurves(pixels, red, green, blue, width, height);
        }

        try {
            outputImage.setPixels(pixels, 0, width, 0, 0, width, height);
        } catch (IllegalStateException ise) {
        }
        return outputImage;
    }

    public static Bitmap doBrightness(int value, Bitmap inputImage) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        int[] pixels = new int[width * height];

        inputImage.getPixels(pixels, 0, width, 0, 0, width, height);
        doBrightness(pixels, value, width, height);
        inputImage.setPixels(pixels, 0, width, 0, 0, width, height);

        return inputImage;
    }

    public static Bitmap doContrast(float value, Bitmap inputImage) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        int[] pixels = new int[width * height];

        inputImage.getPixels(pixels, 0, width, 0, 0, width, height);
        doContrast(pixels, value, width, height);
        inputImage.setPixels(pixels, 0, width, 0, 0, width, height);

        return inputImage;
    }


    public static Bitmap doColorOverlay(int depth, float red, float green, float blue, Bitmap inputImage) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        int[] pixels = new int[width * height];

        inputImage.getPixels(pixels, 0, width, 0, 0, width, height);
        doColorOverlay(pixels, depth, red, green, blue, width, height);
        inputImage.setPixels(pixels, 0, width, 0, 0, width, height);

        return inputImage;
    }

    public static Bitmap doSaturation(Bitmap inputImage, float level) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        int[] pixels = new int[width * height];

        inputImage.getPixels(pixels, 0, width, 0, 0, width, height);
        doSaturation(pixels, level, width, height);
        inputImage.setPixels(pixels, 0, width, 0, 0, width, height);
        return inputImage;
    }

    public static Bitmap process(Bitmap originalBmp, Point[] rgbKnots, Point[] redKnots, Point[] greenKnots, Point[] blueKnots) {
        Point[] mRgbKnots = sortPointsOnXAxis(rgbKnots);
        Point[] mRedKnots = sortPointsOnXAxis(redKnots);
        Point[] mGreenKnots = sortPointsOnXAxis(greenKnots);
        Point[] mBlueKnots = sortPointsOnXAxis(blueKnots);
        int[] rgb = null;
        int[] r = null;
        int[] g = null;
        int[] b = null;
        Point[] straightKnots = new Point[2];
        straightKnots[0] = new Point(0, 0);
        straightKnots[1] = new Point(255, 255);
        if (mRgbKnots == null) {
            mRgbKnots = straightKnots;
        }
        if (mRedKnots == null) {
            mRedKnots = straightKnots;
        }
        if (mGreenKnots == null) {
            mGreenKnots = straightKnots;
        }
        if (mBlueKnots == null) {
            mBlueKnots = straightKnots;
        }
        if (rgb == null) {
            rgb = BezierSpline.curveGenerator(mRgbKnots);
        }

        if (r == null) {
            r = BezierSpline.curveGenerator(mRedKnots);
        }

        if (g == null) {
            g = BezierSpline.curveGenerator(mGreenKnots);
        }

        if (b == null) {
            b = BezierSpline.curveGenerator(mBlueKnots);
        }

        try {
            return applyCurves(rgb, r, g, b, originalBmp);
        } catch (OutOfMemoryError oe) {
            System.gc();
            try {
                return applyCurves(rgb, r, g, b, originalBmp);
            } catch (OutOfMemoryError ignored) {
            }
        }
        return applyCurves(rgb, r, g, b, originalBmp);
    }

    public static Bitmap getStarLitBitmap(Bitmap mOriginalBmp, int width, int height) {
        Point[] rgbKnots;
        rgbKnots = new Point[8];
        rgbKnots[0] = new Point(0, 0);
        rgbKnots[1] = new Point(34, 6);
        rgbKnots[2] = new Point(69, 23);
        rgbKnots[3] = new Point(100, 58);
        rgbKnots[4] = new Point(150, 154);
        rgbKnots[5] = new Point(176, 196);
        rgbKnots[6] = new Point(207, 233);
        rgbKnots[7] = new Point(255, 255);
        return process(mOriginalBmp, rgbKnots, null, null, null);
    }

    public static Bitmap getBlueMessBitmap(Bitmap mOriginalBmp, int width, int height) {
        BezierSpline.Point[] redKnots;
        redKnots = new BezierSpline.Point[8];
        redKnots[0] = new BezierSpline.Point(0, 0);
        redKnots[1] = new BezierSpline.Point(86, 34);
        redKnots[2] = new Point(117, 41);
        redKnots[3] = new Point(146, 80);
        redKnots[4] = new Point(170, 151);
        redKnots[5] = new Point(200, 214);
        redKnots[6] = new Point(225, 242);
        redKnots[7] = new Point(255, 255);
        return process(mOriginalBmp, null, redKnots, null, null);
    }

    public static Bitmap getAweStruckVibeBitmap(Bitmap mOriginalBmp, int width, int height) {
        Point[] rgbKnots;
        Point[] redKnots;
        Point[] greenKnots;
        Point[] blueKnots;

        rgbKnots = new Point[5];
        rgbKnots[0] = new Point(0, 0);
        rgbKnots[1] = new Point(80, 43);
        rgbKnots[2] = new Point(149, 102);
        rgbKnots[3] = new Point(201, 173);
        rgbKnots[4] = new Point(255, 255);

        redKnots = new Point[5];
        redKnots[0] = new Point(0, 0);
        redKnots[1] = new Point(125, 147);
        redKnots[2] = new Point(177, 199);
        redKnots[3] = new Point(213, 228);
        redKnots[4] = new Point(255, 255);


        greenKnots = new Point[6];
        greenKnots[0] = new Point(0, 0);
        greenKnots[1] = new Point(57, 76);
        greenKnots[2] = new Point(103, 130);
        greenKnots[3] = new Point(167, 192);
        greenKnots[4] = new Point(211, 229);
        greenKnots[5] = new Point(255, 255);


        blueKnots = new Point[7];
        blueKnots[0] = new Point(0, 0);
        blueKnots[1] = new Point(38, 62);
        blueKnots[2] = new Point(75, 112);
        blueKnots[3] = new Point(116, 158);
        blueKnots[4] = new Point(171, 204);
        blueKnots[5] = new Point(212, 233);
        blueKnots[6] = new Point(255, 255);
        return process(mOriginalBmp, rgbKnots, redKnots, greenKnots, blueKnots);
    }

    public static Bitmap getLimeStutterBitmap(Bitmap mOriginalBmp, int width, int height) {
        Point[] blueKnots;
        blueKnots = new Point[3];
        blueKnots[0] = new Point(0, 0);
        blueKnots[1] = new Point(165, 114);
        blueKnots[2] = new Point(255, 255);
        return process(mOriginalBmp, null, null, null, blueKnots);
    }

    public static Bitmap getNightWhisperBitmap(Bitmap mOriginalBmp, int width, int height) {
        Point[] rgbKnots;
        Point[] redKnots;
        Point[] greenKnots;
        Point[] blueKnots;

        rgbKnots = new Point[3];
        rgbKnots[0] = new Point(0, 0);
        rgbKnots[1] = new Point(174, 109);
        rgbKnots[2] = new Point(255, 255);

        redKnots = new Point[4];
        redKnots[0] = new Point(0, 0);
        redKnots[1] = new Point(70, 114);
        redKnots[2] = new Point(157, 145);
        redKnots[3] = new Point(255, 255);

        greenKnots = new Point[3];
        greenKnots[0] = new Point(0, 0);
        greenKnots[1] = new Point(109, 138);
        greenKnots[2] = new Point(255, 255);

        blueKnots = new Point[3];
        blueKnots[0] = new Point(0, 0);
        blueKnots[1] = new Point(113, 152);
        blueKnots[2] = new Point(255, 255);
        return process(mOriginalBmp, rgbKnots, redKnots, greenKnots, blueKnots);
    }

    public static Point[] sortPointsOnXAxis(Point[] points) {
        if (points == null) {
            return null;
        }
        for (int s = 1; s < points.length - 1; s++) {
            for (int k = 0; k <= points.length - 2; k++) {
                if (points[k].x > points[k + 1].x) {
                    float temp = 0;
                    temp = points[k].x;
                    points[k].x = points[k + 1].x; //swapping values
                    points[k + 1].x = temp;
                }
            }
        }
        return points;
    }

    private static native int[] applyRGBCurve(int[] pixels, int[] rgb, int width, int height);

    private static native int[] applyChannelCurves(int[] pixels, int[] r, int[] g, int[] b, int width, int height);

    private static native int[] doBrightness(int[] pixels, int value, int width, int height);

    private static native int[] doContrast(int[] pixels, float value, int width, int height);

    private static native int[] doColorOverlay(int[] pixels, int depth,
                                               float red, float green, float blue,
                                               int width, int height);

    private static native int[] doSaturation(int[] pixels, float level, int width, int height);
}
