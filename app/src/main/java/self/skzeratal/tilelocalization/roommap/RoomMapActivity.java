package self.skzeratal.tilelocalization.roommap;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.widget.Toast;
import android.widget.GridLayout;

import self.skzeratal.tilelocalization.R;

public class RoomMapActivity extends Activity implements Tile.OnToggledListener {
    GridLayout gridLayout;
    Tile[] tiles;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roommap);

        Intent intent = getIntent();
        String roomName = intent.getStringExtra("roomName");
        intent.getSerializableExtra(roomName);

        gridLayout = findViewById(R.id.gridLayout);
        gridLayout.setColumnCount(5);
        gridLayout.setRowCount(8);

        tiles = new Tile[40];

        for(int yPos = 0; yPos < 8; yPos++) {
            for(int xPos = 0; xPos < 5; xPos++){
                Tile tile = new Tile(this, xPos, yPos);
                tile.setOnToggledListener(this);
                tile.setBackgroundColor(Color.RED);
                tiles[yPos * 5 + xPos] = tile;
                gridLayout.addView(tile);
            }
        }

        gridLayout.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
    }

    ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            final int MARGIN = 10;

            int columnCount = gridLayout.getColumnCount();
            int rowCount = gridLayout.getRowCount();
            int width = 150;//gridLayout.getWidth() / columnCount;
            int height = 150;//gridLayout.getHeight() / rowCount;

            for(int yPos = 0; yPos < rowCount; yPos++){
                for(int xPos = 0; xPos < columnCount; xPos++){
                    GridLayout.LayoutParams params = (GridLayout.LayoutParams) tiles[yPos * columnCount + xPos].getLayoutParams();
                    params.width = width - 2 * MARGIN;
                    params.height = height - 2 * MARGIN;
                    params.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);
                    tiles[yPos * columnCount + xPos].setLayoutParams(params);
                }
            }
        }
    };

    @Override
    public void OnToggled(Tile tile, boolean touchOn) {
        String idString = tile.indexX + ":" + tile.indexY;
        Toast.makeText(RoomMapActivity.this,"Toogled:\n" + idString + "\n" + touchOn, Toast.LENGTH_SHORT).show();
    }
}