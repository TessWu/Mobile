package self.skzeratal.tilelocalization.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import self.skzeratal.tilelocalization.R;
import self.skzeratal.tilelocalization.croppedimage.CroppedImageActivity;

import static android.content.ContentValues.TAG;

public class CameraActivity extends Activity implements SensorEventListener {
    TextView inclineTextView;
    TextView directionTextView;
    Button returnButton;
    Button snapshotButton;
    SurfaceView surfaceView;
    AdaptiveTextureView adaptiveTextureView;
    CameraHandler cameraHandler;
    SensorManager sensorManager;

    private float[] mGravity;
    private float[] mGeomagnetic;
    private float[] angleValue360;
    private float[] angleValue540;
    private int count = 0;
    private final int size = 1;
    private float azimuth;
    private float angleOfCamera;

    private static final float buildingOrientaion = 130.0f;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        Log.v("tess","it's camera activity onCreate");
        azimuth = 0;
        angleValue360 = new float[size];
        angleValue540 = new float[size];
        cameraHandler = new CameraHandler(this);

        inclineTextView = findViewById(R.id.inclineTextView);
        directionTextView = findViewById(R.id.directionTextView);

        returnButton = findViewById(R.id.returnButton);
        returnButton.setOnClickListener(returnButtonOnClickListener);
        snapshotButton = findViewById(R.id.snapshotButton);
        snapshotButton.setOnClickListener(snapshotButtonOnClickListener);

        surfaceView = findViewById(R.id.surfaceView);
        surfaceView.setZOrderOnTop(true);

        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        surfaceHolder.addCallback(surfaceHolderCallback);

        adaptiveTextureView = findViewById(R.id.adaptiveTextureView);
        adaptiveTextureView.setSurfaceTextureListener(surfaceTextureListener);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CameraActivity.this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, 101);
            return; //這邊在拿權限的
        }
    }

    Button.OnClickListener returnButtonOnClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            finish();
        }
    };

    Button.OnClickListener snapshotButtonOnClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            try {
                cameraHandler.takePicture();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    };

    public void uploadImage()
    {
        Intent intent = new Intent(this, CroppedImageActivity.class); //要先指定一個intent類別的變數，並指定要開啟的activity，這裡表示在cameraActivity欲透或intent開啟croppedImageActivity
        intent.putExtra("Type", getIntent().getStringExtra("Type"));
        intent.putExtra("LastNameX", getIntent().getStringExtra("LastNameX"));
        intent.putExtra("LastNameY", getIntent().getStringExtra("LastNameY"));
        intent.putExtra("x", getIntent().getFloatExtra("x", 0f));
        intent.putExtra("y", getIntent().getFloatExtra("y", 0f));
        intent.putExtra("o", azimuth);
        intent.putExtra("a", angleOfCamera);
        startActivityForResult(intent, 0);
        Log.v("tess","it's upload image");
    }

    TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            cameraHandler.openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            cameraHandler.configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {  //畫綠色點的地方
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas == null) {
                Log.e(TAG, "Cannot draw onto the canvas as it's null");
            } else {
                Paint paint = new Paint();
                paint.setColor(Color.rgb(20, 150, 50));
                paint.setStrokeWidth(20);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(cameraHandler.OverlayLeft, cameraHandler.OverlayTop, cameraHandler.OverlayRight, cameraHandler.OverlayBottom, paint);
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    };

    @Override
    protected void onResume() {
        Log.v("tess","it's Camera onResume");
        super.onResume();
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
        cameraHandler.startBackgroundThread();
        if (adaptiveTextureView.isAvailable()) {
            cameraHandler.openCamera(adaptiveTextureView.getWidth(), adaptiveTextureView.getHeight());
        }
        else {
            adaptiveTextureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        Log.v("tess","it's camera onPause");
        try {
            cameraHandler.stopBackgroundThread();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {//要去和使用者拿權限
        if (requestCode == 101) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(getApplicationContext(), "Sorry, camera permission is necessary", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Intent intent = new Intent();
            intent.putExtra("X", data.getIntExtra("X", -1));
            intent.putExtra("Y", data.getIntExtra("Y", -1));
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
        else if (resultCode == 100) {
            Intent intent = new Intent();
            intent.putExtra("NameX", data.getStringExtra("NameX"));
            intent.putExtra("NameY", data.getStringExtra("NameY"));
            setResult(100, intent);
            finish();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                double value = sensorEvent.values[2];
                value = value - 0.15;
                value = Math.abs(value);
                if (value > 9.8) {
                    value = 9.8;
                }
                value = value / 9.8;
                value = Math.round(value * 100.0) / 100.0;
                value = Math.asin(value);
                value = Math.toDegrees(value);
                value = Math.round(value / 5) * 5;
                //if (value >= 45)
                //{
                //    snapshotButton.setEnabled(true);
                //}
                //else
                //{
                //    snapshotButton.setEnabled(false);
                //}
                angleOfCamera = (float) value;
                inclineTextView.setText(String.valueOf(value));
                mGravity = sensorEvent.values;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mGeomagnetic = sensorEvent.values;
                break;
        }

        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];

            if (SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)) {

                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);

                float azimuthInRadians = orientation[0];
                float azimuthInDegrees360 = (float) (Math.toDegrees(azimuthInRadians) + 360) % 360;
                float azimuthInDegrees540 = (float) (Math.toDegrees(azimuthInRadians) + 540) % 360;

                angleValue360[count % size] = azimuthInDegrees360;
                angleValue540[count % size] = azimuthInDegrees540;
                count++;

                float sum = 0;
                if (azimuthInDegrees360 > 315 || azimuthInDegrees360 < 45) {
                    for (float angle : angleValue540) {
                        sum += angle;
                    }

                    sum += 180 * size;
                }
                else {
                    for (float angle : angleValue360) {
                        sum += angle;
                    }
                }

                sum /= size;
                sum %= 360;
                sum = Math.round(sum);
                azimuth = sum;
                directionTextView.setText(String.valueOf(azimuth));
                // Log.d("azimuth", String.valueOf(azimuthInDegrees360));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
