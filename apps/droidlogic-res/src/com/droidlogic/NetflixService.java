/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description:
 *     AMLOGIC NetflixService
 */

package com.droidlogic;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Service;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioFormat;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.database.ContentObserver;
import android.content.ContentResolver;
import org.json.JSONObject;
import android.os.Handler;
import java.io.File;
import java.lang.StringBuffer;
import java.util.List;
import java.util.Scanner;

import com.droidlogic.app.DroidLogicUtils;
import com.droidlogic.app.SystemControlManager;
import com.droidlogic.app.OutputModeManager;

public class NetflixService extends Service {
    private static final String TAG = "NetflixService";

    /**
     * Feature for {@link #getSystemAvailableFeatures} and {@link #hasSystemFeature}:
     * This device supports netflix
     */
    public static final String FEATURE_SOFTWARE_NETFLIX     = "droidlogic.software.netflix";

    public static final String NETFLIX_PKG_NAME             = "com.netflix.ninja";
    public static final String YOUTUBE_PKG_NAME             = "com.google.android.youtube.tv";
    public static final String NETFLIX_STATUS_CHANGE        = "com.netflix.action.STATUS_CHANGE";
    public static final String NETFLIX_DIAL_STOP            = "com.netflix.action.DIAL_STOP";
    private static final String VIDEO_SIZE_DEVICE           = "/sys/class/video/device_resolution";
    private static final String SYS_AUDIO_CAP               = "/sys/class/amhdmitx/amhdmitx0/aud_cap";
    private static final String WAKEUP_REASON_DEVICE        = "/sys/class/meson_pm/suspend_reason";
    private static final String WAKEUP_REASON_DEVICE_OTHER  = "/sys/devices/platform/aml_pm/suspend_reason";
    private static final String NRDP_PLATFORM_CAP           = "nrdp_platform_capabilities";
    private static final String NRDP_AUDIO_PLATFORM_CAP     = "nrdp_audio_platform_capabilities";
    private static final String NRDP_AUDIO_PLATFORM_CAP_MS12= "nrdp_audio_platform_capabilities_ms12";
    private static final String NRDP_PLATFORM_CONFIG_DIR    = "/vendor/etc/";
    private static final String NRDP_EXTERNAL_SURROUND      = "nrdp_external_surround_sound_enabled";
    private static final int WAKEUP_REASON_CUSTOM           = 9;
    private static boolean mLaunchDialService               = true;
    private static boolean atmosSupported                   = false;
    private static boolean doblySupported                   = false;

    private boolean mIsNetflixFg = false;
    private boolean mIsYoutubeFg = false;
    private boolean hasMS12 = false;
    private Context mContext;
    SystemControlManager mSCM;
    AudioManager mAudioManager;
    private SettingsObserver mSettingsObserver;
    private OutputModeManager mOutputModeManager = null;

