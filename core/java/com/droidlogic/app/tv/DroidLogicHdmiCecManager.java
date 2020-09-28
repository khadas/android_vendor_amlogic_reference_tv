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
import android.hardware.hdmi.HdmiControlManager;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.HdmiClient;
import android.hardware.hdmi.HdmiTvClient;
import android.hardware.hdmi.HdmiTvClient.SelectCallback;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputInfo;
import android.util.Log;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import com.droidlogic.app.SystemControlManager;

public class DroidLogicHdmiCecManager {
    private static final String TAG = "DroidLogicHdmiCecManager";
    private static boolean DEBUG = Log.isLoggable("HDMI", Log.DEBUG);

    private static final String HDMI_CONTROL_ENABLED = "hdmi_control_enabled";

    private static final int DEVICE_SELECT_PROTECTION_TIME = 2000;

    private static final int MSG_DEVICE_SELECT = 0;
    private static final int MSG_PORT_SELECT = 1;
    private static final int MSG_SELECT_PROTECTION = 2;
    private static final int MSG_SEND_KEY_EVENT = 3;

    private static DroidLogicHdmiCecManager mInstance;

    private Context mContext;
    private HdmiControlManager mHdmiControlManager;
    private HdmiTvClient mTvClient;
    private HdmiClient mClient;
    private TvInputManager mTvInputManager;
    private TvControlDataManager mTvControlDataManager;
    private TvControlManager mTvControlManager;
    private SystemControlManager mSystemControlManager;

    private int mSelectedLogicalAddress;
    private int mSelectedPortId;
    private int mSelectedDeviceId;

    // If we have selected a valid hdmi source, we should not device select
    // internal address 0 within the protection time.
    private boolean mInSelectProtection;

    static final String PROPERTY_VENDOR_DEVICE_TYPE = "ro.vendor.platform.hdmi.device_type";

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DEVICE_SELECT:
                    setDeviceIdForCec(mSelectedDeviceId);
                    deviceSelect(mSelectedLogicalAddress);

