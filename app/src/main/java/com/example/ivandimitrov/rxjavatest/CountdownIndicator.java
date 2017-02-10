package com.example.ivandimitrov.rxjavatest;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Ivan Dimitrov on 2/8/2017.
 */

public class CountdownIndicator extends View {

    public static final int DRAW_CLOCKWISE         = 101;
    public static final int DRAW_COUNTER_CLOCKWISE = 102;

    private final Paint mRemainingSectorPaint;
    private final Paint mBorderPaint;
    private static final int DEFAULT_COLOR = 0xff3060c0;
    private float mStartDegree;
    private int   mFrom_color;
    private int   mTo_color;
    private int mDrawDirection = DRAW_CLOCKWISE;


    private double mPhase;

    public CountdownIndicator(Context context) {
        this(context, null);
    }

    public CountdownIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources.Theme theme = context.getTheme();
        TypedArray appearance = theme.obtainStyledAttributes(
                attrs, R.styleable.CountdownIndicator, 0, 0);

        if (appearance != null) {
            int n = appearance.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = appearance.getIndex(i);
                switch (attr) {
                    case R.styleable.CountdownIndicator_from_color:
                        mFrom_color = appearance.getColor(attr, DEFAULT_COLOR);
                        break;
                    case R.styleable.CountdownIndicator_to_color:
                        mTo_color = appearance.getColor(attr, -1);
                        break;
                    case R.styleable.CountdownIndicator_start_degree:
                        mStartDegree = appearance.getInteger(attr, 0);
                        //CHECK FOR INCORRECT INPUT
                        if (mStartDegree > 360) {
                            mStartDegree = 360;
                        } else if (mStartDegree < 0) {
                            mStartDegree = 0;
                        }
                        break;
                    case R.styleable.CountdownIndicator_draw_direction:
                        mDrawDirection = appearance.getInteger(attr, 0);
                        break;
                }
            }
        }

        mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setStrokeWidth(0); // hairline
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setColor(mFrom_color);
        mRemainingSectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRemainingSectorPaint.setColor(mBorderPaint.getColor());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float remainingSectorSweepAngle = (float) (mPhase * 360);
        float remainingSectorStartAngle;

        if (mDrawDirection == DRAW_CLOCKWISE) {
            remainingSectorStartAngle = mStartDegree - remainingSectorSweepAngle;
        } else {
            remainingSectorStartAngle = mStartDegree;
        }

        RectF drawingRect = new RectF(1, 1, getWidth() - 1, getHeight() - 1);

        if (!(mTo_color == -1)) {
            colorTransition();
        }

        if (remainingSectorStartAngle < 360) {
            canvas.drawArc(
                    drawingRect,
                    remainingSectorStartAngle,
                    remainingSectorSweepAngle,
                    true,
                    mRemainingSectorPaint);
        } else {
            canvas.drawOval(drawingRect, mRemainingSectorPaint);
        }
    }

    public void setDirection(int direction) {
        if (direction == DRAW_CLOCKWISE) {
            mDrawDirection = DRAW_CLOCKWISE;
        } else {
            mDrawDirection = DRAW_COUNTER_CLOCKWISE;
        }
    }

    public void setStartDegree(int degree) {
        if (degree > 360 || degree < 0) {
            degree %= 360;
        }
        mStartDegree = degree;
    }

    public void setColor(int from_color, int to_color, boolean isSingle) {
        if (isSingle) {
            mBorderPaint.setColor(from_color);
            mRemainingSectorPaint.setColor(mBorderPaint.getColor());
        } else {
            mFrom_color = from_color;
            mTo_color = to_color;
        }
    }

    public void setPhase(double phase) {
        if ((phase < 0) || (phase > 1)) {
            throw new IllegalArgumentException("phase: " + phase);
        }
        mPhase = phase;
        invalidate();
        getPercentage();
    }

    public double getPercentage() {
        double percentage = mPhase * 100;
        if (percentage > 100) {
            percentage = 100;
        }
        return percentage;
    }

    private void colorTransition() {
        int startColor = mFrom_color;
        int endColor = mTo_color;
        double newColorRed;
        double newColorBlue;
        double newColorGreen;
        //RED
        newColorRed = Color.red(endColor) + mPhase * (Color.red(startColor) - (Color.red(endColor)));

        //BLUE
        newColorBlue = Color.blue(endColor) + mPhase * (Color.blue(startColor) - (Color.blue(endColor)));

        //GREEN
        newColorGreen = Color.green(endColor) + mPhase * (Color.green(startColor) - (Color.green(endColor)));

        mBorderPaint.setColor(Color.rgb((int) newColorRed, (int) newColorGreen, (int) newColorBlue));
        mRemainingSectorPaint.setColor(mBorderPaint.getColor());
    }
}
