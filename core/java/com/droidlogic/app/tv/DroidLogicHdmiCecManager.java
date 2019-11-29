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

    private static Context mContext;
    private HdmiControlManager mHdmiControlManager;
    private HdmiTvClient mTvClient = null;
    private HdmiClient mClient = null;

    private static DroidLogicHdmiCecManager mInstance;
    private TvInputManager mTvInputManager;
    private TvControlDataManager mTvControlDataManager;
    private TvControlManager mTvControlManager;
    private SystemControlManager mSystemControlManager;

    private static final int DEVICE_SELECT_INTERNAL_DELAY = 1000;

    private static final int MSG_DEVICE_SELECT = 0;
    private static final int MSG_PORT_SELECT = 1;
    private static final int MSG_SEND_KEY_EVENT = 2;

    private static final String HDMI = "HDMI";
    private static final String HW = "HW";
    // 0x240004
    private static final int PHY_LOG_ADDRESS_LENGTH = 6;
    private static final int DEVICE_ID_LENGTH = 1;
    private static final String HEX_STRING = "0123456789ABCDEF";

    private static final String HDMI_CONTROL_ENABLED = "hdmi_control_enabled";

    static final String PROPERTY_VENDOR_DEVICE_TYPE = "ro.vendor.platform.hdmi.device_type";

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DEVICE_SELECT:
                    deviceSelect((int)msg.obj);
                    break;
                case MSG_PORT_SELECT:
                    portSelect((int)msg.obj);
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
        Log.d(TAG, "selectHdmiDevice " + logicAddress + " deviceId:" + deviceId);

        // Give cec hal a chance to filter strange <Active Source>
        setDeviceIdForCec(deviceId);

        // Don't need to worry about repeat actions of device selecting. The validation
        // Work is done in HdmiCecLocalDeviceTv deviceSelect method.
        mHandler.removeMessages(MSG_DEVICE_SELECT);
        mHandler.removeMessages(MSG_PORT_SELECT);

        int delayTime = 0;
        if (HdmiDeviceInfo.ADDR_INTERNAL == logicAddress) {
            delayTime = DEVICE_SELECT_INTERNAL_DELAY;
        }
        Message msg = mHandler.obtainMessage(MSG_DEVICE_SELECT, logicAddress);
        mHandler.sendMessageDelayed(msg, delayTime);
    }

    /**
     * use deviceId to do the portSelect job in senarios like enable cec.
     */
    public void selectHdmiDevice(int deviceId) {
        Log.d(TAG, "selectHdmiDevice deviceId:" + deviceId);

        // Give cec hal a chance to filter strange <Active Source>
        setDeviceIdForCec(deviceId);

        int portId = getPortIdByDeviceId(deviceId);
        mHandler.removeMessages(MSG_DEVICE_SELECT);
        mHandler.removeMessages(MSG_PORT_SELECT);

        Message msg = mHandler.obtainMessage(MSG_PORT_SELECT, portId);
        mHandler.sendMessage(msg);
    }

    /**
    * generally used to switch source.
    */
    private void deviceSelect(int logicalAddress) {
        if (mTvClient == null) {
            Log.e(TAG, "switchActiveSource tv client null.");
            return;
        }

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

    /**
     * get logical address from inputid likecom.droidlogic.tvinput/.services.Hdmi2InputService/HDMI240008
     */
    public int getLogicalAddressFromInputId(String inputId) {
        if (TextUtils.isEmpty(inputId)) {
            Log.e(TAG, "getLogicalAddressFromInputId inputId empty " + inputId);
            return -1;
        }

        int index = inputId.indexOf(HDMI);
        if (index == -1) {
            Log.e(TAG, "getLogicalAddressFromInputId has on hdmi " + inputId);
            return -1;
        }

        int logicalAddress = -1;
        try {
            String address = inputId.substring(index + HDMI.length());
            Log.d(TAG, "getLogicalAddressFromInputId address " + address);
            if (address.length() == PHY_LOG_ADDRESS_LENGTH) {
                char logicalAddressChar = address.charAt(PHY_LOG_ADDRESS_LENGTH - 1);
                logicalAddress = HEX_STRING.indexOf(logicalAddressChar);
            }
        } catch(Exception e) {
            Log.e(TAG, "getLogicalAddressFromInputId " + inputId + e);
        }
        Log.d(TAG, "getLogicalAddressFromInputId result " + logicalAddress);
        return logicalAddress;
    }

    /**
     * get deviceId from inputid like com.droidlogic.tvinput/.services.Hdmi2InputService/HW5
     */
    public int getDeviceIdFromInputId(String inputId) {
        if (TextUtils.isEmpty(inputId)) {
            Log.e(TAG, "getDeviceIdFromInputId inputId empty " + inputId);
            return -1;
        }

        int index = inputId.indexOf(HW);
        if (index == -1) {
            Log.e(TAG, "getLogicalAddressFromInputId has on hw " + inputId);
            return -1;
        }

        int deviceId = -1;
        try {
            String address = inputId.substring(index + HW.length());
            Log.d(TAG, "getDeviceIdFromInputId address " + address);
            deviceId = Integer.parseInt(address);
        } catch(Exception e) {
            Log.e(TAG, "getDeviceIdFromInputId " + inputId + e);
        }
        Log.d(TAG, "getDeviceIdFromInputId result " + deviceId);
        return deviceId;
    }

    public boolean hasHdmiCecDevice(int deviceId) {
        Log.d(TAG, "hasHdmiCecDevice, deviceId: " + deviceId);
        if (deviceId >= DroidLogicTvUtils.DEVICE_ID_HDMI1 && deviceId <= DroidLogicTvUtils.DEVICE_ID_HDMI4) {
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

    public void sendKeyEvent(int keyCode, boolean isPressed) {
        Message msg = mHandler.obtainMessage(MSG_SEND_KEY_EVENT, keyCode, isPressed ? 1 : 0);
        mHandler.sendMessageDelayed(msg, 0);
    }
}
