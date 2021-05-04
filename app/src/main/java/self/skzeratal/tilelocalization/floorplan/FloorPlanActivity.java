package self.skzeratal.tilelocalization.floorplan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.lang.reflect.Type;
import java.util.ArrayList;

import self.skzeratal.tilelocalization.OnPostExecuted;
import self.skzeratal.tilelocalization.R;
import self.skzeratal.tilelocalization.SocketClient;
import self.skzeratal.tilelocalization.camera.CameraActivity;

public class FloorPlanActivity extends Activity implements OnPostExecuted {
    public class ImageViewGestureDetector implements android.view.GestureDetector.OnGestureListener {
        @Override
        public boolean onDown(MotionEvent motionEvent) {
            x = motionEvent.getX();
            y = motionEvent.getY();
            return true;
        }

        @Override
        public void onShowPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {
            Intent intent = new Intent(FloorPlanActivity.this, CameraActivity.class);
            intent.putExtra("Type", getIntent().getStringExtra("Type"));
            intent.putExtra("LastNameX", lastNameX);
            intent.putExtra("LastNameY", lastNameY);
            intent.putExtra("x", x);
            intent.putExtra("y", y);
            startActivityForResult(intent, 0);
        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            return false;
        }
    }

    private RelativeLayout relativeLayout;
    private ImageView floorPlanImageView;
    private GestureDetector gestureDetector;
    private View.OnTouchListener listener;
    private float x;
    private float y;
    private int xDiff;
    private int yDiff;
    private boolean isDragging;
    private ArrayList<Pin> PinList;
    private String lastName;
    private String lastNameX;
    private String lastNameY;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floorplan); 

        gestureDetector = new GestureDetector(this, new ImageViewGestureDetector());
        listener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) floorPlanImageView.getLayoutParams();
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        x = event.getRawX();
                        y = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (!isDragging)
                        {
                            xDiff = (int)event.getRawX() - ((RelativeLayout.LayoutParams) floorPlanImageView.getLayoutParams()).leftMargin;
                            yDiff = (int)event.getRawY() - ((RelativeLayout.LayoutParams) floorPlanImageView.getLayoutParams()).topMargin;

                            if (xDiff > 100 || yDiff > 100) {
                                isDragging = true;
                                Log.d("DRAGGING", "TRUE");
                            }
                        }
                        else {
                            int xRaw = (int)event.getRawX();
                            int yRaw = (int)event.getRawY();

                            layoutParams.leftMargin = xRaw - xDiff;
                            layoutParams.topMargin = yRaw - yDiff;

                            Log.d("DRAGGING X", String.valueOf(xRaw));
                            Log.d("DRAGGING Y", String.valueOf(yRaw));
                            floorPlanImageView.setLayoutParams(layoutParams);

                            for (Pin pin:PinList) {
                                RelativeLayout.LayoutParams pinlayoutParams = (RelativeLayout.LayoutParams) pin.imageView.getLayoutParams();
                                pinlayoutParams.leftMargin = xRaw - xDiff + pin.x;
                                pinlayoutParams.topMargin = yRaw - yDiff + pin.y;
                                pin.imageView.setLayoutParams(pinlayoutParams);
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        isDragging = false;
                        Log.d("DRAGGING", "FALSE");
                }
                return gestureDetector.onTouchEvent(event);
            }
        };

        floorPlanImageView = findViewById(R.id.floorPlanImageView);
        floorPlanImageView.setOnTouchListener(listener);

        relativeLayout = findViewById(R.id.relativeLayout);

        PinList = new ArrayList<>();
        getAllPinFromDatabase();

        if (getIntent().getStringExtra("Type") .equals("Localization")) {
            showMatchResult(getIntent().getIntExtra("X", -1), getIntent().getIntExtra("Y", -1));
        }

        lastNameX = "0";
        lastNameY = "0";
    }

    private void getAllPinFromDatabase() {
        SocketClient socketClient = new SocketClient(this);
        socketClient.execute("RESTORE");
        socketClient.Stop();
    }

    public void onPostExecuted(String restoreInfo) {  //把剛剛所要到的字串一樣放在restoreInfo裡
        while (restoreInfo.length() > 0) {
            String singleInfo = restoreInfo.substring(0, restoreInfo.indexOf(";") + 1);//然後去解析它，是用分號去切
            restoreInfo = restoreInfo.substring(restoreInfo.indexOf(";") + 1);

            //每一個字串裡面都會包含某一塊磁磚特徵的位置，我就知道等一下要畫在平面圖上的哪裡
            String name = singleInfo.substring(0, singleInfo.indexOf(","));
            singleInfo = singleInfo.substring(singleInfo.indexOf(",") + 1);
            int x = Math.round(Float.parseFloat(singleInfo.substring(0, singleInfo.indexOf(",")))); //每一個資料裡面都會包含某一塊磁磚的位置，這樣就知道等下這張平面圖會畫在哪裡
            int y = Math.round(Float.parseFloat(singleInfo.substring(singleInfo.indexOf(",") + 1, singleInfo.indexOf(";"))));
            Pin pin = new Pin(new ImageView(FloorPlanActivity.this), name, x, y); //畫完之後把它用成一個pin
            PinList.add(pin);
            //就用成一個pin，pin是自己寫的

            //ImageView pin = new ImageView(FloorPlanActivity.this);
            //pin.setBackgroundColor(Color.GREEN);

            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) floorPlanImageView.getLayoutParams();
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            params.width = 20;
            params.height = 20;
            params.leftMargin = layoutParams.leftMargin + pin.x;
            params.topMargin = layoutParams.topMargin + pin.y;
            relativeLayout.addView(pin.imageView, params);
        }

        if (getIntent().getStringExtra("Type") .equals("Localization")) {
            showMatchResult(getIntent().getIntExtra("X", -1), getIntent().getIntExtra("Y", -1));

        }
    }

    private void showMatchResult(int x, int y) {
        if (x == -1 || y == -1) {
            return;

        }

        for (Pin pin : PinList) {
            if (pin.x == x && pin.y == y) {

                Log.d("TAG","pin x="+x);
                Log.d("TAG","pin y="+y);
                pin.show();
                return;




            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 100) {
            lastNameX = data.getStringExtra("NameX");
            lastNameY = data.getStringExtra("NameY");
            lastName = "X" + lastNameX + "Y" + lastNameY;
            Pin pin = new Pin(new ImageView(FloorPlanActivity.this), lastName, Math.round(x), Math.round(y));
            PinList.add(pin);

            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) floorPlanImageView.getLayoutParams();
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            params.width = 20;
            params.height = 20;
            params.leftMargin = layoutParams.leftMargin + pin.x;
            params.topMargin = layoutParams.topMargin + pin.y;
            relativeLayout.addView(pin.imageView, params);
        }
    }
}