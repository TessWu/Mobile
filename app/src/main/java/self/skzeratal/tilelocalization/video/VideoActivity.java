package self.skzeratal.tilelocalization.video;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ViewUtils;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.VideoView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.ContentValues.TAG;

import self.skzeratal.tilelocalization.OnPostExecuted;
import self.skzeratal.tilelocalization.R;
import self.skzeratal.tilelocalization.SocketClient;
import self.skzeratal.tilelocalization.croppedimage.CroppedImageActivity;

public class  VideoActivity extends AppCompatActivity implements OnPostExecuted {

    private static final int REQUEST_CAMERA_PERMISSION_RESULT=0;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT=1;
    private static final int STATE_PREVIEW= 0;
    private static final int STATE_WAIT_LOCK= 1;
    private int mCaptureState = STATE_PREVIEW;
    private TextureView mTextureView;
    //public  SurfaceView mSurfaceView;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            setupCamera(i, i1);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {

            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };
    private CameraDevice mcameraDevice; //設置camera
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() { //cameraCallback回調接口
        //若鏡頭打開成功則callback此方法
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mcameraDevice = cameraDevice;
            mMediaRecorder=new MediaRecorder();
            if (mIsRecording){
                try {
                    creatVideoFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startRecord();
                mMediaRecorder.start();
            }else {
                startPreview();
            }
            //Toast.makeText(getApplicationContext(),"Camera connection made!",Toast.LENGTH_SHORT).show();
        }
        //若鏡頭連接段開則callback此方法
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            mcameraDevice = null;
        }
        //出現異常則回調此方法
        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            mcameraDevice = null;
        }
    };

    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;
    private String mCameraId;//設置camera ID，因為有前後鏡頭
    private Size mPreviewSize;
    private Size mVideoSize;
    private Size mImageSize;
    @Override
    public void onPostExecuted(String result) {
                if (result.equals("BLURRY")) {
                    Log.d("tess","it's too blurry");
                }
                else if (result.equals("DAMAGED1")) {
                    Log.d("tess","it's DAMAGED1");
                }
                else if (result.equals("DAMAGED2")) {
                    Log.d("tess","it's DAMAGED2");
                }
                else {
                        try
                        {
                            Log.d("tess","try");

                        }
                        catch (StringIndexOutOfBoundsException ex)
                        {
                            Log.d("tess","連線不穩定");
                        }
                    }

    }
    private ImageReader mImageReader;
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            mBackgroundHandler.post(new ImageSaver(imageReader.acquireLatestImage()));
        }
    };//為image設一個Listener，一但拍到了一個image，則這個listener會通知我們



    private class ImageSaver implements Runnable{
        private final Image mImage;
        public ImageSaver(Image image) {
            mImage=image;
        }
        @Override
        public void run() {
            Log.v("tess","it's video imagesaver");
            ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer(); //把圖片轉成二進字的binary檔案
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);//put data into byte buffer

            //put those bytes into file =put it into camera storage
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(mImageFileName);
                fileOutputStream.write(bytes);
                // uploadImage();
            } catch (IOException e) {
                e.printStackTrace();
            }finally { //clean up resource
                Log.v("tess","it's video mimage.close()");
                //uploadImage();
                mImage.close();
                Log.v("tess","it's video mimage.close() end");

                if (fileOutputStream != null){
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
            Log.v("tess","it's video imagesaver end");
            //uploadImage();
        }
    }
    private MediaRecorder mMediaRecorder;
    private Chronometer mChronometer;
    private int mTotalRotation;
    private CameraCaptureSession mPreviewCaptureSession;
    private CameraCaptureSession.CaptureCallback mPreviewCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult captureResult){
            switch (mCaptureState){
                case STATE_PREVIEW:
                    //Do nothing
                    break;
                case STATE_WAIT_LOCK:
                    mCaptureState=STATE_PREVIEW;
                    Integer afState = captureResult.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED){
                        Toast.makeText(getApplicationContext(),"AF LOCKED!!", Toast.LENGTH_SHORT).show();
                         startStillCaptureRequest();
                    }
                    break;
            }
        }
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            process(result);
        }
    };
