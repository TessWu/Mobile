package self.skzeratal.tilelocalization.buildingmap;

import java.io.Serializable;
import java.util.ArrayList;

public class Room implements Serializable {
    public String name;
    public ArrayList<Segment> segments;

    public Room(String name) {
        segments = new ArrayList<>();
        this.name = name;
    }

    public void addSegment(Segment segment) {
        segments.add(segment);
    }

    public float[] minimalRectangle() {
        Segment firstSegment = segments.get(0);

        float left = firstSegment.sourceX, right = firstSegment.sourceX, top = firstSegment.sourceY, bottom = firstSegment.sourceY;

        for (Segment segment : segments) {
            if (segment.sourceX > right) {
                right = segment.sourceX;
            }
            if (segment.sourceX < left) {
                left = segment.sourceX;
            }
            if (segment.sourceY > bottom) {
                bottom = segment.sourceY;
            }
            if (segment.sourceY < top) {
                top = segment.sourceY;
            }
        }

        return new float[] {left - 50, top - 50, right + 50, top - 50, right + 50, top - 50, right + 50, bottom + 50, right + 50, bottom + 50, left - 50, bottom + 50, left - 50, bottom + 50, left - 50, top - 50};
    }

    public int[] centralPoint() {
        Segment firstSegment = segments.get(0);

        float left = firstSegment.sourceX, right = firstSegment.sourceX, top = firstSegment.sourceY, bottom = firstSegment.sourceY;

        for (Segment segment : segments) {
            if (segment.sourceX > right) {
                right = segment.sourceX;
            }
            if (segment.sourceX < left) {
                left = segment.sourceX;
            }
            if (segment.sourceY > bottom) {
                bottom = segment.sourceY;
            }
            if (segment.sourceY < top) {
                top = segment.sourceY;
            }
        }
        return new int[] {Math.round(left + (right - left) / 2), Math.round(top + (bottom - top) / 2)};
    }
}