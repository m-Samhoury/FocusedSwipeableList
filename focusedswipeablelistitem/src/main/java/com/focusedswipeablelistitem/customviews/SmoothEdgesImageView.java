package com.focusedswipeablelistitem.customviews;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Custom {ImageView} that hides jagged edges when the view is tilted
 * Source:
 * https://medium.com/@elye.project/smoothen-jagged-edges-of-rotated-image-view-1e56f6d8b5e9
 */
public class SmoothEdgesImageView extends ImageView {
    Paint paint = new Paint();
    BitmapShader shader;
    private Matrix matrix = new Matrix();

    public SmoothEdgesImageView(Context context) {
        super(context);
    }

    public SmoothEdgesImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SmoothEdgesImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SmoothEdgesImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (getDrawable() instanceof BitmapDrawable) {

            paint.setAntiAlias(true);
            Bitmap bitmap = ((BitmapDrawable) getDrawable()).getBitmap();
            shader =
                    new BitmapShader(bitmap,
                            Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);


            float scale;
            /* Note: this piece of code handling like Centre-Crop scaling */
            if (bitmap.getWidth() > bitmap.getHeight()) {
                scale = (float) canvas.getHeight()
                        / (float) bitmap.getHeight();
                matrix.setScale(scale, scale);
                matrix.postTranslate(
                        (canvas.getWidth() - bitmap.getWidth() * scale) * 0.5f, 0);

            } else {
                scale = (float) canvas.getWidth()
                        / (float) bitmap.getWidth();
                matrix.setScale(scale, scale);
                matrix.postTranslate(0,
                        (canvas.getHeight() - bitmap.getHeight() * scale) * 0.5f);
            }

            shader.setLocalMatrix(matrix);
            paint.setShader(shader);

            // this is where I shrink the image by 1px each side,
            // move it to the center
            canvas.translate(1, 1);
            canvas.drawRect(
                    0.0f, 0.0f, canvas.getWidth() - 2, canvas.getHeight() - 2, paint);
        }
    }
}
