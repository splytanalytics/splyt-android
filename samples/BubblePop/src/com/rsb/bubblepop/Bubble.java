package com.rsb.bubblepop;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.ImageView;

@SuppressLint("ViewConstructor")
public class Bubble extends ImageView
{
    private boolean mHidesStar;

    public Bubble(Context context, OnClickListener listener)
    {
        super(context);

        setImageResource(R.drawable.bubble);
        setOnClickListener(listener);
    }

    public void setHidesStar(boolean value)
    {
        mHidesStar = value;
    }

    public boolean getHidesStar()
    {
        return mHidesStar;
    }
}
