package com.holenstudio.excamera2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.holenstudio.excamera2.util.CameraUtil;
import com.holenstudio.excamera2.util.FileUtil;
import com.holenstudio.excamera2.view.ExCamera2View;
import com.holenstudio.excamera2.view.FrontView;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private final int REQUEST_CODE_CAMERA_PERMISSION = 0x01;

    private Button mCaptureBtn;
    private Button mSwichModeBtn;
    private ImageView mPictureIv;
    private ImageView mSwitchIv;
    private View mFocusArea;
    private SeekBar mCameraZoomSeekBar;
    private TextView mCameraZoomValue;
    private FrontView mCameraFrontView;
    private ExCamera2View mPreview;

    private String mCameraId;
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private CameraCharacteristics mCameraCharacteristics;
    private Size mPreviewSize;
    private Size mVideoSize;
    private MediaRecorder mMediaRecorder;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private ImageReader mImageReader;
    private File mPictureFile;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CaptureRequest.Builder mRecordRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private int mState = CameraUtil.STATE_PREVIEW;
    private List<Surface> mSurfaces;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private boolean mIsRecordingVideo = false;
    private boolean mIsTakingPicture = true;
    private boolean mIsFrontCamera = false;
    private String mRecorderPath;
    /**
     * mPreview(预览窗口)的listener
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    /**
     * 当CameraDevice的状态state发生改变时回调
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            //打开摄像头设备CameraDevice并启动摄像头预览mPreview
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Log.d(TAG, "This occurs error:" + error);
        }

    };

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable"
     * will be called when a still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            mPictureFile = new File(FileUtil.photoDir().getAbsolutePath() + "/" + System.currentTimeMillis() + ".jpg");
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mPictureFile));
        }

    };

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events
     * related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case CameraUtil.STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working
                    // normally.
                    break;
                }
                case CameraUtil.STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState
                            || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = CameraUtil.STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case CameraUtil.STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE
                            || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = CameraUtil.STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case CameraUtil.STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = CameraUtil.STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                        CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                       TotalCaptureResult result) {
            process(result);
        }

    };

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_capture:
                    if (mIsTakingPicture) {
                        takePicture();
                    } else {
                        if (mIsRecordingVideo) {
                            stopRecordingVideo();
                            mCaptureBtn.setBackgroundResource(R.drawable.btn_recorder_background);
                        } else {
//                            if (CameraUtil.prepareVideoRecorder(MainActivity.this, mMediaRecorder, mVideoSize)) {
//                            if (setUpMediaRecorder()) {
                            changeToRecorderPreviewSession();
                            startRecordingVideo();
                            mCaptureBtn.setBackgroundResource(R.drawable.btn_recorder_stop_background);
//                            } else {
//                                mMediaRecorder.release();
//                            }
                        }
                    }
                    break;
                case R.id.btn_switch_mode:
                    if (mIsTakingPicture) {
//                        changeToRecorderPreviewSession();
                        closeCamera();
                        openCamera(mPreview.getWidth(), mPreview.getHeight());
                        mIsTakingPicture = false;
                        mSwichModeBtn.setBackgroundResource(R.drawable.btn_switch_to_camera);
                        mCaptureBtn.setBackgroundResource(R.drawable.btn_recorder_background);
                    } else {
                        mIsTakingPicture = true;
                        mSwichModeBtn.setBackgroundResource(R.drawable.btn_switch_to_video);
                        mCaptureBtn.setBackgroundResource(R.drawable.btn_shutter_background);
                    }
                    break;
                case R.id.iv_switch_camera:
                    switchCamera();
                    break;
                case R.id.iv_picture:
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivity(intent);
                    break;
                default:
                    break;
            }

        }
    };

    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (seekBar.getId() == R.id.camera_zoom_seek_bar) {
                Rect rect2 = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                int radio2 = mCameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM).intValue() / 3;
                int realRadio2 = mCameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM).intValue();
                int centerX2 = rect2.centerX();
                int centerY2 = rect2.centerY();
                int minMidth2 = (rect2.right - ((progress * centerX2) / 100 / radio2) - 1) - 20;
                int minHeight2 = (rect2.bottom - ((progress * centerY2) / 100 / radio2) - 1) - 20;
                if (minMidth2 < rect2.right / realRadio2 || minHeight2 < rect2.bottom / realRadio2) {
                    Log.i("sb_zoom", "sb_zoomsb_zoomsb_zoom");
                    return;
                }
                Rect newRect2 = new Rect(20, 20, rect2.right - ((progress * centerX2) / 100 / radio2) - 1, rect2.bottom - ((progress * centerY2) / 100 / radio2) - 1);
                Log.i("sb_zoom", "left--->" + "20" + ",,,top--->" + "20" + ",,,right--->" + (rect2.right - ((progress * centerX2) / 100 / radio2) - 1) + ",,,bottom--->" + (rect2.bottom - ((progress * centerY2) / 100 / radio2) - 1));
                mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, newRect2);
                mCaptureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, newRect2);
                mCameraZoomValue.setText(String.valueOf(progress));
                updatePreview();
            }
        }
    };

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    return true;

                default:
                    break;
            }
            return false;
        }
    };

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the
     * smallest one whose width and height are at least as large as the
     * respective requested values, and whose aspect ratio matches with the
     * specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended
     *                    output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big
     * enough
     */
    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the
        // preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w && option.getWidth() >= width
                    && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
    }

    private void initView() {
        mPreview = (ExCamera2View) findViewById(R.id.camera_view);
        mCaptureBtn = (Button) findViewById(R.id.btn_capture);
        mSwichModeBtn = (Button) findViewById(R.id.btn_switch_mode);
        mPictureIv = (ImageView) findViewById(R.id.iv_picture);
        mSwitchIv = (ImageView) findViewById(R.id.iv_switch_camera);
        mFocusArea = findViewById(R.id.focus_area);
        mCameraZoomSeekBar = (SeekBar) findViewById(R.id.camera_zoom_seek_bar);
        mCameraZoomValue = (TextView) findViewById(R.id.camera_zoom_value);
        mCameraFrontView = (FrontView) findViewById(R.id.camera_front_view);

        mCaptureBtn.setOnClickListener(mClickListener);
        mSwichModeBtn.setOnClickListener(mClickListener);
        mPictureIv.setOnClickListener(mClickListener);
        mSwitchIv.setOnClickListener(mClickListener);
        mPreview.setOnTouchListener(mTouchListener);
        mCameraZoomSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        // mCameraZoomSeekBar.setMax(mParams.getMaxZoom());
    }

    private void initData() {
        mSurfaces = new ArrayList<>();
    }

    @Override
    protected void onResume() {
        super.onResume();

        startBackgroundThread();
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mPreview.isAvailable()) {
            openCamera(mPreview.getWidth(), mPreview.getHeight());
        } else {
            mPreview.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopBackgroundThread();
        closeCamera();
    }

    protected void openCamera(int width, int height) {
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission();
                return;
            }

            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "CameraAccessException:" + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Parmission handling for Android 6.0
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new AlertDialog.Builder(this)
                    .setMessage("Request Permission")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CODE_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                    .create();
            return;
        }
        //request permission
        requestPermissions(new String[]{Manifest.permission.CAMERA},
                REQUEST_CODE_CAMERA_PERMISSION);
        return;
    }

    private void setUpCameraOutputs(int width, int height) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // choose camera
                if (mIsFrontCamera) {
                    if ((characteristics.get(CameraCharacteristics.LENS_FACING)
                            == CameraCharacteristics.LENS_FACING_BACK)) {
                        continue;
                    }
                } else {
                    if ((characteristics.get(CameraCharacteristics.LENS_FACING)
                            == CameraCharacteristics.LENS_FACING_FRONT)) {
                        continue;
                    }
                }
                mCameraId = cameraId;
                mCameraCharacteristics = manager.getCameraCharacteristics(mCameraId);

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        width, height, largest);
                mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mPreview.setAspectRatio(
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mPreview.setAspectRatio(
                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }
//                setUpMediaRecorder();
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            e.printStackTrace();
        }

    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
