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
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Intent;
import android.hardware.hdmi.HdmiControlManager;
import android.hardware.hdmi.HdmiTvClient;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.HdmiTvClient.SelectCallback;
import android.media.AudioManager;
import android.media.tv.TvContentRating;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.media.tv.TvInputInfo;
import android.media.tv.TvStreamConfig;
import android.media.tv.TvInputManager.Hardware;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.LayoutInflater;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.droidlogic.app.SystemControlManager;
import com.droidlogic.app.tv.DroidLogicHdmiCecManager;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.TvControlManager;
import com.droidlogic.app.tv.TvControlDataManager;

public abstract class TvInputBaseSession extends TvInputService.Session implements Handler.Callback {
    private static final boolean DEBUG = true;
    private static final String TAG = "TvInputBaseSession";

    private static final int    MSG_REGISTER_BROADCAST = 8;
    private static final int    MSG_DO_PRI_CMD              = 9;
    protected static final int  MSG_SUBTITLE_SHOW           = 10;
    protected static final int  MSG_SUBTITLE_HIDE           = 11;
    protected static final int  MSG_DO_RELEASE              = 12;
    protected static final int  MSG_AUDIO_MUTE              = 13;
    protected static final int  MSG_IMAGETEXT_SET           = 14;

    ////add for get hdmi info
    protected static final int MSG_UPDATE_HDMI_HDR = 15;
    protected static final int MSG_UPDATE_HDMI_AUDIO_FORMAT = 16;
    protected static final int MSG_CLEAR_INFO = 17;
    protected static final int MSG_DELAY_PERIOD = 2000;//2s

    protected static final int TVINPUT_BASE_DELAY_SEND_MSG  = 10; // Filter message within 10ms, only the last message is processed
    private Context mContext;
    public int mId;
    private String mInputId;
    private int mDeviceId;
    private AudioManager mAudioManager;
    private TvInputManager mTvInputManager;
    private boolean mHasRetuned = false;
    protected Handler mSessionHandler;
    private TvControlDataManager mTvControlDataManager = null;
    private SystemControlManager mSystemControlManager;
    private TvControlManager mTvControlManager;
    protected DroidLogicOverlayView mOverlayView = null;

    protected boolean isBlockNoRatingEnable = false;
    protected boolean isUnlockCurrent_NR = false;
    DroidLogicHdmiCecManager mDroidLogicHdmiCecManager = null;
    private int mKeyCodeMediaPlayPauseCount = 0;
    private boolean isSurfaceAlive = true;

    //add for get hdmi info
    private boolean isHdmiDevice = false;
    private String mHdmiHdrInfo = null;
    private String mHdmiAudioFormatInfo = null;

    public TvInputBaseSession(Context context, String inputId, int deviceId) {
        super(context);
        mContext = context;
        mInputId = inputId;
        mDeviceId = deviceId;

        Log.d(TAG, "TvInputBaseSession, inputId " + inputId + " deviceId " + deviceId);
        mAudioManager = (AudioManager)context.getSystemService (Context.AUDIO_SERVICE);
        mTvControlDataManager = TvControlDataManager.getInstance(mContext);
        mSystemControlManager = SystemControlManager.getInstance();
        mTvControlManager = TvControlManager.getInstance();
        mSessionHandler = new Handler(context.getMainLooper(), this);
        mTvInputManager = (TvInputManager)mContext.getSystemService(Context.TV_INPUT_SERVICE);
        mDroidLogicHdmiCecManager = DroidLogicHdmiCecManager.getInstance(mContext);
        isHdmiDevice = (DroidLogicTvUtils.parseTvSourceTypeFromDeviceId(deviceId) == TvControlManager.SourceInput_Type.SOURCE_TYPE_HDMI);
        sendSessionMessage(MSG_REGISTER_BROADCAST);
    }

    public void setSessionId(int id) {
        mId = id;
    }

    public int getSessionId() {
        return mId;
    }

    public String getInputId() {
        return mInputId;
    }

    public int getDeviceId() {
        return mDeviceId;
    }