//    private CameraCaptureSession mRecordCaptureSession;
//    private CameraCaptureSession.CaptureCallback mRecordCaptureCallback = new CameraCaptureSession.CaptureCallback() {
//
//        private void process(CaptureResult captureResult){
//            switch (mCaptureState){
//                case STATE_PREVIEW:
//                    //Do nothing
//                    break;
//                case STATE_WAIT_LOCK:
//                    mCaptureState=STATE_PREVIEW;
//                    Integer afState = captureResult.get(CaptureResult.CONTROL_AF_STATE);
//                    if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED){
//                        Toast.makeText(getApplicationContext(),"已截圖!!", Toast.LENGTH_SHORT).show();
//                        startStillCaptureRequest();
//                    }
//                    break;
//            }
//        }
//        @Override
//        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
//            super.onCaptureCompleted(session, request, result);
//
//            process(result);
//        }
//    };
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private ImageButton mStillImageButton;
    private ImageButton mrecordingButton;//設置錄影按鈕
    private Button stop;
    private boolean mIsRecording=false; //set boolean to track whether I am recording or not

    private Timer timer;
    private Task1 task1;
    long delay=0;
    long intervalPeriod = 1*1000;

    VideoHandler videoHandler;

    private File mImageFolder; //放image的地方
    private String mImageFileName;   //每一個image的名字
    private File mVideoFolder; //放video的地方
    private String mVideoFileName;   //每一個video的名字

    private static SparseIntArray Orientations = new SparseIntArray(); //有時候camera和手機上的角度會錯
    static {
        Orientations.append(Surface.ROTATION_0, 0);
        Orientations.append(Surface.ROTATION_90, 90);
        Orientations.append(Surface.ROTATION_180, 180);
        Orientations.append(Surface.ROTATION_270, 270);

    }

    private static class CompareSizeArea implements Comparator<Size> {

        @Override
        public int compare(Size size, Size t1) {
            return Long.signum((long) size.getWidth() * size.getHeight() / (long) t1.getWidth() * t1.getHeight());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        timer = new Timer();
        Log.v("tess","its onStart");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        Log.v("tess","it's onCreate");

        createVideoFolder();
        createImageFolder();
        mMediaRecorder=new MediaRecorder();

        mChronometer =(Chronometer) findViewById(R.id.chronometer);
        mTextureView = (TextureView) findViewById(R.id.textureView);
        mStillImageButton=(ImageButton) findViewById(R.id.cameraButton);
        mStillImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("tess","我要拍照了");
                task1=new Task1();
                timer.schedule(task1,delay,intervalPeriod);
                //lockFocus();
                //uploadImage();
            }
        });
        mrecordingButton=(ImageButton)findViewById(R.id.recordingButton);
//        mSurfaceView=(SurfaceView)findViewById(R.id.surfaceView);
//        mSurfaceView.setZOrderOnTop(true);
        videoHandler = new VideoHandler(this);
