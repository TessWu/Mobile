package self.skzeratal.tilelocalization.floorplan;

import android.graphics.Color;
import android.widget.ImageView;

public class Pin {
    public ImageView imageView;
    public String name; //磁磚的名字
    public int x;  //它應該在哪裡
    public int y; //它應該在哪裡

    public Pin(ImageView imageView, String name, int x, int y) {
        this.imageView = imageView;
        this.name = name;
        this.x = x;
        this.y = y;

        imageView.setBackgroundColor(Color.GREEN);
    }

    public void show() {
        imageView.setBackgroundColor(Color.RED);
    }
}