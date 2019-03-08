/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description: JAVA file
 */

package com.droidlogic.app.tv;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public abstract class DroidLogicOverlayView extends FrameLayout {
    private static final String TAG = "DroidLogicOverlayView";

    protected ImageView mImageView;
    protected ImageView mTuningImageView;
    protected TextView mTextView;
    protected View mSubtitleView;
    protected TextView mEasTextView;

    public DroidLogicOverlayView(Context context) {
        this(context, null);
    }

    public DroidLogicOverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DroidLogicOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected abstract void initSubView();

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initSubView();
    }

    public void setImage(int resId) {
        mImageView.setImageResource(resId);
    }

    public void setImageVisibility(boolean visible) {
        mImageView.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setTuningImageVisibility(boolean visible) {
        mTuningImageView.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setText(int resId) {
        mTextView.setText(resId);
    }

    public void setTextForEas(CharSequence text){
        mEasTextView.setText(text);
    }

    public void setEasTextVisibility(boolean visible) {
        mEasTextView.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setTextVisibility(boolean visible) {
        mTextView.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setSubtitleVisibility(boolean visible) {
        mSubtitleView.setVisibility(visible ? VISIBLE : GONE);
    }

    public View getSubtitleView() {
        return mSubtitleView;
    }

    public void releaseResource() {
        mImageView = null;
        mTextView = null;
        mSubtitleView = null;
    }
}
