/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description: JAVA file
 */

package com.droidlogic.app.tv;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;


import com.droidlogic.tvinput.services.ITvScanService;
import com.droidlogic.tvinput.services.IUpdateUiCallbackListener;
import com.droidlogic.tvinput.services.TvMessage;

/**
 * Created by yu.fang on 2018/7/9.
 */

public class TvScanManager {
    private String TAG = "TvScanManager";
    private boolean mDebug = true;
    private boolean isConnected = false;

    private int RETRY_MAX = 10;

    private ITvScanService mService = null;
    private ArrayList<ScannerMessageListener> mMessageListeners;
    private Context mContext;
    private Intent intent;

    private static volatile TvScanManager sInstance;

    private RemoteCallbackList<IUpdateUiCallbackListener> mListenerList = new RemoteCallbackList<>();

    private TvScanManager(Context context, Intent intent) {
        mContext = context;
        this.intent = intent;
        mMessageListeners = new ArrayList<ScannerMessageListener>();
        getService();
    }

    public static TvScanManager getInstance(Context context, Intent intent) {
        if (sInstance == null) {
            synchronized(TvScanManager.class) {
                if (sInstance == null) {
                    sInstance = new TvScanManager(context, intent);
                }
            }
        }
        return sInstance;
    }

    private void LOGI(String msg) {
        if (mDebug) Log.i(TAG, msg);
    }

    private IUpdateUiCallbackListener.Stub mListener = new IUpdateUiCallbackListener.Stub() {
        @Override
        public void onRespond(TvMessage msg) throws RemoteException {
            Log.d(TAG, "=====receive message from TvScanService");
            if (!mMessageListeners.isEmpty()) {
                for (ScannerMessageListener l : mMessageListeners) {
                    l.onMessage(msg);
                }
            }

        }
    };

    private void getService() {
        LOGI("=====[getService]");
        int retry = RETRY_MAX;
        boolean mIsBind = false;
        try {
            synchronized (this) {
                while (true) {
                    Intent intent = new Intent();
                    intent.setAction("com.droidlogic.tvinput.services.TvScanService");
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
            LOGI("[onServiceDisconnected]mService: " + mService);
            try{
                //unregister callback
                mService.unregisterListener(mListener);
                isConnected = false;
            } catch (RemoteException e){
                e.printStackTrace();
            }
            mService = null;
        }
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ITvScanService.Stub.asInterface(service);
            try{
                //register callback
                mService.registerListener(mListener);
                isConnected = true;
            } catch (RemoteException e){
                e.printStackTrace();
            }
            LOGI("SubTitleClient.onServiceConnected()..mService: " + mService);
        }
    };

    public void unBindService() {
        LOGI("unBindService");
        //mContext.unbindService(serConn);
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void init(){
        try {
            if (intent != null) {
                mService.init(intent);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setAtsccSearchSys(int value){
        try {
            mService.setAtsccSearchSys(value);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void startAutoScan(){
        try {
            mService.startAutoScan();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void startManualScan(){
        try {
            mService.startManualScan();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void stopScan() {
        try {
            mService.stopScan();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /* Set atv or dtv search, or atv and dtv search.
     * @param dtv TRUE will search dtv channel, FALSE not.
     * @param atv TRUE will search atv channel, FALSE not.
     */
    public void setSearchSys(boolean dtv, boolean atv){
        try {
            mService.setSearchSys(dtv, atv);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /* Set channel broadcast system type.
     * @param type See TvContract.Channels.TYPE_XXX.
     * @param atsc_c Default setting 0, When type = TYPE_ATSC_C,
     * atsc_c is available, it will select frequency table,
     * STD = 0;
     * LRC = 1;
     * HRC = 2;
     * AUTO = 3;
     */
    public void setSearchType(String type, int atsc_c) {
        try {
            mService.setSearchType(type, atsc_c);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setFrequency (String value1, String value2) {
        try {
            mService.setFrequency(value1, value2);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void release() {
        LOGI("=====[release]");
        try {
            //mService.unregisterListener(mListener);
            mService.release();
            //mService = null;
            mMessageListeners.clear();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void registerListener(IUpdateUiCallbackListener listener) throws RemoteException {
        mListenerList.register(listener);
    }

    public void unregisterListener(IUpdateUiCallbackListener listener) throws RemoteException {
        mListenerList.unregister(listener);
    }

    public void setMessageListener(ScannerMessageListener l) {
        mMessageListeners.add(l);
    }

    public interface ScannerMessageListener {
        void onMessage(TvMessage msg);
    }
}
