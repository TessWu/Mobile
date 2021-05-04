package self.skzeratal.tilelocalization.buildingmap;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import java.util.ArrayList;

import self.skzeratal.tilelocalization.R;

public class BuildingMap extends View {
    private Paint paint;
    private final int defaultWidth = getResources().getDimensionPixelSize(R.dimen.defaultWidth);
    private final int defaultHeight = getResources().getDimensionPixelSize(R.dimen.defaultHeight);
    private final int defaultSegmentButtonWidth = getResources().getDimensionPixelSize(R.dimen.defaultSegmentButtonWidth);
    private final int defaultSegmentButtonHeight = getResources().getDimensionPixelSize(R.dimen.defaultSegmentButtonHeight);

    public ArrayList<Room> rooms;

    public BuildingMap(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint();
        paint.setStrokeWidth(5);

        rooms = new ArrayList<>();
        rooms.add(new Room("A"));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Room room : rooms) {
            room.addSegment(new Segment(50, 100, 100, 50, false));
            room.addSegment(new Segment(50, 100, 50, 900, false));
            room.addSegment(new Segment(100, 50, 250, 50, false));
            room.addSegment(new Segment(250, 50, 350, 50, true));
            room.addSegment(new Segment(350, 50, 500, 50, false));
            room.addSegment(new Segment(50, 900, 500, 900, false));
            room.addSegment(new Segment(500, 900, 500, 50, false));

            for (Segment segment : room.segments) {
                if (segment.isDoor) {
                    paint.setColor(Color.GRAY);
                }
                else {
                    paint.setColor(Color.WHITE);
                }
                canvas.drawLine(segment.sourceX, segment.sourceY, segment.destinationX, segment.destinationY, paint);
            }
            paint.setColor(Color.GREEN);
            canvas.drawLines(room.minimalRectangle(), paint);

            int[] centralPoint = room.centralPoint();
            BuildingMapActivity activity = null;
            Context context = getContext();
            while (context instanceof ContextWrapper) {
                if (context instanceof Activity) {
                    activity = (BuildingMapActivity) context;
                }
                context = ((ContextWrapper)context).getBaseContext();
            }
            Button roomButton = new Button(getContext());
            roomButton.setText(room.name);
            roomButton.setTextColor(Color.WHITE);
            roomButton.setBackgroundColor(Color.TRANSPARENT);
            RelativeLayout.LayoutParams buttonLayoutParam = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            buttonLayoutParam.width = defaultSegmentButtonWidth;
            buttonLayoutParam.height = defaultSegmentButtonHeight;
            buttonLayoutParam.leftMargin = getLeft() + centralPoint[0] - defaultSegmentButtonWidth / 2;
            buttonLayoutParam.topMargin = getTop() + centralPoint[1] - defaultSegmentButtonHeight / 2;
            roomButton.setLayoutParams(buttonLayoutParam);
            activity.addButton(roomButton);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width, height;

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        }
        else {
            width = defaultWidth;
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        }
        else {
            height = defaultHeight;
        }

        setMeasuredDimension(width, height);
    }
}