//            setUpMediaRecorder();
            SurfaceTexture texture = mPreview.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface previewSurface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(previewSurface);

            mCaptureRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(previewSurface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                        }
                    }, mBackgroundHandler
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void changeToRecorderPreviewSession() {
        try {
            SurfaceTexture texture = mPreview.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            mRecordRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            setRecorderRequestParameters();
            setUpMediaRecorder();

            mCaptureSession.stopRepeating();
            // This is the output Surface we need to start preview.
            Surface previewSurface = new Surface(texture);
            mRecordRequestBuilder.addTarget(previewSurface);
            Surface recorderSurface = mMediaRecorder.getSurface();
            mRecordRequestBuilder.addTarget(recorderSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, recorderSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Finally, we start displaying the camera preview.
                                mCaptureSession.setRepeatingRequest(mRecordRequestBuilder.build(),
                                        null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                        }
                    }, mBackgroundHandler
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mPreview || null == mPreviewSize) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mPreview.setTransform(matrix);
    }

    /**
     * Initiate a still image capture.
     */
    private void takePicture() {
//        lockFocus();
        captureStillPicture();
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            //3A
//            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
//            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
//            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO);
            // Tell #mCaptureCallback to wait for the lock.
            mState = CameraUtil.STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when we
     * get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = CameraUtil.STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
        try {
            if (null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            Surface imageReaderSurfac = mImageReader.getSurface();
            mCaptureRequestBuilder.addTarget(imageReaderSurfac);
            setCaptureResquestParameters();

            // Use the same AE and AF modes as the preview.
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            if (mIsFrontCamera) {
                mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, CameraUtil.FRONT_ORIENTATIONS.get(rotation));
            } else {
                mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, CameraUtil.ORIENTATIONS.get(rotation));
            }

            CameraCaptureSession.CaptureCallback captureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                               TotalCaptureResult result) {
                    Toast.makeText(MainActivity.this, "Saved: " + mPictureFile.getName(), Toast.LENGTH_SHORT).show();
                    showPicture();
                    unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.capture(mCaptureRequestBuilder.build(), captureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void showPicture() {
        if (!mPictureFile.exists()) {
            return;
        }
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mPictureFile.getAbsolutePath(),option);
        option.inSampleSize = CameraUtil.calculateInSampleSize(option, mPictureIv.getMeasuredWidth(),
                mPictureIv.getMeasuredHeight());
        option.inJustDecodeBounds = false;
        mPictureIv.setImageBitmap(BitmapFactory.decodeFile(mPictureFile.getAbsolutePath(),option));
//        try {
////            MediaStore.Images.Media.insertImage(getContentResolver(), mPictureFile.getAbsolutePath(), "ExCamera", null);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + mPictureFile.getAbsolutePath()));
        sendBroadcast(intent);
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is finished.
     */
    private void unlockFocus() {
        try {
            // Reset the autofucos trigger
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
//            mCaptureSession.capture(mCaptureRequestBuilder.build(), null,
//                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = CameraUtil.STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, null,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes larger
     * than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size
     */
    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }

    private boolean setUpMediaRecorder() {
        mMediaRecorder = new MediaRecorder();

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
//        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorderPath = FileUtil.videoDir().getAbsolutePath() + "/" + System.currentTimeMillis() + ".mp4";
        mMediaRecorder.setOutputFile(mRecorderPath);
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int orientation;
        if (mIsFrontCamera) {
            orientation = CameraUtil.FRONT_ORIENTATIONS.get(rotation);
        } else {
            orientation = CameraUtil.ORIENTATIONS.get(rotation);
        }
        mMediaRecorder.setOrientationHint(orientation);
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void startRecordingVideo() {
        try {
            // UI
            mIsRecordingVideo = true;

            // Start recording
            mMediaRecorder.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void stopRecordingVideo() {
        // UI
        mIsRecordingVideo = false;
//        try {
//            mCaptureSession.abortCaptures();
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//        // Stop recording
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + mRecorderPath));
        sendBroadcast(intent);
        mPictureIv.setImageBitmap(CameraUtil.getVideoThumbnail(mRecorderPath, mPictureIv.getWidth(), mPictureIv.getHeight(), MediaStore.Images.Thumbnails.MICRO_KIND));
//        setUpMediaRecorder();
//        changeToRecorderPreviewSession();
//        closeCamera();
        openCamera(mPreview.getWidth(), mPreview.getHeight());
//        createCameraPreviewSession(false);
    }

    private void setCaptureResquestParameters() {
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START);
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
    }

    private void setRecorderRequestParameters() {
        mRecordRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    private void updatePreview() {
        try {
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void switchCamera() {
        closeCamera();
        mIsFrontCamera = !mIsFrontCamera;
        openCamera(mPreview.getWidth(), mPreview.getHeight());
    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        public ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileUtil.saveFile(bytes, mFile);
            mImage.close();
        }

    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    public static class ErrorDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage("This device doesn't support Camera2 API.")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

}