    private class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
           int surround = mOutputModeManager.getDigitalAudioFormatOut();
           Log.i (TAG, "onChange surround: " + DroidLogicUtils.audioFormatOutputToString(surround));
           switch (surround) {
                    case OutputModeManager.DIGITAL_AUDIO_FORMAT_AUTO:
                    case OutputModeManager.DIGITAL_AUDIO_FORMAT_PASSTHROUGH:
                         Log.i (TAG, "onChange auto/passthrough ATMOS: " + atmosSupported);
                         setNrdpCapabilitesIfNeed(NRDP_AUDIO_PLATFORM_CAP, true);
                         setAtmosEnabled(atmosSupported);
                         if (hasMS12) {
                             setUiAudioBufferDelayOffset(doblySupported);
                         }
                         break;
                    case OutputModeManager.DIGITAL_AUDIO_FORMAT_MANUAL:
                        String subformat = Settings.Global.getString(mContext.getContentResolver(),OutputModeManager.DIGITAL_AUDIO_SUBFORMAT);
                        Log.i (TAG, "onChange manual subformat: " + subformat);
                        setAtmosEnabled(subformat.contains(AudioFormat.ENCODING_E_AC3_JOC + ""));
                        if (hasMS12) {
                            setUiAudioBufferDelayOffset(doblySupported);
                        }
                        break;
                   case OutputModeManager.DIGITAL_AUDIO_FORMAT_PCM:
                        if (hasMS12) {
                            setUiAudioBufferDelayOffset(false);
                        }
                        break;
                  default:
                        Log.d(TAG, "error surround format");
                        break;
            }
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "action:" + action);

            if (action.equals(NETFLIX_DIAL_STOP)) {
                int pid = getNetflixPid();
                if (pid > 0) {
                    Log.i (TAG, "Killing active Netflix Service PID: " + pid);
                    android.os.Process.killProcess (pid);
                }
            }
        }
    };

    private BroadcastReceiver mHPReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isConnected = intent.getBooleanExtra("state", false);
            refreshAudioCapabilities(isConnected);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        mSCM = SystemControlManager.getInstance();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mOutputModeManager = OutputModeManager.getInstance(mContext);
        IntentFilter filter = new IntentFilter(NETFLIX_DIAL_STOP);
        filter.setPriority (IntentFilter.SYSTEM_HIGH_PRIORITY);
        // Assuming NetflixDialService runs in each user, singleton service
        // need listen from any user.
        //PlatformAPI.registerReceiverAsUser(mContext, mReceiver,  new UserHandle(-1),
        //        filter, null, null);

        String buildDate = PlatformAPI.getStringProperty("ro.build.version.incremental", "");
        boolean needUpdate = !buildDate.equals(SettingsPref.getSavedBuildDate(mContext));
        hasMS12 = mOutputModeManager.isAudioSupportMs12System();
        setNrdpCapabilitesIfNeed(NRDP_PLATFORM_CAP, needUpdate);
        setNrdpCapabilitesIfNeed(NRDP_AUDIO_PLATFORM_CAP, needUpdate);
        if (needUpdate) {
            SettingsPref.setSavedBuildDate(mContext, buildDate);
        }

        filter = new IntentFilter("android.intent.action.HDMI_PLUGGED");
        registerReceiver(mHPReceiver, filter);
        refreshAudioCapabilities(true);

        mSettingsObserver = new SettingsObserver(new Handler());
        getContentResolver().registerContentObserver(Settings.Global.getUriFor(OutputModeManager.DIGITAL_AUDIO_FORMAT),
                false, mSettingsObserver);
        getContentResolver().registerContentObserver(Settings.Global.getUriFor(OutputModeManager.DIGITAL_AUDIO_SUBFORMAT),
                false, mSettingsObserver);

        startNetflixIfNeed();

        new ObserverThread ("NetflixObserver").start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startNetflixIfNeed() {
        Scanner scanner = null;
        int reason = -1;
        boolean isSysExists = true;
        String wakeupSys = WAKEUP_REASON_DEVICE;

        if (new File(WAKEUP_REASON_DEVICE).exists()) {
            wakeupSys = WAKEUP_REASON_DEVICE;
        } else if (new File(WAKEUP_REASON_DEVICE_OTHER).exists()) {
            wakeupSys = WAKEUP_REASON_DEVICE_OTHER;
       } else {
            isSysExists = false ;
       }

       if (isSysExists) {
           try {
                scanner = new Scanner (new File(wakeupSys));
                reason = scanner.nextInt();
                scanner.close();
           } catch (Exception e) {
               if (scanner != null)
                 scanner.close();
                 e.printStackTrace();
                 return;
           }

       }

       if (reason == WAKEUP_REASON_CUSTOM) {
            /* response slowly while system start, use startActivity instead
            Intent netflixIntent = new Intent();
            netflixIntent.setAction("com.netflix.ninja.intent.action.NETFLIX_KEY");
            netflixIntent.setPackage("com.netflix.ninja");
            netflixIntent.putExtra("power_on", true);
            netflixIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

            Log.i(TAG, "start netflix by power on");
            mContext.sendBroadcast(netflixIntent,"com.netflix.ninja.permission.NETFLIX_KEY");
            */

           boolean isPowerOn = true;  //false for netflixButton, true for powerOnFromNetflixButton
           Intent i = new Intent("com.netflix.action.NETFLIX_KEY_START");
           i.setPackage("com.netflix.ninja");
           i.putExtra("power_on", isPowerOn);  //"power_on" Boolean Extra must be presented
           i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
           mContext.startActivity(i);
       }
    }

    private void setNrdpCapabilitesIfNeed(String capName, boolean needUpdate) {
        String cap = Settings.Global.getString(getContentResolver(), capName);
        String capName_File = capName;
        Log.i(TAG, capName + ":\n" + cap);
        if (!needUpdate && !TextUtils.isEmpty(cap)) {
            return;
        }

        if (capName.startsWith(NRDP_AUDIO_PLATFORM_CAP) && hasMS12 &&
               mOutputModeManager.getDigitalAudioFormatOut() == OutputModeManager.DIGITAL_AUDIO_FORMAT_AUTO) {
            capName_File = NRDP_AUDIO_PLATFORM_CAP_MS12;
        }

        try {
            Scanner scanner = new Scanner(new File(NRDP_PLATFORM_CONFIG_DIR + capName_File + ".json"));
            StringBuffer sb = new StringBuffer();

            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine());
                sb.append('\n');
            }

            Settings.Global.putString(getContentResolver(), capName, sb.toString());
            scanner.close();
        } catch (java.io.FileNotFoundException e) {
            Log.d(TAG, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getNetflixPid() {
        int retry = 10;
        ActivityManager manager = (ActivityManager) mContext.getSystemService (Context.ACTIVITY_SERVICE);

        do {
            List<RunningAppProcessInfo> services = manager.getRunningAppProcesses();
            for (int i = 0; i < services.size(); i++) {
                String servicename = services.get (i).processName;
                if (servicename.contains (NETFLIX_PKG_NAME)) {
                    Log.i (TAG, "find process: " + servicename + " pid: " + services.get (i).pid);
                    return services.get (i).pid;
                }
            }
        } while (--retry > 0);

        return -1;
    }

    public boolean netflixActivityRunning() {
        ActivityManager am = (ActivityManager) mContext.getSystemService (Context.ACTIVITY_SERVICE);
        List< ActivityManager.RunningTaskInfo > task = am.getRunningTasks (1);

        if (task.size() != 0) {
            if (mLaunchDialService) {
                mLaunchDialService = false;

                try {
                    Intent intent = new Intent();
                    intent.setClassName ("com.netflix.dial", "com.netflix.dial.NetflixDialService");
                    //PlatformAPI.startServiceAsUser(mContext, intent,  new UserHandle(-1));
                } catch (SecurityException e) {
                    Log.i (TAG, "Initial launching dial Service failed");
                }
            }

            ComponentName componentInfo = task.get (0).topActivity;
            if (componentInfo.getPackageName().equals (NETFLIX_PKG_NAME) ) {
                //Log.i (TAG, "netflix running as top activity");
                return true;
            }
        }
        return false;
    }

    public boolean youtubeActivityRunning() {
        ActivityManager am = (ActivityManager) mContext.getSystemService (Context.ACTIVITY_SERVICE);
        List< ActivityManager.RunningTaskInfo > task = am.getRunningTasks (1);

        if (task.size() != 0) {
            ComponentName componentInfo = task.get (0).topActivity;
            if (componentInfo.getPackageName().equals (YOUTUBE_PKG_NAME) ) {
                // Log.i (TAG, "youtube running as top activity");
                return true;
            }
        }
        return false;
    }

    private void refreshAudioCapabilities(boolean isHdmiPlugged) {
        boolean isTv = DroidLogicUtils.isTv();
        int surround = mOutputModeManager.getDigitalAudioFormatOut();
        Log.i(TAG, "onReceived HDMI_PLUGGED: " + isHdmiPlugged + ", isTv:" + isTv + ", surround:" +
                DroidLogicUtils.audioFormatOutputToString(surround));
        if (!isTv && (OutputModeManager.DIGITAL_AUDIO_FORMAT_MANUAL == surround)) {
            Log.i(TAG, "Set " + NRDP_EXTERNAL_SURROUND + " to " + (isHdmiPlugged ? 1 : 0));
            Settings.Global.putInt(mContext.getContentResolver(),
                    NRDP_EXTERNAL_SURROUND, isHdmiPlugged ? 1 : 0);
        }

        String audioSinkCap = mSCM.readSysFs(SYS_AUDIO_CAP);
        atmosSupported = audioSinkCap.contains("Dobly_Digital+/ATMOS");
        doblySupported = audioSinkCap.contains("Dobly_Digital");
        if (isHdmiPlugged && (OutputModeManager.DIGITAL_AUDIO_FORMAT_AUTO == surround
                || OutputModeManager.DIGITAL_AUDIO_FORMAT_PASSTHROUGH == surround) ) {
            Log.i (TAG, "ATMOS: " + atmosSupported + ", audioSinkCap: " + audioSinkCap);
            setAtmosEnabled(atmosSupported);
            if (hasMS12) {
                setUiAudioBufferDelayOffset(doblySupported);
            }
        }
    }

    public static String setSystemProperty(String key, String defValue) {
        String getValue = defValue;
        try {
            Class[] typeArgs = new Class[2];
            typeArgs[0] = String.class;
            typeArgs[1] = String.class;

            Object[] valueArgs = new Object[2];
            valueArgs[0] = key;
            valueArgs[1] = defValue;

            getValue = (String)Class.forName("android.os.SystemProperties")
                    .getMethod("set", typeArgs)
                    .invoke(null, valueArgs);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return getValue;
    }

    private void setAtmosEnabled(boolean enabled) {
        // Refer to /vendor/etc/nrdp_audio_platform_capabilities.json
        String audioCap = Settings.Global.getString(getContentResolver(), NRDP_AUDIO_PLATFORM_CAP);
        if (audioCap == null || !hasMS12 ||
               (mOutputModeManager.getDigitalAudioFormatOut() == OutputModeManager.DIGITAL_AUDIO_FORMAT_PASSTHROUGH))
            return;

        try {
            JSONObject rootObject = new JSONObject(audioCap);
            JSONObject audioCapsObject = rootObject.getJSONObject("audiocaps");
            JSONObject atmosObject = audioCapsObject.getJSONObject("atmos");

            boolean isEnabled = atmosObject.getBoolean("enabled");
            if (isEnabled ^ enabled) {
                Log.i(TAG, "set ATMOS support " + isEnabled + " -> " + enabled);
                atmosObject.put("enabled", enabled);
                Settings.Global.putString(getContentResolver(), NRDP_AUDIO_PLATFORM_CAP, rootObject.toString());
            }
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }
    }

    private void setUiAudioBufferDelayOffset(boolean enabled) {
        // Refer to /vendor/etc/nrdp_audio_platform_capabilities.json
        String audioCap = Settings.Global.getString(getContentResolver(), NRDP_AUDIO_PLATFORM_CAP);
        if (audioCap == null)
            return;

        try {
            JSONObject rootObject = new JSONObject(audioCap);
            JSONObject audioCapsObject = rootObject.getJSONObject("audiocaps");
            int uioffset = audioCapsObject.getInt("uiAudioBufferDelayOffset");
            int setoffset = enabled ?  90 : 95;
            if (uioffset != setoffset) {
               Log.i(TAG, "uioffset from  " + uioffset + "to " + setoffset);
               audioCapsObject.put("uiAudioBufferDelayOffset", setoffset);
               Settings.Global.putString(getContentResolver(), NRDP_AUDIO_PLATFORM_CAP, rootObject.toString());
           }
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }
    }

    class ObserverThread extends Thread {
        public ObserverThread (String name) {
            super (name);
        }

        public void run() {
            while (true) {
                try {
                    Thread.sleep (1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                boolean fg = netflixActivityRunning();
                if (fg ^ mIsNetflixFg) {
                    Log.i (TAG, "Netflix status changed from " + (mIsNetflixFg?"fg":"bg")+ " -> " + (fg?"fg":"bg"));

                    mIsNetflixFg = fg;
                    Intent intent = new Intent();
                    intent.setAction (NETFLIX_STATUS_CHANGE);
                    intent.putExtra ("status", fg ? 1 : 0);
                    intent.putExtra ("pid", fg?getNetflixPid():-1);
                    //mContext.sendBroadcastAsUser (intent,  new UserHandle(0));

                    mAudioManager.setParameters("continuous_audio_mode=" + (fg ? "1" : "0"));
                    mSCM.setProperty ("vendor.netflix.state", fg ? "fg" : "bg");
                }
                boolean fgYoutube = youtubeActivityRunning();
                if (fgYoutube ^ mIsYoutubeFg) {
                    Log.i (TAG, "Youtube status changed from " + (mIsYoutubeFg?"fg":"bg")+ " -> " + (fgYoutube?"fg":"bg"));
                    mIsYoutubeFg = fgYoutube;
                    mAudioManager.setParameters("compensate_video_enable=" + (fgYoutube ? "1" : "0"));
                }

/* move setting display-size code to systemcontrol
                if (SystemProperties.getBoolean ("sys.display-size.check", true) ||
                    SystemProperties.getBoolean ("vendor.display-size.check", true)) {
                    try {
                        Scanner sc = new Scanner (new File(VIDEO_SIZE_DEVICE));
                        if (sc.hasNext("\\d+x\\d+")) {
                            String[] parts = sc.nextLine().split ("x");
                            int w = Integer.parseInt (parts[0]);
                            int h = Integer.parseInt (parts[1]);
                            //Log.i(TAG, "Video resolution: " + w + "x" + h);

                            String nexflixProps[] = {"sys.display-size", "vendor.display-size"};
                            for (String propName:nexflixProps) {
                                String prop = SystemProperties.get (propName, "0x0");
                                String[] parts_prop = prop.split ("x");
                                int wd = Integer.parseInt (parts_prop[0]);
                                int wh = Integer.parseInt (parts_prop[1]);

                                if ((w != wd) || (h != wh)) {
                                    mSCM.setProperty(propName, String.format("%dx%d", w, h));
                                    //setSystemProperty(propName, String.format("%dx%d", w, h));
                                    //SystemProperties.set (propName, String.format("%dx%d", w, h));
                                    //Log.i(TAG, "set sys.display-size property to " + String.format("%dx$d", w, h));
                                }
                            }
                        } else {
                            //Log.i(TAG, "Video resolution no pattern found" + sc.nextLine());
                        }
                        sc.close();

                    } catch (Exception e) {
                        Log.i(TAG, "Error parsing video size device node");
                    }
                }
*/
            }
        }
    }
}

