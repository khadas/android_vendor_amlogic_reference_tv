/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description: JAVA file
 */

package com.droidlogic.app.tv;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.tv.TvContract;
import android.os.Build;
import android.os.Handler;
import android.os.HwBinder;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;

import android.util.Log;
import android.view.View;
import android.view.Surface;
import android.view.SurfaceHolder;
import java.lang.reflect.Method;

import static com.droidlogic.app.tv.TvControlCommand.*;
import com.droidlogic.app.tv.EasEvent;
import com.droidlogic.app.tv.RrtEvent;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Objects;

//import android.hidl.manager.V1_0.IServiceManager;
//import android.hidl.manager.V1_0.IServiceNotification;
import vendor.amlogic.hardware.tvserver.V1_0.ITvServer;
import vendor.amlogic.hardware.tvserver.V1_0.ITvServerCallback;
import vendor.amlogic.hardware.tvserver.V1_0.SignalInfo;
import vendor.amlogic.hardware.tvserver.V1_0.FormatInfo;
import vendor.amlogic.hardware.tvserver.V1_0.TvHidlParcel;
import vendor.amlogic.hardware.tvserver.V1_0.ConnectType;
import vendor.amlogic.hardware.tvserver.V1_0.Result;
import vendor.amlogic.hardware.tvserver.V1_0.FreqList;
import vendor.amlogic.hardware.tvserver.V1_0.RRTSearchInfo;
import vendor.amlogic.hardware.tvserver.V1_0.BasicVdecState;

public class TvControlManager {
    private static final String TAG = "TvControlManager";
    private static final String OPEN_TV_LOG_FLG = "open.libtv.log.flg";
    private boolean tvLogFlg =false;

    public static final int AUDIO_MUTE_ON               = 0;
    public static final int AUDIO_MUTE_OFF              = 1;

    public static final int AUDIO_SWITCH_OFF            = 0;
    public static final int AUDIO_SWITCH_ON             = 1;

    //atv media
    public static final int ATV_AUDIO_STD_DK            = 0;
    public static final int ATV_AUDIO_STD_I             = 1;
    public static final int ATV_AUDIO_STD_BG            = 2;
    public static final int ATV_AUDIO_STD_M             = 3;
    public static final int ATV_AUDIO_STD_L             = 4;
    public static final int ATV_AUDIO_STD_LC            = 5;
    public static final int ATV_AUDIO_STD_AUTO          = 6;
    public static final int ATV_AUDIO_STD_MUTE          = 7;

    public static final int ATV_VIDEO_STD_AUTO          = 0;
    public static final int ATV_VIDEO_STD_PAL           = 1;
    public static final int ATV_VIDEO_STD_NTSC          = 2;
    public static final int ATV_VIDEO_STD_SECAM         = 3;

    public static final int V4L2_COLOR_STD_PAL   = 0x04000000;
    public static final int V4L2_COLOR_STD_NTSC  = 0x08000000;
    public static final int V4L2_COLOR_STD_SECAM = 0x10000000;
    public static final int V4L2_STD_PAL_M       = 0x00000100;
    public static final int V4L2_STD_PAL_N       = 0x00000200;
    public static final int V4L2_STD_PAL_Nc      = 0x00000400;
    public static final int V4L2_STD_PAL_60      = 0x00000800;

    //tv run status
    public static final int TV_RUN_STATUS_INIT_ED       = -1;
    public static final int TV_RUN_STATUS_OPEN_ED       = 0;
    public static final int TV_RUN_STATUS_START_ED      = 1;
    public static final int TV_RUN_STATUS_RESUME_ED     = 2;
    public static final int TV_RUN_STATUS_PAUSE_ED      = 3;
    public static final int TV_RUN_STATUS_STOP_ED       = 4;
    public static final int TV_RUN_STATUS_CLOSE_ED      = 5;

    //scene mode
    public static final int SCENE_MODE_STANDARD         = 0;
    public static final int SCENE_MODE_GAME             = 1;
    public static final int SCENE_MODE_FILM             = 2;
    public static final int SCENE_MODE_USER             = 3;
    public static final int SCENE_MODE_MAX              = 4;

    //capture type
    public static final int CAPTURE_VIDEO               = 0;
    public static final int CAPTURE_GRAPHICS            = 1;

    //mts out_mode
    public static final int AUDIO_OUTMODE_MONO          = 0;
    public static final int AUDIO_OUTMODE_STEREO        = 1;
    public static final int AUDIO_OUTMODE_SAP           = 2;

    //audio std
    public static final int AUDIO_STANDARD_BTSC         = 0x00;
    public static final int AUDIO_STANDARD_EIAJ         = 0x01;
    public static final int AUDIO_STANDARD_A2_K         = 0x02;
    public static final int AUDIO_STANDARD_A2_BG        = 0x03;
    public static final int AUDIO_STANDARD_A2_DK1       = 0x04;
    public static final int AUDIO_STANDARD_A2_DK2       = 0x05;
    public static final int AUDIO_STANDARD_A2_DK3       = 0x06;
    public static final int AUDIO_STANDARD_NICAM_I      = 0x07;
    public static final int AUDIO_STANDARD_NICAM_BG     = 0x08;
    public static final int AUDIO_STANDARD_NICAM_L      = 0x09;
    public static final int AUDIO_STANDARD_NICAM_DK     = 0x0A;
    public static final int AUDIO_STANDARD_MONO_BG      = 0x12;
    public static final int AUDIO_STANDARD_MONO_DK      = 0x13;
    public static final int AUDIO_STANDARD_MONO_I       = 0x14;
    public static final int AUDIO_STANDARD_MONO_M       = 0x15;
    public static final int AUDIO_STANDARD_MONO_L       = 0x16;

    //A2 audio mode
    public static final int AUDIO_OUTMODE_A2_MONO       = 0;
    public static final int AUDIO_OUTMODE_A2_STEREO     = 1;
    public static final int AUDIO_OUTMODE_A2_DUAL_A     = 2;
    public static final int AUDIO_OUTMODE_A2_DUAL_B     = 3;
    public static final int AUDIO_OUTMODE_A2_DUAL_AB    = 4;

    //NICAM audio mode
    public static final int AUDIO_OUTMODE_NICAM_MONO    = 0;
    public static final int AUDIO_OUTMODE_NICAM_MONO1   = 1;
    public static final int AUDIO_OUTMODE_NICAM_STEREO  = 2;
    public static final int AUDIO_OUTMODE_NICAM_DUAL_A  = 3;
    public static final int AUDIO_OUTMODE_NICAM_DUAL_B  = 4;
    public static final int AUDIO_OUTMODE_NICAM_DUAL_AB = 5;

    //ATV audio signal input type
    public static final int AUDIO_INMODE_MONO           = 0;//for all
    public static final int AUDIO_INMODE_STEREO         = 1;//for all
    public static final int AUDIO_INMODE_MONO_SAP       = 2;//for btsc
    public static final int AUDIO_INMODE_STEREO_SAP     = 3;//for btsc
    public static final int AUDIO_INMODE_DUAL           = 2;//for nicam and a2
    public static final int AUDIO_INMODE_NICAM_MONO     = 3;//for nicam and a2

    private long mNativeContext; // accessed by native methods
    private EventHandler mEventHandler;
    private ArrayList<TvInSignalInfo.SigInfoChangeListener> mSigInfoChangeListener;
    private TvInSignalInfo.SigChannelSearchListener mSigChanSearchListener = null;
    private Status3DChangeListener mStatus3DChangeListener = null;
    private AdcCalibrationListener mAdcCalibrationListener = null;
    private SourceSwitchListener mSourceSwitchListener = null;
    private ChannelSelectListener mChannelSelectListener = null;
    private SerialCommunicationListener mSerialCommunicationListener = null;
    private CloseCaptionListener mCloseCaptionListener = null;
    private ArrayList<StatusSourceConnectListener> mSourceConnectChangeListener;
    private HDMIRxCECListener mHDMIRxCECListener = null;
    private UpgradeFBCListener mUpgradeFBCListener  = null;
    private SubtitleUpdateListener mSubtitleListener = null;
    private ScannerEventListener mScannerListener = null;
    private StorDBEventListener mStorDBListener = null;
    private ScanningFrameStableListener mScanningFrameStableListener = null;
    private VframBMPEventListener mVframBMPListener = null;
    private EpgEventListener mEpgListener = null;
    private RrtEventListener mRrtListener = null;
    private AVPlaybackListener mAVPlaybackListener = null;
    private PlayerInstanceNoListener mPlayerInstanceNoListener = null;
    private EasEventListener mEasListener = null;
    private AudioEventListener mAudioListener = null;
    private QmsEventListener mQmsEventListener = null;
    private int rrt5XmlLoadStatus = 0;
    public static  int EVENT_RRT_SCAN_START          = 1;
    public static  int EVENT_RRT_SCAN_END            = 3;

    private EasManager easManager = new EasManager();
    private static TvControlManager mInstance;

    private int sendCmdToTv(Parcel p, Parcel r) {
        Log.i(TAG, "sendCmdToTv is abandoned in Android O, please use cmd HIDL way!!cmd:" + p.readInt());

        return -1;
        /*p.setDataPosition(0);

        int ret = processCmd(p, r);
        r.setDataPosition(0);
        return ret;*/
    }

    public int sendCmd(int cmd) {
        Log.i(TAG, "sendCmd is abandoned in Android O, please use cmd HIDL way!!cmd:" + cmd);
        return -1;
        /*libtv_log_open();
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        request.writeInt(cmd);
        request.setDataPosition(0);
        processCmd(request, reply);
        reply.setDataPosition(0);
        int ret = reply.readInt();

        request.recycle();
        reply.recycle();
        return ret;*/
    }

    public int sendCmdIntArray(int cmd, int[] values) {
        Log.i(TAG, "sendCmdIntArray is abandoned in Android O, please use cmd HIDL way!!cmd:" + cmd);
        return -1;
        /*libtv_log_open();
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        request.writeInt(cmd);
        for (int i = 0; i < values.length; i++) {
            request.writeInt(values[i]);
        }
        request.setDataPosition(0);
        processCmd(request, reply);
        reply.setDataPosition(0);
        int ret = reply.readInt();

        request.recycle();
        reply.recycle();
        return ret;*/
    }

    public int sendCmdFloatArray(int cmd, float[] values) {
        Log.i(TAG, "sendCmdFloatArray is abandoned in Android O, please use cmd HIDL way!!cmd:" + cmd);
        return -1;
        /*libtv_log_open();
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        request.writeInt(cmd);

        for (int i = 0; i < values.length; i++) {
            request.writeFloat(values[i]);
        }
        request.setDataPosition(0);
        processCmd(request, reply);
        reply.setDataPosition(0);
        int ret = reply.readInt();

        request.recycle();
        reply.recycle();
        return ret;*/
    }

    public int sendCmdStringArray(int cmd, String[] values) {
        Log.i(TAG, "sendCmdStringArray is abandoned in Android O, please use cmd HIDL way!!cmd:" + cmd);
        return -1;
        /*libtv_log_open();
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        request.writeInt(cmd);

        for (int i = 0; i < values.length; i++) {
            request.writeString(values[i]);
        }
        request.setDataPosition(0);
        processCmd(request, reply);
        reply.setDataPosition(0);
        int ret = reply.readInt();

        request.recycle();
        reply.recycle();
        return ret;*/
    }

    public int sendCmdStringArray(int cmd, int id, String[] values) {
        Log.i(TAG, "sendCmdStringArray is abandoned in Android O, please use cmd HIDL way!!cmd:" + cmd + " id:" + id);
        return -1;
        /*libtv_log_open();
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        request.writeInt(cmd);
        request.writeInt(id);

        for (int i = 0; i < values.length; i++) {
            request.writeString(values[i]);
        }
        request.setDataPosition(0);
        processCmd(request, reply);
        reply.setDataPosition(0);
        int ret = reply.readInt();

        request.recycle();
        reply.recycle();
        return ret;*/
    }

    class EventHandler extends Handler {
        int dataArray[];
        int cmdArray[];
        int msgPdu[];

        public EventHandler(Looper looper) {
            super(looper);
            dataArray = new int[512];//max data buf
            cmdArray = new int[128];
            msgPdu = new int[1200];
        }

        private void readScanEvent(ScannerEvent scan_ev, TvHidlParcel p) {
            int i, j;
            scan_ev.type = p.bodyInt.get(0);
            Log.d(TAG, "scan ev type:"+ scan_ev.type);

            scan_ev.percent = p.bodyInt.get(1);
            scan_ev.totalcount = p.bodyInt.get(2);
            scan_ev.lock = p.bodyInt.get(3);
            scan_ev.cnum = p.bodyInt.get(4);
            scan_ev.freq = p.bodyInt.get(5);
            scan_ev.programName = p.bodyString.get(0);
            scan_ev.srvType = p.bodyInt.get(6);
            scan_ev.paras = p.bodyString.get(1);
            scan_ev.strength = p.bodyInt.get(7);
            scan_ev.quality = p.bodyInt.get(8);
            scan_ev.videoStd = p.bodyInt.get(9);
            scan_ev.audioStd = p.bodyInt.get(10);
            scan_ev.isAutoStd = p.bodyInt.get(11);

            scan_ev.mode = p.bodyInt.get(12);
            scan_ev.sr = p.bodyInt.get(13);
            scan_ev.mod = p.bodyInt.get(14);
            scan_ev.bandwidth = p.bodyInt.get(15);
            scan_ev.reserved = p.bodyInt.get(16);
            scan_ev.ts_id = p.bodyInt.get(17);
            scan_ev.orig_net_id = p.bodyInt.get(18);
            scan_ev.serviceID = p.bodyInt.get(19);
            scan_ev.vid = p.bodyInt.get(20);
            scan_ev.vfmt = p.bodyInt.get(21);
            int acnt = p.bodyInt.get(22);
            if (acnt != 0) {
                scan_ev.aids = new int[acnt];
                for (i=0;i<acnt;i++)
                    scan_ev.aids[i] = p.bodyInt.get(i+23);
                scan_ev.afmts = new int[acnt];
                for (i=0;i<acnt;i++)
                    scan_ev.afmts[i] = p.bodyInt.get(i+acnt+23);
                scan_ev.alangs = new String[acnt];
                for (i=0;i<acnt;i++)
                    scan_ev.alangs[i] = p.bodyString.get(i+2);
                scan_ev.atypes = new int[acnt];
                for (i=0;i<acnt;i++)
                    scan_ev.atypes[i] = p.bodyInt.get(i+2*acnt+23);
                scan_ev.aexts = new int[acnt];
                for (i=0;i<acnt;i++)
                    scan_ev.aexts[i] = p.bodyInt.get(i+3*acnt+23);
            }
            scan_ev.pcr = p.bodyInt.get(4*acnt+23);
            int scnt = p.bodyInt.get(4*acnt+24);
            if (scnt != 0) {
                scan_ev.stypes = new int[scnt];
                for (i=0;i<scnt;i++)
                    scan_ev.stypes[i] = p.bodyInt.get(i+4*acnt+25);
                scan_ev.sids = new int[scnt];
                for (i=0;i<scnt;i++)
                    scan_ev.sids[i] = p.bodyInt.get(i+scnt+4*acnt+25);
                scan_ev.sstypes = new int[scnt];
                for (i=0;i<scnt;i++)
                    scan_ev.sstypes[i] = p.bodyInt.get(i+2*scnt+4*acnt+25);
                scan_ev.sid1s = new int[scnt];
                for (i=0;i<scnt;i++)
                    scan_ev.sid1s[i] = p.bodyInt.get(i+3*scnt+4*acnt+25);
                scan_ev.sid2s = new int[scnt];
                for (i=0;i<scnt;i++)
                    scan_ev.sid2s[i] = p.bodyInt.get(i+4*scnt+4*acnt+25);
                scan_ev.slangs = new String[scnt];
                for (i=0;i<scnt;i++)
                    scan_ev.slangs[i] = p.bodyString.get(i+acnt+2);
            }
            scan_ev.free_ca = p.bodyInt.get(5*scnt+4*acnt+25);
            scan_ev.scrambled = p.bodyInt.get(5*scnt+4*acnt+26);
            scan_ev.scan_mode = p.bodyInt.get(5*scnt+4*acnt+27);
            scan_ev.sdtVersion = p.bodyInt.get(5*scnt+4*acnt+28);
            scan_ev.sort_mode = p.bodyInt.get(5*scnt+4*acnt+29);

            scan_ev.lcnInfo = new ScannerLcnInfo();
            scan_ev.lcnInfo.netId = p.bodyInt.get(5*scnt+4*acnt+30);
            scan_ev.lcnInfo.tsId = p.bodyInt.get(5*scnt+4*acnt+31);
            scan_ev.lcnInfo.serviceId = p.bodyInt.get(5*scnt+4*acnt+32);
            scan_ev.lcnInfo.visible = new int[4];
            scan_ev.lcnInfo.lcn = new int[4];
            scan_ev.lcnInfo.valid = new int[4];
            for (j=0; j<4; j++) {
                scan_ev.lcnInfo.visible[j] = p.bodyInt.get(j*3+5*scnt+4*acnt+33);
                scan_ev.lcnInfo.lcn[j] = p.bodyInt.get(j*3+5*scnt+4*acnt+34);
                scan_ev.lcnInfo.valid[j] = p.bodyInt.get(j*3+5*scnt+4*acnt+35);
            }
            scan_ev.majorChannelNumber = p.bodyInt.get(4*3+5*scnt+4*acnt+33);
            scan_ev.minorChannelNumber = p.bodyInt.get(4*3+5*scnt+4*acnt+34);
            scan_ev.sourceId = p.bodyInt.get(4*3+5*scnt+4*acnt+35);
            scan_ev.accessControlled = p.bodyInt.get(4*3+5*scnt+4*acnt+36);
            scan_ev.hidden = p.bodyInt.get(4*3+5*scnt+4*acnt+37);
            scan_ev.hideGuide = p.bodyInt.get(4*3+5*scnt+4*acnt+38);
            scan_ev.vct = p.bodyString.get(scnt+acnt+2);
            scan_ev.programs_in_pat = p.bodyInt.get(4*3+5*scnt+4*acnt+39);
            scan_ev.pat_ts_id = p.bodyInt.get(4*3+5*scnt+4*acnt+40);
        }

        @Override
        public void handleMessage(Message msg) {
            int i = 0, loop_count = 0, tmp_val = 0;

            TvHidlParcel parcel = ((TvHidlParcel) (msg.obj));
            switch (msg.what) {
                case DTV_AV_PLAYBACK_CALLBACK:
                    if (mAVPlaybackListener != null) {
                        int msgType= parcel.bodyInt.get(0);
                        int programID= parcel.bodyInt.get(1);
                        mAVPlaybackListener.onEvent(msgType, programID);
                    }
                    break;
                case TSPLAYER_INSTANCENO_CALLBACK:
                    if (mPlayerInstanceNoListener != null) {
                        int msgType = parcel.bodyInt.get(0);
                        int type = parcel.bodyInt.get(1);
                        int ID = parcel.bodyInt.get(2);
                        mPlayerInstanceNoListener.PlayerInsEvent(msgType, type, ID);
                    }
                    break;
                case SOURCE_CONNECT_CALLBACK:
                    if (!mSourceConnectChangeListener.isEmpty()) {
                        int source = parcel.bodyInt.get(0);
                        int connectedState = parcel.bodyInt.get(1);
                        if (source > 0) {
                            for (StatusSourceConnectListener l : mSourceConnectChangeListener) {
                                l.onSourceConnectChange(SourceInput.values()[source], connectedState);
                            }
                        }
                    }
                    break;

                case SCAN_EVENT_CALLBACK:
                    ScannerEvent scan_ev = new ScannerEvent();
                    readScanEvent(scan_ev, parcel);
                    if (mScannerListener != null)
                        mScannerListener.onEvent(scan_ev);
                    if (mStorDBListener != null)
                        mStorDBListener.StorDBonEvent(scan_ev);
                    break;

                case RRT_EVENT_CALLBACK:
                    if (mRrtListener != null) {
                        int sectionCount = parcel.bodyInt.get(0);
                        Log.i(TAG,"rrt section count = "+sectionCount);
                        RrtEvent curRrtEvent = new RrtEvent();
                        curRrtEvent.readRrtEvent(parcel);
                        curRrtEvent.printRrtEventInfo();
                        mRrtListener.processDetailsChannelAlert(curRrtEvent);
                    } else {
                        Log.i(TAG,"mRrtListener is null");
                    }
                    break;

                case EAS_EVENT_CALLBACK:
                     Log.i(TAG,"get EAS_event_callBack");
                     if (mEasListener != null) {
                        Log.i(TAG,"mEasLister is not null");
                        int sectionCount = parcel.bodyInt.get(0);
                        Log.i(TAG,"eas section count = "+sectionCount);
                        for (int count = 0; count<sectionCount; count++) {
                            EasEvent curEasEvent = new EasEvent();
                            curEasEvent.readEasEvent(parcel);
                            if (easManager.isEasEventNeedProcess(curEasEvent)) {
                                mEasListener.processDetailsChannelAlert(curEasEvent);
                            }
                        }
                     } else {
                        Log.i(TAG,"mEasLister is null");
                     }
                     break;

                case SUBTITLE_UPDATE_CALLBACK:
                    if (mSubtitleListener != null) {
                        mSubtitleListener.onUpdate();
                    }
                    break;
                case VFRAME_BMP_EVENT_CALLBACK:
                    if (mVframBMPListener != null) {
                        VFrameEvent ev = new VFrameEvent();
                        mVframBMPListener.onEvent(ev);
                        ev.FrameNum = parcel.bodyInt.get(0);
                        ev.FrameSize= parcel.bodyInt.get(1);
                        ev.FrameWidth= parcel.bodyInt.get(2);
                        ev.FrameHeight= parcel.bodyInt.get(3);
                    }
                    break;

                case SCANNING_FRAME_STABLE_CALLBACK:
                    if (mScanningFrameStableListener != null) {
                        ScanningFrameStableEvent ev = new ScanningFrameStableEvent();
                        ev.CurScanningFrq = parcel.bodyInt.get(0);
                        mScanningFrameStableListener.onFrameStable(ev);
                    }
                    break;
                case VCHIP_CALLBACK:
                    Log.i(TAG,"atsc ---VCHIP_CALLBACK-----------------");
                    break;
                case EPG_EVENT_CALLBACK:
                    if (mEpgListener != null) {
                        EpgEvent ev = new EpgEvent();
                        ev.type = parcel.bodyInt.get(0);
                        ev.time = parcel.bodyInt.get(1);
                        ev.programID = parcel.bodyInt.get(2);
                        ev.channelID = parcel.bodyInt.get(3);
                        mEpgListener.onEvent(ev);
                    }
                    break;
                case SEARCH_CALLBACK:
                    if (mSigChanSearchListener != null) {
                        if (msgPdu != null) {
                            loop_count = parcel.bodyInt.get(0);
                            for (i = 0; i < loop_count; i++) {
                                msgPdu[i] = parcel.bodyInt.get(i+1);
                            }
                            mSigChanSearchListener.onChannelSearchChange(msgPdu);
                        }
                    }
                    break;
                case SIGNAL_DETECT_CALLBACK:
                    if (!mSigInfoChangeListener.isEmpty()) {
                        TvInSignalInfo sigInfo = new TvInSignalInfo();
                        sigInfo.transFmt = TvInSignalInfo.TransFmt.values()[parcel.bodyInt.get(0)];
                        sigInfo.sigFmt = TvInSignalInfo.SignalFmt.valueOf(parcel.bodyInt.get(1));
                        sigInfo.sigStatus = TvInSignalInfo.SignalStatus.values()[parcel.bodyInt.get(2)];
                        sigInfo.reserved = parcel.bodyInt.get(3);
                        sigInfo.isPiP = parcel.bodyInt.get(4);
                        for (TvInSignalInfo.SigInfoChangeListener l : mSigInfoChangeListener) {
                            l.onSigChange(sigInfo);
                        }
                        Log.e(TAG,"---SIGNAL_DETECT_CALLBACK-----------------");
                    }
                    break;
                case VGA_CALLBACK:
                    break;
                case STATUS_3D_CALLBACK:
                    if (mStatus3DChangeListener != null) {
                        mStatus3DChangeListener.onStatus3DChange(parcel.bodyInt.get(0));
                    }
                    break;
                case HDMIRX_CEC_CALLBACK:
                    if (mHDMIRxCECListener != null) {
                        if (msgPdu != null) {
                            loop_count = parcel.bodyInt.get(0);
                            for (i = 0; i < loop_count; i++) {
                                msgPdu[i] = parcel.bodyInt.get(i+1);
                            }
                            mHDMIRxCECListener.onHDMIRxCECMessage(loop_count, msgPdu);
                        }
                    }
                    break;
                case UPGRADE_FBC_CALLBACK:
                    if (mUpgradeFBCListener != null) {
                        loop_count = parcel.bodyInt.get(0);
                        tmp_val = parcel.bodyInt.get(1);
                        Log.d(TAG, "state = " + loop_count + "    param = " + tmp_val);
                        mUpgradeFBCListener.onUpgradeStatus(loop_count, tmp_val);
                    }
                    break;
                case DREAM_PANEL_CALLBACK:
                    break;
                case ADC_CALIBRATION_CALLBACK:
                    if (mAdcCalibrationListener != null) {
                        mAdcCalibrationListener.onAdcCalibrationChange(parcel.bodyInt.get(0));
                    }
                    break;
                case SOURCE_SWITCH_CALLBACK:
                    if (mSourceSwitchListener != null) {
                        mSourceSwitchListener.onSourceSwitchStatusChange(
                                SourceInput.values()[(parcel.bodyInt.get(0))], (parcel.bodyInt.get(1)));
                    }
                    break;
                case CHANNEL_SELECT_CALLBACK:
                    if (mChannelSelectListener != null) {
                        if (msgPdu != null) {
                            loop_count = parcel.bodyInt.get(0);
                            for (i = 0; i < loop_count; i++) {
                                msgPdu[i] = parcel.bodyInt.get(i+1);
                            }
                            mChannelSelectListener.onChannelSelect(msgPdu);
                        }
                    }
                    break;
                case SERIAL_COMMUNICATION_CALLBACK:
                    if (mSerialCommunicationListener != null) {
                        if (msgPdu != null) {
                            int dev_id = parcel.bodyInt.get(0);
                            loop_count = parcel.bodyInt.get(1);
                            for (i = 0; i < loop_count; i++) {
                                msgPdu[i] = parcel.bodyInt.get(i+2);
                            }
                            mSerialCommunicationListener.onSerialCommunication(dev_id, loop_count, msgPdu);
                        }
                    }
                    break;
                case CLOSE_CAPTION_CALLBACK:
                    if (mCloseCaptionListener != null) {
                        loop_count = parcel.bodyInt.get(0);
                        Log.d(TAG, "cc listener data count =" + loop_count);
                        for (i = 0; i < loop_count; i++) {
                            dataArray[i] = parcel.bodyInt.get(i+1);
                        }
                        //data len write to end
                        dataArray[dataArray.length - 1] = loop_count;
                        loop_count = parcel.bodyInt.get(loop_count+1);
                        for (i = 0; i < loop_count; i++) {
                            cmdArray[i] = parcel.bodyInt.get(i+loop_count+2);
                        }
                        cmdArray[cmdArray.length - 1] =  loop_count;
                        mCloseCaptionListener.onCloseCaptionProcess(dataArray, cmdArray);
                    }
                    break;

                case RECORDER_EVENT_CALLBACK:
                    if (mRecorderEventListener != null) {
                        RecorderEvent ev = new RecorderEvent();
                        ev.Id = parcel.bodyString.get(0);
                        ev.Status = parcel.bodyInt.get(0);
                        ev.Error = parcel.bodyInt.get(1);
                        mRecorderEventListener.onRecoderEvent(ev);
                    }
                    break;
                case AUDIO_EVENT_CALLBACK:
                    Log.i(TAG,"get AUDIO_EVENT_CALLBACK");
                    if (mAudioListener != null) {
                        int cmd = parcel.bodyInt.get(0);
                        int param1 = parcel.bodyInt.get(1);
                        int param2 = parcel.bodyInt.get(2);

                        Log.d(TAG, "tvinput cmd:"+cmd);
                        Log.d(TAG, "tvinput param1:"+param1);
                        Log.d(TAG, "tvinput param2:"+param2);
                        mAudioListener.HandleAudioEvent(cmd, param1, param2);
                    }
                    break;
                case QMS_EVENT_CALLBACK:
                    Log.i(TAG,"get QMS_EVENT_CALLBACK");
                    if (mQmsEventListener != null) {
                        QmsEvent ev = new QmsEvent();
                        ev.qms_en = parcel.bodyInt.get(0);
                        ev.qms_fps = parcel.bodyInt.get(1);
                        ev.qms_base_fps = parcel.bodyInt.get(2);
                        mQmsEventListener.onEvent(ev);
                    }
                 default:
                     Log.e(TAG, "Unknown message type " + msg.what);
                     break;
            }
        }
    }

