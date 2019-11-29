/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description:
 *     AMLOGIC HdmiCecDeviceInfo
 */

package com.droidlogic.app.tv;

import android.hardware.hdmi.HdmiDeviceInfo;

public class HdmiCecDeviceInfo {
    /**
     * Logical address used to indicate the source comes from internal device. The logical address
     * of TV(0) is used.
     */
    public static final int ADDR_INTERNAL = 0;

    /**
     * Physical address used to indicate the source comes from internal device. The physical address
     * of TV(0) is used.
     */
    public static final int PATH_INTERNAL = 0x0000;

    /** Invalid physical address (routing path) */
    public static final int PATH_INVALID = 0xFFFF;

    /** Internal port ID */
    public static final int PORT_INTERNAL = 0;

    /** Invalid port ID */
    public static final int PORT_INVALID = -1;

    /** Audio Only port ID */
    public static final int PORT_AUDIO_ONLY = -2;

    /** Audio Only port ID */
    /** not impl ARC as an TV Input */
    public static final int PORT_HDMI_ARC_IN = -3;

    /** Invalid device ID */
    public static final int ID_INVALID = 0xFFFF;

    public static final int DEVICE_ID_HDMI1      = 5;
    public static final int DEVICE_ID_HDMI2      = 6;
    public static final int DEVICE_ID_HDMI3      = 7;
    public static final int DEVICE_ID_HDMI4      = 8;

    public static final int PORT_OFFSEF = 4;

    /** Device info used to indicate an inactivated device. */
    public static final HdmiCecDeviceInfo INACTIVE_DEVICE = new HdmiCecDeviceInfo();

    /** Device info used to indicate an internal device like atv. */
    public static final HdmiCecDeviceInfo INTERNAL_DEVICE = new HdmiCecDeviceInfo(
                                                            ADDR_INTERNAL,
                                                            PATH_INTERNAL);

    private int mPhysicalAddress;
    private int mPortId;
    private int mLogicalAddress;
    private int mDeviceId;

    public static HdmiCecDeviceInfo createHdmiCecDeviceInfo(HdmiDeviceInfo device) {
        if (null == device) {
            return INACTIVE_DEVICE;
        }
        return new HdmiCecDeviceInfo(device.getLogicalAddress(), device.getPhysicalAddress());
    }

    /**
     * Constructor. Used to initialize the instance for CEC device.
     *
     * @param logicalAddress logical address of HDMI-CEC device
     * @param physicalAddress physical address of HDMI-CEC device
     * @param portId HDMI port ID (1 for HDMI1)
     */
    public HdmiCecDeviceInfo() {
        mPhysicalAddress = PATH_INVALID;
        mPortId = PORT_INVALID;
        mLogicalAddress = -1;
        mDeviceId = 0;
    }

    /**
     * Constructor. Used to initialize the instance for CEC device.
     *
     * @param logicalAddress logical address of HDMI-CEC device
     * @param physicalAddress physical address of HDMI-CEC device
     * @param portId HDMI port ID (1 for HDMI1)
     */
    public HdmiCecDeviceInfo(int logicalAddress, int physicalAddress) {
        setLogicalAddress(logicalAddress);
        setPhysicalAddress(physicalAddress);
    }

    /**
     * Returns the CEC logical address of the device.
     */
    public int getLogicalAddress() {
        return mLogicalAddress;
    }

    /**
     * Returns the physical address of the device.
     */
    public int getPhysicalAddress() {
        return mPhysicalAddress;
    }

    /**
     * Returns the port ID.
     */
    public int getPortId() {
        return mPortId;
    }

    public int getDeviceId() {
        return mDeviceId;
    }

    /**
     * set the CEC logical address of the device.
     */
    public void setLogicalAddress(int logicalAddress) {
        mLogicalAddress = logicalAddress;
    }

    /**
     * set the physical address of the device.
     */
    public void setPhysicalAddress(int physicalAddress) {
        mPhysicalAddress = physicalAddress;
        mPortId = (mPhysicalAddress & 0xF000) >> 3;
    }

    /**
     * set the port ID.
     */
    public void setPortId(int portId) {
        mPortId = portId;
    }

    public void setDeviceId(int deviceId) {
        mDeviceId = deviceId;
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append("CEC: ");
        s.append("logical_address: ").append(String.format("0x%02X", mLogicalAddress));
        s.append(" ");
        s.append("physical_address: ").append(String.format("0x%04X", mPhysicalAddress));
        s.append(" ");
        s.append("port_id: ").append(mPortId);
        s.append(" ");
        s.append("device_id: ").append(mDeviceId);
        return s.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HdmiCecDeviceInfo)) {
            return false;
        }

        HdmiCecDeviceInfo other = (HdmiCecDeviceInfo) obj;
        return mPhysicalAddress == other.mPhysicalAddress
               && mPortId == other.mPortId
               && mLogicalAddress == other.mLogicalAddress;
    }

}
