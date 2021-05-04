package self.skzeratal.tilelocalization.video;

import android.content.Intent;
import android.graphics.Point;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import self.skzeratal.tilelocalization.camera.CameraActivity;
import self.skzeratal.tilelocalization.croppedimage.CroppedImageActivity;

public class VideoHandler {

    private VideoActivity videoActivity;
    private CameraManager cameraManager;
    private File mVideoFolder; //放video的地方
    private String mVideoFileName;   //每一個video的名字


    public int Overstarx1;
    public int Overstarty1;
    public int Overendx1;
    public int Overendy1;
    public int Overstarx2;
    public int Overstarty2;
    public int Overendx2;
    public int Overendy2;

    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;

    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    public VideoHandler(VideoActivity videoActivity) {
        this.videoActivity = videoActivity;
        Point point = new Point();
        videoActivity.getWindowManager().getDefaultDisplay().getSize(point);
        Overstarx1 = (int) (point.x * 0.027);
        Overstarty1 = (int) (point.y * 0.013);
        Overendx1 = (int) (point.x * 0.027);
        Overendy1 = (int) (point.y * 0.618);
        Log.d("TAG", "starx=" + String.valueOf(Overstarx1));
        Log.d("TAG", "stary=" + String.valueOf(Overstarty1));
        Log.d("TAG", "endx=" + String.valueOf(Overendx1));
        Log.d("TAG", "endy=" + String.valueOf(Overendy1));
        Overstarx2 = (int) (point.x * 0.973);
        Overstarty2 = (int) (point.y * 0.013);
        Overendx2 = (int) (point.x * 0.973);
        Overendy2 = (int) (point.y * 0.618);
    }



}