    public static TvControlManager getInstance() {
        if (null == mInstance) mInstance = new TvControlManager();
        return mInstance;
    }

    public TvControlManager() {
        Looper looper = Looper.myLooper();
        if (looper != null) {
            mEventHandler = new EventHandler(looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            mEventHandler = new EventHandler(looper);
        } else {
            mEventHandler = null;
            Log.e(TAG, "looper is null, so can not do anything");
        }
        mHALCallback = new HALCallback(this);
        mSourceConnectChangeListener = new ArrayList<StatusSourceConnectListener>();
        mSigInfoChangeListener = new ArrayList<TvInSignalInfo.SigInfoChangeListener>();
        //native_setup(new WeakReference<TvControlManager>(this));

        /*
        try {
            boolean ret = IServiceManager.getService()
                    .registerForNotifications("vendor.amlogic.hardware.tvserver@1.0::ITvServer", "", mServiceNotification);
            if (!ret) {
                Log.e(TAG, "Failed to register service start notification");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to register service start notification", e);
        }
        */
        connectToProxy();

        /*String LogFlg = TvMiscConfigGet(OPEN_TV_LOG_FLG, "");
        if ("log_open".equals(LogFlg))
            tvLogFlg =true;
        */
    }

    private static final int TVSERVER_DEATH_COOKIE = 1000;

    // Callback when the UsbPort status is changed by the kernel.
    // Mostly due a command sent by the remote Usb device.
    private HALCallback mHALCallback;

    // Notification object used to listen to the start of the tvserver daemon.
    //private final ServiceNotification mServiceNotification = new ServiceNotification();

    private ITvServer mProxy = null;
    // Mutex for all mutable shared state.
    private final Object mLock = new Object();

    private void connectToProxy() {
        synchronized (mLock) {
            if (mProxy != null) {
                return;
            }

            try {
                mProxy = ITvServer.getService();
                mProxy.linkToDeath(new DeathRecipient(), TVSERVER_DEATH_COOKIE);
                mProxy.setCallback(mHALCallback, ConnectType.TYPE_EXTEND);
            } catch (NoSuchElementException e) {
                Log.e(TAG, "connectToProxy: tvserver HIDL service not found."
                        + " Did the service fail to start?", e);
            } catch (RemoteException e) {
                Log.e(TAG, "connectToProxy: tvserver HIDL service not responding", e);
            }
        }

        Log.i(TAG, "connect to tvserver HIDL service success");
    }

    public String getSupportInputDevices() {
        synchronized (mLock) {
            Mutable<String> resultVal = new Mutable<>();
            try {
                mProxy.getSupportInputDevices((int ret, String v) -> {
                                resultVal.value = v;
                            });
                return resultVal.value;
            } catch (RemoteException e) {
                Log.e(TAG, "getSupportInputDevices:" + e);
            }
        }
        return "";
    }

    public String GetTVSupportCountries() {
        synchronized (mLock) {
            try {
                return mProxy.getTvSupportCountries();
            } catch (RemoteException e) {
                Log.e(TAG, "GetTVSupportCountries:" + e);
            }
        }
        return "";
    }

    public String getTvDefaultCountry() {
        synchronized (mLock) {
            try {
                return mProxy.getTvDefaultCountry();
            } catch (RemoteException e) {
                Log.e(TAG, "getTvDefaultCountry:" + e);
            }
        }
        return "";
    }

    public String GetTvCountryNameById(String country_code) {
        synchronized (mLock) {
            try {
                return mProxy.getTvCountryName(country_code);
            } catch (RemoteException e) {
                Log.e(TAG, "GetTvCountryName:" + e);
            }
        }
        return "";
    }

    public String GetTvSearchMode(String country_code) {
        synchronized (mLock) {
            try {
                return mProxy.getTvSearchMode(country_code);
            } catch (RemoteException e) {
                Log.e(TAG, "GetTvSearchMode:" + e);
            }
        }
        return "";
    }

    public boolean GetTvDtvSupport(String country_code) {
        synchronized (mLock) {
            try {
                return mProxy.getTvDtvSupport(country_code);
            } catch (RemoteException e) {
                Log.e(TAG, "GetTvDtvSupport:" + e);
            }
        }
        return false;
    }

    public String GetTvDtvSystem(String country_code) {
        synchronized (mLock) {
            try {
                return mProxy.getTvDtvSystem(country_code);
            } catch (RemoteException e) {
                Log.e(TAG, "GetTvDtvSystem:" + e);
            }
        }
        return "";
    }

    public boolean GetTvAtvSupport(String country_code) {
        synchronized (mLock) {
            try {
                return mProxy.getTvAtvSupport(country_code);
            } catch (RemoteException e) {
                Log.e(TAG, "GetTvAtvSupport:" + e);
            }
        }
        return false;
    }

    public String GetTvAtvColorSystem(String country_code) {
        synchronized (mLock) {
            try {
                return mProxy.getTvAtvColorSystem(country_code);
            } catch (RemoteException e) {
                Log.e(TAG, "GetTvAtvColorSystem:" + e);
            }
        }
        return "";
    }

    public String GetTvAtvSoundSystem(String country_code) {
        synchronized (mLock) {
            try {
                return mProxy.getTvAtvSoundSystem(country_code);
            } catch (RemoteException e) {
                Log.e(TAG, "GetTvAtvSoundSystem:" + e);
            }
        }
        return "";
    }

    public String GetTvAtvMinMaxFreq(String country_code) {
        synchronized (mLock) {
            try {
                return mProxy.getTvAtvMinMaxFreq(country_code);
            } catch (RemoteException e) {
                Log.e(TAG, "GetTvAtvMinMaxFreq:" + e);
            }
        }
        return "";
    }

    public boolean GetTvAtvStepScan(String country_code) {
        synchronized (mLock) {
            try {
                return mProxy.getTvAtvStepScan(country_code);
            } catch (RemoteException e) {
                Log.e(TAG, "GetTvAtvStepScan:" + e);
            }
        }
        return false;
    }

    public int SetTvCountry(String country) {
        synchronized (mLock) {
            try {
                 mProxy.setTvCountry(country);
                 return 0;
            } catch (RemoteException e) {
                Log.e(TAG, "SetTvCountry:" + e);
            }
        }
        return -1;
    }

    public int SetTvCurrentLanguage(String lang) {
        synchronized (mLock) {
            try {
                 mProxy.setCurrentLanguage(lang);
                 return 0;
            } catch (RemoteException e) {
                Log.e(TAG, "SetTvCurrentLanguage:" + e);
            }
        }
        return -1;
    }

    /*
    public int getCecWakePort() {
        int port = native_GetCecWakePort();
        Log.d(TAG, "getCecWakePort " + port);
        return port;
    }*/

    /**
     * @Function: GetCurrentSignalInfo
     * @Description: Get current signal information
     * @Param:
     * @Return: refer to class tvin_info_t
     */
    public TvInSignalInfo GetCurrentSignalInfo() {
        synchronized (mLock) {
            TvInSignalInfo info = new TvInSignalInfo();
            try {
                SignalInfo hidlInfo = mProxy.getCurSignalInfo();
                info.transFmt = TvInSignalInfo.TransFmt.values()[hidlInfo.transFmt];
                info.sigFmt = TvInSignalInfo.SignalFmt.valueOf(hidlInfo.fmt);
                info.sigStatus = TvInSignalInfo.SignalStatus.values()[hidlInfo.status];
                info.reserved = hidlInfo.frameRate;
                if (isBlockedByChannelLock()) {
                    //means:
                    //1. channel lock config on,
                    //2. channel lock global on
                    //3. channel locked
                    //4. source = atv
                    if (info.sigStatus == TvInSignalInfo.SignalStatus.TVIN_SIG_STATUS_UNSTABLE
                        || info.sigStatus == TvInSignalInfo.SignalStatus.TVIN_SIG_STATUS_NOTSUP
                        || info.sigStatus == TvInSignalInfo.SignalStatus.TVIN_SIG_STATUS_STABLE) {
                        info.sigStatus = TvInSignalInfo.SignalStatus.TVIN_SIG_STATUS_BLOCKED;
                    }
                }
                return info;
            } catch (RemoteException e) {
                Log.e(TAG, "GetCurrentSignalInfo:" + e);
            }
        }
        return null;
    }

    /**
     * @Function: TvMiscConfigSet
     * @Description: Set tv config
     * @Param: key_str tv config name string, value_str tv config set value string
     * @Return: 0 success, -1 fail
     */
    public int TvMiscConfigSet(String key_str, String value_str) {
        synchronized (mLock) {
            try {
                return mProxy.setMiscCfg(key_str, value_str);
            } catch (RemoteException e) {
                Log.e(TAG, "TvMiscConfigSet:" + e);
            }
        }

        return -1;
    }

    /**
     * @Function: TvMiscConfigGet
     * @Description: Get tv config
     * @Param: key_str tv config name string, value_str tv config get value string
     * @Return: 0 success, -1 fail
     */
    public String TvMiscConfigGet(String key_str, String def_str) {
        synchronized (mLock) {
            try {
                return mProxy.getMiscCfg(key_str, def_str);
            } catch (RemoteException e) {
                Log.e(TAG, "TvMiscConfigGet:" + e);
            }
        }
        return "";
    }

    private static class Mutable<E> {
        public E value;

        Mutable() {
            value = null;
        }

        Mutable(E value) {
            this.value = value;
        }
    }

    private static class HALCallback extends ITvServerCallback.Stub {
        TvControlManager tvCtrlMgr;
        HALCallback(TvControlManager tcm) {
            tvCtrlMgr = tcm;
        }

        public void notifyCallback(TvHidlParcel parcel) {
            Log.i(TAG, "notifyCallback msg type:" + parcel.msgType);

            if (tvCtrlMgr.mEventHandler != null) {
                Message msg = tvCtrlMgr.mEventHandler.obtainMessage(parcel.msgType, 0, 0, parcel);
                tvCtrlMgr.mEventHandler.sendMessage(msg);
            }
        }
    }

    final class DeathRecipient implements HwBinder.DeathRecipient {
        DeathRecipient() {
        }

        @Override
        public void serviceDied(long cookie) {
            if (TVSERVER_DEATH_COOKIE == cookie) {
                Log.e(TAG, "tvserver HIDL service died cookie: " + cookie);
                synchronized (mLock) {
                    mProxy = null;
                }
            }
        }
    }

    /*
    final class ServiceNotification extends IServiceNotification.Stub {
        @Override
        public void onRegistration(String fqName, String name, boolean preexisting) {
            Log.i(TAG, "tvserver HIDL service started " + fqName + " " + name);
            connectToProxy();
        }
    }
    */

    protected void finalize() {
        //native_release();
    }

    // when app exit, need release manual
    public final void release() {
        libtv_log_open();
        //native_release();
    }

    // Deprecated, use Channels TYPE_XXXX from TvContract
    public enum dtv_mode_std_e {
        DTV_MODE_STD_AUTO(0),
        DTV_MODE_STD_DTMB(1),
        DTV_MODE_STD_DVB_C(2),
        DTV_MODE_STD_DVB_C2(3),
        DTV_MODE_STD_DVB_T(4),
        DTV_MODE_STD_DVB_T2(5),
        DTV_MODE_STD_DVB_S(6),
        DTV_MODE_STD_DVB_S2(7),
        DTV_MODE_STD_ATSC_T(8),
        DTV_MODE_STD_ATSC_C(9),
        DTV_MODE_STD_ISDB_T(10);

        private int val;

        dtv_mode_std_e(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }

        public static dtv_mode_std_e valueOf(int val) { // int to enum
            switch (val) {
                case 0:
                    return DTV_MODE_STD_AUTO;
                case 1:
                    return DTV_MODE_STD_DTMB;
                case 2:
                    return DTV_MODE_STD_DVB_C;
                case 3:
                    return DTV_MODE_STD_DVB_C2;
                case 4:
                    return DTV_MODE_STD_DVB_T;
                case 5:
                    return DTV_MODE_STD_DVB_T2;
                case 6:
                    return DTV_MODE_STD_DVB_S;
                case 7:
                    return DTV_MODE_STD_DVB_S2;
                case 8:
                    return DTV_MODE_STD_ATSC_T;
                case 9:
                    return DTV_MODE_STD_ATSC_C;
                case 10:
                    return DTV_MODE_STD_ISDB_T;
                default:
                    return null;
            }
        }

        public int value() {
            return this.val;
        }
    }

    // Tv function
    // public int OpenTv();

    /**
     * @Function: CloseTv
     * @Description: Close Tv module
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int CloseTv() {
        return sendCmd(CLOSE_TV);
    }

    /**
     * @Function: StopTv
     * @Description: Stop Tv module
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int StopTv() {
        synchronized (mLock) {
            try {
                return mProxy.stopTv();
            } catch (RemoteException e) {
                Log.e(TAG, "StopTv:" + e);
            }
        }
        return -1;
    }

    public int StartTv() {
        synchronized (mLock) {
            try {
                return mProxy.startTv();
            } catch (RemoteException e) {
                Log.e(TAG, "StartTv:" + e);
            }
        }
        return -1;
    }

    public int GetTvRunStatus() {
        return sendCmd(GET_TV_STATUS);
    }

    /**
     * @Function: GetHotPlugDetect
     * @Description: Get hotplug detect enable status
     * @Param:
     * @Return: true refer to on, and false refers to off
     */
    public boolean GetHotPlugDetectEnableStatus() {
        int ret = sendCmd(HDMIAV_HOTPLUGDETECT_ONOFF);
        return (ret == 1 ? true:false);
    }

    /**
     * @Function: GetLastSourceInput
     * @Description: Get last source input
     * @Param:
     * @Return: refer to enum SourceInput
     */
    public int GetLastSourceInput() {
        return sendCmd(GET_LAST_SOURCE_INPUT);
    }

    /**
     * @Function: GetCurrentSourceInput
     * @Description: Get current source input
     * @Param:
     * @Return: refer to enum SourceInput
     */
    public int GetCurrentSourceInput() {
        synchronized (mLock) {
            try {
                return mProxy.getCurrentSourceInput();
            } catch (RemoteException e) {
                Log.e(TAG, "GetCurrentSourceInput:" + e);
            }
        }
        return -1;
    }

    /**
     * @Function: GetCurrentVirtualSourceInput
     * @Description: Get current virtual source input
     * @Param:
     * @Return: refer to enum SourceInput
     */
    public int GetCurrentVirtualSourceInput() {
        synchronized (mLock) {
            try {
                return mProxy.getCurrentVirtualSourceInput();
            } catch (RemoteException e) {
                Log.e(TAG, "GetCurrentVirtualSourceInput:" + e);
            }
        }
        return -1;
    }

    /**
     * @Function: GetCurrentSourceInputType
     * @Description: Get current source input type
     * @Param:
     * @Return: refer to enum SourceInput_Type
     */
    public SourceInput_Type GetCurrentSourceInputType() {
        libtv_log_open();
        int source_input = GetCurrentSourceInput();
        if (source_input == SourceInput.TV.toInt()) {
            return SourceInput_Type.SOURCE_TYPE_TV;
        } else if (source_input == SourceInput.AV1.toInt() || source_input == SourceInput.AV2.toInt()) {
            return SourceInput_Type.SOURCE_TYPE_AV;
        } else if (source_input == SourceInput.YPBPR1.toInt() || source_input == SourceInput.YPBPR2.toInt()) {
            return SourceInput_Type.SOURCE_TYPE_COMPONENT;
        } else if (source_input == SourceInput.VGA.toInt()) {
            return SourceInput_Type.SOURCE_TYPE_VGA;
        } else if (source_input == SourceInput.HDMI1.toInt() || source_input == SourceInput.HDMI2.toInt() || source_input == SourceInput.HDMI3.toInt()) {
            return SourceInput_Type.SOURCE_TYPE_HDMI;
        } else if (source_input == SourceInput.DTV.toInt()) {
            return SourceInput_Type.SOURCE_TYPE_DTV;
        } else if (source_input == SourceInput.ADTV.toInt()) {
            return SourceInput_Type.SOURCE_TYPE_ADTV;
        } else if (source_input == SourceInput.AUX.toInt()) {
            return SourceInput_Type.SOURCE_TYPE_AUX;
        } else if (source_input == SourceInput.ARC.toInt()) {
            return SourceInput_Type.SOURCE_TYPE_ARC;
        }else {
            return SourceInput_Type.SOURCE_TYPE_MPEG;
        }
    }

    /**
     * @Function: SetSourceInput
     * @Description: Set source input to switch source,
     * @Param: source_input, refer to enum SourceInput; win_pos, refer to class window_pos_t
     * @Return: 0 success, -1 fail
     */
    public int SetSourceInput(SourceInput srcInput) {
        synchronized (mLock) {
            try {
                return mProxy.setSourceInput(srcInput.toInt());
            } catch (RemoteException e) {
                Log.e(TAG, "SetSourceInput:" + e);
            }
        }
        return -1;
    }

    /**
     * @Function: SetSourceInputExt
     * @Description: Set source input to switch source,
     * @Param: source_input, refer to enum SourceInput;
     * @Return: 0 success, -1 fail
     */
    public int SetSourceInput(SourceInput srcInput, SourceInput virtualSrcInput) {
        synchronized (mLock) {
            try {
                return mProxy.setSourceInputExt(srcInput.toInt(), virtualSrcInput.toInt());
            } catch (RemoteException e) {
                Log.e(TAG, "SetSourceInput:" + e);
            }
        }
        return -1;
    }

    /**
     * @Function: IsDviSignal
     * @Description: To check if current signal is dvi signal
     * @Param:
     * @Return: true, false
     */
    public boolean IsDviSignal() {
        synchronized (mLock) {
            try {
                int value = mProxy.isDviSignal();
                Log.d(TAG, "IsDviSignal:" + value);
                 if (value == 1) {
                     return true;
                 } else {
                     return false;
                 }
            } catch (Exception e) {
                Log.e(TAG, "IsDviSignal:" + e);
            }
        }
        return false;
    }

    /**
     * @Function: IsPcFmtTiming
     * @Description: To check if current hdmi signal is pc signal
     * @Param:
     * @Return: true, false
     */
    public boolean IsPcFmtTiming() {
        synchronized (mLock) {
            try {
                 if (mProxy.isVgaTimingInHdmi() == 1) {
                     return true;
                 } else {
                     return false;
                 }
            } catch (RemoteException e) {
                Log.e(TAG, "IsPcFmtTiming:" + e);
            }
        }
        return false;
    }

    /**
     * @Function: SetPreviewWindow
     * @Description: Set source input preview window axis
     * @Param: win_pos, refer to class window_pos_t
     * @Return: 0 success, -1 fail
     */
    public int SetPreviewWindow(int x1, int y1, int x2, int y2) {
        int val[] = new int[]{x1, y1, x2, y2};
        return sendCmdIntArray(SET_PREVIEW_WINDOW, val);
    }

    /**
     * only used for preview.
     * need set preview mode to true when entering into preview window.
     * of course, the mode need revert when exiting from preview window.
     */
    public int SetPreviewWindowMode(boolean enable) {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_PREVIEW_WINDOW_MODE);
        cmd.writeInt(enable ? 1 : 0);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        cmd.recycle();
        r.recycle();
        return ret;
    }

    /**
     * @Function: GetSourceConnectStatus
     * @Description: Get source connect status
     * @Param: source_input, refer to enum SourceInput
     * @Return: 0:plug out 1:plug in
     */
    public int GetSourceConnectStatus(SourceInput srcInput) {
        synchronized (mLock) {
            try {
                return mProxy.getInputSrcConnectStatus(srcInput.toInt());
            } catch (RemoteException e) {
                Log.e(TAG, "GetSourceConnectStatus:" + e);
            }
        }
        return -1;
    }