    public void sendSessionMessage(int cmd) {
        Message msg = mSessionHandler.obtainMessage(cmd);
        mSessionHandler.removeMessages(msg.what);
        msg.sendToTarget();
    }
    public void doRelease() {
        Log.d(TAG, "doRelease,session: " + this);
        // When tv not show hdmi cec source, try to take the active source to INACTIVE_DEVICE.
        // It can be removed when the "Active Source" filter could be removed in hal. Related
        // with setDeviceIdForCec which finally call HdmiCecControl::TvEventListner::notify
        // HdmiCecControl::TvEventListner::TV_EVENT_SOURCE_SWITCH.
        mDroidLogicHdmiCecManager.selectHdmiDevice(HdmiDeviceInfo.ADDR_INTERNAL, HdmiDeviceInfo.DEVICE_INACTIVE);
        mContext.unregisterReceiver(mBroadcastReceiver);
    }

    public void doAppPrivateCmd(String action, Bundle bundle) {}
    public void doUnblockContent(TvContentRating rating) {}

    @Override
    public void onSurfaceChanged(int format, int width, int height) {
        Log.d(TAG, "onSurfaceChanged: format="+format + " width=" + width + " height=" + height);
        if (width < 720 || height < 480) {
            mTvControlManager.SetPreviewWindowMode(true);
        } else {
            mTvControlManager.SetPreviewWindowMode(false);
        }
    }

    @Override
    public void onSetStreamVolume(float volume) {
        //this function used for parental control, so HDMI source don't need it.
        if ((mDeviceId >= DroidLogicTvUtils.DEVICE_ID_HDMI1 && mDeviceId <= DroidLogicTvUtils.DEVICE_ID_HDMI4)) {
            return;
        }
        if (DEBUG)
            Log.d(TAG, "onSetStreamVolume volume = " + volume);
        Message msg = mSessionHandler.obtainMessage(MSG_AUDIO_MUTE);
        if (0.0 == volume) {
            msg.arg1 = 0;
        } else {
            msg.arg1 = 1;
        }
        mSessionHandler.removeMessages(msg.what);
        mSessionHandler.sendMessageDelayed(msg, TVINPUT_BASE_DELAY_SEND_MSG);
    }

    @Override
    public void onAppPrivateCommand(String action, Bundle data) {
        if (DEBUG)
            Log.d(TAG, "onAppPrivateCommand, action = " + action);

        if (mSessionHandler == null)
            return;
        Message msg = mSessionHandler.obtainMessage(MSG_DO_PRI_CMD);
        mSessionHandler.removeMessages(msg.what);
        msg.setData(data);
        msg.obj = action;
        msg.sendToTarget();
    }

    @Override
    public void onSetCaptionEnabled(boolean enabled) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onUnblockContent(TvContentRating unblockedRating) {
        if (DEBUG)
            Log.d(TAG, "onUnblockContent");

        doUnblockContent(unblockedRating);
    }

    public void initOverlayView(int resId) {
        LayoutInflater inflater = (LayoutInflater)
                mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mOverlayView = (DroidLogicOverlayView)inflater.inflate(resId, null);
        setOverlayViewEnabled(true);
    }

