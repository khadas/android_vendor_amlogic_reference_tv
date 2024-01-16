/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description: JAVA file
 */

package com.droidlogic.app.tv;

import android.content.ActivityNotFoundException;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.hdmi.HdmiControlManager;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.HdmiTvClient;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.util.Log;
import android.os.Handler;
import android.os.SystemProperties;
import android.text.TextUtils;

import java.util.List;

/**
 * Hdmi adapter for InputChangeListener
 */

public class InputChangeAdapter {
    private static final String TAG = "InputChangeAdapter";

    private static final String ACTION_OTP_INPUT_SOURCE_CHANGE = "droidlogic.tv.action.OTP_INPUT_SOURCE_CHANGED";
    // For test use. Please reboot if you want to change this property.
    private static final String PROP_OTP_INPUT_CHANGE = "persist.vendor.tv.otp.inputchange";
    private static final String PACKAGE_TV_INPUT = "com.droidlogic.tvinput";
    private static final String PACKAGE_LIVETV = "com.droidlogic.android.tv";

    private volatile static InputChangeAdapter sInstance;

    private Runnable mBootOtp;
    private Context mContext;
    private boolean mBootComplete = false;
    private Handler mHandler = new Handler();

    private InputChangeAdapter() {}

    private InputChangeAdapter(Context context) {
        mContext = context;
        TvInputManager manager = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
        if (null == manager) {
            Log.e(TAG, "InputChangeAdapter but TvInputManager is null!");
            return;
        }

        if (SystemProperties.getBoolean(PROP_OTP_INPUT_CHANGE, true)) {
            registerInputChangeListener(context);
        }
    }

    public void sendBootOtpIntent() {
        mBootComplete = true;
        if (mBootOtp != null && mContext != null) {
            Log.d(TAG, "send boot otp message");
            mHandler.post(mBootOtp);
        }
    }

    private void registerInputChangeListener(Context context) {
        HdmiControlManager hdmiControlManager = (HdmiControlManager) context.getSystemService(Context.HDMI_CONTROL_SERVICE);
        if (null == hdmiControlManager) {
            Log.e(TAG, "failed to get HdmiControlManager");
            return;
        }

        HdmiTvClient tvClient = hdmiControlManager.getTvClient();
        if (null == tvClient) {
            Log.e(TAG, "failed to get HdmiTvClient");
            return;
        }

        tvClient.setInputChangeListener(new HdmiTvClient.InputChangeListener() {
            @Override
            public void onChanged(HdmiDeviceInfo info) {
                Log.d(TAG, "onChanged: " + info);

                TvInputManager manager = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
                if (null == manager) {
                    Log.e(TAG, "TvInputManager null!");
                    return;
                }

                List<TvInputInfo> tvInputList = manager.getTvInputList();
                String inputId = "";
                 String parentInputId = "";
                for (TvInputInfo tvInputInfo : tvInputList) {
                    HdmiDeviceInfo hdmiInfo = tvInputInfo.getHdmiDeviceInfo();
                    if (hdmiInfo != null && hdmiInfo.getLogicalAddress() == info.getLogicalAddress()) {
                        inputId = tvInputInfo.getId();
                        parentInputId = tvInputInfo.getParentId();
                        break;
                    }
                }

                if (TextUtils.isEmpty(inputId)) {
                    Log.d(TAG, "no input id found for " + info);
                    return;
                }

                String currentSelectInput = DroidLogicHdmiCecManager.getInstance(context).getCurrentInput();

                Log.d(TAG, "input id:" + inputId + " parent:" + parentInputId + " current:" + currentSelectInput);
                if (currentSelectInput.equals(inputId)
                    || currentSelectInput.equals(parentInputId)) {
                    Log.d(TAG, "same input id no need to broadcast");
                    return;
                }

                if (isAppForeground(PACKAGE_TV_INPUT) || isAppForeground(PACKAGE_LIVETV)) {
                    final Intent intent = new Intent(ACTION_OTP_INPUT_SOURCE_CHANGE);
                    intent.putExtra(TvInputInfo.EXTRA_INPUT_ID, inputId);

                    Log.d(TAG, "One Touch Play event, send broadcast intent " + intent);
                    context.sendBroadcast(intent);
                } else {
                    final String finalInput = inputId;
                    if (!mBootComplete) {
                        Log.d(TAG, "One Touch Play event, but not boot finished yet");
                        //1. first notify mbox launcher if possible directly.
                        final Intent intent = new Intent(ACTION_OTP_INPUT_SOURCE_CHANGE);
                        intent.putExtra(TvInputInfo.EXTRA_INPUT_ID, inputId);
                        mContext.sendBroadcast(intent);

                        //2. notify tv app if possible.
                        mBootOtp = () -> {
                            switchToTvInput(finalInput);
                        };
                    } else {
                        switchToTvInput(finalInput);
                    }

                }
            }
        });
    }

    private void switchToTvInput(String inputId) {
        Log.d(TAG, "switchToTvInput " + inputId);
        try {
            mContext.startActivity(new Intent(Intent.ACTION_VIEW,
                    TvContract.buildChannelUriForPassthroughInput(inputId))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Can't find activity to switch to " + inputId, e);
        }
    }

    public static InputChangeAdapter getInstance(Context context) {
        if (null == sInstance) {
            synchronized(InputChangeAdapter.class) {
                if (null == sInstance) {
                    sInstance = new InputChangeAdapter(context);
                }
            }
        }
        return sInstance;
    }

    private boolean isAppForeground(String packageName) {
        //com.droidlogic.tvinput/.settings.ChannelSearchActivity
        //com.droidlogic.android.tv/com.android.tv.MainActivity
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processInfoList = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : processInfoList) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && (processInfo.pkgList[0].equals(packageName) || processInfo.pkgList[0].equals(packageName))) {
                return true;
            }
        }
        return false;
    }
}
