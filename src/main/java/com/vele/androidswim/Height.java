package com.vele.androidswim;

public class Height
{
    private OnHeightChangeListener listener;

    private float height;

    public void setOnHeightChangeListener(OnHeightChangeListener listener)
    {
        this.listener = listener;
    }

    public float get()
    {
        return height;
    }

    public void set(float value)
    {
        this.height = value;

        if(listener != null)
        {
            listener.onFloatChanged(value);
        }
    }
}