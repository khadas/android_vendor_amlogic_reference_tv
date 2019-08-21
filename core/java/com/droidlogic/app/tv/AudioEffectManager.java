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
    public static final  int DAP_MODULE                                 = 0;
    public static final  int EQ_MODULE                                  = 1;

    /* EQ sound mode type */
    public static final int MODE_STANDARD                               = 0;
    public static final int MODE_MUSIC                                  = 1;
    public static final int MODE_NEWS                                   = 2;
    public static final int MODE_THEATER                                = 3;
    public static final int MODE_GAME                                   = 4;
    public static final int MODE_CUSTOM                                 = 5;

    /* custom sound mode EQ band type*/
    public static final int EQ_SOUND_MODE_EFFECT_BAND1                  = 0;
    public static final int EQ_SOUND_MODE_EFFECT_BAND2                  = 1;
    public static final int EQ_SOUND_MODE_EFFECT_BAND3                  = 2;
    public static final int EQ_SOUND_MODE_EFFECT_BAND4                  = 3;
    public static final int EQ_SOUND_MODE_EFFECT_BAND5                  = 4;

    /* DBX sound mode param type */
    public static final int DBX_ADVANCED_MODE_PRARM_TYPE_SONICS         = 0;
    public static final int DBX_ADVANCED_MODE_PRARM_TYPE_VOLUME         = 1;
    public static final int DBX_ADVANCED_MODE_PRARM_TYPE_SURROUND       = 2;

    /* DBX sound mode */
    public static final int DBX_SOUND_MODE_STANDARD                     = 0;
    public static final int DBX_SOUND_MODE_MUSIC                        = 1;
    public static final int DBX_SOUND_MODE_MOVIE                        = 2;
    public static final int DBX_SOUND_MODE_THEATER                      = 3;
    public static final int DBX_SOUND_MODE_ADVANCED                     = 4;

    //surround value definition
    public static final int SURROUND_ON                                 = 0;
    public static final int SURROUND_OFF                                = 1;
    //bass boost value definition
    public static final int BASS_BOOST_ON                               = 0;
    public static final int BASS_BOOST_OFF                              = 1;
    //amlogic add
    public static final int SPDIF_OFF                                   = 0;
    public static final int SPDIF_PCM                                   = 1;
    public static final int SPDIF_RAW                                   = 2;
    public static final int SPDIF_AUTO                                  = 3;

    public static final int SOUND_SPEAKER_OUT                           = 0;
    public static final int SOUND_SPDIF_OUT                             = 1;
    public static final int SOUND_ARC_OUT                               = 2;

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
    public static final int SOUND_EFFECT_VIRTUALX_MODE_DEFAULT      = SOUND_EFFECT_VIRTUALX_MODE_OFF;
    public static final int SOUND_EFFECT_TRUVOLUME_HD_ENABLE_DEFAULT= 0;        // OFF
    public static final int SOUND_EFFECT_DBX_ENABLE_DEFAULT         = 0;        // OFF
    public static final int SOUND_EFFECT_DBX_SOUND_MODE_DEFAULT     = DBX_SOUND_MODE_STANDARD;

    // DBX sound mode default param [sonics, volume, surround]
    public static final byte[][] SOUND_EFFECT_DBX_SOUND_MODE_ARRAY_DEFAULT = {
            {4, 2, 2},  // standard mode
            {0, 2, 2},  // music mode
            {0, 2, 0},  // movie mode
            {0, 1, 2},  // theater mode
            {4, 2, 2},  // advance mode default db value
    };

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

    public void setUserSoundModeParam(int bandNumber, int value) {
        try {
            mAudioEffectService.setUserSoundModeParam(bandNumber, value);
        } catch (RemoteException e) {
            Log.e(TAG, "setUserSoundModeParam failed:" + e);
        }
    }

    public int getUserSoundModeParam(int bandNumber) {
        try {
            return mAudioEffectService.getUserSoundModeParam(bandNumber);
        } catch (RemoteException e) {
            Log.e(TAG, "getUserSoundModeParam failed:" + e);
        }
        return -1;
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

    public void setAgsEnable(int enable) {
        try {
            mAudioEffectService.setAgsEnable(enable);
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

    public void setDbxEnable(boolean enable) {
        try {
            mAudioEffectService.setDbxEnable(enable);
        } catch (RemoteException e) {
            Log.e(TAG, "setDbxEnable failed:" + e);
        }
    }

    public boolean getDbxEnable() {
        try {
            return mAudioEffectService.getDbxEnable();
        } catch (RemoteException e) {
            Log.e(TAG, "getDbxEnable failed:" + e);
        }
        return false;
    }

    public void setDbxSoundMode(int dbxMode) {
        try {
            mAudioEffectService.setDbxSoundMode(dbxMode);
        } catch (RemoteException e) {
            Log.e(TAG, "setDbxSoundMode failed:" + e);
        }
    }

    public int getDbxSoundMode() {
        try {
            return mAudioEffectService.getDbxSoundMode();
        } catch (RemoteException e) {
            Log.e(TAG, "getDbxSoundMode failed:" + e);
        }
        return -1;
    }

    public void setDbxAdvancedModeParam(int paramType, int value) {
        try {
            mAudioEffectService.setDbxAdvancedModeParam(paramType, value);
        } catch (RemoteException e) {
            Log.e(TAG, "setDbxAdvancedModeParam failed:" + e);
        }
    }

    public int getDbxAdvancedModeParam(int paramType) {
        try {
            return mAudioEffectService.getDbxAdvancedModeParam(paramType);
        } catch (RemoteException e) {
            Log.e(TAG, "getDbxAdvancedModeParam failed:" + e);
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
