/*
 * Copyright (c) 2019 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description: JAVA file
 */

package com.droidlogic.app.tv;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.droidlogic.tvinput.services.IAudioEffectsService;

public class AudioEffectManager {
    private String TAG = "AudioEffectManager";
    private IAudioEffectsService mAudioEffectService = null;
    private Context mContext;

    private boolean mDebug = true;
    private int RETRY_MAX = 10;
    //soundmode set by eq or dap module, first use dap if exist
    public static final  int DAP_MODULE             = 0;
    public static final  int EQ_MODULE              = 1;
    /* Modes of sound effects */
    public static final int MODE_STANDARD           = 0;
    public static final int MODE_MUSIC              = 1;
    public static final int MODE_NEWS               = 2;
    public static final int MODE_THEATER            = 3;
    public static final int MODE_GAME               = 4;
    public static final int MODE_CUSTOM             = 5;

    /* Modes of sound effects */
    public static final int EXTEND_MODE_STANDARD    = 0;
    public static final int EXTEND_MODE_MUSIC       = 1;
    public static final int EXTEND_MODE_NEWS        = 2;
    public static final int EXTEND_MODE_THEATER     = 3;
    public static final int EXTEND_MODE_GAME        = 4;
    public static final int EXTEND_MODE_CUSTOM      = 5;

    //surround value definition
    public static final int SURROUND_ON             = 0;
    public static final int SURROUND_OFF            = 1;
    //bass boost value definition
    public static final int BASS_BOOST_ON           = 0;
    public static final int BASS_BOOST_OFF          = 1;
    //amlogic add
    public static final int SPDIF_OFF               = 0;
    public static final int SPDIF_PCM               = 1;
    public static final int SPDIF_RAW               = 2;
    public static final int SPDIF_AUTO              = 3;

    public static final int SOUND_SPEAKER_OUT       = 0;
    public static final int SOUND_SPDIF_OUT         = 1;
    public static final int SOUND_ARC_OUT           = 2;

    private static final String AUDIO_SOUND_MODE            = "audio_sound_mode";
    private static final String AUDIO_TREBLE_LEVEL          = "audio_treble_level";
    private static final String AUDIO_BASS_LEVEL            = "audio_bass_level";
    private static final String AUDIO_BALANCE_LEVEL         = "audio_balance_level";
    private static final String AUDIO_SURROUND_MODE         = "audio_suround_mode";
    private static final String AUDIO_DIALOG_CLARITY_MODE   = "audio_dialog_charity_mode";
    private static final String AUDIO_BASS_BOOST_MODE       = "audio_bass_boost_mode";
    private static final String AUDIO_SOUND_EFFECT_BAND1    = "audio_sound_effect_band1";
    private static final String AUDIO_SOUND_EFFECT_BAND2    = "audio_sound_effect_band2";
    private static final String AUDIO_SOUND_EFFECT_BAND3    = "audio_sound_effect_band3";
    private static final String AUDIO_SOUND_EFFECT_BAND4    = "audio_sound_effect_band4";
    private static final String AUDIO_SOUND_EFFECT_BAND5    = "audio_sound_effect_band5";

