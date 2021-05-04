package self.skzeratal.tilelocalization.roommap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;

public class Tile extends View {

    public interface OnToggledListener {
        void OnToggled(Tile tile, boolean touchOn);
    }

    private OnToggledListener toggledListener;
    private boolean touchOn;
    private boolean downTouch = false;

    public int indexX;
    public int indexY;

    public Tile(Context context, int indexX, int indexY) {
        super(context);
        this.indexX = indexX;
        this.indexY = indexY;
        touchOn = false;

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (touchOn) {
            canvas.drawColor(Color.GREEN);
        } else {
            canvas.drawColor(Color.LTGRAY);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchOn = !touchOn;
                invalidate();

                if(toggledListener != null){
                    toggledListener.OnToggled(this, touchOn);
                }

                downTouch = true;
                return true;

            case MotionEvent.ACTION_UP:
                if (downTouch) {
                    downTouch = false;
                    performClick();
                    return true;
                }
        }
        return false;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    public void setOnToggledListener(OnToggledListener listener){
        toggledListener = listener;
    }
}