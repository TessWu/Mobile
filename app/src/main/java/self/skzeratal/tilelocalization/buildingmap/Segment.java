package self.skzeratal.tilelocalization.buildingmap;

import java.io.Serializable;

public class Segment implements Serializable {
    public float sourceX;
    public float sourceY;
    public float destinationX;
    public float destinationY;
    public boolean isDoor;

    public Segment(float sourceX, float sourceY, float destinationX, float destinationY, boolean isDoor) {
        this.sourceX = sourceX;
        this.sourceY = sourceY;
        this.destinationX = destinationX;
        this.destinationY = destinationY;
        this.isDoor = isDoor;
    }
}