    // defined index ID for DB storage
    public static final String DB_ID_SOUND_EFFECT_BASS                          = "db_id_sound_effect_bass";
    public static final String DB_ID_SOUND_EFFECT_TREBLE                        = "db_id_sound_effect_treble";
    public static final String DB_ID_SOUND_EFFECT_BALANCE                       = "db_id_sound_effect_balance";
    public static final String DB_ID_SOUND_EFFECT_DIALOG_CLARITY                = "db_id_sound_effect_dialog_clarity";
    public static final String DB_ID_SOUND_EFFECT_SURROUND                      = "db_id_sound_effect_surround";
    public static final String DB_ID_SOUND_EFFECT_TRUBASS                       = "db_id_sound_effect_tru_bass";
    public static final String DB_ID_SOUND_EFFECT_SOUND_MODE                    = "db_id_sound_effect_sound_mode";
    public static final String DB_ID_SOUND_EFFECT_SOUND_MODE_TYPE               = "db_id_sound_effect_sound_mode_type";
    public static final String DB_ID_SOUND_EFFECT_SOUND_MODE_TYPE_DAP           = "db_id_sound_effect_sound_mode_type_dap";
    public static final String DB_ID_SOUND_EFFECT_SOUND_MODE_TYPE_EQ            = "db_id_sound_effect_sound_mode_type_eq";
    public static final String DB_ID_SOUND_EFFECT_SOUND_MODE_DAP_VALUE          = "db_id_sound_effect_sound_mode_dap";
    public static final String DB_ID_SOUND_EFFECT_SOUND_MODE_EQ_VALUE           = "db_id_sound_effect_sound_mode_eq";
    public static final String DB_ID_SOUND_EFFECT_BAND1                         = "db_id_sound_effect_band1";
    public static final String DB_ID_SOUND_EFFECT_BAND2                         = "db_id_sound_effect_band2";
    public static final String DB_ID_SOUND_EFFECT_BAND3                         = "db_id_sound_effect_band3";
    public static final String DB_ID_SOUND_EFFECT_BAND4                         = "db_id_sound_effect_band4";
    public static final String DB_ID_SOUND_EFFECT_BAND5                         = "db_id_sound_effect_band5";
    public static final String DB_ID_SOUND_EFFECT_AGC_ENABLE                    = "db_id_sound_effect_agc_on";
    public static final String DB_ID_SOUND_EFFECT_AGC_MAX_LEVEL                 = "db_id_sound_effect_agc_level";
    public static final String DB_ID_SOUND_EFFECT_AGC_ATTRACK_TIME              = "db_id_sound_effect_agc_attrack";
    public static final String DB_ID_SOUND_EFFECT_AGC_RELEASE_TIME              = "db_id_sound_effect_agc_release";
    public static final String DB_ID_SOUND_EFFECT_AGC_SOURCE_ID                 = "db_id_sound_avl_source_id";
    public static final String DB_ID_SOUND_EFFECT_VIRTUALX_MODE                 = "db_id_sound_effect_virtualx_mode";
    public static final String DB_ID_SOUND_EFFECT_TREVOLUME_HD                  = "db_id_sound_effect_truvolume_hd";

    //set id
    public static final int SET_BASS                                = 0;
    public static final int SET_TREBLE                              = 1;
    public static final int SET_BALANCE                             = 2;
    public static final int SET_DIALOG_CLARITY_MODE                 = 3;
    public static final int SET_SURROUND_ENABLE                     = 4;
    public static final int SET_TRUBASS_ENABLE                      = 5;
    public static final int SET_SOUND_MODE                          = 6;
    public static final int SET_EFFECT_BAND1                        = 7;
    public static final int SET_EFFECT_BAND2                        = 8;
    public static final int SET_EFFECT_BAND3                        = 9;
    public static final int SET_EFFECT_BAND4                        = 10;
    public static final int SET_EFFECT_BAND5                        = 11;
    public static final int SET_AGC_ENABLE                          = 12;
    public static final int SET_AGC_MAX_LEVEL                       = 13;
    public static final int SET_AGC_ATTRACK_TIME                    = 14;
    public static final int SET_AGC_RELEASE_TIME                    = 15;
    public static final int SET_AGC_SOURCE_ID                       = 16;
    public static final int SET_VIRTUAL_SURROUND                    = 17;
    public static final int SET_VIRTUALX_MODE                       = 18;
    public static final int SET_TRUVOLUME_HD_ENABLE                 = 19;

    public static final int EFFECT_BASS_DEFAULT     = 50;   // 0 - 100
    public static final int EFFECT_TREBLE_DEFAULT   = 50;   // 0 - 100
    public static final int EFFECT_BALANCE_DEFAULT  = 50;   // 0 - 100

