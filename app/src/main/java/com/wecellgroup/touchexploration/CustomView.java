package com.wecellgroup.touchexploration;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CustomView extends View {
    private RectF topRect;
    private RectF bottomRect;
    private RectF leftRect;
    private RectF rightRect;
    private Paint paintTop;
    private Paint paintBottom;
    private Paint paintRight;
    private Paint paintLeft;

    public CustomView(Context context) {
        super(context);
    }

    public CustomView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        init();
    }

    private void init() {

        float x = getX();
        float y = getY();
        float height = getMeasuredHeight();
        float width = getMeasuredWidth();

        topRect = new RectF(x,
                y,
                x + width, (int) (y + height * 0.25));

        bottomRect = new RectF(x,
                (int) (y + height * 0.25),
                x + width, (int) (y + height));

        leftRect = new RectF(x,
                (int) (y + height * 0.25),
                (int) (x + width * 0.25), (int) (y + height));

        rightRect = new RectF((int) (x + width * 0.75),
                y,
                x + width, y + height);


        paintTop = new Paint();
        paintTop.setColor(Color.RED);

        paintBottom = new Paint();
        paintBottom.setColor(Color.YELLOW);

        paintRight = new Paint();
        paintRight.setColor(Color.GREEN);

        paintLeft = new Paint();
        paintLeft.setColor(Color.BLUE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRect(topRect, paintTop);
        canvas.drawRect(leftRect, paintLeft);

        canvas.drawRect(rightRect, paintRight);
        canvas.drawRect(bottomRect, paintBottom);
    }
}
