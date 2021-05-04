package self.skzeratal.tilelocalization.croppedimage;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import self.skzeratal.tilelocalization.BuildConfig;
import self.skzeratal.tilelocalization.OnPostExecuted;
import self.skzeratal.tilelocalization.R;
import self.skzeratal.tilelocalization.SocketClient;

public class CroppedImageActivity extends Activity implements OnPostExecuted {
    private TextView textView;
    private EditText editTextX;
    private EditText editTextY;
    private Button uploadButton;
    private String type;
    private float angleOfCamera;
    private float orientation;
    private float var;
    private boolean canQuit;

    protected void onCreate(Bundle savedInstanceState) {
        Log.v("tess","it's croppedimage onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_croppedimage);
        if (angleOfCamera < 45/* || var < 100*/)
        {
            canQuit = true;
        }
        else
        {
            canQuit = false;
        }
        //剛進來會有三張圖，一張是原本的圖，一張是切過的圖，另一張是切了又轉的圖
        Uri originalImageUri = Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/OriginalImage.png");
        Uri croppedImageUri = Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/CroppedImage.png");
        Uri AdjustedImageUri = Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/AdjustedImage.png");
        Log.v("tess","it's  croppedimage我得到3張照片");

        orientation = getIntent().getFloatExtra("o", 0f) - 125 + 45; //建築物的角度-125 +45是固定45
        angleOfCamera = getIntent().getFloatExtra("a", 0f);
        cropAndAdjustImage (originalImageUri, croppedImageUri, AdjustedImageUri, orientation);

        editTextX = findViewById(R.id.editTextX);
        editTextY = findViewById(R.id.editTextY);
        ImageView imageView = findViewById(R.id.imageView);
        imageView.setImageURI(croppedImageUri);
        Button returnButton = findViewById(R.id.returnButton);
        returnButton.setOnClickListener(returnButtonOnClickListener);
        uploadButton = findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(uploadButtonOnClickListener);
        Button buttonXPlus = findViewById(R.id.buttonXPlus);
        buttonXPlus.setOnClickListener(buttonXPlusOnClickListener);
        Button buttonYPlus = findViewById(R.id.buttonYPlus);
        buttonYPlus.setOnClickListener(buttonYPlusOnClickListener);
        Button buttonXMinus = findViewById(R.id.buttonXMinus);
        buttonXMinus.setOnClickListener(buttonXMinusOnClickListener);
        Button buttonYMinus = findViewById(R.id.buttonYMinus);
        buttonYMinus.setOnClickListener(buttonYMinusOnClickListener);
        textView = findViewById(R.id.textView);
        TextView textViewX = findViewById(R.id.textViewX);
        TextView textViewY = findViewById(R.id.textViewY);
        type = getIntent().getStringExtra("Type");
        String lastNameX = getIntent().getStringExtra("LastNameX");
        String lastNameY = getIntent().getStringExtra("LastNameY");
        textView.setText("");
        textView.setTextColor(Color.BLACK);
        switch (type) {
            case "Localization": //這邊是使用者，把調名字那些的隱形，這樣他們就看不到了
                buttonXPlus.setVisibility(View.INVISIBLE);
                buttonYPlus.setVisibility(View.INVISIBLE);
                buttonXMinus.setVisibility(View.INVISIBLE);
                buttonYMinus.setVisibility(View.INVISIBLE);
                editTextX.setVisibility(View.INVISIBLE);
                editTextY.setVisibility(View.INVISIBLE);
                textViewX.setVisibility(View.INVISIBLE);
                textViewY.setVisibility(View.INVISIBLE);
                break;
            case "UpdateFeature": //管理員就看得到
                buttonXPlus.setVisibility(View.VISIBLE);
                buttonYPlus.setVisibility(View.VISIBLE);
                buttonXMinus.setVisibility(View.VISIBLE);
                buttonYMinus.setVisibility(View.VISIBLE);
                editTextX.setVisibility(View.VISIBLE);
                editTextY.setVisibility(View.VISIBLE);
                textViewX.setVisibility(View.VISIBLE);
                textViewY.setVisibility(View.VISIBLE);
                editTextX.setText(lastNameX);
                editTextY.setText(lastNameY);
                break;
        }
    }
    //return 回到上一頁
    Button.OnClickListener returnButtonOnClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            finish();
        }
    };
    //upload要傳2張，一張是切過的，一張是轉完的圖片
    Button.OnClickListener uploadButtonOnClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.v("tess","it's  croppedimage uploadButton");
            textView.setText("");
            textView.setTextColor(Color.BLACK);
            uploadButton.setEnabled(false);
            File croppedImage = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/CroppedImage.png"); //有切的圖片
            File adjustedImage = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/AdjustedImage.png"); //已經轉完(調整過角度)的圖片
            //以下是把圖用成byte的形式傳
            int croppedImageSize = (int) croppedImage.length();
            int adjustedImageSize = (int) adjustedImage.length();
            byte[] croppedImageBytes = new byte[croppedImageSize];
            byte[] adjustedImageBytes = new byte[adjustedImageSize];
            try {
                BufferedInputStream croppedImageBuffer = new BufferedInputStream(new FileInputStream(croppedImage));
                BufferedInputStream adjustedImageBuffer = new BufferedInputStream(new FileInputStream(adjustedImage));
                croppedImageBuffer.read(croppedImageBytes, 0, croppedImageBytes.length);
                croppedImageBuffer.close();
                adjustedImageBuffer.read(adjustedImageBytes, 0, adjustedImageBytes.length);
                adjustedImageBuffer.close();
                Log.v("tess","it's  croppedimage uploadButtonend");
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            //byte[] croppedImageBytes = Files.readAllBytes(croppedImage.getAbsolutePath());
            //byte[] adjustedImageBytes = Files.readAllBytes(adjustedImage.getAbsolutePath());
            //把圖加密 把binary轉成字串 croppedImagePayload代表那張圖片
            String croppedImagePayload = Base64.encodeToString(croppedImageBytes, Base64.DEFAULT);
            String adjustedImagePayload = Base64.encodeToString(adjustedImageBytes, Base64.DEFAULT);

            //開socket去傳東西
            Log.v("tess","it's  croppedimage 開啟socket了");
            SocketClient socketClient1 = new SocketClient(CroppedImageActivity.this);
            SocketClient socketClient2 = new SocketClient(CroppedImageActivity.this);
            //會到SocketClien new一個socket

            switch (type) {
                case "Localization": //我要定位用的
                    socketClient1.execute("MATC1", croppedImagePayload, String.valueOf(angleOfCamera)); //server那邊會有一個字串識別，看"MATC1"是甚麼東西我就做甚麼事情，我的圖片跟我拍照的角度
                    Log.v("tess","croppedimage 我傳了croppedimagepayloade過去");
                    if (angleOfCamera >= 60 && var >= 100)//如果大於60度把第2張也傳過去
                    {
                        socketClient2.execute("MATC2", adjustedImagePayload, String.valueOf(angleOfCamera));
                        Log.v("tess","croppedimage 我傳了adjustimagepayloade過去");
                    }
                    socketClient1.Stop();
                    socketClient2.Stop();
                    Log.v("tess","it's  croppedimage 關閉socke了");
                    break;
                case "UpdateFeature": //我要抓特徵用得
                    String name = "X" + editTextX.getText().toString() + "Y" + editTextY.getText().toString();
                    String x = String.valueOf(getIntent().getFloatExtra("x", 0f));
                    String y = String.valueOf(getIntent().getFloatExtra("y", 0f));

                    socketClient1.execute("STORE", adjustedImagePayload, name, x, y, String.valueOf(angleOfCamera));
                    socketClient2.Stop();
                    break;
            }
        }
    };
    //建模用的xy，只是讓使用者可以按的，不用在那邊輸入
    Button.OnClickListener buttonXPlusOnClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            String currentX = editTextX.getText().toString();
            editTextX.setText(String.valueOf(Integer.parseInt(currentX) + 1));
        }
    };

    Button.OnClickListener buttonYPlusOnClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            String currentY = editTextY.getText().toString();
            editTextY.setText(String.valueOf(Integer.parseInt(currentY) + 1));
        }
    };

    Button.OnClickListener buttonXMinusOnClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            String currentX = editTextX.getText().toString();
            editTextX.setText(String.valueOf(Integer.parseInt(currentX) - 1));
        }
    };

    Button.OnClickListener buttonYMinusOnClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            String currentY = editTextY.getText().toString();
            editTextY.setText(String.valueOf(Integer.parseInt(currentY) - 1));
        }
    };

    public void onPostExecuted(String result) { //result=server傳到queue的東西
        switch (type) {
            case "Localization":
                if (result.equals("BLURRY")) {
                    textView.setText("The Image is too Blurry");
                    textView.setTextColor(Color.RED);
                    uploadButton.setEnabled(true);
                }
                else if (result.equals("DAMAGED1")) {
                    textView.setText("連線不穩定1");
                    textView.setTextColor(Color.RED);
                    uploadButton.setEnabled(true);
                }
                else if (result.equals("DAMAGED2")) {
                    textView.setText("連線不穩定2");
                    textView.setTextColor(Color.RED);
                    uploadButton.setEnabled(true);
                }
                else {
                    textView.setText("MATC1 成功");
                    if (canQuit)
                    {
                        try
                        {
                            String name = result.substring(0, result.indexOf(","));
                            result = result.substring(result.indexOf(",") + 1);
                            int x = Math.round(Float.parseFloat(result.substring(0, result.indexOf(","))));
                            int y = Math.round(Float.parseFloat(result.substring(result.indexOf(",") + 1, result.indexOf(";"))));

                            Log.d("tess",String.valueOf(x));
                            Log.d("tess",String.valueOf(y));

                            Intent intent = new Intent();

                            intent.putExtra("Name", name);
                            intent.putExtra("X", x);
                            intent.putExtra("Y", y);
                            setResult(Activity.RESULT_OK, intent);
                            finish();
                        }
                        catch (StringIndexOutOfBoundsException ex)
                        {
                            textView.setText("連線不穩定3");
                            textView.setTextColor(Color.RED);
                            uploadButton.setEnabled(true);
                        }
                    }
                    else
                    {
                        canQuit = true;
                    }
                }
                break;
            case "UpdateFeature":
                if (result.equals("NAMEEXISTED")) {
                    textView.setText("The Name Already Exist");
                    textView.setTextColor(Color.RED);
                }
                else if (result.equals("BLURRY")) {
                    textView.setText("The Image is too Blurry");
                    textView.setTextColor(Color.RED);
                }
                else if (result.equals("DAMAGED1")) {
                    textView.setText("連線不穩定");
                    textView.setTextColor(Color.RED);
                }
                else if (result.equals("DAMAGED2")) {
                    textView.setText("連線不穩定");
                    textView.setTextColor(Color.RED);
                }
                else if (result.equals("SUCCESS")) {
                    Intent intent = new Intent();
                    intent.putExtra("NameX", editTextX.getText().toString());
                    intent.putExtra("NameY", editTextY.getText().toString());
                    setResult(100, intent);
                    finish();
                }
                break;
        }
    }
     //如何切
    private void cropAndAdjustImage(Uri originalImageUri, Uri croppedImageUri, Uri AdjustedImageUri, float orientation)
    {
        Log.v("tess","it's  croppedimage cropandadjustimage");
        Mat originalImage = Imgcodecs.imread(originalImageUri.toString()); //把圖片讀出來
        int middleX = (int) originalImage.width() / 2;  //一張圖先取中間點
        int middleY = (int) originalImage.height() / 2;
        int length; //要切的正方形的長的1/4
        if (originalImage.width() > originalImage.height()) //橫拍
        {
            length = (int) (originalImage.height() * 0.25);
        }
        else
        {
            length = (int) (originalImage.width() * 0.25);//直拍
        }
        Rect rect = new Rect(middleX - length, middleY - length, length * 2, length * 2); //會畫一個矩形
        Mat croppedImage = new Mat(originalImage, rect); //切完
        Imgcodecs.imwrite(croppedImageUri.toString(), croppedImage); //切完後存起來，存在剛剛的地方
        Mat adjustedImage = new Mat(length * 2, length * 2, CvType.CV_8UC4); //這邊是旋轉

        //以下都是openCV的寫法
        List<Point> destination = new ArrayList<>(); //來轉圖片
        //以下會有4個點會存座標，筆記第p215頁，把黑色變成紅色
        destination.add(new org.opencv.core.Point(0, length * 2));
        destination.add(new org.opencv.core.Point(0, 0));
        destination.add(new org.opencv.core.Point(length * 2, 0));
        destination.add(new org.opencv.core.Point(length * 2, length * 2));
        Mat destinationMat = Converters.vector_Point2f_to_Mat(destination);
        //以上是算轉成甚麼地方
        int radius = (int) Math.round(Math.pow(2, 0.5) * length);
        List<Point> source = new ArrayList<>();
        source.add(new org.opencv.core.Point(middleX + radius * Math.cos(Math.toRadians((180 - orientation) % 360)),middleY + radius * Math.sin(Math.toRadians((180 - orientation) % 360))));
        source.add(new org.opencv.core.Point(middleX + radius * Math.cos(Math.toRadians((270 - orientation) % 360)),middleY + radius * Math.sin(Math.toRadians((270 - orientation) % 360))));
        source.add(new org.opencv.core.Point(middleX + radius * Math.cos(Math.toRadians((-orientation) % 360)),middleY + radius * Math.sin(Math.toRadians((-orientation ) % 360))));
        source.add(new org.opencv.core.Point(middleX + radius * Math.cos(Math.toRadians((90 - orientation) % 360)),middleY + radius * Math.sin(Math.toRadians((90 - orientation) % 360))));
        Mat sourceMat = Converters.vector_Point2f_to_Mat(source);
        //以上是算它在哪裡

        //以下三行是把圖轉正
        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(sourceMat, destinationMat);
        Imgproc.warpPerspective(originalImage, adjustedImage, perspectiveTransform, new org.opencv.core.Size(length * 2, length * 2), Imgproc.INTER_CUBIC);
        Imgcodecs.imwrite(AdjustedImageUri.toString(), adjustedImage);//一開始用string，這邊用uri然後再轉成string

        //以下是在算清晰程度
        Mat laplacianDestinationMat = new Mat();
        Mat laplacianGrayMat = new Mat();

        Imgproc.cvtColor(croppedImage, laplacianGrayMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Laplacian(laplacianGrayMat, laplacianDestinationMat, 3);
        MatOfDouble median = new MatOfDouble();
        MatOfDouble standard = new MatOfDouble();
        Core.meanStdDev(laplacianDestinationMat, median , standard);

        var = (float) Math.pow(standard.get(0,0)[0],2);
        Log.v("tess","it's  croppedimage cropandadjustimagend");
    }
}