    public String GetSourceInputList() {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_SOURCE_INPUT_LIST);
        sendCmdToTv(cmd, r);
        String str = r.readString();
        cmd.recycle();
        r.recycle();
        return str;
    }
    // Tv function END

    // HDMI
	/**
	 * @Function: LoadEdidData
	 * @Description: load all edid bin data
	 * @Param: isNeedBlackScreen, Need Black Screen or not
	 * @Param: isDolbyVisionEnable, dolbyvision enable or not
	 * @Return: 0 success, -1 fail
	 */
	public int LoadEdidData(int isNeedBlackScreen, int isDolbyVisionEnable) {
		  synchronized (mLock) {
			try {
				return mProxy.loadEdidData(isNeedBlackScreen, isDolbyVisionEnable);
			} catch (Exception e) {
				Log.e(TAG, "LoadEdidData:" + e);
			}
		}
		return -1;
	}
	/**
	* @Function: UpdateEdidData
	* @Description: load edid bin data for special port
	* @Param: srcInput, the source need update edid
	* @Param: edidData, edid data
	* @Return: 0 success, -1 fail
	*/
    public int UpdateEdidData(SourceInput srcInput, String edidData) {
		 synchronized (mLock) {
		   try {
			   return mProxy.updateEdidData(srcInput.toInt(), edidData);
		   } catch (Exception e) {
			   Log.e(TAG, "UpdateEdidData:" + e);
		   }
	   }
	   return -1;
    }

	/**
     * @Function: SetHdmiEdidVersion
     * @Description: set hdmi edid version to 1.4 or 2.0
     * @Param: port_id is hdmi port id; ver is set version
     * @Return: 0 success, -1 fail
     */
    public int SetHdmiEdidVersion(HdmiPortID port_id, HdmiEdidVer ver) {
          synchronized (mLock) {
            try {
                return mProxy.setHdmiEdidVersion(port_id.toInt(), ver.toInt());
            } catch (RemoteException e) {
                Log.e(TAG, "SetHdmiEdidVersion:" + e);
            }
        }
        return -1;
    }

    /**
     * @Function: GetHdmiEdidVersion
     * @Description: Get hdmi edid version
     * @Param: port_id is hdmi port id
     * @Return: hdmi edid version
     */
    public int GetHdmiEdidVersion(HdmiPortID port_id) {
          synchronized (mLock) {
            try {
                return mProxy.getHdmiEdidVersion(port_id.toInt());
            } catch (RemoteException e) {
                Log.e(TAG, "GetHdmiEdidVersion:" + e);
            }
        }
        return -1;
    }

    /**
     * @Function: SaveHdmiEdidVersion
     * @Description: save hdmi edid version
     * @Param: port_id is hdmi port id
               ver is save version.
     * @Return: 0 success, -1 fail
     */
    public int SaveHdmiEdidVersion(HdmiPortID port_id, HdmiEdidVer ver) {
          synchronized (mLock) {
            try {
                return mProxy.saveHdmiEdidVersion(port_id.toInt(), ver.toInt());
            } catch (RemoteException e) {
                Log.e(TAG, "SaveHdmiEdidVersion:" + e);
            }
        }
        return -1;

    }

    /**
     * @Function: SetDLGEnable
     * @Description: control DLG func open/close
     * @Param: 1:ENABLE,0:DISABLE
     * @Return: 0 success, -1 fail
     */
    public int SetDLGEnable(int isEnable) {
          synchronized (mLock) {
            try {
                return mProxy.Tv_SetDLGEnable(isEnable);
            } catch (Exception e) {
                Log.e(TAG, "SetDLGEnable:" + e);
            }
        }
        return -1;
    }

        /**
     * @Function: GetDLGEnable
     * @Description: get DLG status
     * @Return: 1:enable, 0: disable, -1: get fail
     */
    public int GetDLGEnable() {
          synchronized (mLock) {
            try {
                return mProxy.Tv_GetDLGEnable();
            } catch (Exception e) {
                Log.e(TAG, "SetDLGEnable:" + e);
            }
        }
        return -1;
    }

   /**
    * @Function: IsSupportDLG.
    * @Description: get if support DLG func.
    * @Return: true or false.
    */
   public boolean IsSupportDLG() {
         synchronized (mLock) {
           try {
                if (mProxy.isSupportDLG() == 0) {
                    return true;
                } else {
                    return false;
                }
           } catch (Exception e) {
               Log.e(TAG, "IsSupportDLG:" + e);
           }
       }
       return false;
   }

   /**
     * @Function: SetHdmiHdcpKeyEnable
     * @Description: enable or disable hdmi hdcp kdy
     * @Param: isEnable is enable or disable
     * @Return: 0 success, -1 fail
     */
    public int SetHdmiHdcpKeyEnable(HdcpKeyIsEnable isEnable) {
        int val[] = new int[]{isEnable.toInt()};
        return sendCmdIntArray(SET_HDCP_KEY_ENABLE, val);
    }
    // HDMI END

    /**
     * @Function: SetBacklight_Switch
     * @Description: Set current backlight switch
     * @Param: value onoff
     * @Return: 0 success, -1 fail
     */
    public int SetBacklight_Switch(int onoff) {
        int val[] = new int[]{onoff};
        return sendCmdIntArray(SET_BACKLIGHT_SWITCH, val);
    }

    /**
     * @Function: GetBacklight_Switch
     * @Description: Get current backlight switch
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int GetBacklight_Switch() {
        return sendCmd(GET_BACKLIGHT_SWITCH);
    }

    /**
     * @Function: SetHdmiColorRangeMode
     * @Description: Set hdmi color range mode
     * @Param: color range mode refer to enum HdmiColorRangeMode
     * @Return: 0 success, -1 fail
     */
    public int SetHdmiColorRangeMode(HdmiColorRangeMode mode) {
        synchronized (mLock) {
            try {
                return mProxy.setHdmiColorRangeMode(mode.toInt());
            } catch (RemoteException e) {
                Log.e(TAG, "SetHdmiColorRangeMode:" + e);
            }
        }
        return -1;
    }

    /**
     * @Function: GetHdmiColorRangeMode
     * @Description: Get color range mode for HDMI port
     * @Param:
     * @Return: color range mode refer to enum HdmiColorRangeMode
     */
    public int GetHdmiColorRangeMode() {
        synchronized (mLock) {
            try {
                return mProxy.getHdmiColorRangeMode();
            } catch (RemoteException e) {
                Log.e(TAG, "getHdmiColorRangeMode:" + e);
            }
        }
        return -1;
    }

    public int SetAudioOutmode (int mode) {
        synchronized (mLock) {
            try {
                return mProxy.setAudioOutmode(mode);
            } catch (RemoteException e) {
                Log.e(TAG, "SetAudioOutmode:" + e);
            }
        }
        return -1;
    }

    public int GetAudioOutmode(){
        synchronized (mLock) {
            try {
                return mProxy.getAudioOutmode();
            } catch (RemoteException e) {
                Log.e(TAG, "GetAudioOutmode:" + e);
            }
        }
        return -1;
    }

    public int GetAudioStreamOutmode(){
        synchronized (mLock) {
            try {
                return mProxy.getAudioStreamOutmode();
            } catch (RemoteException e) {
                Log.e(TAG, "GetAudioStreamOutmode:" + e);
            }
        }
        return -1;
    }

    public int GetAtvAutoScanMode() {
        synchronized (mLock) {
            try {
                return mProxy.getAtvAutoScanMode();
            } catch (RemoteException e) {
                Log.e(TAG, "GetAtvAutoScanMode:" + e);
            }
        }
        return -1;
    }

    public static final int AM_AUDIO_TV = 0;
    public static final int AM_AUDIO_AV1 = 1;
    public static final int AM_AUDIO_AV2 = 2;
    public static final int AM_AUDIO_YPBPR1 = 3;
    public static final int AM_AUDIO_YPBPR2 = 4;
    public static final int AM_AUDIO_HDMI1 = 5;
    public static final int AM_AUDIO_HDMI2 = 6;
    public static final int AM_AUDIO_HDMI3 = 7;
    public static final int AM_AUDIO_HDMI4 = 8;
    public static final int AM_AUDIO_VGA = 9;
    public static final int AM_AUDIO_MPEG = 10;
    public static final int AM_AUDIO_DTV = 11;
    public static final int AM_AUDIO_SVIDEO = 12;
    public static final int AM_AUDIO_IPTV = 13;
    public static final int AM_AUDIO_DUMMY = 14;
    public static final int AM_AUDIO_SPDIF = 15;
    public static final int AM_AUDIO_ADTV = 16;

    private void setProperties(String Key, String StrVal) {
        try {
            Class clazz = ClassLoader.getSystemClassLoader().loadClass("android.os.SystemProperties");
            Method method = clazz.getMethod("set", String.class, String.class);
            method.invoke(clazz, Key, StrVal);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public int SetAmAudioVolume(int volume, int source) {
        int val[] = new int[]{volume};
        if (source == AM_AUDIO_MPEG) {
            setProperties("persist.vendor.media.player.volume", String.valueOf(volume));
        }

        return sendCmdIntArray(SET_AMAUDIO_VOLUME, val);
    }

    public int GetAmAudioVolume() {
        return sendCmd(GET_AMAUDIO_VOLUME);
    }

    public int SaveAmAudioVolume(int volume, int source) {
        int val[] = new int[]{volume, source};
        return sendCmdIntArray(SAVE_AMAUDIO_VOLUME, val);
    }

    public int GetSaveAmAudioVolume(int source) {
        int val[] = new int[]{source};
        return sendCmdIntArray(GET_SAVE_AMAUDIO_VOLUME, val);
    }

    // FACTORY
    public enum TEST_PATTERN {
        TEST_PATTERN_NONE(0),
        TEST_PATTERN_RED(1),
        TEST_PATTERN_GREEN(2),
        TEST_PATTERN_BLUE(3),
        TEST_PATTERN_WHITE(4),
        TEST_PATTERN_BLACK(5),
        TEST_PATTERN_MAX(6);

        private int val;

        TEST_PATTERN(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    /**
     * @Function: FactorySetTestPattern
     * @Description: Set test patten for factory menu conctrol
     * @Param: pattern refer to enum TEST_PATTERN
     * @Return: 0 success, -1 fail
     */
    public int FactorySetTestPattern(int pattern) {
        int val[] = new int[]{pattern};
        return sendCmdIntArray(FACTORY_SETTESTPATTERN, val);
    }

    /**
     * @Function: FactoryGetTestPattern
     * @Description: Get current test patten for factory menu conctrol
     * @Param:
     * @Return: patten value refer to enum TEST_PATTERN
     */
    public int FactoryGetTestPattern() {
        return sendCmd(FACTORY_GETTESTPATTERN);
    }

    public int FactoryCleanAllTableForProgram() {
        synchronized (mLock) {
            try {
                return mProxy.FactoryCleanAllTableForProgram();
            } catch (RemoteException e) {
                Log.e(TAG, "FactoryCleanAllTableForProgram:" + e);
            }
        }
        return -1;
        //return sendCmd(FACTORY_CLEAN_ALL_TABLE_FOR_PROGRAM);
    }

    public int FactorySetPatternYUV(int mask, int y, int u, int v) {
        int val[] = new int[]{mask, y, u, v};
        return sendCmdIntArray(FACTORY_SETPATTERN_YUV, val);
    }

    public int FactorySetGammaPattern(int gamma_r, int gamma_g, int gamma_b) {
        int val[] = new int[]{gamma_r, gamma_g, gamma_b};
        return sendCmdIntArray(FACTORY_SET_GAMMA_PATTERN, val);
    }

    // FACTORY END

    // AUDIO
    // Audio macro declare
    public enum Sound_Mode {
        SOUND_MODE_STD(0),
        SOUND_MODE_MUSIC(1),
        SOUND_MODE_NEWS(2),
        SOUND_MODE_THEATER(3),
        SOUND_MODE_GAME(4),
        SOUND_MODE_USER(5);

        private int val;

        Sound_Mode(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum EQ_Mode {
        EQ_MODE_NORMAL(0),
        EQ_MODE_POP(1),
        EQ_MODE_JAZZ(2),
        EQ_MODE_ROCK(3),
        EQ_MODE_CLASSIC(4),
        EQ_MODE_DANCE(5),
        EQ_MODE_PARTY(6),
        EQ_MODE_BASS(7),
        EQ_MODE_TREBLE(8),
        EQ_MODE_CUSTOM(9);

        private int val;

        EQ_Mode(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum CC_AUD_SPDIF_MODE {
        CC_SPDIF_MODE_PCM(0),
        CC_SPDIF_MODE_SOURCE(1);

        private int val;

        CC_AUD_SPDIF_MODE(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    // Audio Mute

    /**
     * @Function: SetAudioMuteForTv
     * @Description: Set audio mute or unmute for TV
     * @Param: KeyStatus refer to enum SET_AUDIO_MUTE_FOR_TV
     * @Return: 0 success, -1 fail
     */
     public int SetAudioMuteForTv(int muteOrUnmute) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_MUTE_FOR_TV);
        cmd.writeInt(muteOrUnmute);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetAudioMuteKeyStatus
     * @Description: Set audio mute or unmute according to mute key press up or press down
     * @Param: KeyStatus refer to enum CC_AUDIO_MUTE_KEY_STATUS
     * @Return: 0 success, -1 fail
     */
    public int SetAudioMuteKeyStatus(int KeyStatus) {
        int val[] = new int[]{KeyStatus};
        return sendCmdIntArray(SET_AUDIO_MUTEKEY_STATUS, val);
    }

    /**
     * @Function: GetAudioMuteKeyStatus
     * @Description: Get audio mute or unmute key
     * @Param:
     * @Return: KeyStatus value refer to enum CC_AUDIO_MUTE_KEY_STATUS
     */
    public int GetAudioMuteKeyStatus() {
        return sendCmd(GET_AUDIO_MUTEKEY_STATUS);
    }

    /**
     * @Function: SetAudioAVoutMute
     * @Description: Set av out mute
     * @Param: AvoutMuteStatus AUDIO_MUTE_ON or AUDIO_MUTE_OFF
     * @Return: 0 success, -1 fail
     */
    public int SetAudioAVoutMute(int AvoutMuteStatus) {
        int val[] = new int[]{AvoutMuteStatus};
        return sendCmdIntArray(SET_AUDIO_AVOUT_MUTE_STATUS, val);
    }

    /**
     * @Function: GetAudioAVoutMute
     * @Description: Get av out mute status
     * @Param:
     * @Return: AUDIO_MUTE_ON or AUDIO_MUTE_OFF
     */
    public int GetAudioAVoutMute() {
        return sendCmd(GET_AUDIO_AVOUT_MUTE_STATUS);
    }

    /**
     * @Function: SetAudioSPDIFMute
     * @Description: Set spdif mute
     * @Param: SPDIFMuteStatus AUDIO_MUTE_ON or AUDIO_MUTE_OFF
     * @Return: 0 success, -1 fail
     */
    public int SetAudioSPDIFMute(int SPDIFMuteStatus) {
        int val[] = new int[]{SPDIFMuteStatus};
        return sendCmdIntArray(SET_AUDIO_SPDIF_MUTE_STATUS, val);
    }

    /**
     * @Function: GetAudioSPDIFMute
     * @Description: Get spdif mute status
     * @Param:
     * @Return: spdif mute status AUDIO_MUTE_ON or AUDIO_MUTE_OFF
     */
    public int GetAudioSPDIFMute() {
        return sendCmd(GET_AUDIO_SPDIF_MUTE_STATUS);
    }

    // Audio Master Volume

    /**
     * @Function: SetAudioMasterVolume
     * @Description: Set audio master volume
     * @Param: value between 0 and 100
     * @Return: 0 success, -1 fail
     */
    public int SetAudioMasterVolume(int tmp_vol) {
        int val[] = new int[]{tmp_vol};
        return sendCmdIntArray(SET_AUDIO_MASTER_VOLUME, val);
    }

    /**
     * @Function: GetAudioMasterVolume
     * @Description: Get audio master volume
     * @Param:
     * @Return: value between 0 and 100
     */
    public int GetSaveAudioMasterVolume() {
        return sendCmd(GET_AUDIO_MASTER_VOLUME);
    }

    /**
     * @Function: SaveCurAudioMasterVolume
     * @Description: Save audio master volume(stored in flash)
     * @Param: value between 0 and 100
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioMasterVolume(int vol) {
        int val[] = new int[]{vol};
        return sendCmdIntArray(SAVE_CUR_AUDIO_MASTER_VOLUME, val);
    }

    /**
     * @Function: GetCurAudioMasterVolume
     * @Description: Get audio master volume(stored in flash)
     * @Param:
     * @Return: value between 0 and 100
     */
    public int GetCurAudioMasterVolume() {
        return sendCmd(GET_CUR_AUDIO_MASTER_VOLUME);
    }

    // Audio Balance

    /**
     * @Function: SetAudioBalance
     * @Description: Set audio balance
     * @Param: value between 0 and 100
     * @Return: 0 success, -1 fail
     */
    public int SetAudioBalance(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(SET_AUDIO_BALANCE, val);
    }

    /**
     * @Function: GetAudioBalance
     * @Description: Get audio balance
     * @Param:
     * @Return: value between 0 and 100
     */
    public int GetSaveAudioBalance() {
        return sendCmd(GET_AUDIO_BALANCE);
    }

    /**
     * @Function: SaveCurAudioBalance
     * @Description: Save audio balance(stored in flash)
     * @Param: value between 0 and 100
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioBalance(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(SAVE_CUR_AUDIO_BALANCE, val);
    }

    /**
     * @Function: GetCurAudioBalance
     * @Description: Get audio balance(stored in flash)
     * @Param:
     * @Return: value between 0 and 100
     */
    public int GetCurAudioBalance() {
        return 0;//wxl add for debug hidl 20180131
        //return sendCmd(GET_CUR_AUDIO_BALANCE);
    }

    // Audio SupperBass Volume

    /**
     * @Function: SetAudioSupperBassVolume
     * @Description: Get audio supperbass volume
     * @Param:
     * @Return: value between 0 and 100
     */
    public int SetAudioSupperBassVolume(int vol) {
        int val[] = new int[]{vol};
        return sendCmdIntArray(SET_AUDIO_SUPPER_BASS_VOLUME, val);
    }

    /**
     * @Function: GetAudioSupperBassVolume
     * @Description: Get audio supperbass volume
     * @Param:
     * @Return: value between 0 and 100
     */
    public int GetSaveAudioSupperBassVolume() {
        return sendCmd(GET_AUDIO_SUPPER_BASS_VOLUME);
    }

    /**
     * @Function: SaveCurAudioSupperBassVolume
     * @Description: Save audio supperbass volume(stored in flash)
     * @Param: value between 0 and 100
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioSupperBassVolume(int vol) {
        int val[] = new int[]{vol};
        return sendCmdIntArray(SAVE_CUR_AUDIO_SUPPER_BASS_VOLUME, val);
    }

    /**
     * @Function: GetCurAudioSupperBassVolume
     * @Description: Get audio supperbass volume(stored in flash)
     * @Param:
     * @Return: value between 0 and 100
     */
    public int GetCurAudioSupperBassVolume() {
        return sendCmd(GET_CUR_AUDIO_SUPPER_BASS_VOLUME);
    }

    // Audio SupperBass Switch

    /**
     * @Function: SetAudioSupperBassSwitch
     * @Description: Set audio supperbass switch
     * @Param: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     * @Return: 0 success, -1 fail
     */
    public int SetAudioSupperBassSwitch(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(SET_AUDIO_SUPPER_BASS_SWITCH, val);
    }

    /**
     * @Function: GetAudioSupperBassSwitch
     * @Description: Get audio supperbass switch
     * @Param:
     * @Return: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     */
    public int GetSaveAudioSupperBassSwitch() {
        return sendCmd(GET_AUDIO_SUPPER_BASS_SWITCH);
    }

    /**
     * @Function: SaveCurAudioSupperBassSwitch
     * @Description: Save audio supperbass switch(stored in flash)
     * @Param: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioSupperBassSwitch(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(SAVE_CUR_AUDIO_SUPPER_BASS_SWITCH, val);
    }

    /**
     * @Function: GetCurAudioSupperBassSwitch
     * @Description: Get audio supperbass switch(stored in flash)
     * @Param:
     * @Return: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     */
    public int GetCurAudioSupperBassSwitch() {
        return sendCmd(GET_CUR_AUDIO_SUPPER_BASS_SWITCH);
    }

    // Audio SRS Surround switch

    /**
     * @Function: SetAudioSrsSurround
     * @Description: Set audio SRS Surround switch
     * @Param: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     * @Return: 0 success, -1 fail
     */
    public int SetAudioSrsSurround(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(SET_AUDIO_SRS_SURROUND, val);
    }

    /**
     * @Function: GetAudioSrsSurround
     * @Description: Get audio SRS Surround switch
     * @Param:
     * @Return: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     */
    public int GetSaveAudioSrsSurround() {
        return sendCmd(GET_AUDIO_SRS_SURROUND);
    }

    /**
     * @Function: SaveCurAudioSrsSurround
     * @Description: Save audio SRS Surround switch(stored in flash)
     * @Param: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioSrsSurround(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(SAVE_CUR_AUDIO_SRS_SURROUND, val);
    }

    /**
     * @Function: GetCurAudioSrsSurround
     * @Description: Get audio SRS Surround switch(stored in flash)
     * @Param:
     * @Return: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     */
    public int GetCurAudioSrsSurround() {
        return 0;//wxl add for debug hidl 20180131
        //return sendCmd(GET_CUR_AUDIO_SRS_SURROUND);
    }

    // Audio SRS Dialog Clarity

    /**
     * @Function: SetAudioSrsDialogClarity
     * @Description: Set audio SRS Dialog Clarity switch
     * @Param: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     * @Return: 0 success, -1 fail
     */
    public int SetAudioSrsDialogClarity(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(SET_AUDIO_SRS_DIALOG_CLARITY, val);
    }

    /**
     * @Function: GetAudioSrsDialogClarity
     * @Description: Get audio SRS Dialog Clarity switch
     * @Param:
     * @Return: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     */
    public int GetSaveAudioSrsDialogClarity() {
        return sendCmd(GET_AUDIO_SRS_DIALOG_CLARITY);
    }

    /**
     * @Function: SaveCurAudioSrsDialogClarity
     * @Description: Save audio SRS Dialog Clarity switch(stored in flash)
     * @Param: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioSrsDialogClarity(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(SAVE_CUR_AUDIO_SRS_DIALOG_CLARITY, val);
    }

    /**
     * @Function: GetCurAudioSrsDialogClarity
     * @Description: Get audio SRS Dialog Clarity switch(stored in flash)
     * @Param:
     * @Return: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     */
    public int GetCurAudioSrsDialogClarity() {
        return 0;//wxl add for debug hidl 20180131
        //return sendCmd(GET_CUR_AUDIO_SRS_DIALOG_CLARITY);
    }

    // Audio SRS Trubass

    /**
     * @Function: SetAudioSrsTruBass
     * @Description: Set audio SRS TruBass switch
     * @Param: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     * @Return: 0 success, -1 fail
     */
    public int SetAudioSrsTruBass(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(SET_AUDIO_SRS_TRU_BASS, val);
    }

    /**
     * @Function: GetAudioSrsTruBass
     * @Description: Get audio SRS TruBass switch
     * @Param:
     * @Return: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     */
    public int GetSaveAudioSrsTruBass() {
        return sendCmd(GET_AUDIO_SRS_TRU_BASS);
    }

    /**
     * @Function: SaveCurAudioSrsTruBass
     * @Description: Save audio SRS TruBass switch(stored in flash)
     * @Param: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioSrsTruBass(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(SAVE_CUR_AUDIO_SRS_TRU_BASS, val);
    }

    /**
     * @Function: GetCurAudioSrsTruBass
     * @Description: Get audio SRS TruBass switch(stored in flash)
     * @Param:
     * @Return: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     */
    public int GetCurAudioSrsTruBass() {
        return 0;//wxl add for debug hidl 20180131
        //return sendCmd(GET_CUR_AUDIO_SRS_TRU_BASS);
    }

    // Audio Bass

    /**
     * @Function: SetAudioBassVolume
     * @Description: Get audio bass volume
     * @Param:
     * @Return: value between 0 and 100
     */
    public int SetAudioBassVolume(int vol) {
        int val[] = new int[]{vol};
        return sendCmdIntArray(SET_AUDIO_BASS_VOLUME, val);
    }

    /**
     * @Function: GetAudioBassVolume
     * @Description: Get audio bass volume
     * @Param:
     * @Return: value between 0 and 100
     */
    public int GetSaveAudioBassVolume() {
        return sendCmd(GET_AUDIO_BASS_VOLUME);
    }

    /**
     * @Function: SaveCurAudioBassVolume
     * @Description: Save audio bass volume(stored in flash)
     * @Param: value between 0 and 100
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioBassVolume(int vol) {
        int val[] = new int[]{vol};
        return sendCmdIntArray(SAVE_CUR_AUDIO_BASS_VOLUME, val);
    }

    /**
     * @Function: GetCurAudioBassVolume
     * @Description: Get audio bass volume(stored in flash)
     * @Param:
     * @Return: value between 0 and 100
     */
    public int GetCurAudioBassVolume() {
        return 0;//wxl add for debug hidl 20180131
        //return sendCmd(GET_CUR_AUDIO_BASS_VOLUME);
    }

    // Audio Treble

    /**
     * @Function: SetAudioTrebleVolume
     * @Description: Get audio Treble volume
     * @Param:
     * @Return: value between 0 and 100
     */
    public int SetAudioTrebleVolume(int vol) {
        int val[] = new int[]{vol};
        return sendCmdIntArray(SET_AUDIO_TREBLE_VOLUME, val);
    }

    /**
     * @Function: GetAudioTrebleVolume
     * @Description: Get audio Treble volume
     * @Param:
     * @Return: value between 0 and 100
     */
    public int GetSaveAudioTrebleVolume() {
        return sendCmd(GET_AUDIO_TREBLE_VOLUME);
    }

    /**
     * @Function: SaveCurAudioTrebleVolume
     * @Description: Save audio Treble volume(stored in flash)
     * @Param: value between 0 and 100
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioTrebleVolume(int vol) {
        int val[] = new int[]{vol};
        return sendCmdIntArray(SAVE_CUR_AUDIO_TREBLE_VOLUME, val);
    }

    /**
     * @Function: GetCurAudioTrebleVolume
     * @Description: Get audio Treble volume(stored in flash)
     * @Param:
     * @Return: value between 0 and 100
     */
    public int GetCurAudioTrebleVolume() {
        return 0;//wxl add for debug hidl 20180131
        //return sendCmd(GET_CUR_AUDIO_TREBLE_VOLUME);
    }

    // Audio Sound Mode

    /**
     * @Function: SetAudioSoundMode
     * @Description: Get audio sound mode
     * @Param:
     * @Return: value refer to enum Sound_Mode
     */
    public int SetAudioSoundMode(Sound_Mode tmp_val) {
        int val[] = new int[]{tmp_val.toInt()};
        return sendCmdIntArray(SET_AUDIO_SOUND_MODE, val);
    }

    public int SetAudioSoundMode(int tmp_val) {
        int val[] = new int[]{tmp_val};
        return sendCmdIntArray(SET_AUDIO_SOUND_MODE, val);
    }

    /**
     * @Function: GetAudioSoundMode
     * @Description: Get audio sound mode
     * @Param:
     * @Return: value refer to enum Sound_Mode
     */
    public int GetSaveAudioSoundMode() {
        return sendCmd(GET_AUDIO_SOUND_MODE);
    }

    /**
     * @Function: SaveCurAudioSoundMode
     * @Description: Save audio sound mode(stored in flash)
     * @Param: value refer to enum Sound_Mode
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioSoundMode(int tmp_val) {
        int val[] = new int[]{tmp_val};
        return sendCmdIntArray(SAVE_CUR_AUDIO_SOUND_MODE, val);
    }

    /**
     * @Function: GetCurAudioSoundMode
     * @Description: Get audio sound mode(stored in flash)
     * @Param:
     * @Return: value refer to enum Sound_Mode
     */
    public int GetCurAudioSoundMode() {
        return 0;//wxl add for debug hidl 20180131
        //return sendCmd(GET_CUR_AUDIO_SOUND_MODE);
    }

    // Audio Wall Effect
    /**
     * @Function: SetAudioWallEffect
     * @Description: Set audio Wall Effect switch
     * @Param: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     * @Return: 0 success, -1 fail
     */
    public int SetAudioWallEffect(int tmp_val) {
        int val[] = new int[]{tmp_val};
        return sendCmdIntArray(SET_AUDIO_WALL_EFFECT, val);
    }

    /**
     * @Function: GetAudioWallEffect
     * @Description: Get audio Wall Effect switch
     * @Param:
     * @Return: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     */
    public int GetSaveAudioWallEffect() {
        return sendCmd(GET_AUDIO_WALL_EFFECT);
    }

    /**
     * @Function: SaveCurAudioWallEffect
     * @Description: Save audio Wall Effect switch(stored in flash)
     * @Param: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioWallEffect(int tmp_val) {
        int val[] = new int[]{tmp_val};
        return sendCmdIntArray(SAVE_CUR_AUDIO_WALL_EFFECT, val);
    }

    /**
     * @Function: GetCurAudioWallEffect
     * @Description: Get audio Wall Effect switch(stored in flash)
     * @Param:
     * @Return: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     */
    public int GetCurAudioWallEffect() {
        return sendCmd(GET_CUR_AUDIO_WALL_EFFECT);
    }

    // Audio EQ Mode
    /**
     * @Function: SetAudioEQMode
     * @Description: Set audio EQ Mode
     * @Param: value refer to enum EQ_Mode
     * @Return: 0 success, -1 fail
     */
    public int SetAudioEQMode(EQ_Mode tmp_val) {
        int val[] = new int[]{tmp_val.toInt()};
        return sendCmdIntArray(SET_AUDIO_EQ_MODE, val);
    }

    /**
     * @Function: GetAudioEQMode
     * @Description: Get audio EQ Mode
     * @Param:
     * @Return: value refer to enum EQ_Mode
     */
    public int GetSaveAudioEQMode() {
        return sendCmd(GET_AUDIO_EQ_MODE);
    }

    /**
     * @Function: SaveCurAudioEQMode
     * @Description: Save audio EQ Mode(stored in flash)
     * @Param: value refer to enum EQ_Mode
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioEQMode(int tmp_val) {
        int val[] = new int[]{tmp_val};
        return sendCmdIntArray(SAVE_CUR_AUDIO_EQ_MODE, val);
    }

    /**
     * @Function: GetCurAudioEQMode
     * @Description: Get audio EQ Mode(stored in flash)
     * @Param:
     * @Return: value refer to enum EQ_Mode
     */
    public int GetCurAudioEQMode() {
        return sendCmd(GET_CUR_AUDIO_EQ_MODE);
    }

    // Audio EQ Gain
    /**
     * @Function: GetAudioEQRange
     * @Description: Get audio EQ Range
     * @Param:
     * @Return: value -128~127
     */
    public int GetAudioEQRange(int range_buf[]) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_EQ_RANGE);
        sendCmdToTv(cmd, r);
        range_buf[0] = r.readInt();
        range_buf[1] = r.readInt();
        int ret = r.readInt();
        cmd.recycle();
        r.recycle();
        return ret;
    }

    /**
     * @Function: GetAudioEQBandCount
     * @Description: Get audio EQ band count
     * @Param:
     * @Return: value 0~255
     */
    public int GetAudioEQBandCount() {
        return sendCmd(GET_AUDIO_EQ_BAND_COUNT);
    }

    /**
     * @Function: SetAudioEQGain
     * @Description: Set audio EQ Gain
     * @Param: value buffer of eq gain. (range --- get by GetAudioEQRange function)
     * @Return: 0 success, -1 fail
     */
    public int SetAudioEQGain(int gain_buf[]) {
        return sendCmdIntArray(SET_AUDIO_EQ_GAIN, gain_buf);
    }

    /**
     * @Function: GetAudioEQGain
     * @Description: Get audio EQ gain
     * @Param: value buffer of eq gain. (range --- get by GetAudioEQRange function)
     * @Return: 0 success, -1 fail
     */
    public int GetAudioEQGain(int gain_buf[]) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_EQ_GAIN);
        sendCmdToTv(cmd, r);

        int size = r.readInt();
        for (int i = 0; i < size; i++) {
            gain_buf[i] = r.readInt();
        }
        int ret = r.readInt();
        cmd.recycle();
        r.recycle();
        return ret;
    }

    /**
     * @Function: SaveCurAudioEQGain
     * @Description: Get audio EQ Gain(stored in flash)
     * @Param: value buffer of eq gain. (range --- get by GetAudioEQRange function)
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioEQGain(int gain_buf[]) {
        return sendCmdIntArray(SAVE_CUR_AUDIO_EQ_GAIN, gain_buf);
    }

    /**
     * @Function: GetCurEQGain
     * @Description: Save audio EQ Gain(stored in flash)
     * @Param: value buffer of eq gain. (range --- get by GetAudioEQRange function)
     * @Return: 0 success, -1 fail
     */
    public int GetCurEQGain(int gain_buf[]) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_CUR_EQ_GAIN);
        sendCmdToTv(cmd, r);

        int size = r.readInt();
        for (int i = 0; i < size; i++) {
            gain_buf[i] = r.readInt();
        }
        int ret = r.readInt();
        cmd.recycle();
        r.recycle();
        return ret;
    }

    /**
     * @Function: SetAudioEQSwitch
     * @Description: Set audio EQ switch
     * @Param: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     * @Return: 0 success, -1 fail
     */
    public int SetAudioEQSwitch(int tmp_val) {
        int val[] = new int[]{tmp_val};
        return sendCmdIntArray(SET_AUDIO_EQ_SWITCH, val);
    }

    //Audio SPDIF Switch
    /**
     * @Function: SetAudioSPDIFSwitch
     * @Description: Set audio SPDIF Switch
     * @Param: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     * @Return: 0 success, -1 fail
     */
    public int SetAudioSPDIFSwitch(int tmp_val) {
        int val[] = new int[]{tmp_val};
        return sendCmdIntArray(SET_AUDIO_SPDIF_SWITCH, val);
    }

    /**
     * @Function: SaveCurAudioSPDIFSwitch
     * @Description: Save audio SPDIF Switch(stored in flash)
     * @Param: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioSPDIFSwitch(int tmp_val) {
        int val[] = new int[]{tmp_val};
        return sendCmdIntArray(SAVE_CUR_AUDIO_SPDIF_SWITCH, val);
    }

    /**
     * @Function: GetCurAudioSPDIFSwitch
     * @Description: Get audio SPDIF Switch(stored in flash)
     * @Param:
     * @Return: value refer to AUDIO_SWITCH_OFF or AUDIO_SWITCH_ON
     */
    public int GetCurAudioSPDIFSwitch() {
        return 0;//wxl add for debug hidl 20180131
        //return sendCmd(GET_CUR_AUDIO_SPDIF_SWITCH);
    }

    //Audio SPDIF Mode
    /**
     * @Function: SetAudioSPDIFMode
     * @Description: Set audio SPDIF Mode
     * @Param: value refer to enum CC_AUD_SPDIF_MODE
     * @Return: 0 success, -1 fail
     */
    public int SetAudioSPDIFMode(int tmp_val) {
        int val[] = new int[]{tmp_val};
        return sendCmdIntArray(SET_AUDIO_SPDIF_MODE, val);
    }

    /**
     * @Function: SaveCurAudioSPDIFMode
     * @Description: Save audio SPDIF Mode(stored in flash)
     * @Param: value refer to enum CC_AUD_SPDIF_MODE
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioSPDIFMode(int tmp_val) {
        int val[] = new int[]{tmp_val};
        return sendCmdIntArray(SAVE_CUR_AUDIO_SPDIF_MODE, val);
    }

    /**
     * @Function: GetCurAudioSPDIFMode
     * @Description: Get audio SPDIF Mode(stored in flash)
     * @Param:
     * @Return: value refer to enum CC_AUD_SPDIF_MODE
     */
    public int GetCurAudioSPDIFMode() {
        return 0;//wxl add for debug hidl 20180131
        //return sendCmd(GET_CUR_AUDIO_SPDIF_MODE);
    }

    /**
     * @Function: SetAmAudioOutputMode
     * @Description: set amaudio output mode
     * @Param: mode, amaudio output mode
     * @Return: 0 success, -1 fail
     */
    public int SetAmAudioOutputMode(int mode) {
        int val[] = new int[]{mode};
        return sendCmdIntArray(SET_AMAUDIO_OUTPUT_MODE, val);
    }

    /**
     * @Function: SetAmAudioMusicGain
     * @Description: set amaudio music gain
     * @Param: gain, gain value
     * @Return: 0 success, -1 fail
     */
    public int SetAmAudioMusicGain(int gain) {
        int val[] = new int[]{gain};
        return sendCmdIntArray(SET_AMAUDIO_MUSIC_GAIN, val);
    }

    /**
     * @Function: SetAmAudioLeftGain
     * @Description: set amaudio left gain
     * @Param: gain, gain value
     * @Return: 0 success, -1 fail
     */
    public int SetAmAudioLeftGain(int gain) {
        int val[] = new int[]{gain};
        return sendCmdIntArray(SET_AMAUDIO_LEFT_GAIN, val);
    }

    /**
     * @Function: SetAmAudioRightGain
     * @Description: set amaudio right gain
     * @Param: gain, gain value
     * @Return: 0 success, -1 fail
     */
    public int SetAmAudioRightGain(int gain) {
        int val[] = new int[]{gain};
        return sendCmdIntArray(SET_AMAUDIO_RIGHT_GAIN, val);
    }

    /**
     * @Function: setAmAudioPreGain
     * @Description: set amaudio pre gain
     * @Param: pre_gain, pre_gain value
     * @Return: 0 success, -1 fail
     */
    public int setAmAudioPreGain(float pre_gain) {
        float val[] = new float[]{pre_gain};
        return sendCmdFloatArray(SET_AMAUDIO_PRE_GAIN, val);
    }

    /**
     * @Function: setAmAudioPreMute
     * @Description: set amaudio pre mute
     * @Param: pre_mute, mute or unmute
     * @Return: 0 success, -1 fail
     */
    public int setAmAudioPreMute(int pre_mute) {
        synchronized (mLock) {
            try {
                return mProxy.setAmAudioPreMute(pre_mute);
            } catch (RemoteException e) {
                Log.e(TAG, "setAmAudioPreMute:" + e);
            }
        }
        return -1;
    }

    /**
     * @Function: getAmAudioPreMute
     * @Description: get amaudio pre gain mute or unmute
     * @Return: mute or unmute
     */
    public int getAmAudioPreMute() {
        return sendCmd(GET_AMAUDIO_PRE_MUTE);
    }

    /**
     * @Function: SetCurProgVolumeCompesition
     * @Description: SET Audio Volume Compesition
     * @Param: 0~10
     * @Return: 0 success, -1 fail
     */
    public int SetCurProgVolumeCompesition(int tmp_val) {
        int val[] = new int[]{tmp_val};
        return sendCmdIntArray(SET_AUDIO_VOL_COMP, val);
    }

    /**
     * @Function: ATVGetVolumeCompesition
     * @Description: Audio Handle Headset PullOut
     * @Param:
     * @Return: 0~10
     */
    public int GetVolumeCompesition() {
        return sendCmd(GET_AUDIO_VOL_COMP);
    }

    /**
     * @Function: SelectLineInChannel
     * @Description: select line in channel
     * @Param: value 0~7
     * @Return: 0 success, -1 fail
     */
    public int SelectLineInChannel(int channel) {
        int val[] = new int[]{channel};
        return sendCmdIntArray(SELECT_LINE_IN_CHANNEL, val);
    }

    /**
     * @Function: SetLineInCaptureVolume
     * @Description: set line in capture volume
     * @Param: left channel volume(0~84)  right channel volume(0~84)
     * @Return: 0 success, -1 fail
     */
    public int SetLineInCaptureVolume(int l_vol, int r_vol) {
        int val[] = new int[]{l_vol, r_vol};
        return sendCmdIntArray(SET_LINE_IN_CAPTURE_VOL, val);
    }

   /**
     * @Function: SetAudioVirtualizer
     * @Description: set audio virtualizer parameters
     * @Param: enable : 1, EffectLevel (0~100)
     * @Return: 0 success, -1 fail
     */
     public int SetAudioVirtualizer(int enable, int EffectLevel) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SET_AUDIO_VIRTUAL);
        cmd.writeInt(enable);
        cmd.writeInt(EffectLevel);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

  /**
    * @Function: GetAudioVirtualizerEnable
    * @Description: get audio virtualizer enable
    * @Return: enable : 1, disable: 0
    */
    public int GetAudioVirtualizerEnable() {
        return 0;//wxl add for debug hidl 20180131
        /*libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_VIRTUAL_ENABLE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;*/
    }

  /**
    * @Function: GetAudioVirtualizerLevel
    * @Description: get audio virtualizer level
    * @Return: level (0~100)
    */
    public int GetAudioVirtualizerLevel() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_VIRTUAL_LEVEL);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }
    // AUDIO END

    // SSM
    /**
     * @Function: SSMInitDevice
     * @Description: Init ssm device
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int SSMInitDevice() {
        synchronized (mLock) {
            try {
                return mProxy.SSMInitDevice();
            } catch (RemoteException e) {
                Log.e(TAG, "SSMInitDevice:" + e);
            }
        }
        return -1;
        //return sendCmd(SSM_INIT_DEVICE);
    }

    /**
     * @Function: SSMWriteOneByte
     * @Description: Write one byte to ssm
     * @Param: offset pos in ssm for this byte, val one byte value
     * @Return: 0 success, -1 fail
     */
    public int SSMWriteOneByte(int offset, int value) {
        int val[] = new int[]{offset, value};
        return sendCmdIntArray(SSM_SAVE_ONE_BYTE, val);
    }

    /**
     * @Function: SSMReadOneByte
     * @Description: Read one byte from ssm
     * @Param: offset pos in ssm for this byte to read
     * @Return: one byte read value
     */
    public int SSMReadOneByte(int offset) {
        int val[] = new int[]{offset};
        return sendCmdIntArray(SSM_READ_ONE_BYTE, val);
    }

    /**
     * @Function: SSMWriteNByte
     * @Description: Write n bytes to ssm
     * @Param: offset pos in ssm for the bytes, data_len how many bytes, data_buf n bytes write buffer
     * @Return: 0 success, -1 fail
     */
    public int SSMWriteNBytes(int offset, int data_len, int data_buf[]) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();

        cmd.writeInt(SSM_SAVE_N_BYTES);
        cmd.writeInt(offset);
        cmd.writeInt(data_len);
        for (int i = 0; i < data_len; i++) {
            cmd.writeInt(data_buf[i]);
        }

        sendCmdToTv(cmd, r);
        int ret = r.readInt();;
        cmd.recycle();
        r.recycle();
        return ret;
    }

    /**
     * @Function: SSMReadNByte
     * @Description: Read one byte from ssm
     * @Param: offset pos in ssm for the bytes, data_len how many bytes, data_buf n bytes read buffer
     * @Return: 0 success, -1 fail
     */
    public int SSMReadNBytes(int offset, int data_len, int data_buf[]) {
        libtv_log_open();
        int i = 0, tmp_data_len = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();

        cmd.writeInt(SSM_READ_N_BYTES);
        cmd.writeInt(offset);
        cmd.writeInt(data_len);

        sendCmdToTv(cmd, r);

        data_len = r.readInt();
        for (i = 0; i < data_len; i++) {
            data_buf[i] = r.readInt();
        }

        ret = r.readInt();
        cmd.recycle();
        r.recycle();
        return ret;
    }

    /**
     * @Function: SSMSavePowerOnOffChannel
     * @Description: Save power on off channel num to ssm for last channel play
     * @Param: channel_type last channel value refer to enum POWERON_SOURCE_TYPE
     * @Return: 0 success, -1 fail
     */
    public int SSMSavePowerOnOffChannel(int channel_type) {
        int val[] = new int[]{channel_type};
        return sendCmdIntArray(SSM_SAVE_POWER_ON_OFF_CHANNEL, val);
    }

    /**
     * @Function: SSMReadPowerOnOffChannel
     * @Description: Read last channel num from ssm
     * @Param:
     * @Return: last channel num
     */
    public int SSMReadPowerOnOffChannel() {
        return sendCmd(SSM_READ_POWER_ON_OFF_CHANNEL);
    }

    /**
     * @Function: SSMSaveSourceInput
     * @Description: Save current source input to ssm for power on last source select
     * @Param: source_input refer to enum SourceInput.
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveSourceInput(int source_input) {
        int val[] = new int[]{source_input};
        return sendCmdIntArray(SSM_SAVE_SOURCE_INPUT, val);
    }

    /**
     * @Function: SSMReadSourceInput
     * @Description: Read last source input from ssm
     * @Param:
     * @Return: source input value refer to enum SourceInput
     */
    public int SSMReadSourceInput() {
        return sendCmd(SSM_READ_SOURCE_INPUT);
    }

    /**
     * @Function: SSMSaveLastSelectSourceInput
     * @Description: Save last source input to ssm for power on last source select
     * @Param: source_input refer to enum SourceInput, if you wanna save as last source input, just set it as SourceInput.DUMMY.
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveLastSelectSourceInput(int source_input) {
        int val[] = new int[]{source_input};
        return sendCmdIntArray(SSM_SAVE_LAST_SOURCE_INPUT, val);
    }

    /**
     * @Function: SSMReadLastSelectSourceInput
     * @Description: Read last source input from ssm
     * @Param:
     * @Return: source input value refer to enum SourceInput
     */
    public int SSMReadLastSelectSourceInput() {
        return sendCmd(SSM_READ_LAST_SOURCE_INPUT);
    }

    /**
     * @Function: SSMSaveSystemLanguage
     * @Description: Save system language
     * @Param: tmp_val language id
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveSystemLanguage(int tmp_val) {
        int val[] = new int[]{tmp_val};
        return sendCmdIntArray(SSM_SAVE_SYS_LANGUAGE, val);
    }

    /**
     * @Function: SSMReadSystemLanguage
     * @Description: Read last source input from ssm
     * @Param:
     * @Return: language id value
     */
    public int SSMReadSystemLanguage() {
        return sendCmd(SSM_READ_SYS_LANGUAGE);
    }

    public int SSMSaveAgingMode(int tmp_val) {
        int val[] = new int[]{tmp_val};
        return sendCmdIntArray(SSM_SAVE_AGING_MODE, val);
    }

    public int SSMReadAgingMode() {
        return sendCmd(SSM_READ_AGING_MODE);
    }

    /**
     * @Function: SSMSavePanelType
     * @Description: Save panel type for multi-panel select
     * @Param: tmp_val panel type id
     * @Return: 0 success, -1 fail
     */
    public int SSMSavePanelType(int tmp_val) {
        int val[] = new int[]{tmp_val};
        return sendCmdIntArray(SSM_SAVE_PANEL_TYPE, val);
    }

    /**
     * @Function: SSMReadPanelType
     * @Description: Read panel type id
     * @Param:
     * @Return: panel type id
     */
    public int SSMReadPanelType() {
        return sendCmd(SSM_READ_PANEL_TYPE);
    }

    /**
     * @Function: SSMSaveMacAddress
     * @Description: Save mac address
     * @Param: data_buf write buffer for mac address
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveMacAddress(int data_buf[]) {
        synchronized (mLock) {
            try {
                return mProxy.saveMacAddress(data_buf);
            } catch (RemoteException e) {
                Log.e(TAG, "SSMSaveMacAddress:" + e);
            }
        }
        return -1;
    }

    /**
     * @Function: SSMReadMacAddress
     * @Description: Read mac address
     * @Param: data_buf read buffer for mac address
     * @Return: 0 success, -1 fail
     */
    public int SSMReadMacAddress(int data_buf[]) {
        synchronized (mLock) {
            try {
                mProxy.readMacAddress((int ret, final int[] v) -> {
                                if (Result.OK == ret) {
                                    for (int i = 0; i < 6; i++)
                                        data_buf[i] = v[i];
                                }
                            });
                return 0;
            } catch (RemoteException e) {
                Log.e(TAG, "SSMReadMacAddress:" + e);
            }
        }
        return -1;
    }

    /**
     * @Function: SSMSaveBarCode
     * @Description: Save bar code
     * @Param: data_buf write buffer for bar code
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveBarCode(int data_buf[]) {
        libtv_log_open();
        int i = 0, tmp_buf_size = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_SAVE_BAR_CODE);

        tmp_buf_size = data_buf.length;
        cmd.writeInt(tmp_buf_size);
        for (i = 0; i < tmp_buf_size; i++) {
            cmd.writeInt(data_buf[i]);
        }

        sendCmdToTv(cmd, r);
        ret = r.readInt();
        cmd.recycle();
        r.recycle();
        return ret;
    }

    /**
     * @Function: SSMReadBarCode
     * @Description: Read bar code
     * @Param: data_buf read buffer for bar code
     * @Return: 0 success, -1 fail
     */
    public int SSMReadBarCode(int data_buf[]) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_READ_BAR_CODE);
        sendCmdToTv(cmd, r);

        int size = r.readInt();
        for (int i = 0; i < size; i++) {
            data_buf[i] = r.readInt();
        }
        int ret = r.readInt();
        cmd.recycle();
        r.recycle();
        return ret;
    }

    /**
     * @Function: SSMSavePowerOnMusicSwitch
     * @Description: Save power on music on/off flag
     * @Param: tmp_val on off flag
     * @Return: 0 success, -1 fail
     */
    public int SSMSavePowerOnMusicSwitch(int tmp_val) {
        int val[] = new int[]{tmp_val};
        return sendCmdIntArray(SSM_SAVE_POWER_ON_MUSIC_SWITCH, val);
    }

    /**
     * @Function: SSMReadPowerOnMusicSwitch
     * @Description: Read power on music on/off flag
     * @Param:
     * @Return: on off flag
     */
    public int SSMReadPowerOnMusicSwitch() {
        return sendCmd(SSM_READ_POWER_ON_MUSIC_SWITCH);
    }

    /**
     * @Function: SSMSavePowerOnMusicVolume
     * @Description: Save power on music volume value
     * @Param: tmp_val volume value
     * @Return: 0 success, -1 fail
     */
    public int SSMSavePowerOnMusicVolume(int tmp_val) {
        int val[] = new int[]{tmp_val};
        return sendCmdIntArray(SSM_SAVE_POWER_ON_MUSIC_VOL, val);
    }

    /**
     * @Function: SSMReadPowerOnMusicVolume
     * @Description: Read power on music volume value
     * @Param:
     * @Return: volume value
     */
    public int SSMReadPowerOnMusicVolume() {
        return sendCmd(SSM_READ_POWER_ON_MUSIC_VOL);
    }

    /**
     * @Function: SSMSaveSystemSleepTimer
     * @Description: Save system sleep timer value
     * @Param: tmp_val sleep timer value
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveSystemSleepTimer(int tmp_val) {
        int val[] = new int[]{tmp_val};
        return sendCmdIntArray(SSM_SAVE_SYS_SLEEP_TIMER, val);
    }

    /**
     * @Function: SSMReadSystemSleepTimer
     * @Description: Read system sleep timer value
     * @Param:
     * @Return: volume value
     */
    public int SSMReadSystemSleepTimer() {
        return sendCmd(SSM_READ_SYS_SLEEP_TIMER);
    }

    /**
     * @Function: SSMSaveInputSourceParentalControl
     * @Description: Save parental control flag to corresponding source input
     * @Param: source_input refer to enum SourceInput, ctl_flag enable or disable this source input
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveInputSourceParentalControl(int source_input, int ctl_flag) {
        int val[] = new int[]{source_input, ctl_flag};
        return sendCmdIntArray(SSM_SAVE_INPUT_SRC_PARENTAL_CTL, val);
    }

    /**
     * @Function: SSMReadInputSourceParentalControl
     * @Description: Read parental control flag of corresponding source input
     * @Param: source_input refer to enum SourceInput
     * @Return: parental control flag
     */
    public int SSMReadInputSourceParentalControl(int source_input) {
        int val[] = new int[]{source_input};
        return sendCmdIntArray(SSM_READ_INPUT_SRC_PARENTAL_CTL, val);
    }

    /**
     * @Function: SSMSaveInputSourceParentalControl
     * @Description: Save parental control on off flag
     * @Param: switch_flag on off flag
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveParentalControlSwitch(int switch_flag) {
        int val[] = new int[]{switch_flag};
        return sendCmdIntArray(SSM_SAVE_PARENTAL_CTL_SWITCH, val);
    }

    /**
     * @Function: SSMReadParentalControlSwitch
     * @Description: Read parental control on off flag
     * @Param:
     * @Return: on off flag
     */
    public int SSMReadParentalControlSwitch() {
        return sendCmd(SSM_READ_PARENTAL_CTL_SWITCH);
    }

    /**
     * @Function: SSMSaveParentalControlPassWord
     * @Description: Save parental control password
     * @Param: pass_wd_str password string
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveParentalControlPassWord(String pass_wd_str) {
        String val[] = new String[]{pass_wd_str};
        return sendCmdStringArray(SSM_SAVE_PARENTAL_CTL_PASS_WORD, val);
    }

    /**
     * @Function: SSMGetCustomerDataStart
     * @Description: Get ssm customer data segment start pos
     * @Param:
     * @Return: start offset pos in ssm data segment
     */
    public int SSMGetCustomerDataStart() {
        return sendCmd(SSM_GET_CUSTOMER_DATA_START);
    }

    /**
     * @Function: SSMGetCustomerDataLen
     * @Description: Get ssm customer data segment length
     * @Param:
     * @Return: length
     */
    public int SSMGetCustomerDataLen() {
        return sendCmd(SSM_GET_CUSTOMER_DATA_LEN);
    }

    /**
     * @Function: SSMSaveStandbyMode
     * @Description: Save standby mode, suspend/resume mode or reboot mode
     * @Param: flag standby mode flag
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveStandbyMode(int flag) {
        int val[] = new int[]{flag};
        return sendCmdIntArray(SSM_SAVE_STANDBY_MODE, val);
    }

    /**
     * @Function: SSMReadStandbyMode
     * @Description: Read standby mode, suspend/resume mode or reboot mode
     * @Param:
     * @Return: standby mode flag
     */
    public int SSMReadStandbyMode() {
        return sendCmd(SSM_READ_STANDBY_MODE);
    }

    /**
     * @Function: SSMSaveLogoOnOffFlag
     * @Description: Save standby logo on off flag
     * @Param: flag on off
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveLogoOnOffFlag(int flag) {
        int val[] = new int[]{flag};
        return sendCmdIntArray(SSM_SAVE_LOGO_ON_OFF_FLAG, val);
    }

    /**
     * @Function: SSMReadStandbyMode
     * @Description: Read standby logo on off flag
     * @Param:
     * @Return: on off flag
     */
    public int SSMReadLogoOnOffFlag() {
        return sendCmd(SSM_READ_LOGO_ON_OFF_FLAG);
    }

    /**
     * @Function: SSMSaveHDMIEQMode
     * @Description: Save hdmi eq mode
     * @Param: flag eq mode
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveHDMIEQMode(int flag) {
        int val[] = new int[]{flag};
        return sendCmdIntArray(SSM_SAVE_HDMIEQ_MODE, val);
    }

    /**
     * @Function: SSMReadHDMIEQMode
     * @Description: Read hdmi eq mode
     * @Param:
     * @Return: hdmi eq mode
     */
    public int SSMReadHDMIEQMode() {
        return sendCmd(SSM_READ_HDMIEQ_MODE);
    }

    /**
     * @Function: SSMSaveHDMIInternalMode
     * @Description: Save hdmi internal mode
     * @Param: flag internal mode
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveHDMIInternalMode(int flag) {
        int val[] = new int[]{flag};
        return sendCmdIntArray(SSM_SAVE_HDMIINTERNAL_MODE, val);
    }

    /**
     * @Function: SSMReadHDMIInternalMode
     * @Description: Read hdmi internal mode
     * @Param:
     * @Return: hdmi internal mode
     */
    public int SSMReadHDMIInternalMode() {
        return sendCmd(SSM_READ_HDMIINTERNAL_MODE);
    }

    /**
     * @Function: SSMSaveGlobalOgoEnable
     * @Description: Save enable global ogo flag
     * @Param: flag enable flag
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveGlobalOgoEnable(int enable) {
        int val[] = new int[]{enable};
        return sendCmdIntArray(SSM_SAVE_GLOBAL_OGOENABLE, val);
    }

    /**
     * @Function: SSMReadGlobalOgoEnable
     * @Description: Read enable global ogo flag
     * @Param:
     * @Return: enable flag
     */
    public int SSMReadGlobalOgoEnable() {
        return sendCmd(SSM_READ_GLOBAL_OGOENABLE);
    }

    /**
     * @Function: SSMSaveAdbSwitchStatus
     * @Description: Save adb debug enable flag
     * @Param: flag enable flag
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveAdbSwitchStatus(int flag) {
        int val[] = new int[]{flag};
        return sendCmdIntArray(SSM_SAVE_ADB_SWITCH_STATUS, val);
    }

    /**
     * @Function: SSMSaveSerialCMDSwitchValue
     * @Description: Save serial cmd switch value
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveSerialCMDSwitchValue(int switch_val) {
        int val[] = new int[]{switch_val};
        return sendCmdIntArray(SSM_SAVE_SERIAL_CMD_SWITCH_STATUS, val);
    }

    /**
     * @Function: SSMReadSerialCMDSwitchValue
     * @Description: Save serial cmd switch value
     * @Param:
     * @Return: enable flag
     */
    public int SSMReadSerialCMDSwitchValue() {
        return sendCmd(SSM_READ_SERIAL_CMD_SWITCH_STATUS);
    }

    /**
     * @Function: SSMSetHDCPKey
     * @Description: Save hdmi hdcp key
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int SSMSetHDCPKey() {
        return sendCmd(SSM_SET_HDCP_KEY);
    }

    /**
     * @Function: SSMRefreshHDCPKey
     * @Description: Refresh hdmi hdcp key after burn
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int SSMRefreshHDCPKey() {
        return sendCmd(SSM_REFRESH_HDCPKEY);
    }

    /**
     * @Function: SSMSaveChromaStatus
     * @Description: Save chroma status
     * @Param: flag chroma status on off
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveChromaStatus(int flag) {
        int val[] = new int[]{flag};
        return sendCmdIntArray(SSM_SAVE_CHROMA_STATUS, val);
    }

    /**
     * @Function: SSMSaveCABufferSize
     * @Description: Save dtv ca buffer size
     * @Param: buffersize ca buffer size
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveCABufferSize(int buffersize) {
        int val[] = new int[]{buffersize};
        return sendCmdIntArray(SSM_SAVE_CA_BUFFER_SIZE, val);
    }

    /**
     * @Function: SSMReadCABufferSize
     * @Description: Read dtv ca buffer size
     * @Param:
     * @Return: size
     */
    public int SSMReadCABufferSize() {
        return sendCmd(SSM_READ_CA_BUFFER_SIZE);
    }

    /**
     * @Function: SSMSaveNoiseGateThreshold
     * @Description: Save noise gate threshold
     * @Param: flag noise gate threshold flag
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveNoiseGateThreshold(int flag) {
        int val[] = new int[]{flag};
        return sendCmdIntArray(SSM_SAVE_NOISE_GATE_THRESHOLD_STATUS, val);
    }

    /**
     * @Function: SSMReadNoiseGateThreshold
     * @Description: Read noise gate threshold flag
     * @Param:
     * @Return: flag
     */
    public int SSMReadNoiseGateThreshold() {
        return sendCmd(SSM_READ_NOISE_GATE_THRESHOLD_STATUS);
    }

    /**
     * @Function: SSMSaveHDCPKeyEnable
     * @Description: save hdmi HDCP key enable or disable
     * @Param: isEnable
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveHDCPKeyEnable(HdcpKeyIsEnable isEnable) {
        int val[] = new int[]{isEnable.toInt()};
        return sendCmdIntArray(SSM_SAVE_HDCP_KEY_ENABLE, val);
    }

        /**
     * @Function: SSMReadHDCPKeyEnable
     * @Description: Read hdmi HDCP key enable or disable
     * @Param:
     * @Return: enable or enable
     */
    public int SSMReadHDCPKeyEnable() {
        return sendCmd(SSM_READ_HDCP_KEY_ENABLE);
    }

    /**
     * @Function: SSMSaveHDCPKey
     * @Description: save hdcp key
     * @Param: data_buf write buffer hdcp key
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveHDCPKey(int data_buf[]) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_SAVE_HDCPKEY);

        int size = data_buf.length;
        cmd.writeInt(size);
        for (int i = 0; i < size; i++) {
            cmd.writeInt(data_buf[i]);
        }

        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        cmd.recycle();
        r.recycle();
        return ret;
    }

    /**
     * @Function: SSMReadHDCPKey
     * @Description: read hdcp key
     * @Param: data_buf read buffer hdcp key
     * @Return: 0 success, -1 fail
     */
    public int SSMReadHDCPKey(int data_buf[]) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_READ_HDCPKEY);
        sendCmdToTv(cmd, r);

        int size = r.readInt();
        for (int i = 0; i < size; i++) {
            data_buf[i] = r.readInt();
        }
        int ret = r.readInt();
        cmd.recycle();
        r.recycle();
        return ret;
    }
    // SSM END

    //MISC

    /**
     * @Function: TvMiscSetGPIOCtrl
     * @Description: Set gpio level
     * @Param: op_cmd_str gpio set cmd string
     * @Return: 0 success, -1 fail
     */
    public int TvMiscSetGPIOCtrl(String op_cmd_str) {
        String val[] = new String[]{op_cmd_str};
        return sendCmdStringArray(MISC_SET_GPIO_CTL, val);
    }

    /**
     * @Function: TvMiscGetGPIOCtrl
     * @Description: Get gpio level
     * @Param: key_str gpio read cmd string, def_str gpio read status string
     * @Return: 0 success, -1 fail
     */
    public int TvMiscGetGPIOCtrl(String key_str, String def_str) {
        return sendCmd(MISC_GET_GPIO_CTL);
    }

    /**
     * @Function: TvMiscSetUserCounter
     * @Description: Enable user counter
     * @Param: counter 1 enable or 0 disable user counter
     * @Return: 0 success, -1 fail
     */
    public int TvMiscSetUserCounter(int counter) {
        int val[] = new int[]{counter};
        return sendCmdIntArray(MISC_SET_WDT_USER_PET, val);
    }

    /**
     * @Function: TvMiscSetUserCounterTimeOut
     * @Description: Set user counter timeout
     * @Param: counter_timer_out time out number
     * @Return: 0 success, -1 fail
     */
    public int TvMiscSetUserCounterTimeOut(int counter_timer_out) {
        int val[] = new int[]{counter_timer_out};
        return sendCmdIntArray(MISC_SET_WDT_USER_COUNTER, val);
    }

    /**
     * @Function: TvMiscSetUserPetResetEnable
     * @Description: Enable or disable user pet reset
     * @Param: enable 1 enable or 0 disable
     * @Return: 0 success, -1 fail
     */
    public int TvMiscSetUserPetResetEnable(int enable) {
        int val[] = new int[]{enable};
        return sendCmdIntArray(MISC_SET_WDT_USER_PET_RESET_ENABLE, val);
    }

    // tv version info
    public static class kernel_ver_info {
        public String linux_ver_info;
        public String build_usr_info;
        public String build_time_info;
    }

    public class tvapi_ver_info {
        public String git_branch_info;
        public String git_commit_info;
        public String last_change_time_info;
        public String build_time_info;
        public String build_usr_info;
    }

    public class dvb_ver_info {
        public String git_branch_info;
        public String git_commit_info;
        public String last_change_time_info;
        public String build_time_info;
        public String build_usr_info;
    }

    public class version_info {
        public String ubootVer;
        public kernel_ver_info kernel_ver;
        public tvapi_ver_info tvapi_ver;
        public dvb_ver_info dvb_ver;
    }

    /**
     * @Function: TvMiscGetKernelVersion
     * @Description: Get kernel version
     * @Param: none
     * @Return: kernel_ver_info
     */
    public kernel_ver_info TvMiscGetKernelVersion() {
        libtv_log_open();
        kernel_ver_info tmpInfo = new kernel_ver_info();
        String info = "";
        InputStream inputStream = null;

        tmpInfo.linux_ver_info = "unknown";
        tmpInfo.build_usr_info = "unknown";
        tmpInfo.build_time_info = "unknown";

        try {
            inputStream = new FileInputStream("/proc/version");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "Regex did not match on /proc/version: " + info);
            return tmpInfo;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(
                    inputStream), 8 * 1024);
        try {
            info = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return tmpInfo;
        }
        finally {
            try {
                reader.close();
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                return tmpInfo;
            }
        }

        final String PROC_VERSION_REGEX =
            "Linux version (\\S+) " + /* group 1: "3.0.31-g6fb96c9" */
            "\\((\\S+?)\\) " +        /* group 2: "x@y.com" (kernel builder) */
            "(?:\\(gcc.+? \\)) " +    /* ignore: GCC version information */
            "([^\\s]+)\\s+" +         /* group 3: "#1" */
            "(?:.*?)?" +              /* ignore: optional SMP, PREEMPT, and any CONFIG_FLAGS */
            "((Sun|Mon|Tue|Wed|Thu|Fri|Sat).+)"; /* group 4: "Thu Jun 28 11:02:39 PDT 2012" */

        Matcher m = Pattern.compile(PROC_VERSION_REGEX).matcher(info);

        if (!m.matches()) {
            Log.e(TAG, "Regex did not match on /proc/version: " + info);
            return tmpInfo;
        } else if (m.groupCount() < 4) {
            Log.e(TAG,
                    "Regex match on /proc/version only returned " + m.groupCount()
                    + " groups");
            return tmpInfo;
        }

        tmpInfo.linux_ver_info = m.group(1);
        tmpInfo.build_usr_info = m.group(2) + " " + m.group(3);
        tmpInfo.build_time_info = m.group(4);

        return tmpInfo;
    }

    /**
     * @Function: TvMiscGetTVAPIVersion
     * @Description: Get TV API version
     * @Param: none
     * @Return: tvapi_ver_info
     */
    public tvapi_ver_info TvMiscGetTVAPIVersion() {
        libtv_log_open();
        tvapi_ver_info tmpInfo = new tvapi_ver_info();

        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(MISC_GET_TV_API_VERSION);
        sendCmdToTv(cmd, r);

        tmpInfo.git_branch_info = r.readString();
        tmpInfo.git_commit_info = r.readString();
        tmpInfo.last_change_time_info = r.readString();
        tmpInfo.build_time_info = r.readString();
        tmpInfo.build_usr_info = r.readString();
        cmd.recycle();
        r.recycle();

        return tmpInfo;
    }

    /**
     * @Function: TvMiscGetDVBAPIVersion
     * @Description: Get DVB API version
     * @Param: none
     * @Return: dvb_ver_info
     */
    public dvb_ver_info TvMiscGetDVBAPIVersion() {
        libtv_log_open();
        dvb_ver_info tmpInfo = new dvb_ver_info();

        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(MISC_GET_DVB_API_VERSION);
        sendCmdToTv(cmd, r);

        tmpInfo.git_branch_info = r.readString();
        tmpInfo.git_commit_info = r.readString();
        tmpInfo.last_change_time_info = r.readString();
        tmpInfo.build_time_info = r.readString();
        tmpInfo.build_usr_info = r.readString();
        cmd.recycle();
        r.recycle();

        return tmpInfo;
    }

    /**
     * @Function: TvMiscGetVersion
     * @Description: Get version
     * @Param: none
     * @Return: version_info
     */
    public version_info TvMiscGetVersion() {
        libtv_log_open();
        version_info tmpInfo = new version_info();

        tmpInfo.ubootVer = "";
        tmpInfo.kernel_ver = TvMiscGetKernelVersion();
        tmpInfo.tvapi_ver = TvMiscGetTVAPIVersion();
        tmpInfo.dvb_ver = TvMiscGetDVBAPIVersion();

        return tmpInfo;
    }

    public enum SerialDeviceID {
        SERIAL_A(0),
        SERIAL_B(1),
        SERIAL_C(2);

        private int val;

        SerialDeviceID(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    /**
     * @Function: SetSerialSwitch
     * @Description: Set special serial switch
     * @Param: dev_id, refer to enum SerialDeviceID
     *         tmp_val, 1 is enable special serial, 0 is disable special serial
     * @Return: 0 success, -1 fail
     */
    public int SetSerialSwitch(SerialDeviceID dev_id, int tmp_val) {
        int val[] = new int[]{dev_id.toInt(), tmp_val};
        return sendCmdIntArray(MISC_SERIAL_SWITCH, val);
    }

    /**
     * @Function: SendSerialData
     * @Description: send serial data
     * @Param: dev_id, refer to enum SerialDeviceID
     *         data_len, the length will be send
     *         data_buf, the data will be send
     * @Return: 0 success, -1 fail
     */
    public int SendSerialData(SerialDeviceID dev_id, int data_len, int data_buf[]) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();

        if (data_len > data_buf.length) {
            return -1;
        }

        cmd.writeInt(MISC_SERIAL_SEND_DATA);

        cmd.writeInt(dev_id.toInt());
        cmd.writeInt(data_len);
        for (int i = 0; i < data_len; i++) {
            cmd.writeInt(data_buf[i]);
        }

        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        cmd.recycle();
        r.recycle();
        return ret;
     }
    //MISC END

    public static class ScanType {
        public static final int SCAN_DTV_AUTO = 0x1;
        public static final int SCAN_DTV_MANUAL = 0x2;
        public static final int SCAN_DTV_ALLBAND = 0x3;
        public static final int SCAN_DTV_NONE = 0x7;

        public static final int SCAN_ATV_AUTO = 0x1;
        public static final int SCAN_ATV_MANUAL = 0x2;
        public static final int SCAN_ATV_FREQ = 0x3;
        public static final int SCAN_ATV_NONE = 0x7;

        public static final int SCAN_ATV_AUTO_FREQ_LIST = 0x0; /* 0: freq table list scan mode */
        public static final int SCAN_ATV_AUTO_ALL_BAND = 0x1;  /* 1: all band scan mode */

        public ScanType() {}
    }

    public static String baseModeToType(int baseMode) {
        String type = "";
        switch (baseMode) {
            case TvChannelParams.MODE_DTMB:
                type = TvContract.Channels.TYPE_DTMB;
                break;
            case TvChannelParams.MODE_QPSK:
                type = TvContract.Channels.TYPE_DVB_S;
                break;
            case TvChannelParams.MODE_QAM:
                type = TvContract.Channels.TYPE_DVB_C;
                break;
            case TvChannelParams.MODE_OFDM:
                type = TvContract.Channels.TYPE_DVB_T;
                break;
            case TvChannelParams.MODE_ATSC:
                type = TvContract.Channels.TYPE_ATSC_C;
                break;
            case TvChannelParams.MODE_ANALOG:
                type = TvContract.Channels.TYPE_PAL;
                break;
            case TvChannelParams.MODE_ISDBT:
                type = TvContract.Channels.TYPE_ISDB_T;
                break;
            default:
                break;
        }
        return type;
    }


    public static class TvMode {
        /*
                mode:0xaabbccdd
                aa - reserved
                bb - scanlist/country etc.
                cc - t/t2 s/s2 c/c2 identifier
                dd - femode FE_XXXX
        */
        private int mMode;
        private String mType;

        public TvMode() {}
        public TvMode(int baseMode) {
            setBase(baseMode);
        }
        public TvMode(TvMode mode) {
            this.mMode = mode.mMode;
        }
        public TvMode(String type) {
            mType = type;
            setBase(typeToBaseMode(mType))
                .setGen(typeToGen(mType))
                .setList(typeToList(mType));
        }
        public static TvMode fromMode(int mode) {
            TvMode m = new TvMode();
            m.mMode = mode;
            return m;
        }
        public int getMode() {
            return mMode;
        }
        public int getBase() {
            return get8(0);
        }
        public int getGen() {
            return get8(1);
        }
        public int getList() {
            return get8(2);
        }
        public int getExt() {
            return get8(3);
        }
        public TvMode setBase(int base) {
            return set8(0, base);
        }
        public TvMode setGen(int gen) {
            return set8(1, gen);
        }
        public TvMode setList(int list) {
            return set8(2, list);
        }
        public TvMode setExt(int ext) {
            return set8(3, ext);
        }

        private TvMode set8(int n, int v) {
            mMode = ((mMode & ~(0xff << (8 * n))) | ((v & 0xff) << (8 * n)));
            return this;
        }
        private int get8(int n) {
            return (mMode >> (8 * n)) & 0xff;
        }

        public String toType() {
            String type = "";
            switch (getBase()) {
                case TvChannelParams.MODE_DTMB:
                    type = TvContract.Channels.TYPE_DTMB;
                    break;
                case TvChannelParams.MODE_QPSK:
                    type = TvContract.Channels.TYPE_DVB_S;
                    if (getGen() == 1)
                        type = TvContract.Channels.TYPE_DVB_S2;
                    break;
                case TvChannelParams.MODE_QAM:
                    type = TvContract.Channels.TYPE_DVB_C;
                    if (getGen() == 1)
                        type = TvContract.Channels.TYPE_DVB_C2;
                    break;
                case TvChannelParams.MODE_OFDM:
                    type = TvContract.Channels.TYPE_DVB_T;
                    if (getGen() == 1)
                        type = TvContract.Channels.TYPE_DVB_T2;
                    break;
                case TvChannelParams.MODE_ATSC:
                    type = TvContract.Channels.TYPE_ATSC_T;
                    if (getGen() == 1)
                        type = TvContract.Channels.TYPE_ATSC_C;
                    break;
                case TvChannelParams.MODE_ANALOG:
                    type = TvContract.Channels.TYPE_PAL;
                    break;
                case TvChannelParams.MODE_ISDBT:
                    type = TvContract.Channels.TYPE_ISDB_T;
                    break;
                default:
                    break;
            }
            return type;
        }
        private int typeToBaseMode(String type) {
            int mode = TvChannelParams.MODE_DTMB;
            if (TextUtils.equals(type, TvContract.Channels.TYPE_DTMB)) {
                mode = TvChannelParams.MODE_DTMB;
            } else if (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_S)
                || (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_S2))) {
                mode = TvChannelParams.MODE_QPSK;
            } else if (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_C)
                || (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_C2))) {
                mode = TvChannelParams.MODE_QAM;
            } else if (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_T)
                || (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_T2))) {
                mode = TvChannelParams.MODE_OFDM;
            } else if (TextUtils.equals(type, TvContract.Channels.TYPE_ATSC_T)
                || (TextUtils.equals(type, TvContract.Channels.TYPE_ATSC_C))) {
                mode = TvChannelParams.MODE_ATSC;
            } else if (TextUtils.equals(type, TvContract.Channels.TYPE_PAL)
                || TextUtils.equals(type, TvContract.Channels.TYPE_NTSC)
                || TextUtils.equals(type, TvContract.Channels.TYPE_SECAM)) {
                mode = TvChannelParams.MODE_ANALOG;
            } else if (TextUtils.equals(type, TvContract.Channels.TYPE_ISDB_T)) {
                mode = TvChannelParams.MODE_ISDBT;
            }
            return mode;
        }

        private int typeToGen(String type) {
            int ext = 0;
            if (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_S2)) {
                ext = 1;
            } else if (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_C2)) {
                ext = 1;
            } else if (TextUtils.equals(type, TvContract.Channels.TYPE_DVB_T2)) {
                ext = 1;
            } else if (TextUtils.equals(type, TvContract.Channels.TYPE_ATSC_C)) {
                ext = 1;
            }
            return ext;
        }

        private int typeToList(String type) {
            int ext = 0;
            if (TextUtils.equals(type, TvContract.Channels.TYPE_ATSC_T)) {
                ext = 0;
            } else if (TextUtils.equals(type, TvContract.Channels.TYPE_ATSC_C)) {
                ext = 1;
            } else if (TextUtils.equals(type, TvContract.Channels.TYPE_ATSC_M_H)) {
                ext = 0;//fix me
            }
            return ext;
        }
    }

    public int DtvScan(int mode, int type, int freq, int para1, int para2) {
        synchronized (mLock) {
            try {
                SetTvCurrentLanguage(TvMultilingualText.getLocalLang());
                return mProxy.dtvScan(mode, type, freq, freq, para1, para2);
            } catch (RemoteException e) {
                Log.e(TAG, "DtvScan:" + e);
            }
        }
        return -1;
    }

    public int DtvAutoScan(int mode) {
        return DtvScan(mode, ScanType.SCAN_DTV_ALLBAND, 0, -1, -1);
    }

    public int DtvManualScan(int mode, int freq, int para1, int para2) {
        return DtvScan(mode, ScanType.SCAN_DTV_MANUAL, freq, para1, para2);
    }

    public int DtvManualScan(int mode, int freq) {
        return DtvManualScan(mode, freq, -1, -1);
    }

    public int DtvAutoScan() {
        return sendCmd(DTV_SCAN_AUTO);
    }

    public int DtvManualScan(int beginFreq, int endFreq, int modulation) {
        int val[] = new int[]{beginFreq, endFreq, modulation};
        return sendCmdIntArray(DTV_SCAN_MANUAL_BETWEEN_FREQ, val);
    }

    public int DtvManualScan(int freq) {
        int val[] = new int[]{freq};
        return sendCmdIntArray(DTV_SCAN_MANUAL, val);
    }

    public int AtvAutoScan(int videoStd, int audioStd) {
        return AtvAutoScan(videoStd, audioStd, 0, 0);
    }

    public int AtvAutoScan(int videoStd, int audioStd, int storeType) {
        return AtvAutoScan(videoStd, audioStd, storeType, 0);
    }

    public int AtvAutoScan(int videoStd, int audioStd, int storeType, int procMode) {
        synchronized (mLock) {
            try {
                SetTvCurrentLanguage(TvMultilingualText.getLocalLang());
                return mProxy.atvAutoScan(videoStd, audioStd, storeType, procMode);
            } catch (RemoteException e) {
                Log.e(TAG, "AtvAutoScan:" + e);
            }
        }
        return -1;
    }

    /**
     * @Function: AtvManualScan
     * @Description: atv manual scan
     * @Param: currentNum:current Channel Number
     * @Param: starFreq:start frequency
     * @Param: endFreq:end frequency
     * @Param: videoStd:scan video standard
     * @Param: audioStd:scan audio standard
     * @Return: 0 ok or -1 error
     */
    public int AtvManualScan(int startFreq, int endFreq, int videoStd,
            int audioStd, int storeType, int currentNum) {
        int val[] = new int[]{startFreq, endFreq, videoStd, audioStd, storeType, currentNum};
        return sendCmdIntArray(ATV_SCAN_MANUAL_BY_NUMBER, val);
    }

    /**
     * @Function: AtvManualScan
     * @Description: atv manual scan
     * @Param: starFreq:start frequency
     * @Param: endFreq:end frequency
     * @Param: videoStd:scan video standard
     * @Param: audioStd:scan audio standard
     * @Return: 0 ok or -1 error
     */
    public int AtvManualScan(int startFreq, int endFreq, int videoStd,
            int audioStd) {
        synchronized (mLock) {
            try {
                SetTvCurrentLanguage(TvMultilingualText.getLocalLang());
                return 0/*mProxy.atvManualScan(startFreq, endFreq, videoStd, audioStd)*/;
            } catch (Exception e) {
                Log.e(TAG, "AtvManualScan:" + e);
            }
        }
        return -1;
    }

    public int AtvDtvPauseScan() {
        synchronized (mLock) {
            try {
                return mProxy.pauseScan();
            } catch (RemoteException e) {
                Log.e(TAG, "AtvDtvPauseScan:" + e);
            }
        }
        return -1;
    }

    public int AtvDtvResumeScan() {
        synchronized (mLock) {
            try {
                return mProxy.resumeScan();
            } catch (RemoteException e) {
                Log.e(TAG, "AtvDtvResumeScan:" + e);
            }
        }
        return -1;
    }

    public int OpenDevForScan(int type) {
        synchronized (mLock) {
            try {
                return mProxy.operateDeviceForScan(type);
            } catch (RemoteException e) {
                Log.e(TAG, "OpenDevForScan:" + e);
            }
        }
        return -1;
    }
    public static final int ATV_DTV_SCAN_STATUS_RUNNING = 0;
    public static final int ATV_DTV_SCAN_STATUS_PAUSED = 1;
    public static final int ATV_DTV_SCAN_STATUS_PAUSED_USER = 2;

    public int AtvDtvGetScanStatus() {
        synchronized (mLock) {
            try {
                return mProxy.atvdtvGetScanStatus();
            } catch (RemoteException e) {
                Log.e(TAG, "AtvDtvGetScanStatus:" + e);
            }
        }
        return -1;
    }

    public int clearFrontEnd(int arg0) {
        int val[] = new int[]{arg0};
        return sendCmdIntArray(TV_CLEAR_FRONTEND, val);
    }

    public int DtvSetTextCoding(String coding) {
        synchronized (mLock) {
            try {
                return mProxy.setDvbTextCoding(coding);
            } catch (RemoteException e) {
                Log.e(TAG, "DtvSetTextCoding:" + e);
            }
        }
        return -1;
    }


    /**
     * @Function: clearAllProgram
     * @Description: clearAllProgram
     * @Param: arg0, not used currently
     * @Return: 0 ok or -1 error
     */
    public int clearAllProgram(int arg0){
        int val[] = new int[]{arg0};
        return sendCmdIntArray(TV_CLEAR_ALL_PROGRAM, val);
    }

     /**
     * @Function: IsAllmInfo
     * @Description: IsAllmInfo status
     * @Param:
     * @Return: true: enable; false: disable;
     */
    public boolean IsAllmInfo() {
        synchronized (mLock) {
            try {
                int value = mProxy.GetAllmInfo();
                Log.d(TAG, "IsAllmInfo:" + value);
                 if (value == 1) {
                     return true;
                 } else {
                     return false;
                 }
            } catch (Exception e) {
                Log.e(TAG, "IsAllmInfo:" + e);
            }
        }
        return false;
    }

    /**
     * @Function: SetVRREnable
     * @Description: SetVRREnable or disable
     * @Param: true: enable; false: disenable
     * @Return: 0 ok or -1 error
     */
    public int SetVRREnable(int enable) {
        synchronized (mLock) {
            try {
                return mProxy.SetVRREnable(enable);
            } catch (Exception e) {
                Log.e(TAG, "SetVRREnable:" + e);
            }
        }
        return -1;
    }

     /**
     * @Function: GetVRREnable
     * @Description: GetVRREnable status
     * @Param:
     * @Return: 1: enable ; 0: disable ; -1: fail
     */
    public int GetVRREnable() {
        synchronized (mLock) {
            try {
                return mProxy.GetVRREnable();
            } catch (Exception e) {
                Log.e(TAG, "GetVRREnable:" + e);
            }
        }
        return -1;
    }

     //VRR mode
     public static final int VDIN_VRR_OFF = 0;
     public static final int VDIN_VRR_BASIC = 1;
     public static final int VDIN_VRR_FREESYNC = 2;
     public static final int VDIN_VRR_FREESYNC_PREMIUM = 3;
     public static final int VDIN_VRR_FREESYNC_PREMIUM_PRO = 4;
     public static final int VDIN_VRR_FREESYNC_PREMIUM_G_SYNC = 5;
     public static final int VDIN_VRR_NUM = 6;

     /**
     * @Function: GetVRRMode
     * @Description: Get VRR Mode status
     * @Param:
     * @Return: refer to  Vrr Mode, -1 means error.
     */
    public int GetVRRMode() {
        synchronized (mLock) {
            try {
                return mProxy.GetVRRMode();
            } catch (Exception e) {
                Log.e(TAG, "GetVRREnable:" + e);
            }
        }
        return -1;
    }

    public String GetVRRModeString() {
        int mode = GetVRRMode();
        String modestring = "";
        switch (mode) {
            case VDIN_VRR_BASIC:
                modestring = "Vrr Basic";
                break;
            case VDIN_VRR_FREESYNC:
                modestring = "FreeSync";
                break;
            case VDIN_VRR_FREESYNC_PREMIUM:
                modestring = "FreeSync Premium";
                break;
            case VDIN_VRR_FREESYNC_PREMIUM_PRO:
                modestring = "FreeSync Premium Pro";
                break;
            case VDIN_VRR_FREESYNC_PREMIUM_G_SYNC:
                modestring = "FreeSync_Premium G Sync";
                break;
            case VDIN_VRR_NUM:
                modestring = "VRR NUM";
                break;
            default:
                modestring = "VRR OFF";
        }
        return modestring;
    }

    //enable: 0  is disable , 1  is enable.      when enable it , can black video for switching program
    public int setBlackoutEnable(int enable, int isSave){
        synchronized (mLock) {
            try {
                return mProxy.setBlackoutEnable(enable, isSave);
            } catch (RemoteException e) {
                Log.e(TAG, "setBlackoutEnable:" + e);
            }
        }
        return -1;
    }

    //ref to setBlackoutEnable fun
    public int getBlackoutEnable() {
        synchronized (mLock) {
            try {
                return mProxy.getBlackoutEnable();
            } catch (RemoteException e) {
                Log.e(TAG, "getBlackoutEnable:" + e);
            }
        }
        return 0;
    }

    public void startAutoBacklight() {
        Log.i(TAG, "interface removed");
    }

    public void stopAutoBacklight() {
        Log.i(TAG, "interface removed");
    }

    /**
     * @return 1:on,0:off
     */
    public int isAutoBackLighting() {
        return sendCmd(IS_AUTO_BACKLIGHTING);
    }

    public int getAverageLut() {
        return sendCmd(GET_AVERAGE_LUMA);
    }

    public int getAutoBacklightData(int data[]) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUTO_BACKLIGHT_DATA);
        sendCmdToTv(cmd, r);

        int size = r.readInt();
        for (int i = 0; i < size; i++) {
            data[i] = r.readInt();
        }
        cmd.recycle();
        r.recycle();

        return size;
    }

    public int setAutoBacklightData(HashMap<String,Integer> map) {
        String data ="opcSwitch:" + map.get("opcSwitch") +
            ",MinBacklight:"+ map.get("MinBacklight") +
            ",Offset:" + map.get("Offset") +
            ",MaxStep:" + map.get("MaxStep") +
            ",MinStep:" + map.get("MinStep");
        String val[] = new String[]{data};
        sendCmdStringArray(SET_AUTO_BACKLIGHT_DATA, val);
        return 0;
    }

    /**
     * @Function: ATVGetChanInfo
     * @Description: Get atv current channel info
     * @Param: dbID,program's in the srv_table of DB
     * @out: dataBuf[0]:freq
     * @out: dataBuf[1]  finefreq
     * @out: dataBuf[2]:video standard
     * @out: dataBuf[3]:audio standard
     * @out: dataBuf[4]:is auto color std? 1, is auto,   0  is not auto
     * @Return: 0 ok or -1 error
     */
    public int ATVGetChanInfo(int dbID, int dataBuf[]) {
        libtv_log_open();
        int tmpRet = -1,tmp_buf_size = 0, i = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();

        cmd.writeInt(ATV_GET_CHANNEL_INFO);
        cmd.writeInt(dbID);
        sendCmdToTv(cmd, r);

        dataBuf[0] = r.readInt();
        dataBuf[1] = r.readInt();
        dataBuf[2] = r.readInt();
        dataBuf[3] = r.readInt();
        dataBuf[4] = r.readInt();

        tmpRet = r.readInt();
        cmd.recycle();
        r.recycle();
        return tmpRet;
    }

    /**
     * @Function: ATVGetVideoCenterFreq
     * @Description: Get atv current channel video center freq
     * @Param: dbID,program's in the srv_table of DB
     * @Return: 0 ok or -1 error
     */
    public int ATVGetVideoCenterFreq(int dbID) {
        int val[] = new int[]{dbID};
        return sendCmdIntArray(ATV_GET_VIDEO_CENTER_FREQ, val);
    }

    /**
     * @Function: ATVGetLastProgramID
     * @Description: ATV Get Last Program's ID
     * @Return: ATV Last Program's ID
     */
    public int ATVGetLastProgramID() {
        return sendCmd(ATV_GET_CURRENT_PROGRAM_ID);
    }

    /**
     * @Function: DTVGetLastProgramID
     * @Description: DTV Get Last Program's ID
     * @Return: DTV Last Program's ID
     */
    public int DTVGetLastProgramID() {
        return sendCmd(DTV_GET_CURRENT_PROGRAM_ID);
    }

    /**
     * @Function: ATVGetMinMaxFreq
     * @Description: ATV Get Min Max Freq
     * @Param:dataBuf[0]:min freq
     * @Param:dataBuf[1]:max freq
     * @Return: 0 or -1
     */
    public int ATVGetMinMaxFreq(int dataBuf[]) {
        synchronized (mLock) {
            Mutable<Integer> minFreqV = new Mutable<>();
            Mutable<Integer> maxFreqV = new Mutable<>();
            Mutable<Integer> retV = new Mutable<>();
            try {
                mProxy.getATVMinMaxFreq((int ret, int minFreq, int maxFreq) -> {
                                    minFreqV.value = minFreq;
                                    maxFreqV.value = maxFreq;
                                    retV.value = ret;
                            });
                dataBuf[0] = minFreqV.value;
                dataBuf[1] = maxFreqV.value;
                return retV.value;
            } catch (RemoteException e) {
                Log.e(TAG, "ATVGetMinMaxFreq:" + e);
            }
        }
        return -1;
    }

    /**
     * @Function: DTVGetScanFreqList
     * @Description: DTVGetScanFreqList
     * @Param:
     * @Return: FreqList
     */
    public ArrayList<FreqList> DTVGetScanFreqList() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_GET_SCAN_FREQUENCY_LIST);
        sendCmdToTv(cmd, r);
        int size = r.readInt();
        int base = 1 ;
        ArrayList<FreqList> FList = new ArrayList<FreqList>();
        FreqList bpl = new FreqList();
        base = r.readInt() - 1;
        bpl.ID = 1 ;
        bpl.freq= r.readInt();
        bpl.channelNum = r.readInt();
        FList.add(bpl);
        for (int i = 1; i < size; i++) {
            FreqList pl = new FreqList();
            pl.ID = r.readInt() - base;
            pl.freq= r.readInt();
            pl.channelNum = r.readInt();
            FList.add(pl);
        }
        cmd.recycle();
        r.recycle();
        return FList;
    }

    public ArrayList<FreqList> DTVGetScanFreqList(int mode) {
        libtv_log_open();
        Log.d(TAG, "TvControlManager TVGetScanFreqList" + mode);
        synchronized (mLock) {
            try {
                ArrayList<FreqList> hidlFreqList = new ArrayList<FreqList>();
                hidlFreqList = mProxy.dtvGetScanFreqListMode(mode);
                int size = hidlFreqList.size();
                if (size <= 0)
                {
                    Log.d(TAG, "hidlFreqList size is 0");
                    return null;
                }
                /*for (int i = 0; i < size; i++) {
                    Log.d(TAG, "get dtv scan freq list hidlFreqList.ID =" +
                    hidlFreqList.get(i).ID + "hidlFreqList.Freq =" +
                    hidlFreqList.get(i).freq + "hidlFreqList.channelNum =" +
                    hidlFreqList.get(i).channelNum);
                }
                */
                return hidlFreqList;
            } catch (RemoteException e) {
                Log.e(TAG, "DTVGetScanFreqList:" + e);
            }
        }

        return null;
        /*
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_GET_SCAN_FREQUENCY_LIST_MODE);
        cmd.writeInt(mode);
        sendCmdToTv(cmd, r);
        int size = r.readInt();
        int base = 1 ;
        ArrayList<FreqList> FList = new ArrayList<FreqList>();
        FreqList bpl = new FreqList();
        base = r.readInt() - 1;
        bpl.ID = 1 ;
        bpl.freq= r.readInt();
        bpl.channelNum = r.readInt();
        FList.add(bpl);
        for (int i = 1; i < size; i++) {
            FreqList pl = new FreqList();
            pl.ID = r.readInt() - base;
            pl.freq= r.readInt();
            pl.channelNum = r.readInt();
            FList.add(pl);
        }
        cmd.recycle();
        r.recycle();
        return FList;
        */
    }

    /**
     * @Function: DTVGetChanInfo
     * @Description: Get dtv current channel info
     * @Param: dbID:program's in the srv_table of DB
     * @Param: dataBuf[0]:freq
     * @Param: dataBuf[1]:strength
     * @Param: dataBuf[2]:snr
     * @Param: dataBuf[2]:ber
     * @Return: 0 ok or -1 error
     */
    public int DTVGetChanInfo(int dbID, int dataBuf[]) {
        libtv_log_open();
        int tmpRet = -1,tmp_buf_size = 0, i = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();

        cmd.writeInt(DTV_GET_CHANNEL_INFO);
        cmd.writeInt(dbID);
        sendCmdToTv(cmd, r);

        dataBuf[0] = r.readInt();
        dataBuf[1] = r.readInt();
        dataBuf[2] = r.readInt();
        dataBuf[3] = r.readInt();

        tmpRet = r.readInt();
        cmd.recycle();
        r.recycle();
        return tmpRet;
    }

    public void setSubtitleUpdateListener(SubtitleUpdateListener l) {
        libtv_log_open();
        mSubtitleListener = l;
    }
    //scanner
    public void setScannerListener(ScannerEventListener l) {
        libtv_log_open();
        mScannerListener = l;
    }

    public void setStorDBListener(StorDBEventListener l) {
        libtv_log_open();
        mStorDBListener = l;
        if (l == null)
            Log.i(TAG,"setStorDBListener null");
    }

    public void setScanningFrameStableListener(ScanningFrameStableListener l) {
        libtv_log_open();
        mScanningFrameStableListener = l;
    }

    public final static int EVENT_SCAN_PROGRESS             = 0;
    public final static int EVENT_STORE_BEGIN               = 1;
    public final static int EVENT_STORE_END                 = 2;
    public final static int EVENT_SCAN_END                  = 3;
    public final static int EVENT_BLINDSCAN_PROGRESS        = 4;
    public final static int EVENT_BLINDSCAN_NEWCHANNEL      = 5;
    public final static int EVENT_BLINDSCAN_END             = 6;
    public final static int EVENT_ATV_PROG_DATA             = 7;
    public final static int EVENT_DTV_PROG_DATA             = 8;
    public final static int EVENT_SCAN_EXIT                 = 9;
    public final static int EVENT_SCAN_BEGIN                = 10;
    public final static int EVENT_LCN_INFO_DATA             = 11;

    public class ScannerEvent {
        public int type;
        public int percent;
        public int totalcount;
        public int lock;
        public int cnum;
        public int freq;
        public String programName;
        public int srvType;
        public String paras;
        public int strength;
        public int quality;

        //for ATV
        public int videoStd;
        public int audioStd;
        public int isAutoStd;
        public int fineTune;

        //for DTV
        public int mode;
        public int sr;
        public int mod;
        public int bandwidth;
        public int reserved;
        public int ts_id;
        public int orig_net_id;

        public int serviceID;
        public int vid;
        public int vfmt;
        public int[] aids;
        public int[] afmts;
        public String[] alangs;
        public int[] atypes;
        public int[] aexts;
        public int pcr;

        public int[] stypes;
        public int[] sids;
        public int[] sstypes;
        public int[] sid1s;
        public int[] sid2s;
        public String[] slangs;

        public int free_ca;
        public int scrambled;

        public int scan_mode;

        public int sdtVersion;

        public int sort_mode;
        public ScannerLcnInfo lcnInfo;

        public int majorChannelNumber;
        public int minorChannelNumber;
        public int sourceId;
        public int accessControlled;
        public int hidden;
        public int hideGuide;
        public String vct;
        public int programs_in_pat;
        public int pat_ts_id;
    }

    public class ScannerLcnInfo {
        public int netId;
        public int tsId;
        public int serviceId;
        public int[] visible;
        public int[] lcn;
        public int[] valid;
    }

    public static class ScanMode {
        private int scanMode;
        public static final int TV_SCAN_DTVMODE_MANUAL = 0x02;

        ScanMode(int ScanMode) {
            scanMode = ScanMode;
        }

        public int getMode() {
            return (scanMode >> 24) & 0xf;
        }

        public int getATVMode() {
            return (scanMode >> 16) & 0xf;
        }

        public int getDTVMode() {
            return (scanMode & 0xFF);
        }

        public boolean isDTVManulScan() {
            return (getDTVMode() == 0x2);
        }

        public boolean isDTVAutoScan() {
            return (getDTVMode() == 0x1);
        }

        public boolean isATVScan() {
            return (getATVMode() != 0x7);
        }

        public boolean isATVManualScan() {
            return (getATVMode() == 0x2);
        }

        public boolean isATVAutoScan() {
            return (getATVMode() == 0x1);
        }
    }

    public static class SortMode {
        private int sortMode;

        SortMode(int SortMode) {
            sortMode = SortMode;
        }
        public int getDTVSortMode() {
            return (sortMode&0xFFFF);
        }

        public boolean isLCNSort() {
            return (getDTVSortMode() == 0x2);
        }
        public int getStandard() {
            return (sortMode>>16)&0xFF;
        }
        public boolean isATSCStandard() {
            return (getStandard() == 0x1);
        }
        public boolean isDVBStandard() {
            return (getStandard() == 0);
        }
    }

    public interface ScannerEventListener {
        void onEvent(ScannerEvent ev);
    }

    public interface StorDBEventListener {
        void StorDBonEvent(ScannerEvent ev);
    }


    // frame stable when scanning
    public class ScanningFrameStableEvent {
        public int CurScanningFrq;
    }

    public interface ScanningFrameStableListener {
        void onFrameStable(ScanningFrameStableEvent ev);
    }

    //epg
    public void setEpgListener(EpgEventListener l) {
        libtv_log_open();
        mEpgListener = l;
    }

    public class EpgEvent {
        public int type;
        public int channelID;
        public int programID;
        public int dvbOrigNetID;
        public int dvbTSID;
        public int dvbServiceID;
        public long time;
        public int dvbVersion;
    }

    public interface EpgEventListener {
        void onEvent(EpgEvent ev);
    }

    //rrt
    public void setRrtListener(RrtEventListener l) {
        mRrtListener = l;
    }

    public interface RrtEventListener {
        void processDetailsChannelAlert(RrtEvent ev);
    }

    public int updateRRTRes(int freq, int modulation, int mode) {
        if (rrt5XmlLoadStatus == EVENT_RRT_SCAN_START) {
            Log.d(TAG, "abandon updateRRTRes,because current status is : " + rrt5XmlLoadStatus);
            return -1;
        } else {
            synchronized (mLock) {
                try {
                    Log.d(TAG, "updateRRTRes,freq: " + freq+",module:"+modulation+",mode:"+mode);
                    return mProxy.updateRRT(freq, modulation, mode);
                } catch (RemoteException e) {
                    Log.e(TAG, "updateRRTRes:" + e);
                }
            }
            return -1;
        }
    }

    public class RrtSearchInfo {
        public String rating_region_name;
        public String dimensions_name;
        public String rating_value_text;
        public int status = 0;
    }

    public RrtSearchInfo SearchRrtInfo(int rating_region_id, int dimension_id, int value_id, int programid) {
        synchronized (mLock) {
            RrtSearchInfo info = new RrtSearchInfo();
            try {
                RRTSearchInfo tempInfo= mProxy.searchRrtInfo(rating_region_id, dimension_id, value_id, programid);
                info.rating_region_name = tempInfo.RatingRegionName;
                info.dimensions_name = tempInfo.DimensionsName;
                info.rating_value_text = tempInfo.RatingValueText;
                info.status = tempInfo.status;
                Log.d(TAG, "programid=" + programid + ", rating_region_name: " + info.dimensions_name);
                Log.d(TAG, "programid=" + programid + ",dimensions_name: " + info.rating_region_name);
                Log.d(TAG, "programid=" + programid + ",rating_value_text: " + info.rating_value_text);
                Log.d(TAG, "programid=" + programid + ",status: " + info.status);

                return info;
            } catch (RemoteException e) {
                Log.e(TAG, "SearchRrtInfo:" + e);
            }
        }
        return null;
    }

    public void setEasListener(EasEventListener l) {
        mEasListener = l;
    }
    public interface EasEventListener {
        void processDetailsChannelAlert(EasEvent ev);
    }

    public class QmsEvent {
        public int qms_en;
        public int qms_fps;//fps
        public int qms_base_fps;//base fps

    }

    public void setQmsListener(QmsEventListener l) {
        mQmsEventListener = l;
    }

    public interface QmsEventListener {
        void onEvent(QmsEvent ev);
    }

    /**
     * @Function: IsSupportQMS.
     * @Description: get if support QMS func.
     * @Return: true or false.
     */
    public boolean IsSupportQMS() {
          boolean ret = false;
          String tmp = request("Get.SupportQms","");
           try {
               JSONObject data = new JSONObject(tmp);
               int callRet = data.optInt("ret", 0);
               if (callRet == 0) {
                   ret = data.optBoolean("support", false);
               }
           } catch (JSONException e) {
           }
           return ret;
    }

    /**
     * @Function: SetQMSEnable
     * @Description: control QMS func open/close
     * @Param: 1:ENABLE,0:DISABLE
     * @Return: 0 success, -1 fail
     */
    public int SetQMSEnable(int isEnable) {
         int ret = 0;
         String tmp = request("Set.TV_SetQMSEnable","{\"enable\":"+ isEnable + "}");
         try {
             JSONObject data = new JSONObject(tmp);
             ret = data.optInt("ret", 0);
         } catch (JSONException e) {
         }
         return ret;
    }

        /**
     * @Function: GetQMSEnable
     * @Description: get QMS status
     * @Return: true:enable, false: disable
     */
    public boolean GetQMSEnable() {
        boolean ret = false;
        String tmp = request("Get.TV_GetQMSEnable","");
         try {
             JSONObject data = new JSONObject(tmp);
             int callRet = data.optInt("ret", 0);
             if (callRet == 0) {
                 ret = data.optBoolean("enabled", false);
             }
         } catch (JSONException e) {
         }
         return ret;
    }

    public class VFrameEvent{
        public int FrameNum;
        public int FrameSize;
        public int FrameWidth;
        public int FrameHeight;
    }

    public interface VframBMPEventListener{
        void onEvent(VFrameEvent ev);
    }

    public void setGetVframBMPListener(VframBMPEventListener l) {
        libtv_log_open();
        mVframBMPListener = l;
    }

    public interface SubtitleUpdateListener {
        void onUpdate();
    }

    public int DtvStopScan() {
        synchronized (mLock) {
            try {
                return mProxy.dtvStopScan();
            } catch (RemoteException e) {
                Log.e(TAG, "DtvStopScan:" + e);
            }
        }
        return -1;
    }

    public int DtvGetSignalSNR() {
        return sendCmd(DTV_GET_SNR);
    }

    public int DtvGetSignalBER() {
        return sendCmd(DTV_GET_BER);
    }

    public int DtvGetSignalStrength() {
        synchronized (mLock) {
            try {
                return mProxy.dtvGetSignalStrength();
            } catch (RemoteException e) {
                Log.e(TAG, "DtvGetSignalStrength:" + e);
            }
        }
        return -1;
        // return sendCmd(DTV_GET_STRENGTH);
    }

    /**
     * @Function: DtvGetAudioTrackNum
     * @Description: Get number audio track of program
     * @Param: [in] prog_id is in db srv table
     * @Return: number audio track
     */
    public int DtvGetAudioTrackNum(int prog_id) {
        int val[] = new int[]{prog_id};
        return sendCmdIntArray(DTV_GET_AUDIO_TRACK_NUM, val);
    }

    /**
     * @Function: DtvGetCurrAudioTrackIndex
     * @Description: Get number audio track of program
     * @Param: [in] prog_id is in db srv table
     * @Return: current audio track index
     */

    public int DtvGetCurrAudioTrackIndex(int prog_id) {
        int val[] = new int[]{prog_id};
        return sendCmdIntArray(DTV_GET_CURR_AUDIO_TRACK_INDEX, val);
    }

    public class DtvAudioTrackInfo {
        public String language;
        public int audio_fmt;
        public int aPid;
    }

    public DtvAudioTrackInfo DtvGetAudioTrackInfo(int prog_id, int audio_ind) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_GET_AUDIO_TRACK_INFO);
        cmd.writeInt(prog_id);
        cmd.writeInt(audio_ind);
        sendCmdToTv(cmd, r);

        DtvAudioTrackInfo tmpRet = new DtvAudioTrackInfo();
        tmpRet.audio_fmt = r.readInt();
        tmpRet.language = r.readString();
        cmd.recycle();
        r.recycle();

        return tmpRet;
    }

    /**
     * @Function: DtvSetAudioChannleMod
     * @Description: set audio channel mod
     * @Param: [in] audioChannelMod is [0 Stereo] [1 left] [2 right ] [3 swap left right]
     * @Return:
     */
    public int DtvSetAudioChannleMod(int audioChannelMod) {
        synchronized (mLock) {
            try {
                return mProxy.dtvSetAudioChannleMod(audioChannelMod);
            } catch (RemoteException e) {
                Log.e(TAG, "DtvSetAudioChannleMod:" + e);
            }
        }
        return 0;
    }

    /**
     * @Function: DtvGetAudioChannleMod
     * @Description: set audio channel mod
     * @Param:
     * @Return: [OUT] audioChannelMod is [0 Stereo] [1 left] [2 right ] [3 swap left right]
     */
    public int DtvGetAudioChannleMod() {
        return sendCmd(DTV_GET_AUDIO_CHANNEL_MOD);
    }

    public int DtvGetFreqByProgId(int progId) {
        int val[] = new int[]{progId};
        return sendCmdIntArray(DTV_GET_FREQ_BY_PROG_ID, val);
    }

    public class EpgInfoEvent {
        public String programName;
        public String programDescription;
        public String programExtDescription;
        public long startTime;
        public long endTime;
        public int subFlag;
        public int evtId;
    }

    public EpgInfoEvent DtvEpgInfoPointInTime(int progId, long iUtcTime) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        EpgInfoEvent epgInfoEvent = new EpgInfoEvent();

        cmd.writeInt(DTV_GET_EPG_INFO_POINT_IN_TIME);
        cmd.writeInt(progId);
        cmd.writeInt((int)iUtcTime);
        sendCmdToTv(cmd, r);
        epgInfoEvent.programName = r.readString();
        epgInfoEvent.programDescription = r.readString();
        epgInfoEvent.programExtDescription = r.readString();
        epgInfoEvent.startTime = r.readInt();
        epgInfoEvent.endTime = r.readInt();
        epgInfoEvent.subFlag = r.readInt();
        epgInfoEvent.evtId =  r.readInt();
        cmd.recycle();
        r.recycle();
        return epgInfoEvent;
    }

    public ArrayList<EpgInfoEvent> GetEpgInfoEventDuration(int progId,long iStartTime,long iDuration) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_GET_EPG_INFO_DURATION);
        cmd.writeInt(progId);
        cmd.writeInt((int)iStartTime);
        cmd.writeInt((int)iDuration);
        sendCmdToTv(cmd, r);
        int size = r.readInt();
        ArrayList<EpgInfoEvent> pEpgInfoList = new ArrayList<EpgInfoEvent>();
        for (int i = 0; i < size; i++) {
            EpgInfoEvent pl = new EpgInfoEvent();
            pl.programName = r.readString();
            pl.programDescription = r.readString();
            pl.programExtDescription = r.readString();
            pl.startTime = r.readInt();
            pl.endTime = r.readInt();
            pl.subFlag = r.readInt();
            pl.evtId =  r.readInt();
            pEpgInfoList.add(pl);
        }
        cmd.recycle();
        r.recycle();
        return pEpgInfoList;
    }

    public int DtvSwitchAudioTrack(int audio_pid, int audio_format, int audio_param) {
        synchronized (mLock) {
            try {
                return mProxy.DtvSwitchAudioTrack3(audio_pid, audio_format,audio_param);
            } catch (RemoteException e) {
                Log.e(TAG, "DtvSwitchAudioTrack:" + e);
            }
        }
        return -1;
    }

    public int DtvSwitchAudioTrack(int prog_id, int audio_track_id) {
        synchronized (mLock) {
            try {
                return mProxy.DtvSwitchAudioTrack(prog_id, audio_track_id);
            } catch (RemoteException e) {
                Log.e(TAG, "DtvSwitchAudioTrack:" + e);
            }
        }
        return -1;
    }

    public int DtvSetAudioAD(int enable, int audio_pid, int audio_format) {
        synchronized (mLock) {
            try {
                return mProxy.DtvSetAudioAD(enable, audio_pid, audio_format);
            } catch (RemoteException e) {
                Log.e(TAG, "DtvSetAudioAD:" + e);
            }
        }
        return -1;
    }

    public long DtvGetEpgUtcTime() {
        return sendCmd(DTV_GET_EPG_UTC_TIME);
    }

    public class VideoFormatInfo {
        public int width;
        public int height;
        public int fps;
        public int interlace;
    }

    public VideoFormatInfo DtvGetVideoFormatInfo() {
        synchronized (mLock) {
            VideoFormatInfo pVideoFormatInfo = new VideoFormatInfo();
            try {
                FormatInfo  formatInfo= mProxy.dtvGetVideoFormatInfo();
                pVideoFormatInfo.width = formatInfo.width;
                pVideoFormatInfo.height= formatInfo.height;
                pVideoFormatInfo.fps= formatInfo.fps;
                pVideoFormatInfo.interlace= formatInfo.interlace;
                return pVideoFormatInfo;
            } catch (RemoteException e) {
                Log.e(TAG, "DtvGetVideoFormatInfo:" + e);
            }
        }
        return null;
    }

    public class AudioFormatInfo {
        public int Format;
        public int SampleRate;
        public int Resolution;
        public int Channels;
        public int LFEPresent;
        public int FormatOriginal;
        public int SampleRateOriginal;
        public int ResolutionOriginal;
        public int ChannelsOriginal;
        public int LFEPresentOriginal;
        public int Frames;
        public int ABSize;
        public int ABData;
        public int ABFree;
    }

    public AudioFormatInfo DtvGetAudioFormatInfo() {
        libtv_log_open();
        AudioFormatInfo pAudioFormatInfo = new AudioFormatInfo();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();

        cmd.writeInt(DTV_GET_AUDIO_FMT_INFO);
        sendCmdToTv(cmd, r);
        pAudioFormatInfo.Format = r.readInt();
        pAudioFormatInfo.FormatOriginal = r.readInt();
        pAudioFormatInfo.SampleRate = r.readInt();
        pAudioFormatInfo.SampleRateOriginal = r.readInt();
        pAudioFormatInfo.Resolution = r.readInt();
        pAudioFormatInfo.ResolutionOriginal = r.readInt();
        pAudioFormatInfo.Channels = r.readInt();
        pAudioFormatInfo.ChannelsOriginal = r.readInt();
        pAudioFormatInfo.LFEPresent = r.readInt();
        pAudioFormatInfo.LFEPresentOriginal = r.readInt();
        pAudioFormatInfo.Frames = r.readInt();
        pAudioFormatInfo.ABSize = r.readInt();
        pAudioFormatInfo.ABData = r.readInt();
        pAudioFormatInfo.ABFree = r.readInt();
        cmd.recycle();
        r.recycle();
        return pAudioFormatInfo;
    }

    public class BookEventInfo {
        public String programName;
        public String envName;
        public long startTime;
        public long durationTime;
        public int bookId;
        public int progId;
        public int evtId;
    }

    public ArrayList<BookEventInfo> getBookedEvent() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_GET_BOOKED_EVENT);
        sendCmdToTv(cmd, r);

        int size = r.readInt();
        ArrayList<BookEventInfo> pBookEventInfoList = new ArrayList<BookEventInfo>();
        for (int i = 0; i < size; i++) {
            BookEventInfo pl = new BookEventInfo();
            pl.programName = r.readString();
            pl.envName = r.readString();
            pl.startTime = r.readInt();
            pl.durationTime = r.readInt();
            pl.bookId = r.readInt();
            pl.progId = r.readInt();
            pl.evtId = r.readInt();
            pBookEventInfoList.add(pl);
        }
        cmd.recycle();
        r.recycle();
        return pBookEventInfoList;
    }

    public int setProgramName(int id, String name) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_SET_PROGRAM_NAME);
        cmd.writeInt(id);
        cmd.writeString(name);
        sendCmdToTv(cmd, r);
        cmd.recycle();
        r.recycle();
        return 0;
    }

    public int setProgramSkipped(int id, int skipped) {
        int val[] = new int[]{id, skipped};
        sendCmdIntArray(DTV_SET_PROGRAM_SKIPPED, val);
        return 0;
    }

    public int setProgramFavorite(int id, int favorite) {
        int val[] = new int[]{id, favorite};
        sendCmdIntArray(DTV_SET_PROGRAM_FAVORITE, val);
        return 0;
    }

    public int deleteProgram(int id) {
        int val[] = new int[]{id};
        sendCmdIntArray(DTV_DELETE_PROGRAM, val);
        return 0;
    }

    public int swapProgram(int first_id, int second_id) {
        int val[] = new int[]{first_id, second_id};
        sendCmdIntArray(DTV_SWAP_PROGRAM, val);
        return 0;
    }

    public int setProgramLocked (int id, int locked) {
        int val[] = new int[]{id, locked};
        sendCmdIntArray(DTV_SET_PROGRAM_LOCKED, val);
        return 0;
    }

    public int PlayATVProgram(int freq, int videoStd, int audioStd, int videoFmt, int soundsys, int fineTune, int audioCompetition) {
        int val[] = new int[]{4, freq, videoStd, audioStd, videoFmt, soundsys, fineTune, audioCompetition};
        return sendCmdIntArray(PLAY_PROGRAM, val);
    }

    public int PlayDTVProgram(int mode, int freq, int para1, int para2, int vid, int vfmt, int aid, int afmt, int pcr, int audioCompetition) {
        int val[] = new int[]{mode, freq, para1, para2, vid, vfmt, aid, afmt, pcr, audioCompetition};
        return sendCmdIntArray(PLAY_PROGRAM, val);
    }

    public int PlayDTVProgram(int mode, int freq, int para1, int para2, int vid, int vfmt, int aid, int afmt, int pcr, int audioCompetition, boolean adPrepare) {
        setProperties("media.audio.enable_asso", (adPrepare)? "1" : "0");
        return PlayDTVProgram(mode, freq, para1, para2, vid, vfmt, aid, afmt, pcr, audioCompetition);
    }

    public int PlayDTVProgram(int mode, int freq, int para1, int para2, int vid, int vfmt, int aid, int afmt, int pcr, int audioCompetition, boolean adPrepare, int adMixingLevel) {
        setProperties("media.audio.mix_asso", String.valueOf(adMixingLevel));
        return PlayDTVProgram(mode, freq, para1, para2, vid, vfmt, aid, afmt, pcr, audioCompetition, adPrepare);
    }

    public static class Paras {
        private Map<String, String> mParas;

        public Paras() {
            mParas = new HashMap<String, String>();
        }
        public Paras(String paras) {
            mParas = DroidLogicTvUtils.jsonToMap(paras);
        }
        public String getString(String key) {
            return mParas.get(key);
        }
        public int getInt(String key, int def) {
            try {
                int v = Integer.parseInt(mParas.get(key));
                return v;
            } catch (Exception e) {
                return def;
            }
        }
        public void set(String key, String str) {
            mParas.put(key, str);
        }
        public void set(String key, int i) {
            mParas.put(key, String.valueOf(i));
        }

        public String toString(String name) {
            return DroidLogicTvUtils.mapToJson(name, mParas);
        }
        public String toString() {
            return toString(null);
        }
    }

    public static class FEParas extends Paras {
        public static final String K_MODE = "mode";
        public static final String K_FREQ = "freq";
        public static final String K_FREQ2 = "freq2";
        public static final String K_BW = "bw";
        public static final String K_SR = "sr";
        public static final String K_MOD = "mod";
        public static final String K_PLP = "plp";
        public static final String K_LAYR = "layr";
        public static final String K_VSTD = "vtd";
        public static final String K_ASTD = "atd";
        public static final String K_AFC = "afc";
        public static final String K_VFMT = "vfmt";
        public static final String K_SOUNDSYS = "soundsys";

        public FEParas() { super(); }
        public FEParas(String paras) { super(paras); }

        public String toNamedString() {
            return toString("fe");
        }

        public TvMode getMode() {
            return TvMode.fromMode(getInt(K_MODE, 0));
        }
        public int getFrequency() {
            return getInt(K_FREQ, 0);
        }
        public int getFrequency2() {
            return getInt(K_FREQ2, 0);
        }
        public int getBandwidth() {
            return getInt(K_BW, 0);
        }
        public int getSymbolrate() {
            return getInt(K_SR, 0);
        }
        public int getModulation() {
            return getInt(K_MOD, 0);
        }
        public int getPlp() {
            return getInt(K_PLP, 0);
        }
        public int getLayer() {
            return getInt(K_LAYR, 0);
        }
        public int getVideoStd() {
            return getInt(K_VSTD, 0);
        }
        public int getAudioStd() {
            return getInt(K_ASTD, 0);
        }
        public int getAfc() {
            return getInt(K_AFC, 0);
        }
        public int getVfmt() {
            return getInt(K_VFMT, 0);
        }
        public int getAudioOutPutMode() {
            return getInt(K_SOUNDSYS, -1);
        }
        public FEParas setMode(TvMode mode) {
            set(K_MODE, mode.getMode());
            return this;
        }
        public FEParas setFrequency(int frequency) {
            set(K_FREQ, frequency);
            return this;
        }
        public FEParas setFrequency2(int frequency) {
            set(K_FREQ2, frequency);
            return this;
        }
        public FEParas setBandwidth(int bandwidth) {
            set(K_BW, bandwidth);
            return this;
        }
        public FEParas setSymbolrate(int symbolrate) {
            set(K_SR, symbolrate);
            return this;
        }
        public FEParas setModulation(int modulation) {
            set(K_MOD, modulation);
            return this;
        }
        public FEParas setPlp(int plp) {
            set(K_PLP, plp);
            return this;
        }
        public FEParas setLayer(int layer) {
            set(K_LAYR, layer);
            return this;
        }
        public FEParas setVideoStd(int std) {
            set(K_VSTD, std);
            return this;
        }
        public FEParas setAudioStd(int std) {
            set(K_ASTD, std);
            return this;
        }
        public FEParas setAfc(int afc) {
            set(K_AFC, afc);
            return this;
        }
        public FEParas setVfmt(int vfmt) {
            set(K_VFMT, vfmt);
            return this;
        }
        public FEParas setAudioOutPutMode(int mode) {
            set(K_SOUNDSYS, mode);
            return this;
        }
    }

    public int PlayDTVProgram(FEParas fe, int vid, int vfmt, int aid, int afmt, int pcr, int audioCompetition) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(PLAY_PROGRAM_2);
        cmd.writeString(fe.toString());
        cmd.writeInt(vid);
        cmd.writeInt(vfmt);
        cmd.writeInt(aid);
        cmd.writeInt(afmt);
        cmd.writeInt(pcr);
        cmd.writeInt(audioCompetition);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        cmd.recycle();
        r.recycle();
        return tmpRet;
    }
    public int PlayDTVProgram(FEParas fe, int vid, int vfmt, int aid, int afmt, int pcr, int audioCompetition, boolean adPrepare) {
        setProperties("media.audio.enable_asso", (adPrepare)? "1" : "0");
        return PlayDTVProgram(fe, vid, vfmt, aid, afmt, pcr, audioCompetition);
    }
    public int PlayDTVProgram(FEParas fe, int vid, int vfmt, int aid, int afmt, int pcr, int audioCompetition, boolean adPrepare, int adMixingLevel) {
        setProperties("media.audio.mix_asso", String.valueOf(adMixingLevel));
        return PlayDTVProgram(fe, vid, vfmt, aid, afmt, pcr, audioCompetition, adPrepare);
    }

    public int StopPlayProgram() {
        return sendCmd(STOP_PROGRAM_PLAY);
    }


    public static class ScanParas extends Paras {
        public static final String K_MODE = "m";
        public static final String K_ATVMODE = "am";
        public static final String K_DTVMODE = "dm";
        public static final String K_ATVFREQ1 = "af1";
        public static final String K_ATVFREQ2 = "af2";
        public static final String K_DTVFREQ1 = "df1";
        public static final String K_DTVFREQ2 = "df2";
        public static final String K_PROC = "prc";
        public static final String K_DTVSTD = "dstd";
        public static final String KM_ATVMODIFIER = "_amod";
        public static final String KM_DTVMODIFIER = "_dmod";

        public static int MODE_ATV_DTV = 0;
        public static int MODE_DTV_ATV = 1;
        public static int MODE_ADTV = 2;

        public static int ATVMODE_AUTO = 1;
        public static int ATVMODE_MANUAL = 2;
        public static int ATVMODE_FREQUENCY = 3;
        public static int ATVMODE_NONE = 7;

        public static int DTVMODE_AUTO = 1;
        public static int DTVMODE_MANUAL = 2;
        public static int DTVMODE_ALLBAND = 3;
        public static int DTVMODE_NONE = 7;

        public static int DTVSTD_DVB = 0;
        public static int DTVSTD_ATSC = 1;
        public static int DTVSTD_ISDB = 2;

        public static int SCAN_PROCMODE_NORMAL                  = 0x00; /**< normal mode*/
        public static int SCAN_PROCMODE_AUTOPAUSE_ON_ATV_FOUND  = 0x01; /**< auto pause when found atv*/
        public static int SCAN_PROCMODE_AUTOPAUSE_ON_DTV_FOUND  = 0x02; /**< auto pause when found dtv*/

        public ScanParas() { super(); }
        public ScanParas(String paras) { super(paras); }

        public String toNamedString() {
            return toString("scan");
        }

        public int getMode() {
            return getInt(K_MODE, 0);
        }
        public int getAtvMode() {
            return getInt(K_ATVMODE, 0);
        }
        public int getDtvMode() {
            return getInt(K_DTVMODE, 0);
        }
        public int getAtvFrequency1() {
            return getInt(K_ATVFREQ1, 0);
        }
        public int getAtvFrequency2() {
            return getInt(K_ATVFREQ2, 0);
        }
        public int getDtvFrequency1() {
            return getInt(K_DTVFREQ1, 0);
        }
        public int getDtvFrequency2() {
            return getInt(K_DTVFREQ2, 0);
        }
        public int getProc() {
            return getInt(K_PROC, 0);
        }
        public int getDtvStandard() {
            return getInt(K_DTVSTD, -1);
        }
        public int getAtvModifier(String name, int def) {
            return getInt(name+KM_ATVMODIFIER, def);
        }
        public int getDtvModifier(String name, int def) {
            return getInt(name+KM_DTVMODIFIER, def);
        }

        public ScanParas setMode(int mode) {
            set(K_MODE, mode);
            return this;
        }
        public ScanParas setAtvMode(int atvMode) {
            set(K_ATVMODE, atvMode);
            return this;
        }
        public ScanParas setDtvMode(int dtvMode) {
            set(K_DTVMODE, dtvMode);
            return this;
        }
        public ScanParas setAtvFrequency1(int freq) {
            set(K_ATVFREQ1, freq);
            return this;
        }
        public ScanParas setAtvFrequency2(int freq) {
            set(K_ATVFREQ2, freq);
            return this;
        }
        public ScanParas setDtvFrequency1(int freq) {
            set(K_DTVFREQ1, freq);
            return this;
        }
        public ScanParas setDtvFrequency2(int freq) {
            set(K_DTVFREQ2, freq);
            return this;
        }
        public ScanParas setProc(int proc) {
            set(K_PROC, proc);
            return this;
        }
        public ScanParas setDtvStandard(int std) {
            set(K_DTVSTD, std);
            return this;
        }
        public ScanParas setAtvModifier(String name, int m) {
            set(name+KM_ATVMODIFIER, m);
            return this;
        }
        public ScanParas setDtvModifier(String name, int m) {
            set(name+KM_DTVMODIFIER, m);
            return this;
        }
    }

    public int TvScan(FEParas fe, ScanParas scan) {
        synchronized (mLock) {
            try {
                SetTvCurrentLanguage(TvMultilingualText.getLocalLang());
                return mProxy.Scan(fe.toString(), scan.toString());
            } catch (RemoteException e) {
                Log.e(TAG, "TvScan:" + e);
            }
        }
        return -1;
    }

    public int TvSetFrontEnd(FEParas fe, boolean force) {
        synchronized (mLock) {
            try {
                return mProxy.tvSetFrontEnd(fe.toString(), force? 1 : 0);
            } catch (RemoteException e) {
                Log.e(TAG, "TvSetFrontEnd:" + e);
            }
        }
        return -1;
    }

    public int TvSetFrontEnd(FEParas fe) {
        return TvSetFrontEnd(fe, false);
    }

    public enum tv_fe_type_e {
        TV_FE_QPSK(0),
        TV_FE_QAM(1),
        TV_FE_OFDM(2),
        TV_FE_ATSC(3),
        TV_FE_ANALOG(4),
        TV_FE_DTMB(5);

        private int val;

        tv_fe_type_e(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    /**
     * @Function:SetFrontendParms
     * @Description:set frontend parameters
     * @Param:dataBuf[0]:feType, tv_fe_type_e
     * @Param:dataBuf[1]:freq, set freq to tuner
     * @Param:dataBuf[2]:videoStd, video std
     * @Param:dataBuf[3]:audioStd, audio std
     * @Param:dataBuf[4]:parm1
     * @Param:dataBuf[5]:parm2
     * @Return: 0 ok or -1 error
     */
    public int SetFrontendParms(tv_fe_type_e feType, int freq, int vStd, int aStd, int vfmt, int soundsys, int p1, int p2) {
        //int val[] = new int[]{feType.toInt(), freq, vStd, aStd, vfmt, soundsys, p1, p2};
        //return sendCmdIntArray(SET_FRONTEND_PARA, val);
        synchronized (mLock) {
            try {
                return mProxy.tvSetFrontendParms(feType.toInt(), freq, vStd, aStd, vfmt, soundsys, p1, p2);
            } catch (RemoteException e) {
                Log.e(TAG, "SetFrontendParms:" + e);
            }
        }
        return -1;
    }

    public enum CC_PARAM_COUNTRY {
        CC_PARAM_COUNTRY_USA(0),
        CC_PARAM_COUNTRY_KOREA(1);

        private int val;

        CC_PARAM_COUNTRY(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum CC_PARAM_SOURCE_TYPE {
        CC_PARAM_SOURCE_VBIDATA(0),
        CC_PARAM_SOURCE_USERDATA(1);

        private int val;

        CC_PARAM_SOURCE_TYPE(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum CC_PARAM_CAPTION_TYPE {
        CC_PARAM_ANALOG_CAPTION_TYPE_CC1(0),
        CC_PARAM_ANALOG_CAPTION_TYPE_CC2(1),
        CC_PARAM_ANALOG_CAPTION_TYPE_CC3(2),
        CC_PARAM_ANALOG_CAPTION_TYPE_CC4(3),
        CC_PARAM_ANALOG_CAPTION_TYPE_TEXT1(4),
        CC_PARAM_ANALOG_CAPTION_TYPE_TEXT2(5),
        CC_PARAM_ANALOG_CAPTION_TYPE_TEXT3(6),
        CC_PARAM_ANALOG_CAPTION_TYPE_TEXT4(7),
        //
        CC_PARAM_DIGITAL_CAPTION_TYPE_SERVICE1(8),
        CC_PARAM_DIGITAL_CAPTION_TYPE_SERVICE2(9),
        CC_PARAM_DIGITAL_CAPTION_TYPE_SERVICE3(10),
        CC_PARAM_DIGITAL_CAPTION_TYPE_SERVICE4(11),
        CC_PARAM_DIGITAL_CAPTION_TYPE_SERVICE5(12),
        CC_PARAM_DIGITAL_CAPTION_TYPE_SERVICE6(13);

        private int val;

        CC_PARAM_CAPTION_TYPE(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }


    /*
     * 1, Set the country first and parameters should be either USA or KOREA
    #define CMD_SET_COUNTRY_USA                 0x5001
    #define CMD_SET_COUNTRY_KOREA            0x5002

    2, Set the source type which including
    a)VBI data(for analog program only)
    b)USER data(for AIR or Cable service)
    CMD_CC_SET_VBIDATA   = 0x7001,
    CMD_CC_SET_USERDATA = 0x7002,
    2.1 If the frontend type is Analog we must set the channel Index
    with command 'CMD_CC_SET_CHAN_NUM' and the parameter is like 57M
    we set 0x20000, this should according to USA standard frequency
    table.

    3, Next is to set the CC service type

    #define CMD_CC_1                        0x3001
    #define CMD_CC_2                        0x3002
    #define CMD_CC_3                        0x3003
    #define CMD_CC_4                        0x3004

        //this doesn't support currently
    #define CMD_TT_1                        0x3005
    #define CMD_TT_2                        0x3006
    #define CMD_TT_3                        0x3007
    #define CMD_TT_4                        0x3008

    #define CMD_SERVICE_1                 0x4001
    #define CMD_SERVICE_2                 0x4002
    #define CMD_SERVICE_3                 0x4003
    #define CMD_SERVICE_4                 0x4004
    #define CMD_SERVICE_5                 0x4005
    #define CMD_SERVICE_6                 0x4006

    4, Then set CMD_CC_START to start the CC service, and you needn't to stop

    CC service while switching services

    5, CMD_CC_STOP should be called in some cases like switch source, change

    program, no signal, blocked...*/

    //channel_num == 0 ,if frontend is dtv
    //else != 0
    public int StartCC(CC_PARAM_COUNTRY country, CC_PARAM_SOURCE_TYPE src_type, int channel_num, CC_PARAM_CAPTION_TYPE caption_type) {
        int val[] = new int[]{country.toInt(), src_type.toInt(), channel_num, caption_type.toInt()};
        return sendCmdIntArray(DTV_START_CC, val);
    }

    public int StopCC() {
        return sendCmd(DTV_STOP_CC);
    }

    private void libtv_log_open(){
        //if (tvLogFlg) {
            StackTraceElement traceElement = ((new Exception()).getStackTrace())[1];
            Log.i(TAG, traceElement.getMethodName());
        //}
    }

    public enum LEFT_RIGHT_SOUND_CHANNEL {
        LEFT_RIGHT_SOUND_CHANNEL_STEREO(0),
        LEFT_RIGHT_SOUND_CHANNEL_LEFT(1),
        LEFT_RIGHT_SOUND_CHANNEL_RIGHT(2),
        LEFT_RIGHT_SOUND_CHANNEL_SWAP(3);
        private int val;

        LEFT_RIGHT_SOUND_CHANNEL(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public class ProgList {
        public String name;
        public int Id;
        public int chanOrderNum;
        public int major;
        public int minor;
        public int type;//service_type
        public int skipFlag;
        public int favoriteFlag;
        public int videoFmt;
        public int tsID;
        public int serviceID;
        public int pcrID;
        public int vPid;
        public ArrayList<DtvAudioTrackInfo> audioInfoList;
        public int chFreq;
    }

    /**
     * @Function:GetProgramList
     * @Description,get program list
     * @Param:serType,get diff program list by diff service type
     * @Param:skip,default 0(it shows no skip)
     * @Return:ProgList
     */
    public ArrayList<ProgList> GetProgramList(tv_program_type serType, program_skip_type_e skip) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(GET_PROGRAM_LIST);
        cmd.writeInt(serType.toInt());
        cmd.writeInt(skip.toInt());
        sendCmdToTv(cmd, r);
        int size = r.readInt();
        ArrayList<ProgList> pList = new ArrayList<ProgList>();
        for (int i = 0; i < size; i++) {
            ProgList pl = new ProgList();
            pl.Id = r.readInt();
            pl.chanOrderNum = r.readInt();
            pl.major = r.readInt();
            pl.minor = r.readInt();
            pl.type = r.readInt();
            pl.name = r.readString();
            pl.skipFlag = r.readInt();
            pl.favoriteFlag = r.readInt();
            pl.videoFmt = r.readInt();
            pl.tsID = r.readInt();
            pl.serviceID = r.readInt();
            pl.pcrID = r.readInt();
            pl.vPid = r.readInt();
            int trackSize = r.readInt();
            pl.audioInfoList = new ArrayList<DtvAudioTrackInfo>();
            for (int j = 0; j < trackSize; j++) {
                DtvAudioTrackInfo info = new DtvAudioTrackInfo();
                info.language =r.readString();
                info.audio_fmt =r.readInt();
                info.aPid = r.readInt();
                pl.audioInfoList.add(info);
            }
            pl.chFreq = r.readInt();
            pList.add(pl);
        }
        Log.i(TAG,"get prog list size = "+pList.size());
        cmd.recycle();
        r.recycle();
        return pList;
    }

    public int GetHdmiHdcpKeyKsvInfo(int data_buf[]) {
        int ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(HDMIRX_GET_KSV_INFO);
        sendCmdToTv(cmd, r);

        ret = r.readInt();
        data_buf[0] = r.readInt();
        data_buf[1] = r.readInt();
        cmd.recycle();
        r.recycle();
        return ret;
    }

    public enum FBCUpgradeState {
        STATE_STOPED(0),
        STATE_RUNNING(1),
        STATE_FINISHED(2),
        STATE_ABORT(3);

        private int val;

        FBCUpgradeState(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum FBCUpgradeErrorCode {
        ERR_SERIAL_CONNECT(-1),
        ERR_OPEN_BIN_FILE(-2),
        ERR_BIN_FILE_SIZE(-3);

        private int val;

        FBCUpgradeErrorCode(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    /**
     * @Function: StartUpgradeFBC
     * @Description: start upgrade fbc
     * @Param: file_name: upgrade bin file name
     *         mode: value refer to enum FBCUpgradeState
     * @Return: 0 success, -1 fail
     */
    public int StartUpgradeFBC(String file_name, int mode) {
        return StartUpgradeFBC(file_name, mode, 0x10000);
    }

    /**
     * @Function: StartUpgradeFBC
     * @Description: start upgrade fbc
     * @Param: file_name: upgrade bin file name
     *         mode: value refer to enum FBCUpgradeState
     *         upgrade_blk_size: upgrade block size (min is 4KB)
     * @Return: 0 success, -1 fail
     */
    public int StartUpgradeFBC(String file_name, int mode, int upgrade_blk_size) {
        return 0;
    }

    /**
     * @Function: FactorySet_FBC_Brightness
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Brightness(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(FACTORY_FBC_SET_BRIGHTNESS, val);
    }

    /**
     * @Function: FactoryGet_FBC_Brightness
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Brightness() {
        return sendCmd(FACTORY_FBC_GET_BRIGHTNESS);
    }

    /**
     * @Function: FactorySet_FBC_Contrast
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Contrast(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(FACTORY_FBC_SET_CONTRAST, val);
    }

    /**
     * @Function: FactoryGet_FBC_Contrast
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Contrast() {
        return sendCmd(FACTORY_FBC_GET_CONTRAST);
    }

    /**
     * @Function: FactorySet_FBC_Saturation
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Saturation(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(FACTORY_FBC_SET_SATURATION, val);
    }

    /**
     * @Function: FactoryGet_FBC_Saturation
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Saturation() {
        return sendCmd(FACTORY_FBC_GET_SATURATION);
    }

    /**
     * @Function: FactorySet_FBC_HueColorTint
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_HueColorTint(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(FACTORY_FBC_SET_HUE, val);
    }

    /**
     * @Function: FactoryGet_FBC_HueColorTint
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_HueColorTint() {
        return sendCmd(FACTORY_FBC_GET_HUE);
    }

    /**
     * @Function: FactorySet_FBC_Backlight
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Backlight(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(FACTORY_FBC_SET_BACKLIGHT, val);
    }

    /**
     * @Function: FactoryGet_FBC_Backlight
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Backlight() {
        return sendCmd(FACTORY_FBC_GET_BACKLIGHT);
    }

    /**
     * @Function: FactorySet_backlight_onoff
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_backlight_onoff(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(FACTORY_FBC_SET_BACKLIGHT_EN, val);
    }

    /**
     * @Function: FactoryGet_FBC_Backlight
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_backlight_onoff() {
        return sendCmd(FACTORY_FBC_GET_BACKLIGHT_EN);
    }

    /**
     * @Function: FactorySet_SET_LVDS_SSG
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_SET_LVDS_SSG(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(FACTORY_FBC_SET_LVDS_SSG, val);
    }

    /**
     * @Function: FactorySet_AUTO_ELEC_MODE
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_AUTO_ELEC_MODE(int mode) {
        int val[] = new int[]{mode};
        return sendCmdIntArray(FACTORY_FBC_SET_ELEC_MODE, val);
    }

    /**
     * @Function: FactoryGet_AUTO_ELEC_MODE
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_AUTO_ELEC_MODE() {
        return sendCmd(FACTORY_FBC_GET_ELEC_MODE);
    }

    /**
     * @Function: FactorySet_FBC_Picture_Mode
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Picture_Mode(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(FACTORY_FBC_SET_PIC_MODE, val);
    }

    /**
     * @Function: FactoryGet_FBC_Picture_Mode
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Picture_Mode() {
        return sendCmd(FACTORY_FBC_GET_PIC_MODE);
    }

    /**
     * @Function: FactorySet_FBC_Test_Pattern
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Test_Pattern(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(FACTORY_FBC_SET_TEST_PATTERN, val);
    }

    /**
     * @Function: FactoryGet_FBC_Test_Pattern
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Test_Pattern() {
        return sendCmd(FACTORY_FBC_GET_TEST_PATTERN);
    }

    /**
     * @Function: FactorySet_FBC_Gain_Red
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Gain_Red(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(FACTORY_FBC_SET_GAIN_RED, val);
    }

    /**
     * @Function: FactoryGet_FBC_Gain_Red
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Gain_Red() {
        return sendCmd(FACTORY_FBC_GET_GAIN_RED);
    }

    /**
     * @Function: FactorySet_FBC_Gain_Green
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Gain_Green(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(FACTORY_FBC_SET_GAIN_GREEN, val);
    }

    /**
     * @Function: FactoryGet_FBC_Gain_Green
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Gain_Green() {
        return sendCmd(FACTORY_FBC_GET_GAIN_GREEN);
    }

    /**
     * @Function: FactorySet_FBC_Gain_Blue
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Gain_Blue(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(FACTORY_FBC_SET_GAIN_BLUE, val);
    }

    /**
     * @Function: FactoryGet_FBC_Gain_Blue
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Gain_Blue() {
        return sendCmd(FACTORY_FBC_GET_GAIN_BLUE);
    }

    /**
     * @Function: FactorySet_FBC_Offset_Red
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Offset_Red(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(FACTORY_FBC_SET_OFFSET_RED, val);
    }

    /**
     * @Function: FactoryGet_FBC_Offset_Red
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Offset_Red() {
        return sendCmd(FACTORY_FBC_GET_OFFSET_RED);
    }

    /**
     * @Function: FactorySet_FBC_Offset_Green
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Offset_Green(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(FACTORY_FBC_SET_OFFSET_GREEN, val);
    }

    /**
     * @Function: FactoryGet_FBC_Offset_Green
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Offset_Green() {
        return sendCmd(FACTORY_FBC_GET_OFFSET_GREEN);
    }

    /**
     * @Function: FactorySet_FBC_Offset_Blue
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Offset_Blue(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(FACTORY_FBC_SET_OFFSET_BLUE, val);
    }

    /**
     * @Function: FactoryGet_FBC_Offset_Blue
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Offset_Blue() {
        return sendCmd(FACTORY_FBC_GET_OFFSET_BLUE);
    }

    /**
     * @Function: FactorySet_FBC_ColorTemp_Mode
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_ColorTemp_Mode(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(FACTORY_FBC_SET_COLORTEMP_MODE, val);
    }

    /**
     * @Function: FactoryGet_FBC_ColorTemp_Mode
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_ColorTemp_Mode() {
        return sendCmd(FACTORY_FBC_GET_COLORTEMP_MODE);
    }

    /**
     * @Function: FactorySet_FBC_WB_Initial
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_WB_Initial(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(FACTORY_FBC_SET_WB_INIT, val);
    }

    /**
     * @Function: FactoryGet_FBC_WB_Initial
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_WB_Initial() {
        return sendCmd(FACTORY_FBC_GET_WB_INIT);
    }

    public class FBC_MAINCODE_INFO {
        public String Version;
        public String LastBuild;
        public String GitVersion;
        public String GitBranch;
        public String BuildName;
    }

    public FBC_MAINCODE_INFO FactoryGet_FBC_Get_MainCode_Version() {
        FBC_MAINCODE_INFO  info = new FBC_MAINCODE_INFO();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_GET_MAINCODE_VERSION);
        sendCmdToTv(cmd, r);
        info.Version = r.readString();
        info.LastBuild = r.readString();
        info.GitVersion = r.readString();
        info.GitBranch = r.readString();
        info.BuildName = r.readString();
        cmd.recycle();
        r.recycle();
        return info;
    }

    /**
     * @Function: FactorySet_FBC_Panel_Power_Switch
     * @Description: set fbc panel power switch
     * @Param: value, 0 is fbc panel power off, 1 is panel power on.
     * @Return:
     */
    public int FactorySet_FBC_Panel_Power_Switch(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(FACTORY_FBC_PANEL_POWER_SWITCH, val);
    }

    /**
     * @Function: FactorySet_FBC_SN_Info
     * @Description: set SN info to FBC save
     * @Param: strFactorySN is string to set. len is SN length
     * @Return 0 is success,else is fail:
     */
    public int FactorySet_FBC_SN_Info(String strFactorySN,int len) {
        String val[] = new String[]{strFactorySN};
        return sendCmdStringArray(FACTORY_SET_SN, val);
    }

    public String FactoryGet_FBC_SN_Info() {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_GET_SN);
        sendCmdToTv(cmd, r);
        String str = r.readString();
        cmd.recycle();
        r.recycle();
        return str;
    }

    public String FactorySet_FBC_Panel_Get_Info() {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_PANEL_GET_INFO);
        sendCmdToTv(cmd, r);
        String str = r.readString();
        cmd.recycle();
        r.recycle();
        return str;
    }

    //@:value ,default 0
    public int FactorySet_FBC_Panel_Suspend(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(FACTORY_FBC_PANEL_SUSPEND, val);
    }

    //@:value ,default 0
    public int FactorySet_FBC_Panel_User_Setting_Default(int value) {
        int val[] = new int[]{value};
        return sendCmdIntArray(FACTORY_FBC_PANEL_USER_SETTING_DEFAULT, val);
    }
    // set listener when not need to listen set null

    public final static int EVENT_AV_PLAYBACK_NODATA            = 1;
    public final static int EVENT_AV_PLAYBACK_RESUME            = 2;
    public final static int EVENT_AV_SCRAMBLED                  = 3;
    public final static int EVENT_AV_UNSUPPORT                  = 4;
    public final static int EVENT_AV_VIDEO_AVAILABLE            = 5;
    public final static int EVENT_AV_TIMESHIFT_REC_FAIL = 6;
    public final static int EVENT_AV_TIMESHIFT_PLAY_FAIL = 7;
    public final static int EVENT_AV_TIMESHIFT_START_TIME_CHANGED = 8;
    public final static int EVENT_AV_TIMESHIFT_CURRENT_TIME_CHANGED = 9;
    public final static int EVENT_AV_PLAYER_BLOCKED = 10;
    public final static int EVENT_AV_PLAYER_UNBLOCK = 11;
    public final static int EVENT_PLAY_INSTANCE     = 12;
    public final static int AUDIO_UNMUTE_FOR_TV                 = 0;
    public final static int AUDIO_MUTE_FOR_TV                   = 1;

    public interface AudioEventListener {
        void HandleAudioEvent(int cmd, int param1, int param2);
    }

    public void SetAudioEventListener (AudioEventListener l) {
        libtv_log_open();
        mAudioListener  = l;
    }

    public interface PlayerInstanceNoListener {
        void PlayerInsEvent(int msgType, int type, int Id);
    };

    public void SetPlayerInstanceNoListener(PlayerInstanceNoListener l) {
        libtv_log_open();
        mPlayerInstanceNoListener = l;
    }

    public interface AVPlaybackListener {
        void onEvent(int msgType, int programID);
    };

    public void SetAVPlaybackListener(AVPlaybackListener l) {
        libtv_log_open();
        mAVPlaybackListener = l;
    }

    public void SetSigInfoChangeListener(TvInSignalInfo.SigInfoChangeListener l) {
        libtv_log_open();
        if (!mSigInfoChangeListener.contains(l)) {
            mSigInfoChangeListener.add(l);
        }
    }

    public void removeSigInfoChangeListener(TvInSignalInfo.SigInfoChangeListener l) {
        libtv_log_open();
        Iterator<TvInSignalInfo.SigInfoChangeListener> iterator = mSigInfoChangeListener.iterator();
        while (iterator.hasNext()) {
            TvInSignalInfo.SigInfoChangeListener listener = iterator.next();
            if (listener.equals(l)) {
                iterator.remove();
            }
        }
    }

    public void SetSigChannelSearchListener(TvInSignalInfo.SigChannelSearchListener l) {
        libtv_log_open();
        mSigChanSearchListener = l;
    }

    public interface Status3DChangeListener {
        void onStatus3DChange(int state);
    }

    public interface StatusSourceConnectListener {
        void onSourceConnectChange(SourceInput source, int connectionState);
    }

    public void SetSourceConnectListener(StatusSourceConnectListener l) {
        libtv_log_open();
        if (!mSourceConnectChangeListener.contains(l)) {
            mSourceConnectChangeListener.add(l);
        }
    }

    public void removeSourceConnectListener(StatusSourceConnectListener l) {
        libtv_log_open();
        Iterator<StatusSourceConnectListener> iterator = mSourceConnectChangeListener.iterator();
        while (iterator.hasNext()) {
            StatusSourceConnectListener listener = iterator.next();
            if (listener.equals(l)) {
                iterator.remove();
            }
        }
    }

    public interface HDMIRxCECListener {
        void onHDMIRxCECMessage(int msg_len, int msg_buf[]);
    }

    public void SetHDMIRxCECListener(HDMIRxCECListener l) {
        libtv_log_open();
        mHDMIRxCECListener = l;
    }

    public interface UpgradeFBCListener {
        void onUpgradeStatus(int state, int param);
    }

    public void SetUpgradeFBCListener(UpgradeFBCListener l) {
        libtv_log_open();
        mUpgradeFBCListener = l;
    }

    public void SetStatus3DChangeListener(Status3DChangeListener l) {
        libtv_log_open();
        mStatus3DChangeListener = l;
    }

    public interface AdcCalibrationListener {
        void onAdcCalibrationChange(int state);
    }

    public void SetAdcCalibrationListener(AdcCalibrationListener l) {
        libtv_log_open();
        mAdcCalibrationListener = l;
    }

    public interface SourceSwitchListener {
        void onSourceSwitchStatusChange(SourceInput input, int state);
    }

    public void SetSourceSwitchListener(SourceSwitchListener l) {
        libtv_log_open();
        mSourceSwitchListener = l;
    }

    public interface ChannelSelectListener {
        void onChannelSelect(int msg_pdu[]);
    }

    public void SetChannelSelectListener(ChannelSelectListener l) {
        libtv_log_open();
        mChannelSelectListener = l;
    }

    public interface SerialCommunicationListener {
        //dev_id, refer to enum SerialDeviceID
        void onSerialCommunication(int dev_id, int msg_len, int msg_pdu[]);
    };

    public void SetSerialCommunicationListener(SerialCommunicationListener l) {
        libtv_log_open();
        mSerialCommunicationListener = l;
    }

    public interface CloseCaptionListener {
        void onCloseCaptionProcess(int data_buf[], int cmd_buf[]);
    };

    public void SetCloseCaptionListener(CloseCaptionListener l) {
        libtv_log_open();
        mCloseCaptionListener = l;
    }

    public enum SourceInput {
        TV(0),
        AV1(1),
        AV2(2),
        YPBPR1(3),
        YPBPR2(4),
        HDMI1(5),
        HDMI2(6),
        HDMI3(7),
        HDMI4(8),
        VGA(9),
        XXXX(10),//not use MPEG source
        DTV(11),
        SVIDEO(12),
        IPTV(13),
        DUMMY(14),
        SOURCE_SPDIF(15),
        ADTV(16),
        AUX(17),
        ARC(18),
        MAX(19);
        private int val;

        SourceInput(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum SourceInput_Type {
        SOURCE_TYPE_TV(0),
        SOURCE_TYPE_AV(1),
        SOURCE_TYPE_COMPONENT(2),
        SOURCE_TYPE_HDMI(3),
        SOURCE_TYPE_VGA(4),
        SOURCE_TYPE_MPEG(5),//only use for vpp, for display ,not a source
        SOURCE_TYPE_DTV(6),
        SOURCE_TYPE_SVIDEO(7),
        SOURCE_TYPE_IPTV(8),
        SOURCE_TYPE_SPDIF(9),
        SOURCE_TYPE_ADTV(10),
        SOURCE_TYPE_AUX(11),
        SOURCE_TYPE_ARC(12),
        SOURCE_TYPE_MAX(13);

        private int val;

        SourceInput_Type(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum tvin_color_system_e {
        COLOR_SYSTEM_AUTO(0),
        COLOR_SYSTEM_PAL(1),
        COLOR_SYSTEM_NTSC(2),
        COLOR_SYSTEM_SECAM(3),
        COLOR_SYSTEM_MAX(4);
        private int val;

        tvin_color_system_e(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum tv_program_type {//program_type
        TV_PROGRAM_UNKNOWN(0),
        TV_PROGRAM_DTV(1),
        TV_PROGRAM_DRADIO(2),
        TV_PROGRAM_ATV(3);
        private int val;

        tv_program_type(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum program_skip_type_e {
        TV_PROGRAM_SKIP_NO(0),
        TV_PROGRAM_SKIP_YES(1),
        TV_PROGRAM_SKIP_UNKNOWN(2);

        private int val;

        program_skip_type_e(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum atsc_antenna_type_t {
        AM_ATSC_ANTENNA_TYPE_MIX(0),
        AM_ATSC_ANTENNA_TYPE_AIR(1),
        AM_ATSC_ANTENNA_TYPE_CABLE_STD(2),
        AM_ATSC_ANTENNA_TYPE_CABLE_IRC(3),
        AM_ATSC_ANTENNA_TYPE_CABLE_HRC(4),
        AM_ATSC_ANTENNA_TYPE_MAX(5);

        private int val;
        atsc_antenna_type_t(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum HdmiPortID {
        HDMI_PORT_1(1),
        HDMI_PORT_2(2),
        HDMI_PORT_3(3),
        HDMI_PORT_4(4);
        private int val;

        HdmiPortID(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum HdmiEdidVer {
        HDMI_EDID_VER_14(0),
        HDMI_EDID_VER_20(1);
        private int val;

        HdmiEdidVer(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum HdcpKeyIsEnable {
        hdcpkey_enable(0),
        hdcpkey_disable(1);
        private int val;

        HdcpKeyIsEnable(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum HdmiColorRangeMode {
        AUTO_RANGE(0),
        FULL_RANGE(1),
        LIMIT_RANGE(2);
        private int val;

        HdmiColorRangeMode(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    /**
     * @description set gpio
     * @param portName name of gpio, e.g "GPIOAO_14"
     * @param isOut true set gpio as out, false gpio is in.
     * @param edge validly when {@param is_out} is true, 1/0 high/low
     */
    public int handleGPIO(String portName, boolean isOut, int edge) {
          synchronized (mLock) {
            try {
                return mProxy.handleGPIO(portName, isOut ? 1 : 0, edge);
            } catch (RemoteException e) {
                Log.e(TAG, "handleGPIO:" + e);
            }
        }
        return -1;
    }

    public VideoFormatInfo GetHdmiFormatInfo() {
        synchronized (mLock) {
            VideoFormatInfo pVideoFormatInfo = new VideoFormatInfo();
            try {
                FormatInfo  formatInfo= mProxy.getHdmiFormatInfo();
                pVideoFormatInfo.width = formatInfo.width;
                pVideoFormatInfo.height= formatInfo.height;
                pVideoFormatInfo.fps= formatInfo.fps;
                pVideoFormatInfo.interlace= formatInfo.interlace;
                return pVideoFormatInfo;
            } catch (Exception e) {
                Log.e(TAG, "GetHdmiFormatInfo:" + e);
            }
        }
        return null;
    }

    /**
     * @description set lcd enable or disable
     * @param enable true/fase means enable/disable
     */
    public int setLcdEnable(boolean enable) {
        synchronized (mLock) {
            try {
                return mProxy.setLcdEnable(enable ? 1 : 0);
            } catch (RemoteException e) {
                Log.e(TAG, "setLcdEnable:" + e);
            }
        }
        return -1;
    }

    // frontend event
    public class RecorderEvent {
        //frontend events
        public static final int EVENT_RECORDER_START         = 1;
        public static final int EVENT_RECORDER_STOP    = 2;

        public int Status;
        public int Error;
        public String Id;
    }
    public interface RecorderEventListener {
        void onRecoderEvent(RecorderEvent ev);
    };

    private RecorderEventListener mRecorderEventListener = null;
    public void SetRecorderEventListener(RecorderEventListener l) {
        libtv_log_open();
        mRecorderEventListener = l;
    }


    public static final int RECORDING_CMD_STOP = 0;
    public static final int RECORDING_CMD_PREPARE = 1;
    public static final int RECORDING_CMD_START = 2;

    public int sendRecordingCmd(int cmd, String id, String param) {
        synchronized (mLock) {
            try {
                Log.d(TAG, "sendRecordingCmd");
                return mProxy.sendRecordingCmd(cmd, id, (param == null) ? "" : param);
            } catch (RemoteException e) {
                Log.e(TAG, "sendRecordingCmd:" + e);
            }
        }
        return -1;
    }

    public int prepareRecording(String id, String param) {
        return sendRecordingCmd(RECORDING_CMD_PREPARE, id, param);
    }

    public int startRecording(String id, String param) {
        return sendRecordingCmd(RECORDING_CMD_START, id, param);
    }

    public int stopRecording(String id, String param) {
        return sendRecordingCmd(RECORDING_CMD_STOP, id, param);
    }

    public static final int PLAY_CMD_STOP = 0;
    public static final int PLAY_CMD_START = 1;
    public static final int PLAY_CMD_PAUSE = 2;
    public static final int PLAY_CMD_RESUME = 3;
    public static final int PLAY_CMD_SEEK = 4;
    public static final int PLAY_CMD_SETPARAM = 5;

    public int sendPlayCmd(int cmd, String id, String param) {
        synchronized (mLock) {
            try {
                return mProxy.sendPlayCmd(cmd, id, (param == null) ? "" : param);
            } catch (RemoteException e) {
                Log.e(TAG, "sendPlayCmd:" + e);
            }
        }
        return -1;
    }

    public int startPlay(String id, String param) {
        //SystemProperties.set ("vendor.media.audio.enable_asso", (adPrepare)? "1" : "0");
        //SystemProperties.set ("vendor.media.audio.mix_asso", String.valueOf(adMixingLevel));
        return sendPlayCmd(PLAY_CMD_START, id, param);
    }

    public int stopPlay(String id, String param) {
        return sendPlayCmd(PLAY_CMD_STOP, id, param);
    }

    public int pausePlay(String id) {
        return sendPlayCmd(PLAY_CMD_PAUSE, id, null);
    }

    public int resumePlay(String id) {
        return sendPlayCmd(PLAY_CMD_RESUME, id, null);
    }

    public int seekPlay(String id, String param) {
        return sendPlayCmd(PLAY_CMD_SEEK, id, param);
    }

    public int setPlayParam(String id, String param) {
        return sendPlayCmd(PLAY_CMD_SETPARAM, id, param);
    }

    public int setDeviceIdForCec(int DeviceId) {
        synchronized (mLock) {
            try {
                return mProxy.setDeviceIdForCec(DeviceId);
            } catch (RemoteException e) {
                Log.e(TAG, "setDeviceIdForCec:" + e);
            }
        }
        return -1;
    }

	/**
	* @Function: setScreenColorForSignalChange
	* @Description: set screen color for switch source/switch channal/no signal
	* @Param: screenColor:screen color:0 is black screen; 1 is blue screen
	* @Param: isSave:whether want to save setting
	* @Return: 0 ok or -1 error
	*/
	public int setScreenColorForSignalChange(int screenColor, int isSave) {
        synchronized (mLock) {
            try {
                return mProxy.setScreenColorForSignalChange(screenColor, isSave);
            } catch (RemoteException e) {
                Log.e(TAG, "setScreenColorForSignalChange:" + e);
            }
        }
        return -1;
	}

    /**
     * @Function: getScreenColorForSignalChange
     * @Description: get screen color for switch source/switch channal/no signal
     * @Return: 0 is black screen; 1 is blue screen;
     */
	public int getScreenColorForSignalChange() {
        synchronized (mLock) {
            try {
                return mProxy.getScreenColorForSignalChange();
            } catch (RemoteException e) {
                Log.e(TAG, "getScreenColorForSignalChange:" + e);
            }
        }

		return -1;
	}

  public void GetIwattRegs() {
        synchronized (mLock) {
            try {
                mProxy.getIwattRegs();
            } catch (RemoteException e) {
                Log.e(TAG, "GetIwattRegs:" + e);
            }
        }
    }

    public int SetSameSourceEnable(int IsEnable) {
        synchronized (mLock) {
            try {
                return mProxy.setSameSourceEnable(IsEnable);
            } catch (RemoteException e) {
                Log.e(TAG, "SetSameSourceEnable:" + e);
            }
        }
        return -1;
    }

    public class BasicVdecStatusInfo {
        public int decode_time_cost;/*us*/
        public int frame_width;
        public int frame_height;
        public int frame_rate;
        public int error_count;
        public int frame_count;
        public int error_frame_count;
        public int drop_frame_count;
        public int double_write_mode;//original samp_cnt;
    }

    public BasicVdecStatusInfo getBasicVdecSTatusInfo(int vdecId) {

        synchronized (mLock) {
            BasicVdecStatusInfo vInfo = new BasicVdecStatusInfo();
            try {
                BasicVdecState info = mProxy.getBasicVdecStatusInfo(vdecId);
                vInfo.decode_time_cost = info.decode_time_cost;
                vInfo.frame_width = info.frame_width;
                vInfo.frame_height = info.frame_height;
                vInfo.frame_rate = info.frame_rate;
                vInfo.error_count = info.error_count;
                vInfo.frame_count =  info.frame_count;
                vInfo.error_frame_count = info.error_frame_count;
                vInfo.drop_frame_count = info.drop_frame_count;
                vInfo.double_write_mode = info.double_write_mode;
                return vInfo;
            } catch (Exception e) {
                Log.e(TAG, "SetSameSourceEnable:" + e);
            }
        }
        return null;
    }

    public String request(String resource, String jsonParameters) {
        String invalidRet = "{\"ret\":1}";
        if (resource == null || jsonParameters == null) {
            return invalidRet;
        }

        synchronized (mLock) {
            try {
                return mProxy.request(resource, jsonParameters);
            } catch (Exception e) {
                Log.e(TAG, "request:" + e);
            }
        }
        return invalidRet;
    }

    public boolean isBlockedByChannelLock() {
        boolean ret = false;
        String tmp = request("ADTV.isCurrentChannelblocked", "");
        try {
            JSONObject data = new JSONObject(tmp);
            int callRet = data.optInt("ret", 0);
            if (callRet == 0) {
                ret = data.optBoolean("blocked", false);
            }
        } catch (JSONException e) {
        }
        return ret;
    }

    public int SetAudioMute(int mute) {
        int ret = 0;
        String tmp = request("ADTV.AudioSetMute", "{\"ATV_mute\":" + mute + ",\"DTV_mute\":" + mute + "}");
        try {
            JSONObject data = new JSONObject(tmp);
            ret = data.optInt("ret", 0);
        } catch (JSONException e) {
        }
        return ret;
    }

    public int GetAudioMute() {
        int ret = 0;
        String tmp = request("ADTV.GetAudioMute", "");
        try {
            JSONObject data = new JSONObject(tmp);
            int ATV_mute = data.optInt("ATV_mute", 0);
            int DTV_mute = data.optInt("DTV_mute", 0);
            ret = DTV_mute;//current only dtv need,so only return dtv mute status
        } catch (JSONException e) {
        }
        return ret;
    }

    //Preferred language settings
    public interface PreferredChangedListener {
        void onEvent(int type, String language);
    };

    private PreferredChangedListener mPreferredChangedListener = null;
    private String[] languageTable = {"chi", "eng", "chs", "none"};
    private List<String> mLanguageList = Arrays.asList(languageTable);

    public void SetPreferredChangedListener(PreferredChangedListener l) {
        mPreferredChangedListener = l;
    }

    public void SetPreferredLanguage(Context context, int type, String language) {
        String id = getPreferredIdFromType(type);
        if (TextUtils.isEmpty(id)) {
            Log.w(TAG, "Set preferred language with wrong type.");
        } else {
            String current = TvControlDataManager.getInstance(context).
                getStringValue(context, id, null);
            if (!Objects.equals(current, language)) {
                TvControlDataManager.getInstance(context).
                    putStringValue(context, id, (language == null) ? "none" : language);
                if (mPreferredChangedListener != null) {
                    mPreferredChangedListener.onEvent(type, language);
                }
            } else {
                Log.d(TAG, "Skip set the same preferred language.");
            }
        }
    }

    public void setPreferredLanguageWithSelection(Context context, int type, int selection) {
        if (selection < 0 || selection > mLanguageList.size() -1) {
            Log.w(TAG, "Set preferred language out of index.");
            return;
        }
        String lan = mLanguageList.get(selection);
        SetPreferredLanguage(context, type, lan);
    }

    public String GetPreferredLanguage(Context context, int type, String def) {
        String ret = "none";
        String id = getPreferredIdFromType(type);
        if (TextUtils.isEmpty(id)) {
            Log.w(TAG, "Get preferred language with wrong type.");
        } else {
            ret = TvControlDataManager.getInstance(context).
               getStringValue(context, id, def);
            if (ret == null) {
                if (type == DroidLogicTvUtils.PREFERRED_SUB_DEFAULT
                    || type == DroidLogicTvUtils.PREFERRED_AUD_DEFAULT)
                    ret = mLanguageList.get(0);
                else
                    ret = mLanguageList.get(1);
            } else if (type <= DroidLogicTvUtils.PREFERRED_AUD_SECONDARY && !mLanguageList.contains(ret)) {
                ret = "none";
            }
        }
        return ret;
    }

    public int getCurrentPreferredSelection(Context context, int type) {
        String lan = GetPreferredLanguage(context, type, null);
        int ret = mLanguageList.indexOf(lan);
        if (ret == -1) {
            ret = mLanguageList.size() - 1;
        }
        return ret;
    }

    private String getPreferredIdFromType(int type) {
        String id = null;
        switch (type) {
            case DroidLogicTvUtils.PREFERRED_SUB_DEFAULT:
                id = DroidLogicTvUtils.TV_KEY_SUB_DEFAULT_LANGUAGE;
                break;
            case DroidLogicTvUtils.PREFERRED_SUB_SECONDARY:
                id = DroidLogicTvUtils.TV_KEY_SUB_SECOND_LANGUAGE;
                break;
            case DroidLogicTvUtils.PREFERRED_AUD_DEFAULT:
                id = DroidLogicTvUtils.TV_KEY_AUD_DEFAULT_LANGUAGE;
                break;
            case DroidLogicTvUtils.PREFERRED_AUD_SECONDARY:
                id = DroidLogicTvUtils.TV_KEY_AUD_SECOND_LANGUAGE;
                break;
            case DroidLogicTvUtils.PREFERRED_ATSC_AUD_DEFAULT:
                id = DroidLogicTvUtils.TV_KEY_ATSC_AUD_PREFERRED_LANGUAGE;
                break;
            case DroidLogicTvUtils.PREFERRED_ATSC_AUD_SECONDARY:
                id = DroidLogicTvUtils.TV_KEY_ATSC_AUD_SECOND_LANGUAGE;
                break;
        }
        return id;
    }
    //Preferred language settings end
 }