                    if (mSelectedLogicalAddress != HdmiDeviceInfo.ADDR_INTERNAL) {
                        mInSelectProtection = true;
                    }
                    break;
                case MSG_PORT_SELECT:
                    setDeviceIdForCec(mSelectedDeviceId);
                    portSelect(mSelectedPortId);
                    mInSelectProtection = true;
                    break;
                case MSG_SELECT_PROTECTION:
                    mInSelectProtection = false;
                    break;
                case MSG_SEND_KEY_EVENT:
                    if (mTvClient == null) {
                        Log.e(TAG, "mHandler sendKeyEvent fail, mTvClient is null ?: " + (mTvClient == null));
                        return;
                    }
                    Log.d(TAG, "mHandler sendKeyEvent, keyCode: " + msg.arg1 + " isPressed: " + msg.arg2);
                    mTvClient.sendKeyEvent((int)msg.arg1, (((int)msg.arg2 == 1) ?  true : false));
                    break;
                default:
                    break;
            }
        }
    };

    private SelectCallback mSelectCallback = new SelectCallback() {
        @Override
        public void onComplete(int result) {
            Log.d(TAG, "select onComplete result = " + result);
        }
    };

    public static synchronized DroidLogicHdmiCecManager getInstance(Context context) {
        if (mInstance == null) {
            Log.d(TAG, "mInstance is null...");
            mInstance = new DroidLogicHdmiCecManager(context);
        }
        return mInstance;
    }

    public DroidLogicHdmiCecManager(Context context) {
        Log.d(TAG, "DroidLogicHdmiCecManager create");
        mContext = context;
        mHdmiControlManager = (HdmiControlManager) context.getSystemService(Context.HDMI_CONTROL_SERVICE);

        if (mHdmiControlManager != null) {
            List<Integer> mDeviceTypes;
            mDeviceTypes = getIntList(SystemProperties.get(PROPERTY_VENDOR_DEVICE_TYPE));
            for (int type : mDeviceTypes) {
                Log.i(TAG, "DroidLogicHdmiCecManager hdmi device type " + type);
                if (type == HdmiDeviceInfo.DEVICE_TV) {
                    mTvClient = mHdmiControlManager.getTvClient();
                    mClient = mHdmiControlManager.getTvClient();
                } else if (type == HdmiDeviceInfo.DEVICE_AUDIO_SYSTEM) {
                    mClient = mHdmiControlManager.getClient(HdmiDeviceInfo.DEVICE_AUDIO_SYSTEM);
                }
            }
        }

        mTvInputManager = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
        mTvControlDataManager = TvControlDataManager.getInstance(mContext);
        mTvControlManager = TvControlManager.getInstance();
        mSystemControlManager = SystemControlManager.getInstance();
    }

    protected static List<Integer> getIntList(String string) {
        ArrayList<Integer> list = new ArrayList<>();
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(',');
        splitter.setString(string);
        for (String item : splitter) {
            try {
                list.add(Integer.parseInt(item));
            } catch (NumberFormatException e) {
                Log.d(TAG, "Can't parseInt: " + item);
            }
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * Use logicalAddress to switch source in senarios like InputService.Session setMain()
     */
    public void selectHdmiDevice(int logicAddress, int deviceId) {
        Log.d(TAG, "selectHdmiDevice " + logicAddress + " deviceId " + deviceId + " " + mInSelectProtection);

        int delayTime = 0;
        if ((HdmiDeviceInfo.ADDR_INTERNAL == logicAddress) && deviceId == HdmiDeviceInfo.ID_INVALID) {
            // TvInputBaseSession onRelease might could be done either before  or just after the new Session onSetMain.
            // If the old one onRelease after the new one onSetMain, then the active source will be reset to tv's 0.
            if (mInSelectProtection) {
                Log.e(TAG, "selectHdmiDevice protection time and no select internal address");
                return;
            } else {
                // Not directly select internal address, give the incoming hdmi tune a change to remove it.
                delayTime = DEVICE_SELECT_PROTECTION_TIME;
            }
        } else {
            // For hdmi routing we should remove previous messages, this works to avoid the Routing Change to
            // internal address 0 during normal hdmi channel switches.
            removePreviousMessages();
            mHandler.sendEmptyMessageDelayed(MSG_SELECT_PROTECTION, DEVICE_SELECT_PROTECTION_TIME);
        }

        mSelectedDeviceId = deviceId;
        mSelectedLogicalAddress = logicAddress;

        Message msg = mHandler.obtainMessage(MSG_DEVICE_SELECT);
        mHandler.sendMessageDelayed(msg, delayTime);
    }

    /**
     * Use deviceId to do the portSelect job in senarios like enable cec.
     */
    public void selectHdmiDevice(int deviceId) {
        Log.d(TAG, "selectHdmiDevice deviceId " + deviceId);
        int portId = getPortIdByDeviceId(deviceId);

        mSelectedDeviceId = deviceId;
        mSelectedPortId = portId;

        removePreviousMessages();
        mHandler.sendEmptyMessageDelayed(MSG_SELECT_PROTECTION, DEVICE_SELECT_PROTECTION_TIME);

        Message msg = mHandler.obtainMessage(MSG_PORT_SELECT, portId);
        mHandler.sendMessage(msg);
    }

    /**
     * When there is a new device select request, it's need to remove the previous
     * request first. A customed senario is that when user switches to a different
     * channel, there is a deviceSelect 0 first and then a deviceSelect 4, we need
     * to make sure the deviceSelect 0 is not finally performed so that there are
     * not so many meaningless routing messages.
     *
     * Besides, when using Handler remove messages, please be careful that only
     * when the Message is absolutely equal then Handler could remove it. Handler
     * can't remove a Message with 'what' and 'obj' using removeMessages(int what).
     */
    private void removePreviousMessages() {
        if (mHandler.hasMessages(MSG_DEVICE_SELECT)) {
            Log.d(TAG, "removePreviousMessages logical address:" + mSelectedLogicalAddress);
            mHandler.removeMessages(MSG_DEVICE_SELECT);
        } else if (mHandler.hasMessages(MSG_PORT_SELECT)) {
            Log.d(TAG, "removePreviousMessages port id:" + mSelectedPortId);
            mHandler.removeMessages(MSG_PORT_SELECT);
        } else if (mHandler.hasMessages(MSG_SELECT_PROTECTION)) {
            mHandler.removeMessages(MSG_SELECT_PROTECTION);
        }
    }

    /**
    * generally used to switch source.
    */
    private void deviceSelect(int logicalAddress) {
        if (mTvClient == null) {
            Log.e(TAG, "switchActiveSource tv client null.");
            return;
        }

        Log.d(TAG, "deviceSelect " + logicalAddress);
        mTvClient.deviceSelect(logicalAddress, mSelectCallback);
    }

    /**
     * only used in special senarios where can't get logical address like
     * open cec switch. Tv will not do the tune action and the hdmi device
     * list has not been created for the connected devices.
     */
    private void portSelect(int portId) {
        if (mTvClient == null) {
            Log.e(TAG, "switchActiveSource tv client null.");
            return;
        }

        Log.d(TAG, "portSelect " + portId);
        mTvClient.portSelect(portId, mSelectCallback);
    }

    public HdmiDeviceInfo getHdmiDeviceInfo(String iputId) {
        List<TvInputInfo> tvInputList = mTvInputManager.getTvInputList();
        for (TvInputInfo info : tvInputList) {
            HdmiDeviceInfo hdmiDeviceInfo = info.getHdmiDeviceInfo();
            if (hdmiDeviceInfo != null) {
                if (iputId.equals(info.getId()) || iputId.equals(info.getParentId())) {
                    return hdmiDeviceInfo;
                }
            }
        }
        return null;
    }

    public void setDeviceIdForCec(int deviceId){
        // Give cec hal a chance to filter strange <Active Source>
        if (mTvControlManager != null) {
            Log.d(TAG, "setDeviceIdForCec " + deviceId);
            mTvControlManager.setDeviceIdForCec(deviceId);
        }
    }

    public int getPortIdByDeviceId(int deviceId) {
        List<TvInputHardwareInfo> hardwareList = mTvInputManager.getHardwareList();
        if (hardwareList == null || hardwareList.size() == 0) {
            return -1;
        }

        for (TvInputHardwareInfo hardwareInfo : hardwareList) {
            if (deviceId == hardwareInfo.getDeviceId()) {
                return hardwareInfo.getHdmiPortId();
            }
        }
        return -1;
    }

    public boolean hasHdmiCecDevice(int deviceId) {
        Log.d(TAG, "hasHdmiCecDevice, deviceId: " + deviceId);
        if (isHdmiDeviceId(deviceId)) {
            int id = getPortIdByDeviceId(deviceId);

            if (mClient == null) {
                Log.e(TAG, "hasHdmiCecDevice HdmiClient null!");
                return false;
            }
            if (mTvClient != null) {
                for (HdmiDeviceInfo info : mTvClient.getDeviceList()) {
                    if (id == ((int)info.getPortId())) {
                        Log.d(TAG, "hasHdmiCecDevice find active device " + info);
                        return true;
                    }
                }
            } else {
                Log.d(TAG, "hasHdmiCecDevice no check devicelist if it's not tv.");
                return true;
            }
        }
        return false;
    }

    public int getInputSourceDeviceId() {
        return  mTvControlDataManager.getInt(mContext.getContentResolver(), DroidLogicTvUtils.TV_CURRENT_DEVICE_ID, 0);
    }

    public boolean isHdmiDeviceId(int deviceId) {
        return deviceId >= DroidLogicTvUtils.DEVICE_ID_HDMI1
                && deviceId <= DroidLogicTvUtils.DEVICE_ID_HDMI4;
    }

    public void sendKeyEvent(int keyCode, boolean isPressed) {
        Message msg = mHandler.obtainMessage(MSG_SEND_KEY_EVENT, keyCode, isPressed ? 1 : 0);
        mHandler.sendMessageDelayed(msg, 0);
    }
}