    private  BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                if (DEBUG) Log.d(TAG, "Received ACTION_SCREEN_OFF");
                setOverlayViewEnabled(false);
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                if (DEBUG) Log.d(TAG, "Received ACTION_SCREEN_ON");
                setOverlayViewEnabled(true);
            }
        }
    };

    @Override
    public View onCreateOverlayView() {
        return mOverlayView;
    }

    @Override
    public void onOverlayViewSizeChanged(int width, int height) {
        Log.d(TAG, "onOverlayViewSizeChanged: "+width+","+height);
    }

    @Override
    public void notifyVideoAvailable() {
        Log.d(TAG, "notifyVideoAvailable ");
        super.notifyVideoAvailable();
        if (mOverlayView != null) {
            mOverlayView.setImageVisibility(false);
            mOverlayView.setTextVisibility(false);
        }
        if (isHdmiDevice) {
            checkHdmiInfoOnVideoAvailable();
        }
    }

    @Override
    public void notifyVideoUnavailable(int reason) {
        Log.d(TAG, "notifyVideoUnavailable: "+reason);
        super.notifyVideoUnavailable(reason);
        Message msg = mSessionHandler.obtainMessage(MSG_IMAGETEXT_SET);
        mSessionHandler.removeMessages(msg.what);
        msg.sendToTarget();
        if (isHdmiDevice) {
            checkHdmiInfoOnVideoUnavailable();
        }
    }

    @Override
     public boolean onSetSurface(Surface surface) {
        Log.d(TAG, "onSetSurface " + surface + this);
        Exception e = new Exception();
        Log.e(TAG, "setSurfaceInService " + e);

        if (surface == null) {
            mSessionHandler.removeCallbacksAndMessages(null);
            isSurfaceAlive = false;

            setOverlayViewEnabled(false);
            if (mOverlayView != null) {
                mOverlayView.releaseResource();
                mOverlayView = null;
            }
        } else {
            isSurfaceAlive = true;
        }

        return false;
     }

    @Override
    public void onRelease() {
        mSessionHandler.removeCallbacksAndMessages(null);
        if (mSessionHandler == null)
            return;
        Message msg = mSessionHandler.obtainMessage(MSG_DO_RELEASE);
        mSessionHandler.removeMessages(msg.what);
        msg.sendToTarget();
    }

    public void hideUI() {
        if (mOverlayView != null) {
            mOverlayView.setImageVisibility(false);
            mOverlayView.setTextVisibility(false);
            mOverlayView.setSubtitleVisibility(false);
        }
    }

    private void setAudiodMute(boolean mute) {
        Log.d(TAG, "setAudiodMute="+mute);
        if (mute) {
            mAudioManager.setParameters("parental_control_av_mute=true");
        } else {
            mAudioManager.setParameters("parental_control_av_mute=false");
        }
    }

    public void openTvAudio (int type){
        switch (type) {
            case DroidLogicTvUtils.SOURCE_TYPE_ATV:
                mAudioManager.setParameters("tuner_in=atv");
                break;
            case DroidLogicTvUtils.SOURCE_TYPE_DTV:
                mAudioManager.setParameters("tuner_in=dtv");
                break;
        }
    }


    @Override
    public boolean handleMessage(Message msg) {
        if (!isSurfaceAlive) {
            if (msg.what != MSG_DO_RELEASE && msg.what != MSG_AUDIO_MUTE) {
                return false;
            }
        }

        switch (msg.what) {
            case MSG_REGISTER_BROADCAST:
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
                    intentFilter.addAction(Intent.ACTION_SCREEN_ON);
                    mContext.registerReceiver(mBroadcastReceiver, intentFilter);
                break;
            case MSG_DO_PRI_CMD:
                doAppPrivateCmd((String)msg.obj, msg.getData());
                break;
            case MSG_SUBTITLE_SHOW:
                if (mOverlayView != null) {
                    mOverlayView.setSubtitleVisibility(true);
                }
                break;
            case MSG_SUBTITLE_HIDE:
                if (mOverlayView != null) {
                    mOverlayView.setSubtitleVisibility(false);
                }
                break;
            case MSG_DO_RELEASE:
                doRelease();
                break;
            case MSG_AUDIO_MUTE:
                long startTime = SystemClock.uptimeMillis();
                setAudiodMute(msg.arg1 == 0);
                if (DEBUG) Log.d(TAG, "setAudiodMute used " + (SystemClock.uptimeMillis() - startTime) + " ms");
                break;
            case MSG_IMAGETEXT_SET:
                if (mOverlayView != null) {
                    mOverlayView.setImageVisibility(true);
                    mOverlayView.setTextVisibility(true);
                }
                break;
            case MSG_UPDATE_HDMI_HDR:
                checkHdmiHdrInfo();
                break;
            case MSG_UPDATE_HDMI_AUDIO_FORMAT:
                checkHdmiAudioFormat();
                break;
            case MSG_CLEAR_INFO:
                clearInfo();
                break;
        }
        return false;
    }

    /**
     * Only in here should the TvInput switch to the active source generally.
     * In the previous design way, lots of places including LiveTv,DroidTvSettings,TvInput etc.
     * does portSelect or deviceSelect, and in the selectHdmiDevice method of DroidHdmiCecManager,
     * lots of if-else is used. And there are still issues come out today or tomorrow, especailly
     * for projects like amazon which does not use aml LiveTv apps.
     * The most important idea of refactoring the code is that the control logic should be high
     * aggregated and much simple, and only in this way it could be easy to maintain.
     */
    @Override
    public void onSetMain(boolean isMain) {
        TvInputInfo info = mTvInputManager.getTvInputInfo(mInputId);
        Log.d(TAG, "onSetMain, isMain " + isMain + " deviceId " + mDeviceId + " input " + info);
        if (isMain) {
            if (info == null) {
                return;
            }
            // For projects like Amazon Fireos, it should directly only use the HdmiDeviceInfo
            // In the TvInputInfo to do deviceSelect, as to solve the auto jump issue.
            HdmiDeviceInfo hdmiDevice = info.getHdmiDeviceInfo();
            if (hdmiDevice == null) {
                hdmiDevice = mDroidLogicHdmiCecManager.getHdmiDeviceInfo(mInputId);
            }
            if (hdmiDevice != null) {
                Log.d(TAG, "onSetMain hdmi device " + hdmiDevice);
                mDroidLogicHdmiCecManager.selectHdmiDevice(hdmiDevice.getLogicalAddress(), mDeviceId);
            } else {
                // There is no connected hdmi device, just cold switch
                mDroidLogicHdmiCecManager.selectHdmiDevice(mDeviceId);
            }
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyUp: " + keyCode);
        boolean ret = true;

        if (mDroidLogicHdmiCecManager.hasHdmiCecDevice(mDeviceId)) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    mDroidLogicHdmiCecManager.sendKeyEvent((mKeyCodeMediaPlayPauseCount % 2 == 1 ? KeyEvent.KEYCODE_MEDIA_PAUSE : KeyEvent.KEYCODE_MEDIA_PLAY), false);
                    mKeyCodeMediaPlayPauseCount++;
                    break;
                case KeyEvent.KEYCODE_BACK:
                    Log.d(TAG, "KEYCODE_BACK shoud not send to live tv if cec device exits");
                    mDroidLogicHdmiCecManager.sendKeyEvent(keyCode, false);
                    break;
                default:
                    mDroidLogicHdmiCecManager.sendKeyEvent(keyCode, false);
                    break;
            }
        } else {
            Log.d(TAG, "cec device didn't exist");
            ret = false;
        }
        return ret;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown: " + keyCode);
        boolean ret = true;

        if (mDroidLogicHdmiCecManager.hasHdmiCecDevice(mDeviceId)) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    mDroidLogicHdmiCecManager.sendKeyEvent((mKeyCodeMediaPlayPauseCount % 2 == 1 ? KeyEvent.KEYCODE_MEDIA_PAUSE : KeyEvent.KEYCODE_MEDIA_PLAY), true);
                    break;
                case KeyEvent.KEYCODE_BACK:
                    Log.d(TAG, "KEYCODE_BACK shoud not send to live tv if cec device exits");
                    mDroidLogicHdmiCecManager.sendKeyEvent(keyCode, true);
                    break;
                default:
                    mDroidLogicHdmiCecManager.sendKeyEvent(keyCode, true);
                    break;
            }
        } else {
            Log.d(TAG, "cec device didn't exist");
            ret = false;
        }
        return ret;
    }

    //add for get hdmi info
    private void checkHdmiInfoOnVideoAvailable() {
        Log.d(TAG, "checkHdmiInfoOnVideoAvailable");
        if (mSessionHandler != null) {
            mSessionHandler.removeMessages(MSG_UPDATE_HDMI_HDR);
            mSessionHandler.removeMessages(MSG_UPDATE_HDMI_AUDIO_FORMAT);
            mSessionHandler.removeMessages(MSG_CLEAR_INFO);
            mSessionHandler.sendEmptyMessageDelayed(MSG_UPDATE_HDMI_HDR, MSG_DELAY_PERIOD);
            mSessionHandler.sendEmptyMessageDelayed(MSG_UPDATE_HDMI_AUDIO_FORMAT, MSG_DELAY_PERIOD);
        }
    }

    private void checkHdmiInfoOnVideoUnavailable() {
        Log.d(TAG, "checkHdmiInfoOnVideoUnavailable");
        if (mSessionHandler != null) {
            mSessionHandler.removeMessages(MSG_UPDATE_HDMI_HDR);
            mSessionHandler.removeMessages(MSG_UPDATE_HDMI_AUDIO_FORMAT);
            mSessionHandler.removeMessages(MSG_CLEAR_INFO);
            mSessionHandler.sendEmptyMessageDelayed(MSG_CLEAR_INFO, MSG_DELAY_PERIOD);
        }
    }

    private void sendHdmiHdrInfoByTif(String hdrInfo) {
        Bundle bundle = new Bundle();
        bundle.putString(DroidLogicTvUtils.SIG_INFO_HDMI_HDR, hdrInfo);
        notifySessionEvent(DroidLogicTvUtils.SIG_INFO_HDMI_HDR, bundle);
    }

    private void sendHdmiAudioFormatByTif(String audioFormat) {
        Bundle bundle = new Bundle();
        bundle.putString(DroidLogicTvUtils.SIG_INFO_HDMI_AUDIO_FORMAT, audioFormat);
        notifySessionEvent(DroidLogicTvUtils.SIG_INFO_HDMI_AUDIO_FORMAT, bundle);
    }

    private String getHdmiHdrInfo() {
        String result = null;
        int hdrType = mSystemControlManager.GetSourceHdrType();
        switch (hdrType) {
            case 0:
                result = "UNKOWN";
                break;
            case 1:
                result = "HDR10";
                break;
            case 2:
                result = "HDR10PLUS";
                break;
            case 3:
                result = "DOVI";
                break;
            case 4:
                result = "PRIMESL";
                break;
            case 5:
                result = "HLG";
                break;
            case 6:
                result = "SDR";
                break;
            case 7:
                result = "MVC";
                break;
            default:
                result = "UNKOWN";
                break;
        }
        return result;
    }

    /*
    *PCM audios require  display PCM
    *DD  audios require display Dolby Digital
    *DDP audios require display Dolby Digital Plus
    *DTS  audios require display DTS
    *DTS HD audios require display DTS HD
    */
    private String getHdmiAudioFormat() {
        String result = null;
        String formatInfo = mAudioManager.getParameters("HDMIIN audio format");
        int audioFormat = parseFirstValidIntegerByPattern(formatInfo);
        switch (audioFormat) {
            case 0:
                result = "PCM";
                break;
            case 1:
                result = "Dolby Digital";
                break;
            case 2:
                result = "Dolby Digital Plus";
                break;
            case 3:
                result = "DTS";
                break;
            case 4:
                result = "DTS HD";
                break;
            case 5:
                result = "TRUE HD";
                break;
            default:
                break;
        }
        return result;
    }

    private void checkHdmiHdrInfo() {
        String hdmiHdr = getHdmiHdrInfo();
        if (!TextUtils.equals(hdmiHdr, mHdmiHdrInfo)) {
            mHdmiHdrInfo = hdmiHdr;
            sendHdmiHdrInfoByTif(hdmiHdr);
        }
        if (mSessionHandler != null) {
            mSessionHandler.removeMessages(MSG_UPDATE_HDMI_HDR);
            mSessionHandler.sendEmptyMessageDelayed(MSG_UPDATE_HDMI_HDR, MSG_DELAY_PERIOD);
        }
    }

    private void checkHdmiAudioFormat() {
        String hdmiAudioFormat = getHdmiAudioFormat();
        if (!TextUtils.equals(hdmiAudioFormat, mHdmiAudioFormatInfo)) {
            mHdmiAudioFormatInfo = hdmiAudioFormat;
            sendHdmiAudioFormatByTif(hdmiAudioFormat);
        }
        if (mSessionHandler != null) {
            mSessionHandler.removeMessages(MSG_UPDATE_HDMI_AUDIO_FORMAT);
            mSessionHandler.sendEmptyMessageDelayed(MSG_UPDATE_HDMI_AUDIO_FORMAT, MSG_DELAY_PERIOD);
        }
    }

    private void clearInfo() {
        mHdmiHdrInfo = null;
        mHdmiAudioFormatInfo = null;
        sendHdmiHdrInfoByTif(null);
        sendHdmiAudioFormatByTif(null);
    }

    private int parseFirstValidIntegerByPattern(String info) {
        int result = -1;
        try {
            String regEx="[0-9]+";
            Pattern p = Pattern.compile(regEx);
            Matcher m = p.matcher(info);
            if (m.find()) {
              result = Integer.valueOf(m.group(0));
            } else {
                Log.d(TAG, "parseFirstValidIntergerByPattern not matched");
            }
        } catch (Exception e) {
            Log.d(TAG, "parseFirstValidIntergerByPattern Exception = " + e.getMessage());
        }
        return result;
    }
}
