package com.holenstudio.excamera2.util;

import android.app.Activity;
import android.media.MediaRecorder;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import java.io.IOException;

public class CameraUtil {
    private static final String TAG = "CameraUtil";

    /**
     * Camera state: Showing camera preview.
     */
    public static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    public static final int STATE_WAITING_LOCK = 1;
    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    public static final int STATE_WAITING_PRECAPTURE = 2;
    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    public static final int STATE_WAITING_NON_PRECAPTURE = 3;
    /**
     * Camera state: Picture was taken.
     */
    public static final int STATE_PICTURE_TAKEN = 4;
	/**
     * Conversion from screen rotation to JPEG orientation.
     */
    public static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    public static final SparseIntArray FRONT_ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
        FRONT_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        FRONT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        FRONT_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        FRONT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public static boolean prepareVideoRecorder(Activity activity, MediaRecorder recorder, Size size) {
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setOutputFile(FileUtil.videoDir().getAbsolutePath() + "/" +  System.currentTimeMillis() + ".mp4");
        recorder.setVideoEncodingBitRate(10000000);
        recorder.setVideoFrameRate(30);
        recorder.setVideoSize(size.getWidth(), size.getHeight());
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int orientation = ORIENTATIONS.get(rotation);
        recorder.setOrientationHint(orientation);
        try {
            recorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
