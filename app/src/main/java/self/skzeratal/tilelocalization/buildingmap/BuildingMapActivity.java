package self.skzeratal.tilelocalization.buildingmap;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import self.skzeratal.tilelocalization.R;
import self.skzeratal.tilelocalization.roommap.RoomMapActivity;

public class BuildingMapActivity extends Activity {
    private BuildingMap buildingMap;
    private RelativeLayout relativeLayout;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buildingmap);

        buildingMap = findViewById(R.id.buildingMap);
        relativeLayout = findViewById(R.id.relativeLayout);
    }

    public void addButton(Button button) {
        button.setOnClickListener(roomButtonOnClickListener);
        relativeLayout.addView(button);
    }

    Button.OnClickListener roomButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(BuildingMapActivity.this, RoomMapActivity.class);
            intent.putExtra(buildingMap.rooms.get(0).name, buildingMap.rooms.get(0));
            intent.putExtra("RoomName", buildingMap.rooms.get(0).name);
            startActivity(intent);
        }
    };
}