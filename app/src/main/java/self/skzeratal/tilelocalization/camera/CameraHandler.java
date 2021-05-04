package self.skzeratal.tilelocalization.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraHandler {
    private CameraActivity cameraActivity;
    private CameraManager cameraManager;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String cameraId;

    public int OverlayLeft;
    public int OverlayRight;
    public int OverlayTop;
    public int OverlayBottom;

    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;

    private ImageReader imageReader;

    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private Size previewSize;

    public CameraHandler(CameraActivity cameraActivity) {
        this.cameraActivity = cameraActivity;

        Point point = new Point();
        cameraActivity.getWindowManager().getDefaultDisplay().getSize(point);

        //以下是綠色框框的範圍
        OverlayLeft = (int) (point.x * 0.25);
        OverlayRight = (int) (point.x * 0.75);
        OverlayTop = (int) (point.y / 2 - point.x * 0.25);
        OverlayBottom = (int) (point.y / 2 + point.x * 0.25);

        Log.v("tess","it's camera handler 畫綠色框框的地方");
    }

    public void startBackgroundThread() {
        backgroundThread = new HandlerThread("Camera Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    public void stopBackgroundThread() throws InterruptedException {
        backgroundThread.quitSafely();
        backgroundThread.join();
        backgroundThread = null;
        backgroundHandler = null;
    }

    //takepicture是很重要的部分
    public void takePicture() throws CameraAccessException {
        if (cameraDevice == null) {
            return;
        }
        Log.v("tess","camera handler我要拍照ㄌ");
        List<Surface> outputSurfaces = new ArrayList<>(1);
        outputSurfaces.add(imageReader.getSurface());
        //設定相機的度數
        final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureBuilder.addTarget(imageReader.getSurface());
        captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

        /* Auto Camera Setting*/ //自動曝光演算法
        captureBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, -10);
        //數字設0的話拍起來會很亮，若是-10的話會暗一點，比較符合一般亮度
        /* Auto Camera Setting*/

        /* Manual Camera Setting*/
        //調相機參數
        //captureBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF); //模式關掉
        //captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF); //自動曝光算法關掉
        //captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, Long.valueOf(5000000));//曝光時間，微秒
        //captureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 200);//越高越敏感
        //captureBuilder.set(CaptureRequest.LENS_APERTURE, 1.47f); //光圈半徑
        /* Manual Camera Setting*/

        //會去處理rotation的問題，通常不知道為甚麼拍出來之後會旋轉60度，通常相機app會處理，若是我們自己拍就要自己處理
        int rotation = cameraActivity.getWindowManager().getDefaultDisplay().getRotation();
        cameraManager = (CameraManager) cameraActivity.getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation(characteristics, rotation)); //拍完之後會得到一張圖片是jpg

        final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                try {
                    createCameraPreview();
                    Log.v("tess","it's camerahandler cameracapturesession createcamerapreview");
                }
                catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        };

        cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                try {
                    cameraCaptureSession.capture(captureBuilder.build(), captureCallback, backgroundHandler);
                }
                catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

            }
        }, backgroundHandler);
    }

    public void openCamera(int width, int height) {
        cameraManager = (CameraManager) cameraActivity.getSystemService(Context.CAMERA_SERVICE);

        setUpCameraOutputs(width, height);
        configureTransform(width, height);

        if (ActivityCompat.checkSelfPermission(cameraActivity.getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(cameraActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(cameraActivity, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(cameraActivity, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET }, 101);
            return;
        }

        try {
            CameraCharacteristics cameraCharacteristics  = cameraManager.getCameraCharacteristics(cameraId);
            Range<Integer> range = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);

            cameraManager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.v("tess","it's camerahandler opencamera");
    }

    public final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            CameraHandler.this.cameraDevice = cameraDevice;
            try {
                createCameraPreview();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            CameraHandler.this.cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice cameraDevice, int i) {
            try {
                CameraHandler.this.cameraDevice.close();
                CameraHandler.this.cameraDevice = null;
            }
            catch (NullPointerException exception) {
                exception.printStackTrace();
            }
        }
    };

    private void createCameraPreview() throws CameraAccessException {
        Log.v("tess","it's camerahandler createCameraPreview");
        SurfaceTexture texture = cameraActivity.adaptiveTextureView.getSurfaceTexture();
        texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface surface = new Surface(texture);

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        captureRequestBuilder.addTarget(surface);

        cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                if (cameraDevice == null) {
                    return;
                }

                cameraCaptureSession = session;
                try {
                    updatePreview();
                }
                catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {
                Toast.makeText(cameraActivity.getApplicationContext(), "Configuration Changed", Toast.LENGTH_SHORT).show();
            }
        }, null);
        Log.v("tess","it's camerahandler createCameraPreview end");
    }

    private void updatePreview() throws CameraAccessException {
        if (cameraDevice == null) {
            return;
        }

        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);

        Log.v("tess","it's camerahandler updatepreview");
    }

    private void setUpCameraOutputs(int width, int height) {
        cameraManager = (CameraManager) cameraActivity.getSystemService(Context.CAMERA_SERVICE);

        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);

                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (streamConfigurationMap == null) {
                    continue;
                }

                Size largest = Collections.max(Arrays.asList(streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
                imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, 1);
                imageReader.setOnImageAvailableListener(imageReaderListener, backgroundHandler);

                int displayRotation = cameraActivity.getWindowManager().getDefaultDisplay().getRotation();

                int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (sensorOrientation == 90 || sensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (sensorOrientation == 0 || sensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e("TAG", "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                cameraActivity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > 1920) {
                    maxPreviewWidth = 1920;
                }

                if (maxPreviewHeight > 1080) {
                    maxPreviewHeight = 1080;
                }

                previewSize = chooseOptimalSize(streamConfigurationMap.getOutputSizes(SurfaceTexture.class), rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight, largest);

                int orientation = cameraActivity.getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    cameraActivity.adaptiveTextureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
                } else {
                    cameraActivity.adaptiveTextureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
                }

                this.cameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.v("tess","it's camera handler setcamera outputs");
    }

    // 圖片，處理拿到圖片後要做甚麼事情
    private ImageReader.OnImageAvailableListener imageReaderListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            Log.v("tess","it's camera handler 我進來imagereader了");
            Image image = imageReader.acquireNextImage(); //image會從相機裡拿到第一張照片，我們只拿一張，拿過來後就是制式做法
            switch (image.getFormat()) {
                case ImageFormat.JPEG:
                    // Get bitmap from image
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer(); //把圖片轉成二進字的binary檔案
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null); //把檔案轉成bitmap，bitmap我們才能做事情
                    // Rotate bitmap by 90 degree //!!!上面的rotation是沒有屁用的
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    bitmapImage = Bitmap.createBitmap(bitmapImage, 0, 0, bitmapImage.getWidth(), bitmapImage.getHeight(), matrix, true);
                    // Get mat from bitmap (OpenCV)
                    String matFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/OriginalImage.png"; //決定路徑
                    //以下是openCV的寫法，把剛剛的照到的檔案存到手機的某個地方
                    Mat mat = new Mat(bitmapImage.getWidth(), bitmapImage.getHeight(), CvType.CV_8UC1);
                    Utils.bitmapToMat(bitmapImage, mat);//把bitmap轉成mat(openCV在用的東西) //看到一個mat可以當成一張圖
                    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGB); //都是openCV的寫法，轉成灰階
                    Imgcodecs.imwrite(matFilePath, mat);//再存它，存到剛剛定義的地方matFilePath
                    cameraActivity.uploadImage(); //upload到cameraactivity裡去看
                    image.close();
                    break;
            }
            Log.v("tess","it's camerahandeler 我完成imagereader了");
        }
    };

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e("TAG", "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public void configureTransform(int viewWidth, int viewHeight) {
        if (null == cameraActivity.adaptiveTextureView || null == previewSize) {
            return;
        }
        int rotation = cameraActivity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / previewSize.getHeight(),
                    (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        cameraActivity.adaptiveTextureView.setTransform(matrix);
    }

    private int getJpegOrientation(CameraCharacteristics c, int deviceOrientation) {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return 0;
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // Reverse device orientation for front-facing cameras
        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}