//        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
//        surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
//        surfaceHolder.addCallback(surfaceHolderCallback);
        //mrecordingButton=(ImageButton)findViewById(R.id.recordingButton);
        mrecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mIsRecording){
                    mChronometer.stop();
                    mChronometer.setVisibility(View.INVISIBLE);
                    mIsRecording=false;
                    mrecordingButton.setImageResource(R.mipmap.btn_video_online);
                    startPreview();
                    mMediaRecorder.stop();
                    mMediaRecorder.reset();
                }else{

                    checkWriteStoragePermission();
                }
            }
        });

        stop=(Button) findViewById(R.id.stopButton);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (task1!=null){
                    task1.cancel();
                    task1=null;
                }
                uploadImage();
                Log.v("tess","我按下暫停ㄌ");
            }
        });
    }

    public void uploadImage(){

      File passimage = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/Pictures/tilevideo/IMAGE_20210504_002255_5746818625810147843.png");
      int passimageSize =(int) passimage.length();
      byte[] passimageBytes = new byte[passimageSize];

        try {
            BufferedInputStream passimageBuffer = new BufferedInputStream(new FileInputStream(passimage));
            passimageBuffer.read(passimageBytes,0,passimageBytes.length);
            passimageBuffer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String passimagePayload = Base64.encodeToString(passimageBytes,Base64.DEFAULT);
        Log.v("tess","開啟soclet了");
        SocketClient socketClient1 = new SocketClient(VideoActivity.this);
        socketClient1.execute("MATCH",passimagePayload);
        socketClient1.Stop();
    }


//    SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
//        @Override
//        public void surfaceCreated(SurfaceHolder surfaceHolder) {
//            Canvas canvas = surfaceHolder.lockCanvas();
//            if (canvas == null) {
//                Log.e(TAG, "Cannot draw onto the canvas as it's null");
//            }else{
//                Paint paint = new Paint();
//                paint.setColor(Color.rgb(250,214,137));
//                paint.setStrokeWidth(10);
//                paint.setStyle(Paint.Style.STROKE);
//                //canvas.drawRect(videoHandler.OverlayLeft,videoHandler.OverlayTop,videoHandler.OverlayRight,videoHandler.OverlayBottom,paint);
//                canvas.drawLine(videoHandler.Overstarx1,videoHandler.Overstarty1,videoHandler.Overendx1,videoHandler.Overendy1,paint);
//                canvas.drawLine(videoHandler.Overstarx2,videoHandler.Overstarty2,videoHandler.Overendx2,videoHandler.Overendy2,paint);
//                surfaceHolder.unlockCanvasAndPost(canvas);
//            }
//        }
//
//        @Override
//        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
//
//        }
//
//        @Override
//        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
//
//        }
//    };
    @Override
    protected void onResume() { //一開始mTextureView一定是on available
        super.onResume();

        startBackgroundThread();

        if (mTextureView.isAvailable()) {
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
            connectCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
        Log.v("tess","it's onResume");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {//要去和使用者拿權限
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION_RESULT) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(getApplicationContext(), "Sorry, camera permission is necessary", Toast.LENGTH_SHORT).show();
            }
            Log.v("tess","it's video onRequestPermissionsResult");
        }
        if (requestCode==REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT){
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                mIsRecording=true;
                mrecordingButton.setImageResource(R.mipmap.btn_video_busy);
                try {
                    creatVideoFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Toast.makeText(this,"Permission successfully granted!",Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this,"App needs to save video to run", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();

        if(timer!=null){
            timer.cancel();
            timer.purge();
            timer=null;
        }
        Log.v("tess","it's onPause");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) { //讓畫面變成全螢幕
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if (hasFocus) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    private void setupCamera(int width, int height) { //這邊只要後面的鏡頭，不要前置鏡頭，因為第一個cameraId是前鏡頭，所以我們要忽略它
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap streamConfigurationMap =cameraCharacteristics.get(cameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if(streamConfigurationMap==null){
                    continue;
                }

                Size largest = Collections.max(Arrays.asList(streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)), new CompareSizeArea());
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                mTotalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation = mTotalRotation == 90 || mTotalRotation == 270;

                Point displaySize= new Point();
                getWindowManager().getDefaultDisplay().getSize(displaySize);
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if (swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > 1920) {//改這邊如果maxPrevieWidth = 1080，螢幕上的解析度會出現問題
                    maxPreviewWidth = 1920;
                }

                if (maxPreviewHeight > 1080) {
                    maxPreviewHeight = 1080;
                }

                //mPreviewSize = chooseOptimalpreviewSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight,maxPreviewWidth,maxPreviewHeight,largest);
                mPreviewSize=chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                mVideoSize = chooseOptimalSize(map.getOutputSizes(MediaRecorder.class), rotatedWidth, rotatedHeight);
                mImageSize = chooseOptimalSize(map.getOutputSizes(ImageFormat.JPEG), rotatedWidth, rotatedHeight);
                mImageReader =ImageReader.newInstance(mImageSize.getWidth(),mImageSize.getHeight(),ImageFormat.JPEG,20);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener,mBackgroundHandler);
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.v("tess","it's video setup camera");

    }

    private void  connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.M){
                if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){
                    cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
                }else{
                    if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                        Toast.makeText(this,"Video app requires access to camera ", Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[] {Manifest.permission.CAMERA},REQUEST_CAMERA_PERMISSION_RESULT);
                }
            }else {
                cameraManager.openCamera(mCameraId,mCameraDeviceStateCallback,mBackgroundHandler);
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.v("tess","it's video connect camera");
    }

    private void closeCamera() {
        if (mcameraDevice != null) {
            mcameraDevice.close();
            mcameraDevice = null;
        }
        Log.v("tess","it's video close camera");
    }

    private void startRecord(){//開始錄影 前3行是要知道自己在錄甚麼
        try {
            Log.v("tess","it's video startRecord ");
            setupMediaRecorder();
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            Surface recordSurface= mMediaRecorder.getSurface();
            mCaptureRequestBuilder=mcameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mCaptureRequestBuilder.addTarget(previewSurface);
            mCaptureRequestBuilder.addTarget(recordSurface);
            //create capture builder
            mcameraDevice.createCaptureSession(Arrays.asList(previewSurface, recordSurface,mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //mRecordCaptureSession=cameraCaptureSession;
                    try {
                        cameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),null,null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession  cameraCaptureSession) {

                }
            },null);

        } catch (IOException | CameraAccessException e) {
            e.printStackTrace();
        }
        Log.v("tess","it's video startRecord  end");
    }
    private void startPreview(){ //可以顯示相機畫面
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);

        try {
            mCaptureRequestBuilder= mcameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mcameraDevice.createCaptureSession(Arrays.asList(previewSurface,mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mPreviewCaptureSession=cameraCaptureSession;
                    try {
                        mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),null,mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        Toast.makeText(getApplicationContext(),"Unable to setup camera preview", Toast.LENGTH_SHORT).show();
                }
            },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.v("tess","it's video startpreview");
    }

    private void startStillCaptureRequest(){ //part17
        try {
            mCaptureRequestBuilder = mcameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,mTotalRotation);//有旋轉問題要修正

            CameraCaptureSession.CaptureCallback stillCaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                    try {
                        createImageFileName();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
                mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), stillCaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.v("tess","it's video startstillcapturerequest");
    }

    private void startBackgroundThread() {  //start the backgroundthread on the onResume
        mBackgroundHandlerThread = new HandlerThread("Recording");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() { //stop the background thread on the onPause
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = Orientations.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

//    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
//        final double ASPECT_TOLERANCE=0.1;
//        double targetRation = (double) height/width;
//
//        if (choices==null) return null;
//        Size optimalSize= null;
//        double minDiff =Double.MAX_VALUE;
//        int targetHeight = height;
//
//        for (Size size:choices){
//            double ratio=(double) size.getWidth();
//        }
//
//        List<Size> bigEnough = new ArrayList<Size>();
//        for (Size option : choices) {
//            if (option.getHeight() == option.getWidth() * width / height && option.getWidth() >= width && option.getHeight() >= height) {
//                bigEnough.add(option);
//            }
//        }
//        if (bigEnough.size() > 0) {
//            return Collections.min(bigEnough, new CompareSizeArea());
//        } else {
//            return choices[0];
//        }
//    }
    private static Size chooseOptimalpreviewSize(Size[]  choices, int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio){
        List<Size> bigEnough = new ArrayList<>();
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();

        for(Size option: choices){
            if(option.getWidth()<maxWidth && option.getHeight()<=maxHeight&&option.getHeight()==option.getWidth()*h/w){
                if(option.getWidth()>=textureViewWidth&&option.getHeight()>=textureViewHeight){
                    bigEnough.add(option);
                }else {
                notBigEnough.add(option);
            }
        }
    }
    if(bigEnough.size()>0){
            return Collections.min(bigEnough, new CompareSizeArea());
        }else if (notBigEnough.size()>0){
            return Collections.max(notBigEnough,new CompareSizeArea());
        }else {
            Log.e("TAG","Could  find any suitable preview size");
            return choices[0];
        }
    }

    private Size chooseOptimalSize(Size sizes[], int width, int height){
        final double ASPECT_TOLERANCE=0.1;
        double targetRation=(double)height/width;

        if(sizes == null)  return null;
        Size optimalSize = null;
        double minDiff= Double.MAX_VALUE;

        int targetHeight= height;
        for (Size size:sizes){
            double ratio=(double)size.getWidth()/size.getHeight();
            if (Math.abs(ratio-targetRation)>ASPECT_TOLERANCE){
                continue;
            }
            if (Math.abs(size.getHeight()-targetHeight)<minDiff){
                optimalSize=size;
                minDiff=Math.abs(size.getHeight()-targetHeight);
            }
        }
        if(optimalSize==null){
            minDiff =Double.MAX_VALUE;
            for(Size size:sizes){
                if(Math.abs(size.getHeight()-targetHeight)<minDiff){
                    optimalSize=size;
                    minDiff=Math.abs(size.getHeight()-targetHeight);
                }
            }
        }  return optimalSize;
    }
    public void createVideoFolder(){ //創一個我要放video的地方
        File movieFile= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        mVideoFolder=new File(movieFile,"tileVideo");
        //check if the folder exists or not
        if (mVideoFolder.exists()){
            mVideoFolder.mkdirs();
        }
        Log.v("tess","it's video createVideoFolder()");
    }
    public File creatVideoFileName() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());//如何命名
        String prepend= "VIDEO_"+ timestamp;
        File videoFile = File.createTempFile(prepend, ".mp4", mVideoFolder);//真正的檔案名
        mVideoFileName=videoFile.getAbsolutePath();
        Log.v("tess","it's video createVideoFile()");
        return videoFile;

    }

    public void createImageFolder(){ //創一個我要放image的地方
        File imageFile= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        mImageFolder=new File(imageFile,"tileVideo");
        //check if the folder exists or not
        if (mImageFolder.exists()){
            mImageFolder.mkdirs();
        }
        Log.v("tess","it's video createImageFolder");
    }
    public File createImageFileName() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());//如何命名
        String prepend= "IMAGE_"+ timestamp+ "_";
        File imageFile = File.createTempFile(prepend, ".png", mImageFolder);//真正的檔案名
        mImageFileName=imageFile.getAbsolutePath();
        Log.v("tess","it's video createimagefilename");
        return imageFile;

    }

    public void checkWriteStoragePermission(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){//版本是否大於marshmellow
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED){
                    mIsRecording=true;
                    mrecordingButton.setImageResource(R.mipmap.btn_video_busy);
                    try {
                        creatVideoFileName();
                        } catch (IOException e) {
                        e.printStackTrace();
                    }
                    startRecord();
                    mMediaRecorder.start();
                    mChronometer.setBase(SystemClock.elapsedRealtime()); //part15
                    mChronometer.setVisibility(View.VISIBLE);
                    mChronometer.start(); //start the timer
                }else {
                    if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                        Toast.makeText(this,"app needs to be able to save videos",Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT);
                }
        }else {
                mIsRecording=true;
                mrecordingButton.setImageResource(R.mipmap.btn_video_busy);
                try {
                    creatVideoFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startRecord();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMediaRecorder.start();
                        mChronometer.setBase(SystemClock.elapsedRealtime()); //part15
                        mChronometer.setVisibility(View.VISIBLE);
                        mChronometer.start(); //start the timer
                    }
                });

            }
    }

        private void setupMediaRecorder() throws IOException{
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            //mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setOutputFile(mVideoFileName);
            mMediaRecorder.setVideoEncodingBitRate(5*1024*1024);
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            //mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setOrientationHint(mTotalRotation);
            //mMediaRecorder.setMaxFileSize(100000000);
            mMediaRecorder.prepare();
            Log.v("tess","it's video setupmediarecorder");
        }

        private  class Task1 extends TimerTask {

            @Override
            public void run() {
                Log.v("tess","it's task1 start");
                mCaptureState = STATE_WAIT_LOCK;
                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
                try {
                    mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), mPreviewCaptureCallback, mBackgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                Log.v("tess","it's task1 end");
            }

        }
        private void lockFocus(){
        mCaptureState = STATE_WAIT_LOCK;
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START);
            try {
                mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(),mPreviewCaptureCallback,mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
}
