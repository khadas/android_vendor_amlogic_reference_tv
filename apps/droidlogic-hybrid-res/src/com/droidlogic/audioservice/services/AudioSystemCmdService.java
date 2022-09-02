/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description:
 *     AMLOGIC AudioSystemCmdService
 */

package com.droidlogic.audioservice.services;

import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;

import android.media.AudioDevicePort;
import android.media.AudioFormat;
import android.media.AudioGain;
import android.media.AudioGainConfig;
import android.media.AudioManager;
import android.media.AudioPatch;
import android.media.AudioPort;
import android.media.AudioPortConfig;
import android.media.AudioRoutesInfo;
import android.media.AudioSystem;
import android.media.IAudioRoutesObserver;
import android.media.IAudioService;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import android.widget.Toast;

import com.droidlogic.app.AudioSystemCmdManager;
import com.droidlogic.app.DroidLogicUtils;
import com.droidlogic.app.OutputModeManager;
import com.droidlogic.app.SystemControlEvent;
import com.droidlogic.app.SystemControlManager;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.TvControlManager;
import com.droidlogic.UEventObserver;

//this service used to call audio system commands
public class AudioSystemCmdService extends Service {
    private static final String TAG = AudioSystemCmdService.class.getSimpleName();
    private static AudioSystemCmdService mAudioSystemCmdService = null;
    private SystemControlEvent mSystemControlEvent;
    private List<Integer> mAudioPathIds = new ArrayList<>();
    private SystemControlManager mSystemControlManager;
    private DtvKitAudioEvent mDtvKitAudioEvent = null;
    private ADtvAudioEvent mADtvAudioEvent = null;
    private AudioManager mAudioManager = null;
    private AudioPatch mAudioPatch = null;
    private Context mContext;
    private int mCurrentIndex = 0;
    private final Object mLock = new Object();
    private final Handler mHandler = new Handler();
    private AudioDevicePort mAudioSource;
    private List<AudioDevicePort> mAudioSink = new ArrayList<>();
    private int mDesiredSamplingRate = 0;
    private int mDesiredChannelMask = AudioFormat.CHANNEL_OUT_DEFAULT;
    private int mDesiredFormat = AudioFormat.ENCODING_DEFAULT;
    private int mCurrentFmt = -1;
    private int mCurrentHasDtvVideo = 0;
    private int mDigitalFormat = 0;
    private int mDtvDemuxIdBase = 25;
    private int mDtvDemuxIdCurrentWork = 0;
    private int mCurSourceType = DroidLogicTvUtils.SOURCE_TYPE_OTHER;
    private TvInputManager mTvInputManager;
    protected TvControlManager mTvControlManager;
    private static final String PATH_AUDIOFORMAT_UEVENT = "/devices/platform/auge_sound";
    private static final String PATH_TXLX_AUDIOFORMAT_UEVENT = "/devices/platform/aml_snd_tv";
    private static final String ACTION_AUDIO_FORMAT_CHANGE = "droidlogic.audioservice.action.AUDIO_FORMAT";
    private static final String AUDIO_FORMAT_KEY = "audio_format";
    private static final String AUDIO_FORMAT_VALUE_KEY = "audio_format_value";

    private static final String PARAM_HAL_AUDIO_OUTPUT_FORMAT_PCM        = "hdmi_format=0";
    private static final String PARAM_HAL_AUDIO_OUTPUT_FORMAT_AUTO       = "hdmi_format=5";
    private static final String PARAM_HAL_AUDIO_OUTPUT_FORMAT_PASSTHROUGH= "hdmi_format=6";
    private static final int DIGITAL_AUDIO_FORMAT_PCM                    = 0;
    private static final int DIGITAL_AUDIO_FORMAT_AUTO                   = 1;
    private static final int DIGITAL_AUDIO_FORMAT_MANUAL                 = 2;
    private static final int DIGITAL_AUDIO_FORMAT_PASSTHROUGH            = 3;

