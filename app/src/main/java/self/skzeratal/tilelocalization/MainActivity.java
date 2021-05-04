package self.skzeratal.tilelocalization;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.opencv.android.OpenCVLoader;

import self.skzeratal.tilelocalization.camera.CameraActivity;
import self.skzeratal.tilelocalization.floorplan.FloorPlanActivity;
import self.skzeratal.tilelocalization.video.VideoActivity;

public class  MainActivity extends AppCompatActivity implements OnPostExecuted {
    static
    {
        if (OpenCVLoader.initDebug())
        {
            Log.d("MainActivity", "OpenCV was configured or connected successfully");
        }
        else
        {
            Log.d("MainActivity", "Sorry, OpenCV was BAW");
        }
    }
    Button videoButton;   //button defined  corredponds to activity_main
    Button localizationButton; //button defined
    Button floorPlanButton; //button defined
    //Button testButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoButton = findViewById(R.id.videoButton);  //findViewbyId用來知道是哪一顆buttom//對應到我設計的頁面
        videoButton.setOnClickListener(menuButtonOnClickListener); //set listener，才知道按鈕被按了要幹嘛

        localizationButton = findViewById(R.id.localizationButton);
        localizationButton.setOnClickListener(menuButtonOnClickListener);

        floorPlanButton = findViewById(R.id.floorPlanButton);
        floorPlanButton.setOnClickListener(menuButtonOnClickListener);
    }

    Button.OnClickListener menuButtonOnClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent;

            switch (view.getId()) { //先看一下id是誰
                case R.id.localizationButton:
                    intent = new Intent(MainActivity.this, CameraActivity.class); //從哪裡要去哪裡 從mainactivity 要到cameraactivity
                    intent.putExtra("Type", "Localization");//用字串告訴他我現在要定位
                    startActivityForResult(intent, 0); //有兩種 startactivity 和 startactivityforresult ，若有activityfotresult他有可能會回來，所以要再寫一個onActivityResult
                    break;
                case R.id.floorPlanButton:
                    intent = new Intent(MainActivity.this, FloorPlanActivity.class);
                    intent.putExtra("Type", "UpdateFeature");//要更新我的特徵
                    startActivity(intent);
                    break;
                case R.id.videoButton:
                    intent = new Intent(MainActivity.this, VideoActivity.class);
                    startActivity(intent);
                    //SocketClient socketClient = new SocketClient(MainActivity.this);
                    break;

            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) { //對應到activityforresult會回來
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            int x = data.getIntExtra("X", -1);
            int y = data.getIntExtra("Y", -1);

            Log.e("TAG","X="+x);
            Log.e("TAG","Y="+y);


            if (x != -1 && y != -1) {
                Intent intent = new Intent(MainActivity.this, FloorPlanActivity.class);
                intent.putExtra("Type", "Localization");
                intent.putExtra("X", x);
                intent.putExtra("Y", y);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onPostExecuted(String result) {

    }

}