    /* VirtualX effect mode */
    public static final int SOUND_EFFECT_VIRTUALX_MODE_OFF          = 0;
    public static final int SOUND_EFFECT_VIRTUALX_MODE_BASS         = 1;
    public static final int SOUND_EFFECT_VIRTUALX_MODE_FULL         = 2;

    /* init value for first boot */
    public static final int SOUND_EFFECT_SURROUND_ENABLE_DEFAULT    = 0;        // OFF
    public static final int SOUND_EFFECT_DIALOG_CLARITY_ENABLE_DEFAULT= 0;      // OFF
    public static final int SOUND_EFFECT_TRUBASS_ENABLE_DEFAULT     = 0;        // OFF
    public static final int SOUND_EFFECT_VIRTUALX_MODE_DEFAULT      = 0;        // OFF
    public static final int SOUND_EFFECT_TRUVOLUME_HD_ENABLE_DEFAULT= 0;        // OFF

    private static AudioEffectManager mInstance;

    public static AudioEffectManager getInstance(Context context) {
        if (null == mInstance) {
            mInstance = new AudioEffectManager(context);
        }
        return mInstance;
    }

    public AudioEffectManager(Context context) {
        mContext = context;
        LOGI("construction AudioEffectManager");
        getService();
    }

    private void LOGI(String msg) {
        if (mDebug) Log.i(TAG, msg);
    }

    private void getService() {
        LOGI("=====[getService]");
        int retry = RETRY_MAX;
        boolean mIsBind = false;
        try {
            synchronized (this) {
                while (true) {
                    Intent intent = new Intent();
                    intent.setAction("com.droidlogic.tvinput.services.AudioEffectsService");
                    intent.setPackage("com.droidlogic.tvinput");
                    mIsBind = mContext.bindService(intent, serConn, mContext.BIND_AUTO_CREATE);
                    LOGI("=====[getService] mIsBind: " + mIsBind + ", retry:" + retry);
                    if (mIsBind || retry <= 0) {
                        break;
                    }
                    retry --;
                    Thread.sleep(500);
                }
            }
        } catch (InterruptedException e){}
    }

