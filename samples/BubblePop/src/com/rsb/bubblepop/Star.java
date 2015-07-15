package com.rsb.bubblepop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;

public class Star extends View
{
    private Path mPath;
    private Paint mFillPaint;
    private Paint mOutlinePaint;

    public Star(Context context)
    {
        super(context);
    }

    public void createAndAnimate(int width, int height, Point delta, Animation.AnimationListener listener)
    {
        // Create the path
        mPath = new Path();
        mPath.lineTo(0.375f * width, 0.25f * height);
        mPath.lineTo(0.5f * width, 0.0f);
        mPath.lineTo(0.625f * width, 0.25f * height);
        mPath.lineTo(width, 0.0f);
        mPath.lineTo(0.75f * width, 0.375f * height);
        mPath.lineTo(width, 0.5f * height);
        mPath.lineTo(0.75f * width, 0.625f * height);
        mPath.lineTo(width, height);
        mPath.lineTo(0.625f * width, 0.75f * height);
        mPath.lineTo(0.5f * width, height);
        mPath.lineTo(0.375f * width, 0.75f * height);
        mPath.lineTo(0.0f, height);
        mPath.lineTo(0.25f * width, 0.625f * height);
        mPath.lineTo(0.0f, 0.5f * height);
        mPath.lineTo(0.25f * width, 0.375f * height);
        mPath.close();

        // One paint for a fill and one for an outline
        mFillPaint = new Paint();
        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setColor(0xFF666699);

        mOutlinePaint = new Paint();
        mOutlinePaint.setStyle(Paint.Style.STROKE);
        mOutlinePaint.setStrokeWidth(3);
        mOutlinePaint.setColor(Color.BLACK);

        // Create rotation and translate animation instances for the finishing move
        Animation rotAnim = new RotateAnimation(0.0f, 720.0f, 0.5f * width, 0.5f * height);
        Animation transAnim = new TranslateAnimation(0.0f, delta.x, 0.0f, delta.y);

        // Combine these animations into a set
        AnimationSet animSet = new AnimationSet(true);
        animSet.addAnimation(rotAnim);
        animSet.addAnimation(transAnim);
        animSet.setFillEnabled(true);
        animSet.setFillAfter(true);
        animSet.setDuration(1000);  // duration in ms
        animSet.setAnimationListener(listener);

        // Set the animation
        setAnimation(animSet);
    }

    public void onDraw(Canvas c)
    {
        super.onDraw(c);
        c.drawPath(mPath, mFillPaint);
        c.drawPath(mPath, mOutlinePaint);
    }
}