    private final UEventObserver mObserver = new UEventObserver() {
        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            if (DroidLogicUtils.getAudioDebugEnable()) {
                Log.d(TAG, "UEVENT: " + event.toString());
                Log.d(TAG, "DEVPATH: " + event.get("DEVPATH"));
            }

            if ((PATH_AUDIOFORMAT_UEVENT.equals(event.get("DEVPATH", null))) || PATH_TXLX_AUDIOFORMAT_UEVENT.equals(event.get("DEVPATH", null))) {
                String audioFormatStr = event.get("AUDIO_FORMAT", null);
                if (audioFormatStr == null) {
                    Log.e(TAG, "Error! got audio uevent from kernel, but no AUDIO_FORMAT value set!");
                    return;
                }
                if (DroidLogicUtils.getAudioDebugEnable()) {
                    Log.d(TAG, "AUDIO_FORMAT = " + audioFormatStr);
                }
                final int audioFormat = Integer.parseInt(audioFormatStr.substring(audioFormatStr.indexOf("=")+1));
                if (audioFormat < 0) {
                    Log.d(TAG, "ignoring incorrect audio event format:" + audioFormat);
                } else {
                    String extra = covertAudioFormatIndextToString(audioFormat);
                    Intent intent = new Intent(ACTION_AUDIO_FORMAT_CHANGE);
                    intent.putExtra(AUDIO_FORMAT_KEY, extra);
                    intent.putExtra(AUDIO_FORMAT_VALUE_KEY,audioFormat);
                    mContext.sendBroadcast(intent);
                }
            }
        }
    };

    private String covertAudioFormatIndextToString(int audioFormat) {
        String stringValue = " ";
        switch (audioFormat) {
            case 0:
            case 9:
                stringValue = "PCM";
                break;
            case 1:
                stringValue = "DTS Express";
                break;
            case 3:
                stringValue = "DTS";
                break;
            case 5:
            case 8:
                stringValue = "DTS-HD Master Audio";
                break;
            case 6:
                stringValue = "Multi PCM";
                break;
            case 2:
            case 4:
            case 7:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
                stringValue = "Dolby Audio";
                break;
            default:
                Log.w(TAG, "invalid audioFormat value:" + audioFormat);
                break;
        }
        if (DroidLogicUtils.getAudioDebugEnable()) {
            Log.d(TAG, "covertAudioFormatIndextToEnum: audioFormat:" + audioFormat + ", stringVal:" + stringValue);
        }
        return stringValue;
    }

    private final BroadcastReceiver mVolumeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleVolumeChange(context, intent);
        }
    };
    public AudioSystemCmdService() {
        mAudioSystemCmdService = this;
    }

    private boolean mHasStartedDecoder = false;
    private boolean mHasOpenedDecoder = false;
    private boolean mHasReceivedStartDecoderCmd;
    private boolean  mMixAdSupported;
    private boolean  mNotImptTvHardwareInputService = false;
    private boolean mForceManagePatch = false;
    private IAudioService mAudioService;
    private AudioRoutesInfo mCurAudioRoutesInfo;
    private Runnable mHandleAudioSinkUpdatedRunnable;

    private Runnable mHandleTvAudioRunnable;
    private int mDelayTime = 0;

    final IAudioRoutesObserver.Stub mAudioRoutesObserver = new IAudioRoutesObserver.Stub() {
        @Override
        public void dispatchAudioRoutesChanged(final AudioRoutesInfo newRoutes) {
            int mPreDigitalFormat = mDigitalFormat;
            mDigitalFormat = getDigitalFormats();
            Log.i(TAG, "dispatchAudioRoutesChanged cur device:" + newRoutes.mainType +
                    ", pre device:" + mCurAudioRoutesInfo.mainType);
            if (DroidLogicUtils.getAudioDebugEnable()) {
                Log.d(TAG, "dispatchAudioRoutesChanged newRoutes:" + newRoutes.toString());
                Log.d(TAG, "dispatchAudioRoutesChanged preRoutes:" + mCurAudioRoutesInfo.toString());
                Log.d(TAG, "mDigitalFormat "+ mDigitalFormat + "mPreDigitalFormat " + mPreDigitalFormat);
            }

            if (newRoutes.mainType == mCurAudioRoutesInfo.mainType &&
                newRoutes.toString().equals(mCurAudioRoutesInfo.toString())
                && (mDigitalFormat == mPreDigitalFormat)) {
                return;
            }
            if (DroidLogicUtils.isTv()) {
                if (newRoutes.mainType == AudioRoutesInfo.MAIN_HDMI) {
                    Settings.Global.putInt(mContext.getContentResolver(), OutputModeManager.SOUND_OUTPUT_DEVICE, OutputModeManager.SOUND_OUTPUT_DEVICE_ARC);
                } else {
                    Settings.Global.putInt(mContext.getContentResolver(), OutputModeManager.SOUND_OUTPUT_DEVICE, OutputModeManager.SOUND_OUTPUT_DEVICE_SPEAKER);
                }
            }
            mCurAudioRoutesInfo = newRoutes;
            mHasStartedDecoder = false;
            mHandler.removeCallbacks(mHandleAudioSinkUpdatedRunnable);
            mHandleAudioSinkUpdatedRunnable = new Runnable() {
                public void run() {
                    synchronized (mLock) {
                        if (mHasReceivedStartDecoderCmd) {
                            if (mNotImptTvHardwareInputService)
                                handleAudioSinkUpdated();
                            mHasOpenedDecoder = false;
                            reStartAdecDecoderIfPossible();
                            mHasOpenedDecoder = true;
                        }
                    }
                }
            };
            if (mTvInputManager.getHardwareList() == null) {
                mHandler.post(mHandleAudioSinkUpdatedRunnable);
            } else {
                try {
                    mHandler.postDelayed(mHandleAudioSinkUpdatedRunnable,
                        mAudioService.isBluetoothA2dpOn() ? 2500 : 500);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private final class DtvKitAudioEvent implements SystemControlEvent.AudioEventListener {
        @Override
        public void HandleAudioEvent(int cmd, int param1, int param2, int param3) {
            if (mAudioSystemCmdService != null) {
                mAudioSystemCmdService.HandleAudioEvent(cmd, param1, param2, param3, true);
            } else {
                Log.w(TAG, "DtvKitAudioEvent HandleAudioEvent mAudioSystemCmdService is null");
            }
        }
    }

    private final class ADtvAudioEvent implements TvControlManager.AudioEventListener {
        @Override
        public void HandleAudioEvent(int cmd, int param1, int param2) {
            if (mAudioSystemCmdService != null) {
                mAudioSystemCmdService.HandleAudioEvent(cmd, param1, param2, 0,false);
            } else {
                Log.w(TAG, "ADtvAudioEvent HandleAudioEvent mAudioSystemCmdService is null");
            }
        }
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        super.onCreate();
        mContext = getApplicationContext();
        mSystemControlManager = SystemControlManager.getInstance();
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mTvInputManager = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);

        if (DroidLogicUtils.isBuildLivetv()) {
            mADtvAudioEvent = new ADtvAudioEvent();
            mTvControlManager = TvControlManager.getInstance();
            mTvControlManager.SetAudioEventListener(mADtvAudioEvent);
        }

        mSystemControlEvent = SystemControlEvent.getInstance(mContext);
        mDtvKitAudioEvent = new DtvKitAudioEvent();
        mSystemControlEvent.SetAudioEventListener(mDtvKitAudioEvent);
        mSystemControlManager.setListener(mSystemControlEvent);
        IBinder b = ServiceManager.getService(Context.AUDIO_SERVICE);
        mAudioService = IAudioService.Stub.asInterface(b);
        try {
            mCurAudioRoutesInfo = mAudioService.startWatchingRoutes(mAudioRoutesObserver);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        final IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.VOLUME_CHANGED_ACTION);
        filter.addAction(AudioManager.STREAM_MUTE_CHANGED_ACTION);
        mContext.registerReceiver(mVolumeReceiver, filter);
        mObserver.startObserving(PATH_AUDIOFORMAT_UEVENT);
        mObserver.startObserving(PATH_TXLX_AUDIOFORMAT_UEVENT);
        mNotImptTvHardwareInputService = (mTvInputManager.getHardwareList() == null) || (mTvInputManager.getHardwareList().isEmpty());
        Log.d(TAG, "mNotImptTvHardwareInputService:"+ mNotImptTvHardwareInputService + ", mTvInputManager.getHardwareList():" + mTvInputManager.getHardwareList());
        mForceManagePatch =  SystemProperties.getBoolean("vendor.media.dtv.force.manage.patch", false);
        Log.d(TAG, "mForceManagePatch :" + mForceManagePatch);
        mCurrentIndex = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        getContentResolver().registerContentObserver(Settings.Global.getUriFor(OutputModeManager.DB_ID_AUDIO_OUTPUT_DEVICE_ARC_ENABLE),
                false, mAudioOutputParametersObserver);
        mDigitalFormat =  getDigitalFormats();
    }

    private ContentObserver mAudioOutputParametersObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            int currentArcEnable = Settings.Global.getInt(mContext.getContentResolver(), OutputModeManager.DB_ID_AUDIO_OUTPUT_DEVICE_ARC_ENABLE, 0);
            Log.i(TAG, "onChange ARC enable:" + currentArcEnable + " changed");
            if (uri != null && uri.equals(Settings.Global.getUriFor(OutputModeManager.DB_ID_AUDIO_OUTPUT_DEVICE_ARC_ENABLE))) {
                mAudioManager.setHdmiSystemAudioSupported(currentArcEnable != 0);
            }
        }
    };

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void setAdFunction(int cmd, int param1, int param2, int param3) {
        switch (cmd) {
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_SWITCH_ENABLE:
                mAudioManager.setParameters("ad_switch_enable=" + (param1 > 0 ? "1" : "0"));
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_SET_VOLUME:
                mAudioManager.setParameters("dual_decoder_advol_level=" + param1 + "");
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_DUAL_SUPPORT:
                mAudioManager.setParameters("hal_param_dual_dec_support=" + (param1 > 0 ? "1" : "0"));
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_MIX_SUPPORT://Associated audio mixing on/off
                mAudioManager.setParameters("hal_param_dual_dec_support=" + param1);
                mAudioManager.setParameters("hal_param_ad_mix_enable=" + param1);
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_MIX_LEVEL://Associated audio mixing level
                mAudioManager.setParameters("hal_param_dual_dec_mix_level=" + param2);
                break;
            default:
                Log.w(TAG,"setAdFunction unknown  cmd:" + cmd + ", param1:" + param1);
                break;
        }
    }

    public void HandleAudioEvent(int cmd, int param1, int param2, int param3, boolean isDtvkit) {
        if (DroidLogicUtils.getAudioDebugEnable()) {
            Log.i(TAG, "HandleAudioEvent cmd:" + AudioSystemCmdManager.AudioCmdToString(cmd) +
                    ", param1:" + param1 + ", param2:" + param2 + ", param3:" + param3 + ", is " + (isDtvkit ? "" : "not ") + "Dtvkit.");
        }
        if (mAudioManager == null) {
            Log.e(TAG, "HandleAudioEvent mAudioManager is null");
            return;
        }
        int cmd_index = cmd;
        if (param3 != -1) {
            cmd = cmd + (param3 << mDtvDemuxIdBase);
            param1 = param1 + (param3 << mDtvDemuxIdBase);
            param2 = param2 + (param3 << mDtvDemuxIdBase);
        }
        switch (cmd_index) {
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_SET_SPDIF_PROTECTION_MODE:
                mAudioManager.setParameters("hal_param_dtv_spdif_protection_mode=" + param1);
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_SET_DEMUX_INFO:
                //mAudioManager.setParameters("hal_param_dtv_pid=" + param1);
                mAudioManager.setParameters("hal_param_dtv_demux_id=" + param2);
                mAudioManager.setParameters("hal_param_dtv_cmd=" + cmd);
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_SET_SECURITY_MEM_LEVEL:
                mAudioManager.setParameters("hal_param_security_mem_level=" + param1);
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_SET_MEDIA_SYCN_ID:
                mAudioManager.setParameters("hal_param_media_sync_id=" + param1);
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_SET_MEDIA_FIRST_LANG:
                mAudioManager.setParameters("hal_param_dtv_media_first_lang=" + param1);
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_SET_MEDIA_SECOND_LANG:
                mAudioManager.setParameters("hal_param_dtv_media_second_lang=" + param1);
               break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_SET_HAS_VIDEO:
                mCurrentHasDtvVideo = param1;
                mAudioManager.setParameters("hal_param_has_dtv_video=" + param1);
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_START_DECODE:
                if (isDtvkit) {
                    mHasReceivedStartDecoderCmd = true;
                    mHasStartedDecoder = true;
                }
                mCurrentFmt = param1;
                mDtvDemuxIdCurrentWork = param3;
                mAudioManager.setParameters("hal_param_dtv_audio_fmt=" + param1);
                mAudioManager.setParameters("hal_param_dtv_audio_id=" + param2);
                mAudioManager.setParameters("hal_param_dtv_patch_cmd=" + cmd);
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_PAUSE_DECODE:
                mAudioManager.setParameters("hal_param_dtv_patch_cmd=" + cmd);
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_RESUME_DECODE:
                mAudioManager.setParameters("hal_param_dtv_patch_cmd=" + cmd);
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_STOP_DECODE:
                mHasReceivedStartDecoderCmd = false;
                mAudioManager.setParameters("hal_param_dtv_patch_cmd=" + cmd);
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_SET_DECODE_AD:
                mAudioManager.setParameters("hal_param_dtv_sub_audio_fmt=" + param1);
                mAudioManager.setParameters("hal_param_dtv_sub_audio_pid=" + param2);
                mAudioManager.setParameters("hal_param_dtv_patch_cmd=" + cmd);
                Log.d(TAG, "HandleAudioEvent hal_param_dtv_sub_audio_fmt:" + param1 + ", hal_param_dtv_sub_audio_pid:" + param2);
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_SET_VOLUME:
                //left to do
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_SET_MUTE:
                mAudioManager.setParameters("hal_param_tv_mute=" + param1); /* 1:mute, 0:unmute */
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_SET_OUTPUT_MODE:
                //mAudioManager.setParameters("hal_param_dtv_patch_cmd=" + cmd);
                //mAudioManager.setParameters("hal_param_audio_output_mode=" + param1); /* refer to AM_AOUT_OutputMode_t */
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_SET_PRE_GAIN:
                mAudioManager.setParameters("hal_param_dtv_patch_cmd=" + cmd);
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_SET_PRE_MUTE:
                mAudioManager.setParameters("hal_param_dtv_patch_cmd=" + cmd);
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_OPEN_DECODER:
                updateAudioSourceAndAudioSink();
                if (mNotImptTvHardwareInputService)
                    handleAudioSinkUpdated();
                if (DroidLogicUtils.isTv()) {
                    reStartAdecDecoderIfPossible();
                }
                synchronized (mLock) {
                    mAudioManager.setParameters("hal_param_dtv_fmt=" + param1);
                    mAudioManager.setParameters("hal_param_dtv_pid=" + param2);
                }
                mAudioManager.setParameters("hal_param_dtv_patch_cmd=" + cmd);
                mHasOpenedDecoder = true;
                if (mAudioPatch != null) {
                    if (param3 >=0 && !mAudioPathIds.contains(param3)) {
                        mAudioPathIds.add(param3);
                    }
                } else {
                    mAudioPathIds.clear();
                }
                Log.d(TAG, "OPEN_DECODER("+param3+") audio path count: " + mAudioPathIds.size()+","+mAudioPathIds);
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_CLOSE_DECODER://
                mAudioManager.setParameters("hal_param_dtv_patch_cmd=" + cmd);
                if (mAudioPatch != null) {
                    if (param3 >=0) {
                        if (mAudioPathIds.contains(param3)) {
                            mAudioPathIds.remove(Integer.valueOf(param3));
                        } else {
                            Log.d(TAG, "CLOSE_DECODER("+param3+") maybe already closed");
                        }
                    }
                    Log.d(TAG, "CLOSE_DECODER("+param3+") audio path count: " + mAudioPathIds.size()+","+mAudioPathIds);
                    if (mAudioPathIds.isEmpty()) {
                       Log.d(TAG, "ADEC_CLOSE_DECODER mAudioPatch:"
                            + mAudioPatch);
                        mAudioManager.releaseAudioPatch(mAudioPatch);
                        mAudioPatch = null;
                        mAudioSource = null;
                        mHasStartedDecoder = false;
                        mHasOpenedDecoder = false;
                        mMixAdSupported = false;
                    }
                } else {
                    Log.d(TAG, "CLOSE_DECODER("+param3+") audio patch already released");
                    mAudioPathIds.clear();
                }
                break;

            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_SET_MEDIA_PRESENTATION_ID:
                mAudioManager.setParameters("hal_param_dtv_media_presentation_id=" + param1);
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_SET_AUDIO_PATCH_MANAGE_MODE:
                boolean isDvbPlayback = (param1 == 0);
                int forceManagePatchMode = param2;
                if (param3 != 0) {
                    isDvbPlayback = ((param1 -(param3 << mDtvDemuxIdBase)) == 0);
                    forceManagePatchMode = (param2 -(param3 << mDtvDemuxIdBase));
                }
                boolean hasTif = !(mTvInputManager.getHardwareList() == null || mTvInputManager.getHardwareList().isEmpty());

                if (mForceManagePatch) {
                    mNotImptTvHardwareInputService = true;
                } else if (forceManagePatchMode == 0) {
                    // force disable
                    mNotImptTvHardwareInputService = false;
                } else if (forceManagePatchMode == 1) {
                    // force enable
                    mNotImptTvHardwareInputService = true;
                } else {
                    // auto
                    if (hasTif && isDvbPlayback) {
                        mNotImptTvHardwareInputService = false;
                    } else {
                        mNotImptTvHardwareInputService = true;
                    }
                }
                Log.i(TAG, "mForceManagePatch: " + mForceManagePatch
                    + ", isDvbPlayback: " + isDvbPlayback
                    + ", forceManagePatchMode: " + forceManagePatchMode
                    + ", hasTif: " + hasTif
                    + ", so mNotImptTvHardwareInputService set to " + mNotImptTvHardwareInputService);
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_SWITCH_ENABLE:
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_SET_VOLUME:
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_DUAL_SUPPORT:
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_MIX_SUPPORT:
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_AD_MIX_LEVEL:
                setAdFunction(cmd_index, param1, param2, param3);
                break;
            case AudioSystemCmdManager.AUDIO_SERVICE_CMD_SET_TSPLAYER_CLIENT_DIED:
                releaseTvTunerAudioPatch();
                mAudioPatch = null;
                mAudioSource = null;
                mHasStartedDecoder = false;
                mHasOpenedDecoder = false;
                mMixAdSupported = false;
                break;
            default:
                Log.w(TAG,"HandleAudioEvent unknown audio cmd:" + cmd);
                break;
        }
    }

    private void setAudioPortGain() {
        mCurrentIndex = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        updateAudioSourceAndAudioSink();
        int curInputSrcDev = AudioManager.DEVICE_NONE;
        if (DroidLogicTvUtils.SOURCE_TYPE_ADTV == mCurSourceType ||
                DroidLogicTvUtils.SOURCE_TYPE_ATV == mCurSourceType ||
                DroidLogicTvUtils.SOURCE_TYPE_DTV == mCurSourceType) {
            curInputSrcDev = AudioManager.DEVICE_IN_TV_TUNER;
        } else if (DroidLogicTvUtils.SOURCE_TYPE_AV1 == mCurSourceType || DroidLogicTvUtils.SOURCE_TYPE_AV2 == mCurSourceType) {
            curInputSrcDev = AudioManager.DEVICE_IN_LINE;
        } else if (mCurSourceType >= DroidLogicTvUtils.SOURCE_TYPE_HDMI1 && mCurSourceType <= DroidLogicTvUtils.SOURCE_TYPE_HDMI4) {
            curInputSrcDev = AudioManager.DEVICE_IN_HDMI;
        } else {
            if (mNotImptTvHardwareInputService) {
                 Log.i(TAG,"NON TV setAudioPortGain force DEVICE_IN_TV_TUNER");
                 curInputSrcDev = AudioManager.DEVICE_IN_TV_TUNER;
            } else {
                return;
            }
        }
        mAudioSource = findAudioDevicePort(curInputSrcDev, "");
        if (mAudioSource != null && mAudioSource.gains().length > 0) {
            AudioGain sourceGain = mAudioSource.gains()[0];
            int gainValueMb = (int)(100 * AudioSystem.getStreamVolumeDB(AudioManager.STREAM_MUSIC, mCurrentIndex, AudioManager.DEVICE_OUT_SPEAKER));
            int[] gainValues = new int[]{gainValueMb};
            AudioGainConfig sourceGainConfig = sourceGain.buildConfig(AudioGain.MODE_JOINT, 0, gainValues, 0);
            if (DroidLogicUtils.getAudioDebugEnable()) {
                Log.i(TAG, "setAudioPortGain gainValueMb:" + gainValueMb + ", mCurSourceType:" +
                        DroidLogicTvUtils.sourceTypeToString(mCurSourceType) + ", curInputSrcDev:" + Integer.toHexString(curInputSrcDev));
            }
            mAudioManager.setAudioPortGain(mAudioSource, sourceGainConfig);
        }
    }

    private void releaseTvTunerAudioPatch() {
         ArrayList<AudioPatch> patches = new ArrayList<>();
         int result = mAudioManager.listAudioPatches(patches);
         if (result != AudioManager.SUCCESS) {
             throw new RuntimeException("listAudioPatches failed with code " + result);
         }
         Log.d(TAG, "patches:" + patches.toString());
        // Look for a patch that matches the provided user side handle
         int path_id = 0;
         for (AudioPatch patch : patches) {
             for (AudioPortConfig source : patch.sources()) {
                 if (source.port() instanceof AudioDevicePort) {
                     AudioDevicePort port = (AudioDevicePort)source.port();
                     if (port.type() == AudioManager.DEVICE_IN_TV_TUNER) {
                         // Found it!
                         HandleAudioEvent(AudioSystemCmdManager.AUDIO_SERVICE_CMD_CLOSE_DECODER, 0, 0, path_id, false);
                         path_id++;
                         result = AudioManager.releaseAudioPatch(patch);
                         if (result != AudioManager.SUCCESS) {
                             throw new RuntimeException("releaseAudioPatch failed with code " + result);
                         }
                         continue;
                     }
                 }
            }
        }

         // If we didn't find a match, then something went awry, but it's probably not fatal...
         mAudioPathIds.clear();
         Log.i(TAG, "releaseAudioPatch finished ");
     }

    private void updateAudioSourceAndAudioSink() {
        mAudioSource = findAudioDevicePort(AudioManager.DEVICE_IN_TV_TUNER, "");
        findAudioSinkFromAudioPolicy(mAudioSink);
    }

    private void handleVolumeChange(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case AudioManager.VOLUME_CHANGED_ACTION: {
                int streamType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
                if (streamType != AudioManager.STREAM_MUSIC) {
                    return;
                }
                boolean mAudioHalControlVolumeEnable = mAudioManager.getParameters(HAL_PARAM_HAL_CONTROL_VOL_EN).equals(HAL_PARAM_HAL_CONTROL_VOL_EN + "=1");
                if (!mAudioHalControlVolumeEnable) {
                    showPassthroughWarning();
                }
                int index = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, 0);
                if (index == mCurrentIndex) {
                    return;
                }
                if (DroidLogicUtils.getAudioDebugEnable()) {
                    Log.d(TAG, "handleVolumeChange VOLUME_CHANGED index:" + index);
                }
                mCurrentIndex = index;
                break;
            }
            case AudioManager.STREAM_MUTE_CHANGED_ACTION: {
                int streamType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
                if (streamType != AudioManager.STREAM_MUSIC) {
                    return;
                }
                if (DroidLogicUtils.getAudioDebugEnable()) {
                    Log.d(TAG, "handleVolumeChange MUTE_CHANGED");
                }
                break;
            }
            default:
                Slog.w(TAG, "handleVolumeChange action:" + action + ", Unrecognized intent: " + intent);
                return;
        }

        setAudioPortGain();
    }

    private boolean mShowingPassthroughHint = false;

    private static final String HAL_PARAM_HAL_CONTROL_VOL_EN = "hal_param_hal_control_vol_en";
    private void showPassthroughWarning() {
        if (mShowingPassthroughHint) {
            Slog.d(TAG, "on need to show other passthrough hint");
            return;
        }
        mShowingPassthroughHint = true;
        mHandler.post(()->{
            String hint = "To adjust volume, enable CEC control(Settings > Device Preferences > Display &amp; Sound > HDMI CEC) or adjust the TV remote control.";
            Toast toast = Toast.makeText(mContext, hint, Toast.LENGTH_LONG);
            toast.addCallback(new Toast.Callback() {
                public void onToastHidden() {
                    mShowingPassthroughHint = false;
                }
            });
            toast.show();
        });
    }

    private void handleAudioSinkUpdated() {
        synchronized (mLock) {
            updateAudioConfigLocked();
        }
    }

    private boolean updateAudioSinkLocked() {
        List<AudioDevicePort> previousSink = mAudioSink;
        mAudioSink = new ArrayList<>();
        findAudioSinkFromAudioPolicy(mAudioSink);

        // Returns true if mAudioSink and previousSink differs.
        if (mAudioSink.size() != previousSink.size()) {
            return true;
        }
        previousSink.removeAll(mAudioSink);
        return !previousSink.isEmpty();
    }

    private void findAudioSinkFromAudioPolicy(List<AudioDevicePort> sinks) {
        sinks.clear();
        ArrayList<AudioPort> audioPorts = new ArrayList<>();
        if (AudioManager.listAudioPorts(audioPorts) != AudioManager.SUCCESS) {
            Log.w(TAG, "findAudioSinkFromAudioPolicy listAudioPorts failed");
            return;
        }
        int sinkDevice = mAudioManager.getDevicesForStream(AudioManager.STREAM_MUSIC);
        AudioDevicePort port;
        for (AudioPort audioPort : audioPorts) {
            if (audioPort instanceof AudioDevicePort) {
                port = (AudioDevicePort)audioPort;
                if ((port.type() & sinkDevice) != 0 && mAudioManager.isOutputDevice(port.type())) {
                    sinks.add(port);
                }
            }
        }
    }

    private AudioDevicePort findAudioDevicePort(int type, String address) {
        if (type == AudioManager.DEVICE_NONE) {
            return null;
        }
        ArrayList<AudioPort> audioPorts = new ArrayList<>();
        if (AudioManager.listAudioPorts(audioPorts) != AudioManager.SUCCESS) {
            Log.w(TAG, "findAudioDevicePort listAudioPorts failed");
            return null;
        }
        AudioDevicePort port;
        for (AudioPort audioPort : audioPorts) {
            if (audioPort instanceof AudioDevicePort) {
                port = (AudioDevicePort)audioPort;
                if (port.type() == type && port.address().equals(address)) {
                    return port;
                }
            }
        }
        return null;
    }

    private void reStartAdecDecoderIfPossible() {
        Log.i(TAG, "reStartAdecDecoderIfPossible HasOpenedDecoder:" + mHasOpenedDecoder +
                   " StartDecoderCmd:" + mHasReceivedStartDecoderCmd +
                   ", mMixAdSupported:" + mMixAdSupported);
        if (!mHasOpenedDecoder) {
            setAudioPortGain();
            mAudioManager.setParameters("hal_param_tuner_in=dtv");
            if (mHasReceivedStartDecoderCmd) {
                mAudioManager.setParameters("hal_param_dtv_audio_fmt="+mCurrentFmt);
                mAudioManager.setParameters("hal_param_has_dtv_video="+mCurrentHasDtvVideo);
                int cmd = AudioSystemCmdManager.AUDIO_SERVICE_CMD_START_DECODE + (mDtvDemuxIdCurrentWork << mDtvDemuxIdBase);
                mAudioManager.setParameters("hal_param_dtv_patch_cmd=" + cmd);
                mHasStartedDecoder = true;
             }
        }
    }

    private void updateAudioConfigLocked() {

        boolean sinkUpdated = updateAudioSinkLocked();

        if (mAudioSource == null || mAudioSink.isEmpty()) {
            Log.i(TAG, "updateAudioConfigLocked return, mAudioSource:" +
                    mAudioSource + ", mAudioSink empty:" +  mAudioSink.isEmpty());
            if (mAudioPatch != null) {
                mAudioManager.releaseAudioPatch(mAudioPatch);
                mAudioPatch = null;
                mAudioPathIds.clear();
                mHasStartedDecoder = false;
            }
            return;
        }

        AudioPortConfig sourceConfig = mAudioSource.activeConfig();
        List<AudioPortConfig> sinkConfigs = new ArrayList<>();
        AudioPatch[] audioPatchArray = new AudioPatch[] { mAudioPatch };
        boolean shouldRecreateAudioPatch = sinkUpdated;
        boolean shouldApplyGain = false;

        Log.i(TAG, "updateAudioConfigLocked sinkUpdated:" + sinkUpdated + ", mAudioPatch is empty:"
                + (mAudioPatch == null));
         //mAudioPatch should not be null when current hardware is active.
        if (mAudioPatch == null) {
            shouldRecreateAudioPatch = true;
            mHasStartedDecoder = false;
            mAudioPathIds.clear();
        }

        for (AudioDevicePort audioSink : mAudioSink) {
            AudioPortConfig sinkConfig = audioSink.activeConfig();
            int sinkSamplingRate = mDesiredSamplingRate;
            int sinkChannelMask = mDesiredChannelMask;
            int sinkFormat = mDesiredFormat;
            // If sinkConfig != null and values are set to default,
            // fill in the sinkConfig values.
            if (sinkConfig != null) {
                if (sinkSamplingRate == 0) {
                    sinkSamplingRate = sinkConfig.samplingRate();
                }
                if (sinkChannelMask == AudioFormat.CHANNEL_OUT_DEFAULT) {
                    sinkChannelMask = sinkConfig.channelMask();
                }
                if (sinkFormat == AudioFormat.ENCODING_DEFAULT) {
                    sinkFormat = sinkConfig.format();
                }
            }

            if (sinkConfig == null
                    || sinkConfig.samplingRate() != sinkSamplingRate
                    || sinkConfig.channelMask() != sinkChannelMask
                    || sinkConfig.format() != sinkFormat) {
                // Check for compatibility and reset to default if necessary.
                if (!intArrayContains(audioSink.samplingRates(), sinkSamplingRate)
                        && audioSink.samplingRates().length > 0) {
                    sinkSamplingRate = audioSink.samplingRates()[0];
                }
                if (!intArrayContains(audioSink.channelMasks(), sinkChannelMask)) {
                    sinkChannelMask = AudioFormat.CHANNEL_OUT_DEFAULT;
                }
                if (!intArrayContains(audioSink.formats(), sinkFormat)) {
                    sinkFormat = AudioFormat.ENCODING_DEFAULT;
                }
                sinkConfig = audioSink.buildConfig(sinkSamplingRate, sinkChannelMask,
                        sinkFormat, null);
                shouldRecreateAudioPatch = true;
            }
            sinkConfigs.add(sinkConfig);
        }

        // sinkConfigs.size() == mAudioSink.size(), and mAudioSink is guaranteed to be
        // non-empty at the beginning of this method.
        AudioPortConfig sinkConfig = sinkConfigs.get(0);
        if (sourceConfig == null) {
            int sourceSamplingRate = 0;
            if (intArrayContains(mAudioSource.samplingRates(), sinkConfig.samplingRate())) {
                sourceSamplingRate = sinkConfig.samplingRate();
            } else if (mAudioSource.samplingRates().length > 0) {
                // Use any sampling rate and hope audio patch can handle resampling...
                sourceSamplingRate = mAudioSource.samplingRates()[0];
            }
            int sourceChannelMask = AudioFormat.CHANNEL_IN_DEFAULT;
            for (int inChannelMask : mAudioSource.channelMasks()) {
                if (AudioFormat.channelCountFromOutChannelMask(sinkConfig.channelMask())
                        == AudioFormat.channelCountFromInChannelMask(inChannelMask)) {
                    sourceChannelMask = inChannelMask;
                    break;
                }
            }
            int sourceFormat = AudioFormat.ENCODING_DEFAULT;
            if (intArrayContains(mAudioSource.formats(), sinkConfig.format())) {
                sourceFormat = sinkConfig.format();
            }
            sourceConfig = mAudioSource.buildConfig(sourceSamplingRate, sourceChannelMask,
                    sourceFormat, null);

            if (mAudioPatch != null) {
                shouldApplyGain = true;
            } else {
                mAudioPathIds.clear();
                shouldRecreateAudioPatch = true;
            }
        }
        Log.i(TAG, "updateAudioConfigLocked recreatePatch:" + shouldRecreateAudioPatch);
        if (shouldRecreateAudioPatch) {
            if (mAudioPatch != null) {
                mAudioManager.releaseAudioPatch(mAudioPatch);
                audioPatchArray[0] = null;
                mHasStartedDecoder = false;
            }
            mAudioManager.createAudioPatch(
                    audioPatchArray,
                    new AudioPortConfig[] { sourceConfig },
                    sinkConfigs.toArray(new AudioPortConfig[sinkConfigs.size()]));
            mAudioPatch = audioPatchArray[0];
            Log.d(TAG,"createAudioPatch end" + mAudioPatch);
            setAudioPortGain();
        }
    }

    private static boolean intArrayContains(int[] array, int value) {
        for (int element : array) {
            if (element == value) return true;
        }
        return false;
    }
    private int getDigitalFormats() {
        Log.d(TAG, "getDigitalFormats value:" + mAudioManager.getParameters("hdmi_format"));
        if (mAudioManager.getParameters("hdmi_format").contains(PARAM_HAL_AUDIO_OUTPUT_FORMAT_PCM)) {
          return DIGITAL_AUDIO_FORMAT_PCM;
        } else if (mAudioManager.getParameters("hdmi_format").contains(PARAM_HAL_AUDIO_OUTPUT_FORMAT_AUTO)){
          return DIGITAL_AUDIO_FORMAT_AUTO;
        } else if (mAudioManager.getParameters("hdmi_format").contains(PARAM_HAL_AUDIO_OUTPUT_FORMAT_PASSTHROUGH))  {
          return DIGITAL_AUDIO_FORMAT_PASSTHROUGH;
        } else {
            Log.d(TAG, "getDigitalFormats value:" + mAudioManager.getParameters("hdmi_format"));
            return mDigitalFormat;
        }

    }
    private final IAudioSystemCmdService.Stub mBinder = new IAudioSystemCmdService.Stub() {
        public void setParameters(String arg) {
            if (DroidLogicUtils.getAudioDebugEnable()) {
                Log.d(TAG, "setParameters arg:" + arg);
            }
            mAudioManager.setParameters(arg);
        }

        public String getParameters(String arg) {
            String value = mAudioManager.getParameters(arg);
            if (DroidLogicUtils.getAudioDebugEnable()) {
                Log.d(TAG, "getParameters arg:" + arg +", value:" + value);
            }
            return value;
        }

        public void handleAdtvAudioEvent(int cmd, int param1, int param2) {
            HandleAudioEvent(cmd, param1, param2, 0, false);
        }

        public void updateAudioPortGain(int sourceType) {
            if (DroidLogicUtils.getAudioDebugEnable()) {
                Log.d(TAG, "updateAudioPortGain source type:" + DroidLogicTvUtils.sourceTypeToString(sourceType) + "[" + sourceType + "]");
            }
            mCurSourceType = sourceType;
            setAudioPortGain();
        }

        public void openTvAudio(int sourceType) {
            Log.i(TAG, "openTvAudio set source type:" + DroidLogicTvUtils.sourceTypeToString(sourceType));
            mHandler.removeCallbacks(mHandleTvAudioRunnable);
            if (sourceType == DroidLogicTvUtils.SOURCE_TYPE_ATV) {
                mAudioManager.setParameters("hal_param_tuner_in=atv");
            } else if (sourceType == DroidLogicTvUtils.SOURCE_TYPE_DTV) {
                mAudioManager.setParameters("hal_param_tuner_in=dtv");
            } else {
                Log.w(TAG, "openTvAudio unsupported source type:" + sourceType);
            }
            mCurSourceType = sourceType;
        }

        public void closeTvAudio() {
            Log.i(TAG, "closeTvAudio");
            mHandler.removeCallbacks(mHandleTvAudioRunnable);
            mCurSourceType = DroidLogicTvUtils.SOURCE_TYPE_OTHER;
            mCurrentFmt = -1;
            mCurrentHasDtvVideo = -1;
        }
    };
}