    private ServiceConnection serConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            LOGI("[onServiceDisconnected]mAudioEffectService: " + mAudioEffectService);
            mAudioEffectService = null;

        }
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mAudioEffectService = IAudioEffectsService.Stub.asInterface(service);
            LOGI("SubTitleClient.onServiceConnected()..mAudioEffectService: " + mAudioEffectService);
        }
    };

    public void unBindService() {
        mContext.unbindService(serConn);
    }

    public void createAudioEffects() {
        try {
            mAudioEffectService.createAudioEffects();
        } catch (RemoteException e) {
            Log.e(TAG, "createAudioEffects failed:" + e);
        }
    }

    public boolean isSupportVirtualX() {
        try {
            return mAudioEffectService.isSupportVirtualX();
        } catch (RemoteException e) {
            Log.e(TAG, "isSupportVirtualX failed:" + e);
        }
        return false;
    }

    public void setDtsVirtualXMode(int virtalXMode) {
        try {
            mAudioEffectService.setDtsVirtualXMode(virtalXMode);
        } catch (RemoteException e) {
            Log.e(TAG, "setDtsVirtualXMode failed:" + e);
        }
    }

    public int getDtsVirtualXMode() {
        try {
            return mAudioEffectService.getDtsVirtualXMode();
        } catch (RemoteException e) {
            Log.e(TAG, "getDtsVirtualXMode failed:" + e);
        }
        return -1;
    }

    public void setDtsTruVolumeHdEnable(boolean enable) {
        try {
            mAudioEffectService.setDtsTruVolumeHdEnable(enable);
        } catch (RemoteException e) {
            Log.e(TAG, "setDtsTruVolumeHdEnable failed:" + e);
        }
    }

    public boolean getDtsTruVolumeHdEnable() {
        try {
            return mAudioEffectService.getDtsTruVolumeHdEnable();
        } catch (RemoteException e) {
            Log.e(TAG, "getDtsTruVolumeHdEnable failed:" + e);
        }
        return false;
    }

    public int getSoundModeStatus() {
        try {
            return mAudioEffectService.getSoundModeStatus();
        } catch (RemoteException e) {
            Log.e(TAG, "getSoundModeStatus failed:" + e);
        }
        return -1;
    }

    public int getSoundModule() {
        try {
            return mAudioEffectService.getSoundModule();
        } catch (RemoteException e) {
            Log.e(TAG, "getSoundModule failed:" + e);
        }
        return -1;
    }

    public int getTrebleStatus() {
        try {
            return mAudioEffectService.getTrebleStatus();
        } catch (RemoteException e) {
            Log.e(TAG, "getTrebleStatus failed:" + e);
        }
        return -1;
    }

    public int getBassStatus() {
        try {
            return mAudioEffectService.getBassStatus();
        } catch (RemoteException e) {
            Log.e(TAG, "getBassStatus failed:" + e);
        }
        return -1;
    }
    public int getBalanceStatus() {
        try {
            return mAudioEffectService.getBalanceStatus();
        } catch (RemoteException e) {
            Log.e(TAG, "getBalanceStatus failed:" + e);
        }
        return -1;
    }

    public boolean getAgcEnableStatus() {
        try {
            return mAudioEffectService.getAgcEnableStatus();
        } catch (RemoteException e) {
            Log.e(TAG, "getAgcEnableStatus failed:" + e);
        }
        return false;
    }

    public int getAgcMaxLevelStatus() {
        try {
            return mAudioEffectService.getAgcMaxLevelStatus();
        } catch (RemoteException e) {
            Log.e(TAG, "getAgcMaxLevelStatus failed:" + e);
        }
        return -1;
    }

    public int getAgcAttrackTimeStatus() {
        try {
            return mAudioEffectService.getAgcAttrackTimeStatus();
        } catch (RemoteException e) {
            Log.e(TAG, "getAgcAttrackTimeStatus failed:" + e);
        }
        return -1;
    }

    public int getAgcReleaseTimeStatus() {
        try {
            return mAudioEffectService.getAgcReleaseTimeStatus();
        } catch (RemoteException e) {
            Log.e(TAG, "getAgcReleaseTimeStatus failed:" + e);
        }
        return -1;
    }

    public int getAgcSourceIdStatus() {
        try {
            return mAudioEffectService.getAgcSourceIdStatus();
        } catch (RemoteException e) {
            Log.e(TAG, "getAgcSourceIdStatus failed:" + e);
        }
        return -1;
    }

    public int getVirtualSurroundStatus() {
        try {
            return mAudioEffectService.getVirtualSurroundStatus();
        } catch (RemoteException e) {
            Log.e(TAG, "getVirtualSurroundStatus failed:" + e);
        }
        return -1;
    }

    public void setSoundMode(int mode) {
        try {
            mAudioEffectService.setSoundMode(mode);
        } catch (RemoteException e) {
            Log.e(TAG, "setSoundMode failed:" + e);
        }
    }

    public void setSoundModeByObserver(int mode) {
        try {
            mAudioEffectService.setSoundModeByObserver(mode);
        } catch (RemoteException e) {
            Log.e(TAG, "setSoundModeByObserver failed:" + e);
        }
    }

    public void setDifferentBandEffects(int bandNumber, int value, boolean bNeedSave) {
        try {
            mAudioEffectService.setDifferentBandEffects(bandNumber, value, bNeedSave);
        } catch (RemoteException e) {
            Log.e(TAG, "setDifferentBandEffects failed:" + e);
        }
    }

    public void setTreble(int step) {
        try {
            mAudioEffectService.setTreble(step);
        } catch (RemoteException e) {
            Log.e(TAG, "setTreble failed:" + e);
        }
    }

    public void setBass(int step) {
        try {
            mAudioEffectService.setBass(step);
        } catch (RemoteException e) {
            Log.e(TAG, "setBass failed:" + e);
        }
    }

    public void setBalance(int step) {
        try {
            mAudioEffectService.setBalance(step);
        } catch (RemoteException e) {
            Log.e(TAG, "setBalance failed:" + e);
        }
    }

    public void setSurroundEnable(boolean enable) {
        try {
            mAudioEffectService.setSurroundEnable(enable);
        } catch (RemoteException e) {
            Log.e(TAG, "setSurroundEnable failed:" + e);
        }
    }

    public boolean getSurroundEnable() {
        try {
            return mAudioEffectService.getSurroundEnable();
        } catch (RemoteException e) {
            Log.e(TAG, "getSurroundEnable failed:" + e);
        }
        return false;
    }

    public void setDialogClarityMode(int mode) {
        try {
            mAudioEffectService.setDialogClarityMode(mode);
        } catch (RemoteException e) {
            Log.e(TAG, "setDialogClarityEnable failed:" + e);
        }
    }

    public int getDialogClarityMode() {
        try {
            return mAudioEffectService.getDialogClarityMode();
        } catch (RemoteException e) {
            Log.e(TAG, "getDialogClarityEnable failed:" + e);
        }
        return -1;
    }

    public void setTruBassEnable(boolean enable) {
        try {
            mAudioEffectService.setTruBassEnable(enable);
        } catch (RemoteException e) {
            Log.e(TAG, "setTruBassEnable failed:" + e);
        }
    }

    public boolean getTruBassEnable() {
        try {
            return mAudioEffectService.getTruBassEnable();
        } catch (RemoteException e) {
            Log.e(TAG, "getTruBassEnable failed:" + e);
        }
        return false;
    }

    public void setAgsEnable(int mode) {
        try {
            mAudioEffectService.setAgsEnable(mode);
        } catch (RemoteException e) {
            Log.e(TAG, "setAgsEnable failed:" + e);
        }
    }

    public void setAgsMaxLevel(int step) {
        try {
            mAudioEffectService.setAgsMaxLevel(step);
        } catch (RemoteException e) {
            Log.e(TAG, "setAgsMaxLevel failed:" + e);
        }
    }

    public void setAgsAttrackTime(int step) {
        try {
            mAudioEffectService.setAgsAttrackTime(step);
        } catch (RemoteException e) {
            Log.e(TAG, "setAgsAttrackTime failed:" + e);
        }
    }

    public void setAgsReleaseTime(int step) {
        try {
            mAudioEffectService.setAgsReleaseTime(step);
        } catch (RemoteException e) {
            Log.e(TAG, "setAgsReleaseTime failed:" + e);
        }
    }

    public void setSourceIdForAvl(int step) {
        try {
            mAudioEffectService.setSourceIdForAvl(step);
        } catch (RemoteException e) {
            Log.e(TAG, "setSourceIdForAvl failed:" + e);
        }
    }

    public void setVirtualSurround(int mode) {
        try {
            mAudioEffectService.setVirtualSurround(mode);
        } catch (RemoteException e) {
            Log.e(TAG, "setVirtualSurround failed:" + e);
        }
    }

    public void setParameters(int order, int value) {
        try {
            mAudioEffectService.setParameters(order, value);
        } catch (RemoteException e) {
            Log.e(TAG, "setParameters failed:" + e);
        }
    }

    public int getParameters(int order) {
        try {
            return mAudioEffectService.getParameters(order);
        } catch (RemoteException e) {
            Log.e(TAG, "getParameters failed:" + e);
        }
        return -1;
    }

    public void cleanupAudioEffects() {
        try {
            mAudioEffectService.cleanupAudioEffects();
        } catch (RemoteException e) {
            Log.e(TAG, "cleanupAudioEffects failed:" + e);
        }
    }

    public void initSoundEffectSettings() {
        try {
            mAudioEffectService.initSoundEffectSettings();
        } catch (RemoteException e) {
            Log.e(TAG, "initSoundEffectSettings failed:" + e);
        }
    }

    public void resetSoundEffectSettings() {
        try {
            mAudioEffectService.resetSoundEffectSettings();
        } catch (RemoteException e) {
            Log.e(TAG, "resetSoundEffectSettings failed:" + e);
        }
    }
}
