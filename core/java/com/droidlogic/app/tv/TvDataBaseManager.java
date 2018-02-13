package com.droidlogic.app.tv;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.content.Context;
import android.database.Cursor;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;
import android.media.tv.TvContract.Channels;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Pair;
import android.util.SparseArray;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Objects;

import com.droidlogic.app.tv.DroidLogicTvUtils.*;

public class TvDataBaseManager {
    private static final String TAG = "TvDataBaseManager";
    private static final boolean DEBUG = true;
    private static final int UPDATE_SUCCESS = -1;
    private static final int ATV_FREQUENCE_RANGE = 1000000; // 1Mhz
    private static final SparseArray<String> CHANNEL_MODE_TO_TYPE_MAP = new SparseArray<String>();

    static {
        CHANNEL_MODE_TO_TYPE_MAP.put(TvChannelParams.MODE_DTMB, Channels.TYPE_DTMB);
        CHANNEL_MODE_TO_TYPE_MAP.put(TvChannelParams.MODE_QPSK, Channels.TYPE_DVB_S);
        CHANNEL_MODE_TO_TYPE_MAP.put(TvChannelParams.MODE_QAM, Channels.TYPE_DVB_C);
        CHANNEL_MODE_TO_TYPE_MAP.put(TvChannelParams.MODE_OFDM, Channels.TYPE_DVB_T);
        CHANNEL_MODE_TO_TYPE_MAP.put(TvChannelParams.MODE_ATSC, Channels.TYPE_ATSC_C);
        CHANNEL_MODE_TO_TYPE_MAP.put(TvChannelParams.MODE_ANALOG, Channels.TYPE_PAL);
        CHANNEL_MODE_TO_TYPE_MAP.put(TvChannelParams.MODE_ISDBT, Channels.TYPE_ISDB_T);
    }

    private static final Map<String, Integer> CHANNEL_TYPE_TO_MODE_MAP = new HashMap<String, Integer>();

    static {
        CHANNEL_TYPE_TO_MODE_MAP.put(Channels.TYPE_DTMB, TvChannelParams.MODE_DTMB);
        CHANNEL_TYPE_TO_MODE_MAP.put(Channels.TYPE_DVB_C, TvChannelParams.MODE_QAM);
        CHANNEL_TYPE_TO_MODE_MAP.put(Channels.TYPE_DVB_T, TvChannelParams.MODE_OFDM);
        CHANNEL_TYPE_TO_MODE_MAP.put(Channels.TYPE_PAL, TvChannelParams.MODE_ANALOG);
    }

    private Context mContext;
    private ContentResolver mContentResolver;

    public TvDataBaseManager (Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
    }

    public void deleteChannels(String inputId) {
        deleteChannels(inputId, null);
    }

    public void deleteChannels(String inputId, String type) {
        Uri channelsUri = TvContract.buildChannelsUriForInput(inputId);
        try {
            if (type == null)
                mContentResolver.delete(channelsUri, Channels._ID + "!=-1", null);
            else
                mContentResolver.delete(channelsUri, Channels._ID + "!=-1 and " + Channels.COLUMN_TYPE + "='" + type +"'", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int updateDtvChannel(ChannelInfo channel) {
        int ret = 0;
        Uri channelsUri = TvContract.buildChannelsUriForInput(channel.getInputId());
        String[] projection = {Channels._ID,
            Channels.COLUMN_SERVICE_ID,
            Channels.COLUMN_ORIGINAL_NETWORK_ID,
            Channels.COLUMN_TRANSPORT_STREAM_ID,
            Channels.COLUMN_DISPLAY_NUMBER,
            Channels.COLUMN_DISPLAY_NAME,
            Channels.COLUMN_INTERNAL_PROVIDER_DATA};

        Cursor cursor = null;
        try {
            boolean found = false;
            cursor = mContentResolver.query(channelsUri, projection, Channels.COLUMN_SERVICE_TYPE + "=?", new String[]{channel.getServiceType()}, null);
            while (cursor != null && cursor.moveToNext()) {
                long rowId = cursor.getLong(findPosition(projection, Channels._ID));

                if (channel.getId() == -1) {
                    int serviceId = cursor.getInt(findPosition(projection, Channels.COLUMN_SERVICE_ID));
                    int originalNetworkId = cursor.getInt(findPosition(projection, Channels.COLUMN_ORIGINAL_NETWORK_ID));
                    int transportStreamId = cursor.getInt(findPosition(projection, Channels.COLUMN_TRANSPORT_STREAM_ID));
                    String name = cursor.getString(findPosition(projection, Channels.COLUMN_DISPLAY_NAME));
                    int frequency = 0;
                    int index = cursor.getColumnIndex(Channels.COLUMN_INTERNAL_PROVIDER_DATA);
                    if (index >= 0) {
                        Map<String, String> parsedMap = DroidLogicTvUtils.jsonToMap(cursor.getString(index));
                        frequency = Integer.parseInt(parsedMap.get(ChannelInfo.KEY_FREQUENCY));
                    }
                    if (serviceId == channel.getServiceId()
                        && originalNetworkId == channel.getOriginalNetworkId()
                        && transportStreamId == channel.getTransportStreamId()
                        && frequency == channel.getFrequency()
                        && TextUtils.equals(name, channel.getDisplayName()))
                        found = true;
                } else if (rowId == channel.getId()) {
                    found = true;
                }

                if (found) {
                    Uri uri = TvContract.buildChannelUri(rowId);
                    mContentResolver.update(uri, buildDtvChannelData(channel), null, null);
                    insertLogo(channel.getLogoUrl(), uri);
                    ret = UPDATE_SUCCESS;
                    break;
                }
            }
            if (ret != UPDATE_SUCCESS)
                ret = cursor.getCount();
        } catch (Exception e) {
            //TODO
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (DEBUG)
            Log.d(TAG, "update " + ((ret == UPDATE_SUCCESS) ? "found" : "notfound")
                +" DTV CH: [_id:"+channel.getId()
                +"][sid:"+channel.getServiceId()
                +"][freq:"+channel.getFrequency()
                +"][name:"+channel.getDisplayName()
                +"][num:"+channel.getDisplayNumber()
                +"]");
        return ret;
    }


    /*update atv channel:
          if getId() == -1 (from searching): update that with same frequncy
          else               (from database): update that with same _ID
      */
    private int updateAtvChannel(ChannelInfo channel) {
        int ret = 0;

        Uri channelsUri = TvContract.buildChannelsUriForInput(channel.getInputId());
        String[] projection = {Channels._ID, Channels.COLUMN_INTERNAL_PROVIDER_DATA};

        Cursor cursor = null;
        try {
            boolean found = false;
            cursor = mContentResolver.query(channelsUri, projection, null, null, null);
            while (cursor != null && cursor.moveToNext()) {
                long rowId = cursor.getLong(findPosition(projection, Channels._ID));

                if (channel.getId() == -1) {
                    Map<String, String> parsedMap = parseInternalProviderData(cursor.getString(
                                findPosition(projection, Channels.COLUMN_INTERNAL_PROVIDER_DATA)));
                    int frequency = Integer.parseInt(parsedMap.get(ChannelInfo.KEY_FREQUENCY));
                    if (frequency == channel.getFrequency())
                        found = true;
                } else if (rowId == channel.getId()) {
                    found = true;
                }

                if (found) {
                    Uri uri = TvContract.buildChannelUri(rowId);
                    mContentResolver.update(uri, buildAtvChannelData(channel), null, null);
                    insertLogo(channel.getLogoUrl(), uri);
                    ret = UPDATE_SUCCESS;
                    break;
                }
            }
            if (ret != UPDATE_SUCCESS)
                ret = cursor.getCount();
        } catch (Exception e) {
            //TODO
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (DEBUG)
            Log.d(TAG, "update " + ((ret == UPDATE_SUCCESS) ? "found" : "notfound")
                +" ATV CH: [_id:"+channel.getId()
                +"][freq:"+channel.getFrequency()
                +"][name:"+channel.getDisplayName()
                +"][num:"+channel.getDisplayNumber()
                +"]");
        return ret;
    }

    /*update atv channel:
          if getId() == -1 (from searching): update that with frequncy diff less than 1Mhz
          else               (from database): update that with same _ID
      */
    private int updateAtvChannelFuzzy(ChannelInfo channel) {
        int ret = 0;

        Uri channelsUri = TvContract.buildChannelsUriForInput(channel.getInputId());
        String[] projection = {Channels._ID, Channels.COLUMN_DISPLAY_NUMBER, Channels.COLUMN_INTERNAL_PROVIDER_DATA};

        Cursor cursor = null;
        try {
            boolean found = false;
            cursor = mContentResolver.query(channelsUri, projection, null, null, null);
            while (cursor != null && cursor.moveToNext()) {
                long rowId = cursor.getLong(findPosition(projection, Channels._ID));

                if (channel.getId() == -1) {
                    Map<String, String> parsedMap = parseInternalProviderData(cursor.getString(
                                findPosition(projection, Channels.COLUMN_INTERNAL_PROVIDER_DATA)));
                    int frequency = Integer.parseInt(parsedMap.get(ChannelInfo.KEY_FREQUENCY));
                    int diff = frequency - channel.getFrequency();
                    if ((diff < ATV_FREQUENCE_RANGE) && (diff > -ATV_FREQUENCE_RANGE))
                        found = true;
                } else if (rowId == channel.getId()) {
                    found = true;
                }

                if (found) {
                    Uri uri = TvContract.buildChannelUri(rowId);
                    channel.setDisplayNumber(cursor.getString(findPosition(projection, Channels.COLUMN_DISPLAY_NUMBER)));
                    mContentResolver.update(uri, buildAtvChannelData(channel), null, null);
                    insertLogo(channel.getLogoUrl(), uri);
                    ret = UPDATE_SUCCESS;
                    break;
                }
            }
            if (ret != UPDATE_SUCCESS)
                ret = cursor.getCount();
        } catch (Exception e) {
            //TODO
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (DEBUG)
            Log.d(TAG, "update " + ((ret == UPDATE_SUCCESS) ? "found" : "notfound")
                +" ATV CH: [_id:"+channel.getId()
                +"][freq:"+channel.getFrequency()
                +"][name:"+channel.getDisplayName()
                +"][num:"+channel.getDisplayNumber()
                +"]");
        return ret;
    }

    /*update atv channel:
          if toBeUpdated == null (nothing to be replaced): just wait for insert
          else                       (channel to replace): replace the one with _ID
          toBeUpdated must come from database, toBeUpdated.getId() == -1 is NOT allowed.
      */
    private int updateAtvChannel(ChannelInfo toBeUpdated, ChannelInfo channel) {
        int ret = 0;
        Uri channelsUri = TvContract.buildChannelsUriForInput(channel.getInputId());
        String[] projection = {Channels._ID};
        Cursor cursor = null;

        try {
            if (toBeUpdated == null) {
                cursor = mContentResolver.query(channelsUri, projection, null, null, null);
                if (cursor != null)
                    ret = cursor.getCount();
            } else if (toBeUpdated.getId() != -1) {
                Uri uri = TvContract.buildChannelUri(toBeUpdated.getId());
                channel.setDisplayNumber(toBeUpdated.getDisplayNumber());
                mContentResolver.update(uri, buildAtvChannelData(channel), null, null);
                insertLogo(channel.getLogoUrl(), uri);
                ret = UPDATE_SUCCESS;
            } else {
                Log.w(TAG, "toBeUpdated is illegal, [_id:"+toBeUpdated.getId()+"]");
            }
        } catch (Exception e) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (DEBUG)
            Log.d(TAG, "update " + ((ret == UPDATE_SUCCESS) ? "found" : "notfound")
                +" ATV CH: [_id:"+ ((ret == UPDATE_SUCCESS) ? toBeUpdated.getId() : channel.getId())
                +"][freq:"+channel.getFrequency()
                +"][name:"+channel.getDisplayName()
                +"][num:"+channel.getDisplayNumber()
                +"]");
        return ret;
    }

    public void insertDtvChannel(ChannelInfo channel, int channelNumber) {
        insertDtvChannel(channel, Integer.toString(channelNumber));
    }

    public void insertDtvChannel(ChannelInfo channel, String channelNumber) {
        Uri channelsUri = TvContract.buildChannelsUriForInput(channel.getInputId());
        channel.setDisplayNumber(channelNumber);
        Uri uri = mContentResolver.insert(TvContract.Channels.CONTENT_URI, buildDtvChannelData(channel));
        insertLogo(channel.getLogoUrl(), uri);

        if (DEBUG)
            Log.d(TAG, "Insert DTV CH: [sid:"+channel.getServiceId()
                    +"][freq:"+channel.getFrequency()
                    +"][name:"+channel.getDisplayName()
                    +"][num:"+channel.getDisplayNumber()
                    +"]");
    }

    public void insertAtvChannel(ChannelInfo channel, int channelNumber) {
        insertAtvChannel(channel, Integer.toString(channelNumber));
    }

    public void insertAtvChannel(ChannelInfo channel, String channelNumber) {
        Uri channelsUri = TvContract.buildChannelsUriForInput(channel.getInputId());
        channel.setDisplayNumber(channelNumber);
        Uri uri = mContentResolver.insert(TvContract.Channels.CONTENT_URI, buildAtvChannelData(channel));
        insertLogo(channel.getLogoUrl(), uri);

        if (DEBUG)
            Log.d(TAG, "Insert ATV CH: [freq:"+channel.getFrequency()
                +"][name:"+channel.getDisplayName()
                +"][num:"+channel.getDisplayNumber()
                +"]");
    }

    public int insertAtvChannelWithFreOrder(ChannelInfo channel) {
        int newChannelNum = -1;
        ArrayList<ChannelInfo> atvlist = getChannelList(channel.getInputId(), Channels.SERVICE_TYPE_AUDIO_VIDEO);
        if (atvlist.size() <= 0 || channel.getFrequency() > atvlist.get(atvlist.size() - 1).getFrequency()) {
            newChannelNum = atvlist.size() + 1;
        } else {
            for (int i = 0; i < atvlist.size(); i++) {
                if (channel.getFrequency() == atvlist.get(i).getFrequency()) {
                    Log.d(TAG, "ATV CH[freq:" + channel.getFrequency() + "] already exist");
                    newChannelNum = atvlist.size() + 1;
                    return newChannelNum;
                } else if (channel.getFrequency() < atvlist.get(i).getFrequency()) {
                    if (i == 0 && atvlist.get(i).getNumber() > 1) {
                        newChannelNum = 1;
                        break;
                    } else if (i > 0 && atvlist.get(i).getNumber() - atvlist.get(i - 1).getNumber() > 1) {
                        newChannelNum = atvlist.get(i - 1).getNumber() + 1;
                        break;
                    }

                    newChannelNum = atvlist.get(i).getNumber();
                    int updateNum = atvlist.get(i).getNumber() + 1;
                    for (int j = i; j < atvlist.size(); j++, updateNum++) {
                        atvlist.get(j).setDisplayNumber(String.valueOf(updateNum));
                        updateAtvChannel(atvlist.get(j));
                    }
                    break;
                }
            }
        }
        insertAtvChannel(channel, newChannelNum);
        return newChannelNum;
    }


    private ContentValues  buildDtvChannelData (ChannelInfo channel){
        ContentValues values = new ContentValues();
        values.put(Channels.COLUMN_INPUT_ID, channel.getInputId());
        values.put(Channels.COLUMN_DISPLAY_NAME, channel.getDisplayName());
        values.put(Channels.COLUMN_DISPLAY_NUMBER, channel.getDisplayNumber());
        values.put(Channels.COLUMN_ORIGINAL_NETWORK_ID, channel.getOriginalNetworkId());
        values.put(Channels.COLUMN_TRANSPORT_STREAM_ID, channel.getTransportStreamId());
        values.put(Channels.COLUMN_SERVICE_ID, channel.getServiceId());
        values.put(Channels.COLUMN_TYPE, channel.getType());
        values.put(Channels.COLUMN_BROWSABLE, channel.isBrowsable()? 1 : 0);
        values.put(Channels.COLUMN_SERVICE_TYPE, channel.getServiceType());
        values.put(Channels.COLUMN_VIDEO_FORMAT, channel.getVideoFormat());

        Map<String, String> map = new HashMap<String, String>();
        map.put(ChannelInfo.KEY_VFMT, String.valueOf(channel.getVfmt()));
        map.put(ChannelInfo.KEY_FREQUENCY, String.valueOf(channel.getFrequency()));
        map.put(ChannelInfo.KEY_BAND_WIDTH, String.valueOf(channel.getBandwidth()));
        map.put(ChannelInfo.KEY_SYMBOL_RATE, String.valueOf(channel.getSymbolRate()));
        map.put(ChannelInfo.KEY_MODULATION, String.valueOf(channel.getModulation()));
        map.put(ChannelInfo.KEY_VIDEO_PID, String.valueOf(channel.getVideoPid()));
        map.put(ChannelInfo.KEY_AUDIO_PIDS, Arrays.toString(channel.getAudioPids()));
        map.put(ChannelInfo.KEY_AUDIO_FORMATS, Arrays.toString(channel.getAudioFormats()));
        map.put(ChannelInfo.KEY_AUDIO_EXTS, Arrays.toString(channel.getAudioExts()));
        map.put(ChannelInfo.KEY_AUDIO_LANGS, DroidLogicTvUtils.TvString.toString(channel.getAudioLangs()));
        map.put(ChannelInfo.KEY_PCR_ID, String.valueOf(channel.getPcrPid()));
        map.put(ChannelInfo.KEY_AUDIO_TRACK_INDEX, String.valueOf(channel.getAudioTrackIndex()));
        map.put(ChannelInfo.KEY_AUDIO_COMPENSATION, String.valueOf(channel.getAudioCompensation()));
        map.put(ChannelInfo.KEY_AUDIO_CHANNEL, String.valueOf(channel.getAudioChannel()));
        map.put(ChannelInfo.KEY_IS_FAVOURITE, String.valueOf(channel.isFavourite() ? 1 : 0));
        map.put(ChannelInfo.KEY_SUBT_TYPES, Arrays.toString(channel.getSubtitleTypes()));
        map.put(ChannelInfo.KEY_SUBT_PIDS, Arrays.toString(channel.getSubtitlePids()));
        map.put(ChannelInfo.KEY_SUBT_STYPES, Arrays.toString(channel.getSubtitleStypes()));
        map.put(ChannelInfo.KEY_SUBT_ID1S, Arrays.toString(channel.getSubtitleId1s()));
        map.put(ChannelInfo.KEY_SUBT_ID2S, Arrays.toString(channel.getSubtitleId2s()));
        map.put(ChannelInfo.KEY_SUBT_LANGS, DroidLogicTvUtils.TvString.toString(channel.getSubtitleLangs()));
        map.put(ChannelInfo.KEY_SUBT_TRACK_INDEX, String.valueOf(channel.getSubtitleTrackIndex()));
        map.put(ChannelInfo.KEY_MULTI_NAME, DroidLogicTvUtils.TvString.toString(channel.getDisplayNameMulti()));
        map.put(ChannelInfo.KEY_FREE_CA, String.valueOf(channel.getFreeCa()));
        map.put(ChannelInfo.KEY_SCRAMBLED, String.valueOf(channel.getScrambled()));
        map.put(ChannelInfo.KEY_SDT_VERSION, String.valueOf(channel.getSdtVersion()));
        map.put(ChannelInfo.KEY_FE_PARAS, channel.getFEParas());
        map.put(ChannelInfo.KEY_MAJOR_NUM, String.valueOf(channel.getMajorChannelNumber()));
        map.put(ChannelInfo.KEY_MINOR_NUM, String.valueOf(channel.getMinorChannelNumber()));
        map.put(ChannelInfo.KEY_SOURCE_ID, String.valueOf(channel.getSourceId()));
        map.put(ChannelInfo.KEY_ACCESS_CONTROL, String.valueOf(channel.getAccessControled()));
        map.put(ChannelInfo.KEY_HIDDEN, String.valueOf(channel.getHidden()));
        map.put(ChannelInfo.KEY_HIDE_GUIDE, String.valueOf(channel.getHideGuide()));
        map.put(ChannelInfo.KEY_CONTENT_RATINGS, DroidLogicTvUtils.TvString.toString(channel.getContentRatings()));
        map.put(ChannelInfo.KEY_SIGNAL_TYPE, DroidLogicTvUtils.TvString.toString(channel.getSignalType()));
        map.put(ChannelInfo.KEY_VCT, "\""+channel.getVct()+"\"");
        map.put(ChannelInfo.KEY_EITV, Arrays.toString(channel.getEitVersions()));
        String output = DroidLogicTvUtils.mapToJson(map);
        values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA, output);

        values.put(ChannelInfo.COLUMN_LCN, channel.getLCN());
        values.put(ChannelInfo.COLUMN_LCN1, channel.getLCN1());
        values.put(ChannelInfo.COLUMN_LCN2, channel.getLCN2());

        return values;
    }

    private ContentValues  buildAtvChannelData (ChannelInfo channel){
        ContentValues values = new ContentValues();
        values.put(Channels.COLUMN_INPUT_ID, channel.getInputId());
        values.put(Channels.COLUMN_DISPLAY_NUMBER, channel.getDisplayNumber());
        values.put(Channels.COLUMN_DISPLAY_NAME, channel.getDisplayName());
        values.put(Channels.COLUMN_TYPE, channel.getType());
        values.put(Channels.COLUMN_BROWSABLE, channel.isBrowsable() ? 1 : 0);
        values.put(Channels.COLUMN_SERVICE_TYPE, channel.getServiceType());

        String output = DroidLogicTvUtils.mapToJson(buildAtvChannelMap(channel));
        values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA, output);

        return values;
    }

    private Map<String, String>  buildAtvChannelMap (ChannelInfo channel){
        Map<String, String> map = new HashMap<String, String>();
        map.put(ChannelInfo.KEY_VFMT, String.valueOf(channel.getVfmt()));
        map.put(ChannelInfo.KEY_FREQUENCY, String.valueOf(channel.getFrequency()));
        map.put(ChannelInfo.KEY_VIDEO_STD, String.valueOf(channel.getVideoStd()));
        map.put(ChannelInfo.KEY_AUDIO_STD, String.valueOf(channel.getAudioStd()));
        map.put(ChannelInfo.KEY_IS_AUTO_STD, String.valueOf(channel.getIsAutoStd()));
        map.put(ChannelInfo.KEY_FINE_TUNE, String.valueOf(channel.getFineTune()));
        map.put(ChannelInfo.KEY_AUDIO_COMPENSATION, String.valueOf(channel.getAudioCompensation()));
        map.put(ChannelInfo.KEY_IS_FAVOURITE, String.valueOf(channel.isFavourite() ? 1 : 0));
        map.put(ChannelInfo.KEY_MULTI_NAME, DroidLogicTvUtils.TvString.toString(channel.getDisplayNameMulti()));
        map.put(ChannelInfo.KEY_FE_PARAS, channel.getFEParas());
        map.put(ChannelInfo.KEY_MAJOR_NUM, String.valueOf(channel.getMajorChannelNumber()));
        map.put(ChannelInfo.KEY_MINOR_NUM, String.valueOf(channel.getMinorChannelNumber()));
        map.put(ChannelInfo.KEY_SOURCE_ID, String.valueOf(channel.getSourceId()));
        map.put(ChannelInfo.KEY_ACCESS_CONTROL, String.valueOf(channel.getAccessControled()));
        map.put(ChannelInfo.KEY_HIDDEN, String.valueOf(channel.getHidden()));
        map.put(ChannelInfo.KEY_HIDE_GUIDE, String.valueOf(channel.getHideGuide()));
        map.put(ChannelInfo.KEY_AUDIO_PIDS, Arrays.toString(channel.getAudioPids()));
        map.put(ChannelInfo.KEY_AUDIO_FORMATS, Arrays.toString(channel.getAudioFormats()));
        map.put(ChannelInfo.KEY_AUDIO_EXTS, Arrays.toString(channel.getAudioExts()));
        map.put(ChannelInfo.KEY_AUDIO_LANGS, DroidLogicTvUtils.TvString.toString(channel.getAudioLangs()));
        map.put(ChannelInfo.KEY_AUDIO_TRACK_INDEX, String.valueOf(channel.getAudioTrackIndex()));
        map.put(ChannelInfo.KEY_AUDIO_OUTPUT_MODE, String.valueOf(channel.getAudioOutPutMode()));
        map.put(ChannelInfo.KEY_AUDIO_CHANNEL, String.valueOf(channel.getAudioChannel()));
        map.put(ChannelInfo.KEY_SUBT_TYPES, Arrays.toString(channel.getSubtitleTypes()));
        map.put(ChannelInfo.KEY_SUBT_PIDS, Arrays.toString(channel.getSubtitlePids()));
        map.put(ChannelInfo.KEY_SUBT_STYPES, Arrays.toString(channel.getSubtitleStypes()));
        map.put(ChannelInfo.KEY_SUBT_ID1S, Arrays.toString(channel.getSubtitleId1s()));
        map.put(ChannelInfo.KEY_SUBT_ID2S, Arrays.toString(channel.getSubtitleId2s()));
        map.put(ChannelInfo.KEY_SUBT_LANGS, DroidLogicTvUtils.TvString.toString(channel.getSubtitleLangs()));
        map.put(ChannelInfo.KEY_SUBT_TRACK_INDEX, String.valueOf(channel.getSubtitleTrackIndex()));
        map.put(ChannelInfo.KEY_CONTENT_RATINGS, DroidLogicTvUtils.TvString.toString(channel.getContentRatings()));
        map.put(ChannelInfo.KEY_SIGNAL_TYPE, DroidLogicTvUtils.TvString.toString(channel.getSignalType()));
        return map;
    }

    private void declineChannelNum( ChannelInfo channel) {
        /*
        Uri channelsUri = TvContract.buildChannelsUriForInput(channel.getInputId());
        String[] projection = {Channels._ID, Channels.COLUMN_DISPLAY_NUMBER};
        String condition = Channels.COLUMN_SERVICE_TYPE + " = " + channel.getServiceType() +
            " and " + Channels.COLUMN_DISPLAY_NUMBER + " > " + channel.getNumber();

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(channelsUri, projection, condition, null, null);
            while (cursor != null && cursor.moveToNext()) {
                long rowId = cursor.getLong(findPosition(projection, Channels._ID));
                ContentValues values = new ContentValues();
                values.put(Channels.COLUMN_DISPLAY_NUMBER, channel.getNumber() - 1);
                Uri uri = TvContract.buildChannelUri(rowId);
                mContentResolver.update(uri, values, null, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        */
    }

    public void setChannelName (ChannelInfo channel, String targetName) {
        if (channel != null) {
            channel.setDisplayNameLocal(targetName);
            updateChannelInfo(channel);
        }
    }

    public void deleteChannel(ChannelInfo channel) {
        deleteChannel(channel, false);
    }

    public void deleteChannel(ChannelInfo channel, boolean updateChannelNumber) {
        Uri channelsUri = TvContract.buildChannelsUriForInput(channel.getInputId());
        String[] projection = {Channels._ID, Channels.COLUMN_DISPLAY_NUMBER, Channels.COLUMN_DISPLAY_NAME};
        ArrayList<ContentProviderOperation> ops = new ArrayList();

        int deleteCount = 0;
        deleteCount = mContentResolver.delete(channelsUri, Channels._ID + "=?", new String[]{channel.getId() + ""});

        if ((deleteCount > 0) && updateChannelNumber) {
            Cursor cursor = null;
            try {
                cursor = mContentResolver.query(channelsUri, projection,
                    Channels.COLUMN_SERVICE_TYPE + "=?", new String[]{channel.getServiceType()}, null);
                while (cursor != null && cursor.moveToNext()) {
                    long rowId = cursor.getLong(findPosition(projection, Channels._ID));
                    int number = cursor.getInt(findPosition(projection,Channels.COLUMN_DISPLAY_NUMBER));
                    String name = cursor.getString(findPosition(projection,Channels.COLUMN_DISPLAY_NAME));

                    if (number > channel.getNumber()) {
                        if (DEBUG)
                            Log.d(TAG, "deleteChannel: update channel: number=" + number + " name=" + name);
                        Uri uri = TvContract.buildChannelUri(rowId);
                        ContentValues updateValues = new ContentValues();
                        updateValues.put(Channels.COLUMN_DISPLAY_NUMBER, number -1);
                        ops.add(ContentProviderOperation.newUpdate(uri)
                            .withValues(updateValues)
                            .build());
                    }
                }
                mContentResolver.applyBatch(TvContract.AUTHORITY, ops);
            } catch (Exception e) {
                //TODO
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    public void deleteChannelsContinuous(ArrayList<ChannelInfo> channels) {
        deleteChannelsContinuous(channels, false);
    }

    public void deleteChannels(ArrayList<ChannelInfo> channels) {
        if (channels.size() <= 0)
            return ;

        Uri channelsUri = TvContract.buildChannelsUriForInput(channels.get(0).getInputId());
        ArrayList<ContentProviderOperation> ops = new ArrayList();
        try {
           for (ChannelInfo c : channels) {
               Log.d(TAG, "delete:"+c.getId());
               mContentResolver.delete(channelsUri, Channels._ID + "=?", new String[]{c.getId() + ""});
       /*        ops.add(ContentProviderOperation.newDelete(
                        TvContract.buildChannelUri(c.getId()))
                        .build()
                    );*/
            }

//            mContentResolver.applyBatch(TvContract.AUTHORITY, ops);
        } catch (Exception e) {
            //TODO
        }
   }

    public void deleteChannelsContinuous(ArrayList<ChannelInfo> channels, boolean updateChannelNumber) {
        int count = channels.size();
        if (count <= 0)
            return ;

        Uri channelsUri = TvContract.buildChannelsUriForInput(channels.get(0).getInputId());
        String[] projection = {Channels._ID, Channels.COLUMN_DISPLAY_NUMBER, Channels.COLUMN_DISPLAY_NAME};

        long startID = channels.get(0).getId();
        long endID = channels.get(count-1).getId();

        int deleteCount = 0;
        deleteCount = mContentResolver.delete(channelsUri, Channels._ID + ">=? and " + Channels._ID + "<=?", new String[]{startID + "", endID + ""});
        Log.d(TAG, "delete continuous: [" + startID + " ~ " + endID + "]");

        if ((deleteCount > 0) && updateChannelNumber) {
            Cursor cursor = null;
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            try {
                cursor = mContentResolver.query(channelsUri, projection,
                    Channels.COLUMN_SERVICE_TYPE + "=?", new String[]{channels.get(0).getServiceType()}, null);
                while (cursor != null && cursor.moveToNext()) {
                    long rowId = cursor.getLong(findPosition(projection, Channels._ID));
                    int number = cursor.getInt(findPosition(projection,Channels.COLUMN_DISPLAY_NUMBER));
                    String name = cursor.getString(findPosition(projection,Channels.COLUMN_DISPLAY_NAME));

                    if (number > channels.get(count-1).getNumber()) {
                        if (DEBUG)
                            Log.d(TAG, "deleteChannel: update channel: number=" + number + " name=" + name);
                        Uri uri = TvContract.buildChannelUri(rowId);
                        ContentValues updateValues = new ContentValues();
                        updateValues.put(Channels.COLUMN_DISPLAY_NUMBER, number - count);
                        ops.add(ContentProviderOperation.newUpdate(uri)
                            .withValues(updateValues)
                            .build());
                    }
                }
                mContentResolver.applyBatch(TvContract.AUTHORITY, ops);
            } catch (Exception e) {
                //TODO
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }


    public void swapChannel (ChannelInfo sourceChannel, ChannelInfo targetChannel) {
        if (sourceChannel == null || targetChannel == null
            || sourceChannel.getNumber() == targetChannel.getNumber())
            return;
        ContentValues updateValues = new ContentValues();

        Uri sourceUri = TvContract.buildChannelUri(sourceChannel.getId());
        updateValues.put(Channels.COLUMN_DISPLAY_NUMBER, targetChannel.getNumber());
        mContentResolver.update(sourceUri, updateValues, null, null);

        Uri targetUri = TvContract.buildChannelUri(targetChannel.getId());
        updateValues.put(Channels.COLUMN_DISPLAY_NUMBER, sourceChannel.getNumber());
        mContentResolver.update(targetUri, updateValues, null, null);
    }

    public void moveChannel (ChannelInfo sourceChannel, ChannelInfo targetChannel) {
        if ( sourceChannel == null ||  sourceChannel == null
                || targetChannel.getNumber() == sourceChannel.getNumber())
            return;

        Uri channelsUri = TvContract.buildChannelsUriForInput(sourceChannel.getInputId());
        String[] projection = {Channels._ID, Channels.COLUMN_DISPLAY_NUMBER, Channels.COLUMN_DISPLAY_NAME};

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(channelsUri, projection,
                Channels.COLUMN_SERVICE_TYPE + "=?", new String[]{sourceChannel.getServiceType()}, null);
            while (cursor != null && cursor.moveToNext()) {
                long rowId = cursor.getLong(findPosition(projection, Channels._ID));
                int number = cursor.getInt(findPosition(projection,Channels.COLUMN_DISPLAY_NUMBER));
                String name = cursor.getString(findPosition(projection,Channels.COLUMN_DISPLAY_NAME));

                Uri uri = TvContract.buildChannelUri(rowId);
                ContentValues updateValues = new ContentValues();
                if (targetChannel.getNumber() < sourceChannel.getNumber()) {
                    if (number >= targetChannel.getNumber() && number < sourceChannel.getNumber()) {
                        updateValues.put(Channels.COLUMN_DISPLAY_NUMBER, number + 1);
                        mContentResolver.update(uri, updateValues, null, null);
                    }
                } else if (targetChannel.getNumber() > sourceChannel.getNumber()){
                    if (number > sourceChannel.getNumber() && number <= targetChannel.getNumber()) {
                        updateValues.put(Channels.COLUMN_DISPLAY_NUMBER, number - 1);
                        mContentResolver.update(uri, updateValues, null, null);
                    }
                }

                if (number == sourceChannel.getNumber()) {
                    updateValues.put(Channels.COLUMN_DISPLAY_NUMBER, targetChannel.getNumber());
                    mContentResolver.update(uri, updateValues, null, null);
                }
            }
        } catch (Exception e) {
            //TODO
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void skipChannel (ChannelInfo channel) {
        if (channel != null) {
            if (channel.isBrowsable()) {
                channel.setBrowsable(false);
                channel.setFavourite(false);
            } else
                channel.setBrowsable(true);
            updateChannelInfo(channel);
        }
    }

    public void setFavouriteChannel (ChannelInfo channel) {
        if (channel != null) {
            if (!channel.isFavourite()) {
                channel.setFavourite(true);
                channel.setBrowsable(true);
            } else
                channel.setFavourite(false);
            updateChannelInfo(channel);
        }
    }

    public void updateChannelInfo(ChannelInfo channel) {
        if (channel.getInputId() == null)
            return;

        if (channel.isAnalogChannel()) {
            updateAtvChannel(channel);
        } else {
            updateDtvChannel(channel);
        }
    }

    // If a channel exists, update it. If not, insert a new one.
    public void updateOrinsertDtvChannel(ChannelInfo channel) {
        int updateRet = updateDtvChannel(channel);
        if (updateRet != UPDATE_SUCCESS) {
            insertDtvChannel(channel, updateRet);
        }
    }

    public void updateOrinsertDtvChannelWithNumber(ChannelInfo channel) {
        int updateRet = updateDtvChannel(channel);
        if (updateRet != UPDATE_SUCCESS) {
            insertDtvChannel(channel, channel.getDisplayNumber());
        }
    }

    public void updateOrinsertChannelInList(ArrayList<ChannelInfo> updatelist, ArrayList<ChannelInfo> insertlist, boolean isdtv) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        for (ChannelInfo one : updatelist) {
            long id = one.getId();
            if (id == -1) {
                id = queryChannelIdInDb(one);
                Log.d(TAG, "updateOrinsertChannelInList find id = " + id);
            }
            if (id < 0) {
                ops.add(creatOperation(isdtv, false, id, one));
            } else {
                ops.add(creatOperation(isdtv, true, id, one));
            }
            Log.d(TAG, "updateOrinsertChannelInList add update = " + one.getDisplayNumber());
        }
        for (ChannelInfo one : insertlist) {
            ops.add(creatOperation(isdtv, false, -1, one));
            Log.d(TAG, "updateOrinsertChannelInList add insert = " + one.getDisplayNumber());
        }
        try {
            mContentResolver.applyBatch(TvContract.AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
            Log.e(TAG, "updateOrinsertChannelInList Failed = " + e.getMessage());
        }
        ops.clear();
    }

    private ContentProviderOperation creatOperation(boolean isdtv, boolean isupdate, long id, ChannelInfo ch) {
        if (isdtv) {
            if (isupdate) {
                return ContentProviderOperation.newUpdate(
                                    TvContract.buildChannelUri(id))
                                    .withValues(buildDtvChannelData(ch))
                                    .build();
            } else {
                return ContentProviderOperation.newInsert(
                                    TvContract.Channels.CONTENT_URI)
                                    .withValues(buildDtvChannelData(ch))
                                    .build();
            }
        } else {
            if (isupdate) {
                return ContentProviderOperation.newUpdate(
                                    TvContract.buildChannelUri(id))
                                    .withValues(buildAtvChannelData(ch))
                                    .build();
            } else {
                return ContentProviderOperation.newInsert(
                                    TvContract.Channels.CONTENT_URI)
                                    .withValues(buildAtvChannelData(ch))
                                    .build();
            }
        }
    }

    public long queryChannelIdInDb(ChannelInfo channel) {
        long id = -1;//-1 means not init; -2 means not exist
        if (channel != null) {
             id = channel.getId();
            if (id > -1) {
                return id;
            }
        } else {
            return id;
        }
        Uri channelsUri = TvContract.buildChannelsUriForInput(channel.getInputId());
        String[] projection = {Channels._ID,
            Channels.COLUMN_SERVICE_ID,
            Channels.COLUMN_ORIGINAL_NETWORK_ID,
            Channels.COLUMN_TRANSPORT_STREAM_ID,
            Channels.COLUMN_DISPLAY_NUMBER,
            Channels.COLUMN_DISPLAY_NAME,
            Channels.COLUMN_INTERNAL_PROVIDER_DATA};

        Cursor cursor = null;
        try {
            boolean found = false;
            cursor = mContentResolver.query(channelsUri, projection, Channels.COLUMN_SERVICE_TYPE + "=?", new String[]{channel.getServiceType()}, null);
            while (cursor != null && cursor.moveToNext()) {
                long rowId = cursor.getLong(findPosition(projection, Channels._ID));

                if (channel.getId() == -1) {
                    int serviceId = cursor.getInt(findPosition(projection, Channels.COLUMN_SERVICE_ID));
                    int originalNetworkId = cursor.getInt(findPosition(projection, Channels.COLUMN_ORIGINAL_NETWORK_ID));
                    int transportStreamId = cursor.getInt(findPosition(projection, Channels.COLUMN_TRANSPORT_STREAM_ID));
                    String name = cursor.getString(findPosition(projection, Channels.COLUMN_DISPLAY_NAME));
                    int frequency = 0;
                    int index = cursor.getColumnIndex(Channels.COLUMN_INTERNAL_PROVIDER_DATA);
                    if (index >= 0) {
                        Map<String, String> parsedMap = DroidLogicTvUtils.jsonToMap(cursor.getString(index));
                        frequency = Integer.parseInt(parsedMap.get(ChannelInfo.KEY_FREQUENCY));
                    }
                    if ((serviceId == channel.getServiceId()
                        && originalNetworkId == channel.getOriginalNetworkId()
                        && transportStreamId == channel.getTransportStreamId()
                        && frequency == channel.getFrequency()
                        && TextUtils.equals(name, channel.getDisplayName())) || (channel.isAnalogChannel() && frequency == channel.getFrequency()))
                        found = true;
                }

                if (found) {
                    id = rowId;
                    return id;
                }
            }
            if (id == -1)
                return -2;
        } catch (Exception e) {
            //TODO
            Log.e(TAG, "queryChannelIdInDb Failed = " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return -1;
    }

    // If a channel exists, update it. If not, insert a new one.
    public void updateOrinsertAtvChannel(ChannelInfo channel) {
        int updateRet = updateAtvChannel(channel);
        if (updateRet != UPDATE_SUCCESS) {
            insertAtvChannel(channel, channel.getDisplayNumber());
        }
    }

    // If a channel exists, update it. If not, insert a new one.
    // if the freq diff less than 1Mhz, then it is the same channel
    public void updateOrinsertAtvChannelFuzzy(ChannelInfo channel) {
        int updateRet = updateAtvChannelFuzzy(channel);
        if (updateRet != UPDATE_SUCCESS) {
            insertAtvChannel(channel, channel.getDisplayNumber());
        }
    }

    public void updateOrinsertAtvChannelWithNumber(ChannelInfo channel) {
        int updateRet = updateAtvChannel(channel);
        if (updateRet != UPDATE_SUCCESS) {
            insertAtvChannel(channel, channel.getDisplayNumber());
        }
    }

    public void updateOrinsertAtvChannel(ChannelInfo toBeUpdated, ChannelInfo channel) {
        int updateRet = updateAtvChannel(toBeUpdated, channel);
        if (updateRet != UPDATE_SUCCESS) {
            insertAtvChannel(channel, channel.getDisplayNumber());
        }
    }

    private String getChannelType(int mode) {
        return CHANNEL_MODE_TO_TYPE_MAP.get(mode);
    }

    private int getChannelType(String type) {
        return CHANNEL_TYPE_TO_MODE_MAP.get(type);
    }

    public ArrayList<ChannelInfo> getChannelList(String curInputId, String srvType) {
        return getChannelList(curInputId, srvType, false);
    }

    public ArrayList<ChannelInfo> getChannelList(String curInputId, String srvType, boolean need_browserable) {
        ArrayList<ChannelInfo> channelList = new ArrayList<ChannelInfo>();
        Uri channelsUri = TvContract.buildChannelsUriForInput(curInputId);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(channelsUri, ChannelInfo.COMMON_PROJECTION, null, null, null);
            while (cursor != null && cursor.moveToNext()) {
                int index = cursor.getColumnIndex(Channels.COLUMN_SERVICE_TYPE);
                if (srvType.equals(cursor.getString(index))) {
                    if (!need_browserable) {
                        channelList.add(ChannelInfo.fromCommonCursor(cursor));
                    } else {
                        boolean browserable = cursor.getInt(cursor.getColumnIndex(Channels.COLUMN_BROWSABLE)) == 1 ? true : false;
                        if (browserable) {
                            channelList.add(ChannelInfo.fromCommonCursor(cursor));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (channelList.size() > 0 && channelList.get(0).getType().contains("DTMB"))
            Collections.sort(channelList, new SortIntComparator());
        else
            Collections.sort(channelList, new SortComparator());
        if (DEBUG)
            printList(channelList);
        return channelList;
    }

    public ChannelInfo getChannelInfo(Uri channelUri) {
        int matchId = DroidLogicTvUtils.matchsWhich(channelUri);
        if (matchId == DroidLogicTvUtils.NO_MATCH || matchId == DroidLogicTvUtils.MATCH_PASSTHROUGH_ID) {
            return null;
        }

        Uri uri = channelUri;
        Cursor cursor = null;
        ChannelInfo info = null;

        try {
            cursor = mContentResolver.query(uri, ChannelInfo.COMMON_PROJECTION, null, null, null);
            if (cursor != null) {
                cursor.moveToNext();
                info = ChannelInfo.fromCommonCursor(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get channel info from TvProvider.", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return info;
    }

    public Map<String, String> parseInternalProviderData(String internalData){
        return DroidLogicTvUtils.jsonToMap(internalData);
    }

    public void insertUrl( Uri contentUri, URL sourceUrl){
        if (DEBUG) {
            Log.d(TAG, "Inserting " + sourceUrl + " to " + contentUri);
        }
        InputStream is = null;
        OutputStream os = null;
        try{
            is = sourceUrl.openStream();
            os = mContentResolver.openOutputStream(contentUri);
            copy(is, os);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to write " + sourceUrl + "  to " + contentUri, ioe);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore exception.
                }
            } if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    // Ignore exception.
                }
            }
        }
    }

    public void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
    }

    private void insertLogo (String logoUrl, Uri uri) {
        Map<Uri, String> logos = new HashMap<Uri, String>();
        if (!TextUtils.isEmpty(logoUrl))
            logos.put(TvContract.buildChannelLogoUri(uri), logoUrl);

        if (!logos.isEmpty())
            new InsertLogosTask(mContext).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, logos);
    }

    public class InsertLogosTask extends AsyncTask<Map<Uri, String>, Void, Void> {
        private final Context mContext;

        InsertLogosTask(Context context) {
            mContext = context;
        }

        @Override
            public Void doInBackground(Map<Uri, String>... logosList) {
                for (Map<Uri, String> logos : logosList) {
                    for (Uri uri : logos.keySet()) {
                        try {
                            insertUrl(uri, new URL(logos.get(uri)));
                        } catch (MalformedURLException e) {
                            Log.e(TAG, "Can't load " + logos.get(uri), e);
                        }
                    }
                }
                return null;
            }
    }

    public class SortComparator implements Comparator<ChannelInfo> {
        @Override
        public int compare(ChannelInfo a, ChannelInfo b) {
            if (a.getDisplayNumber() == null)
                return -1;
            return a.getDisplayNumber().compareTo(b.getDisplayNumber());
        }
    }

    public class SortIntComparator implements Comparator<ChannelInfo> {
        @Override
        public int compare(ChannelInfo a, ChannelInfo b) {
            if (a.getDisplayNumber() == null)
                return -1;
            return Integer.parseInt(a.getDisplayNumber()) - Integer.parseInt(b.getDisplayNumber());
        }
    }

    private int findPosition(String[] list, String target) {
        int i;
        for (i = 0; i < list.length; i++) {
            if (list[i].equals(target))
                return i;
        }
        return -1;
    }

    private void printList(ArrayList<ChannelInfo> list) {
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                ChannelInfo info = list.get(i);
                Log.d(TAG, "printList: number=" + info.getDisplayNumber() + " name=" + info.getDisplayName());
            }
        }
    }

    // If a channel exists, update it. If not, insert a new one.
    public Uri updateOrinsertAtvChannel2(ChannelInfo channel) {
        Uri channelUri = null;
        int ret = 0;

        Uri channelsUri = TvContract.buildChannelsUriForInput(channel.getInputId());
        String[] projection = {Channels._ID, Channels.COLUMN_INTERNAL_PROVIDER_DATA};
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(channelsUri, projection, null, null, null);
            while (cursor != null && cursor.moveToNext()) {
                long rowId = cursor.getLong(findPosition(projection, Channels._ID));
                Map<String, String> parsedMap = parseInternalProviderData(cursor.getString(
                            findPosition(projection, Channels.COLUMN_INTERNAL_PROVIDER_DATA)));
                int frequency = Integer.parseInt(parsedMap.get(ChannelInfo.KEY_FREQUENCY));
                if (frequency == channel.getFrequency()) {
                    channelUri = TvContract.buildChannelUri(rowId);
                    mContentResolver.update(channelUri, buildAtvChannelData(channel), null, null);
                    insertLogo(channel.getLogoUrl(), channelUri);
                    ret = UPDATE_SUCCESS;
                } else {
                    ret = cursor.getCount();
                }
            }
            cursor.close();
        } catch (Exception e) {
            //TODO
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (DEBUG)
            Log.d(TAG, "update " + ((ret == UPDATE_SUCCESS) ? "found" : "notfound")
                +"][freq:"+channel.getFrequency()
                +"][name:"+channel.getDisplayName()
                +"][num:"+channel.getDisplayNumber()
                +"]");

        if (ret != UPDATE_SUCCESS) {
            channelUri = insertAtvChannel2(channel, ret);
        }
        return channelUri;
    }

    public Uri insertAtvChannel2(ChannelInfo channel, int channelNumber) {
        Uri channelsUri = TvContract.buildChannelsUriForInput(channel.getInputId());
        channel.setDisplayNumber(Integer.toString(channelNumber));
        Uri uri = mContentResolver.insert(TvContract.Channels.CONTENT_URI, buildAtvChannelData(channel));

        insertLogo(channel.getLogoUrl(), uri);

        return uri;
    }

    // If a channel exists, update it. If not, insert a new one.
    public Uri updateOrinsertDtvChannel2(ChannelInfo channel) {
        int ret = 0;
        Uri channelUri = null;

        Uri channelsUri = TvContract.buildChannelsUriForInput(channel.getInputId());
        String[] projection = {Channels._ID,
            Channels.COLUMN_SERVICE_ID,
            Channels.COLUMN_DISPLAY_NUMBER,
            Channels.COLUMN_DISPLAY_NAME,
            Channels.COLUMN_INTERNAL_PROVIDER_DATA};

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(channelsUri, projection, Channels.COLUMN_SERVICE_TYPE + "=?", new String[]{channel.getServiceType()}, null);
            while (cursor != null && cursor.moveToNext()) {
                long rowId = cursor.getLong(findPosition(projection, Channels._ID));
                int serviceId = cursor.getInt(findPosition(projection, Channels.COLUMN_SERVICE_ID));
                String name = cursor.getString(findPosition(projection, Channels.COLUMN_DISPLAY_NAME));
                int frequency = 0;
                int index = cursor.getColumnIndex(Channels.COLUMN_INTERNAL_PROVIDER_DATA);
                if (index >= 0) {
                    Map<String, String> parsedMap = DroidLogicTvUtils.jsonToMap(cursor.getString(index));
                    frequency = Integer.parseInt(parsedMap.get(ChannelInfo.KEY_FREQUENCY));
                }
                if (serviceId == channel.getServiceId() && frequency == channel.getFrequency()
                    && name.equals(channel.getDisplayName())) {
                    channelUri = TvContract.buildChannelUri(rowId);
                    channel.setDisplayNumber(cursor.getString(findPosition(projection,Channels.COLUMN_DISPLAY_NUMBER)));
                    mContentResolver.update(channelUri, buildDtvChannelData(channel), null, null);
                    insertLogo(channel.getLogoUrl(), channelUri);
                    ret = UPDATE_SUCCESS;
                    break;
                }
            }

            if (ret != UPDATE_SUCCESS)
                ret = cursor.getCount();

        } catch (Exception e) {
            //TODO
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (DEBUG)
            Log.d(TAG, "update " + ((ret == UPDATE_SUCCESS) ? "found" : "notfound")
                +" DTV CH: [sid:"+channel.getServiceId()
                +"][freq:"+channel.getFrequency()
                +"][name:"+channel.getDisplayName()
                +"][num:"+channel.getDisplayNumber()
                +"]");

        if (ret != UPDATE_SUCCESS) {
            channelUri = insertDtvChannel2(channel, ret);
        }

        return channelUri;
    }

    public Uri insertDtvChannel2(ChannelInfo channel, int channelNumber) {
        Uri channelsUri = TvContract.buildChannelsUriForInput(channel.getInputId());
        channel.setDisplayNumber(Integer.toString(channelNumber));
        Uri uri = mContentResolver.insert(TvContract.Channels.CONTENT_URI, buildDtvChannelData(channel));
        insertLogo(channel.getLogoUrl(), uri);

        if (DEBUG)
            Log.d(TAG, "Insert DTV CH: [sid:"+channel.getServiceId()
                    +"][freq:"+channel.getFrequency()
                    +"][name:"+channel.getDisplayName()
                    +"][num:"+channel.getDisplayNumber()
                    +"]");

        return uri;
    }

    public ArrayList<ChannelInfo> getChannelList(String curInputId, String[] projection, String selection, String[] selectionArgs) {
        ArrayList<ChannelInfo> channelList = new ArrayList<>();
        Uri channelsUri = TvContract.buildChannelsUriForInput(curInputId);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(channelsUri, projection, selection, selectionArgs, null);
            if (cursor == null || cursor.getCount() == 0) {
                return channelList;
            }

            while (cursor.moveToNext())
                channelList.add(ChannelInfo.fromCommonCursor(cursor));

        } catch (Exception e) {
            // TODO: handle exception
            Log.d(TAG, "Content provider query: " + e.getStackTrace());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return channelList;
    }


    private static final int BATCH_OPERATION_COUNT = 100;

    private static boolean isATSCSpecialProgram(Program program) {
        //If program's startTime == EndTime == 0,
        //it is a special program for descr update in atsc.
        return program.getStartTimeUtcMillis() == 0
            && program.getEndTimeUtcMillis() == 0;
    }

    /**
     * Updates the system database, TvProvider, with the given programs.
     *
     * <p>If there is any overlap between the given and existing programs, the existing ones
     * will be updated with the given ones if they have the same title or replaced.
     *
     * @param channelUri The channel where the program info will be added.
     * @param newPrograms A list of {@link Program} instances which includes program
     *         information.
     */
    public void updatePrograms(Uri channelUri, List<Program> newPrograms) {
        updatePrograms(channelUri, newPrograms, null, false);
    }

    public static final class ComparatorValues implements Comparator<Program> {
        @Override
        public int compare(Program object1, Program object2) {
            long m1 = object1.getStartTimeUtcMillis();
            long m2 = object2.getStartTimeUtcMillis();
            int result = 0;
            if (m1 > m2)
            {
                result = 1;
            }
            if (m1 < m2)
            {
                result=-1;
            }
            return result;
        }
    }

    public boolean isProgramAtTime(Program program, Long timeUtcMillis) {
        return (timeUtcMillis != null
            && timeUtcMillis >= program.getStartTimeUtcMillis()
            && timeUtcMillis < program.getEndTimeUtcMillis());
    }

    public boolean updatePrograms(Long channelId, List<Program> newPrograms, Long timeUtcMillis) {
        boolean updated = false;
        Log.d(TAG, "updatePrograms epg start-----");
        for (Program p: newPrograms) {
            String sql_start ;
            Program oldProgram = null;
            String sql_query ;
            Cursor cursor ;
            Log.d(TAG, "updatePrograms epg title:"+p.getTitle()+" des:"+p.getDescription()+" chid:"+p.getChannelId()+" id:"+p.getId()+" start:" + p.getStartTimeUtcMillis() + " end:" + p.getEndTimeUtcMillis());
            if (isATSCSpecialProgram(p))
            {
                // if (true)
                //  continue;
                //sql_query = "(_id=" + p.getId() + ")";
                sql_query = "(" + TvContract.Programs.COLUMN_CHANNEL_ID + "=" + channelId + ") AND ("+ TvContract.Programs.COLUMN_INTERNAL_PROVIDER_FLAG2 +"=" + p.getProgramId() + ")";
                cursor =  mContentResolver.query(TvContract.Programs.CONTENT_URI, null, sql_query, null, null);
                if (cursor != null && cursor.getCount() != 0) {
                    cursor.moveToNext();
                    oldProgram = Program.fromCursor(cursor);
                }
                if (cursor != null) {
                    cursor.close();
                }
                if (oldProgram != null && !TextUtils.equals(p.getDescription(), oldProgram.getDescription()))
                {
                    ContentValues vals = new ContentValues();
                    vals.put(TvContract.Programs.COLUMN_SHORT_DESCRIPTION, p.getDescription());
                    Log.d(TAG, "updatePrograms sql sql_query:" + sql_query);
                    mContentResolver.update(TvContract.Programs.CONTENT_URI, vals, sql_query, null);
                }
                else
                {
                    Log.d(TAG, "updatePrograms isATSCSpecialProgram true");
                }
            }
            else
            {
                 sql_start = TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS + "=" + p.getStartTimeUtcMillis() + " AND " + TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS + " = " +p.getEndTimeUtcMillis();
                 oldProgram = null;
                 sql_query = "(" + TvContract.Programs.COLUMN_CHANNEL_ID + "=" + channelId + ") AND (" + sql_start + ")";
                 cursor =  mContentResolver.query(TvContract.Programs.CONTENT_URI, null, sql_query, null, null);
                if (cursor != null && cursor.getCount() != 0) {
                    cursor.moveToNext();
                    oldProgram = Program.fromCursor(cursor);
                }
                if (cursor != null) {
                    cursor.close();
                }
                if (oldProgram == null || !isProgramEq(oldProgram, p))
                {
                    sql_start = TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS + "<=" + p.getStartTimeUtcMillis() + " AND " + TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS + " > " +p.getStartTimeUtcMillis();
                    String sql_end = TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS + "<=" + p.getEndTimeUtcMillis() + " AND " + TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS + " > " +p.getEndTimeUtcMillis();
                    String sql_del = "(" + TvContract.Programs.COLUMN_CHANNEL_ID + "=" + channelId + ") AND ((" + sql_start + ") OR (" + sql_end + "))";
                    if (oldProgram == null) {
                        Log.d(TAG, "updatePrograms oldProgram is null insert sql:" + sql_del);
                    } else {
                        Log.d(TAG, "updatePrograms not eq insert sql:" + sql_del);
                    }
                    mContentResolver.delete(TvContract.Programs.CONTENT_URI, sql_del, null);
                    mContentResolver.insert(TvContract.Programs.CONTENT_URI, p.toContentValues());
                    updated = isProgramAtTime(p, timeUtcMillis);
                }
                else
                {
                    Log.d(TAG, "updatePrograms no insert true");
                }
            }
        }
        Log.d(TAG, "updatePrograms epg end-----");
        return updated;
    }
    public boolean updatePrograms(Uri channelUri, List<Program> newPrograms, Long timeUtcMillis, boolean isAtsc) {
        boolean updated = false;
        final int fetchedProgramsCount = newPrograms.size();
        if (fetchedProgramsCount == 0) {
            return updated;
        }
        List<Program> oldPrograms = getPrograms(TvContract.buildProgramsUriForChannel(channelUri));

        Collections.sort(newPrograms, new ComparatorValues());
        Collections.sort(oldPrograms, new ComparatorValues());
        //Log.d(TAG, "updatePrograms sort programs ");
        Program firstNewProgram = null;
        for (Program program : newPrograms) {
            if (isAtsc && !isATSCSpecialProgram(program)) {
                firstNewProgram = program;
                break;
            }
        }

//        for (Program p : newPrograms) {
//            Log.d(TAG, "epg todo:cid("+p.getChannelId()+")eid("+p.getProgramId()+")desc("+p.getTitle()+")desc2("+p.getDescription()+")time("+p.getStartTimeUtcMillis()+"-"+p.getEndTimeUtcMillis()+")");
//        }

        int oldProgramsIndex = 0;
        int newProgramsIndex = 0;

        // Skip the past programs. They will be automatically removed by the system.
        if (firstNewProgram != null) {
            for (Program program : oldPrograms) {
                if (program.getEndTimeUtcMillis() > firstNewProgram.getStartTimeUtcMillis())
                    break;
                oldProgramsIndex++;
            }
        }

        // Compare the new programs with old programs one by one and update/delete the old one or
        // insert new program if there is no matching program in the database.
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        while (newProgramsIndex < fetchedProgramsCount) {
            Program oldProgram = oldProgramsIndex < oldPrograms.size()
                    ? oldPrograms.get(oldProgramsIndex) : null;
            Program newProgram = newPrograms.get(newProgramsIndex);
            boolean addNewProgram = false;

            if (oldProgram != null) {

                if (isAtsc && isATSCSpecialProgram(newProgram)) {
                    //Log.d(TAG, "ext desr:"+newProgram.getProgramId());
                    for (Program program : oldPrograms) {
                        //Log.d(TAG, "old:"+program.getProgramId());
                        if (program.getProgramId() == newProgram.getProgramId()) {//same event id
                            if (TextUtils.equals(program.getDescription(), newProgram.getDescription()))
                                break;
                            program.setDescription(newProgram.getDescription());
                            ops.add(ContentProviderOperation.newUpdate(
                                    TvContract.buildProgramUri(program.getId()))
                                    .withValues(program.toContentValues())
                                    .build());
                            /*Program p = new Program.Builder(program)
                                .build();
                            ops.add(ContentProviderOperation.newInsert(TvContract.Programs.CONTENT_URI)
                                                        .withValues(p.toContentValues())
                                                        .build());
                            ops.add(ContentProviderOperation.newDelete(TvContract.buildProgramUri(program.getId()))
                                                        .build());
                            */
                            //Log.d(TAG, "\tepg etm:cid("+program.getChannelId()+")eid("+program.getProgramId()+")");
                            break;
                        }
                    }
                    newProgramsIndex++;
                } else if ((isAtsc && oldProgram.matchsWithoutDescription(newProgram))
                    || (oldProgram.equals(newProgram))) {
                    // Exact match. No need to update. Move on to the next programs.
                    oldProgramsIndex++;
                    newProgramsIndex++;
                    //Log.d(TAG, "\tepg match:cid("+newProgram.getChannelId()+")eid("+newProgram.getProgramId()+")desc("+newProgram.getTitle()+")time("+newProgram.getStartTimeUtcMillis()+"-"+newProgram.getEndTimeUtcMillis()+")");
                } else if (needsUpdate(oldProgram, newProgram)) {
                    // Partial match. Update the old program with the new one.
                    // NOTE: Use 'update' in this case instead of 'insert' and 'delete'. There could
                    // be application specific settings which belong to the old program.
                    ops.add(ContentProviderOperation.newUpdate(
                            TvContract.buildProgramUri(oldProgram.getId()))
                            .withValues(newProgram.toContentValues())
                            .build());
                    oldProgramsIndex++;
                    newProgramsIndex++;

                    updated = isProgramAtTime(newProgram, timeUtcMillis);

                    Log.d(TAG, "\tepg update:cid("+newProgram.getChannelId()+")eid("+newProgram.getProgramId()+")desc("+newProgram.getTitle()+")time("+newProgram.getStartTimeUtcMillis()+"-"+newProgram.getEndTimeUtcMillis()+")id("+oldProgram.getId()+")");
                } else if (oldProgram.getEndTimeUtcMillis() < newProgram.getEndTimeUtcMillis()) {
                    // No match. Remove the old program first to see if the next program in
                    // {@code oldPrograms} partially matches the new program.
                    ops.add(ContentProviderOperation.newDelete(
                            TvContract.buildProgramUri(oldProgram.getId()))
                            .build());
                    oldProgramsIndex++;

                    updated = isProgramAtTime(oldProgram, timeUtcMillis);

                    Log.d(TAG, "\tepg delete:"+oldProgram.getId()+":cid("+oldProgram.getChannelId()+")eid("+oldProgram.getProgramId()+")desc("+oldProgram.getTitle()+")time("+oldProgram.getStartTimeUtcMillis()+"-"+oldProgram.getEndTimeUtcMillis()+")");
                } else {
                    if (!isATSCSpecialProgram(newProgram)) {
                        // No match. The new program does not match any of the old programs. Insert it
                        // as a new program.
                        addNewProgram = true;
                        newProgramsIndex++;

                        updated = isProgramAtTime(newProgram, timeUtcMillis);

                        Log.d(TAG, "\tepg new:cid("+newProgram.getChannelId()+")eid("+newProgram.getProgramId()+")desc("+newProgram.getTitle()+")time("+newProgram.getStartTimeUtcMillis()+"-"+newProgram.getEndTimeUtcMillis()+")id("+newProgram.getId()+")");
                    }
                }
            } else {
                if (!isATSCSpecialProgram(newProgram)) {
                    // No old programs. Just insert new programs.
                    addNewProgram = true;

                    updated = isProgramAtTime(newProgram, timeUtcMillis);

                    Log.d(TAG, "\tepg new:(old none):cid("+newProgram.getChannelId()+")eid("+newProgram.getProgramId()+")desc("+newProgram.getTitle()+")time("+newProgram.getStartTimeUtcMillis()+"-"+newProgram.getEndTimeUtcMillis()+")id("+newProgram.getId()+")");
                }
                newProgramsIndex++;
            }

            if (addNewProgram) {
                ops.add(ContentProviderOperation
                        .newInsert(TvContract.Programs.CONTENT_URI)
                        .withValues(newProgram.toContentValues())
                        .build());
            }

            // Throttle the batch operation not to cause TransactionTooLargeException.
            if (ops.size() > BATCH_OPERATION_COUNT
                    || newProgramsIndex >= fetchedProgramsCount) {
                try {
                    mContentResolver.applyBatch(TvContract.AUTHORITY, ops);
                } catch (RemoteException | OperationApplicationException e) {
                    Log.e(TAG, "Failed to insert programs.", e);
                    return updated;
                }
                ops.clear();
            }
        }
        return updated;
    }

    public void updateProgram(Program program) {
        mContentResolver.update(TvContract.buildProgramUri(program.getId()), program.toContentValues(), null, null);
    }

    /**
     * Returns {@code true} if the {@code oldProgram} program needs to be updated with the
     * {@code newProgram} program.
     */
    private static boolean isProgramEq(Program oldProgram, Program newProgram) {
        // NOTE: Here, we update the old program if it has the same title and overlaps with the new
        // program. The test logic is just an example and you can modify this. E.g. check whether
        // the both programs have the same program ID if your EPG supports any ID for the programs.
        if (oldProgram.getTitle() == null || oldProgram.getTitle().isEmpty()) {
            Log.d(TAG, "getTitle is null");
            return false;
        } else if (!oldProgram.getTitle().equals(newProgram.getTitle())) {
            Log.d(TAG, "getTitle is not eq");
            return false;
        } else if (oldProgram.getDescription() == null && newProgram.getDescription() != null) {
            Log.d(TAG, "getDescription is old is null new not null");
            return false;
        } else if (!Objects.equals(Program.contentRatingsToString(oldProgram.getContentRatings()),
            Program.contentRatingsToString(newProgram.getContentRatings()))) {
            Log.d(TAG, "ratings not eq");
            return false;
        } else {
            Log.d(TAG, "isProgramEq is eq true");
            return true;
        }
    }
    /**
     * Returns {@code true} if the {@code oldProgram} program needs to be updated with the
     * {@code newProgram} program.
     */
    private static boolean needsUpdate(Program oldProgram, Program newProgram) {
        // NOTE: Here, we update the old program if it has the same title and overlaps with the new
        // program. The test logic is just an example and you can modify this. E.g. check whether
        // the both programs have the same program ID if your EPG supports any ID for the programs.
        if (oldProgram.getTitle()== null ||oldProgram.getTitle().isEmpty()) {
            return false;
        }else {
            return oldProgram.getTitle().equals(newProgram.getTitle())
                && !(oldProgram.getStartTimeUtcMillis() == newProgram.getStartTimeUtcMillis()
                    && oldProgram.getEndTimeUtcMillis() == newProgram.getEndTimeUtcMillis())
                && oldProgram.getStartTimeUtcMillis() < newProgram.getEndTimeUtcMillis()
                && newProgram.getStartTimeUtcMillis() < oldProgram.getEndTimeUtcMillis();
        }
    }

    public List<Program> getPrograms(Uri uri) {
        Cursor cursor = null;
        List<Program> programs = new ArrayList<>();
        try {
            // TvProvider returns programs chronological order by default.
            cursor = mContentResolver.query(uri, null, null, null, null);
            if (cursor == null || cursor.getCount() == 0) {
                return programs;
            }
            while (cursor.moveToNext()) {
                programs.add(Program.fromCursor(cursor));
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to get programs for " + uri, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return programs;
    }

    public List<Program> getAppointedPrograms() {
        Uri uri = TvContract.Programs.CONTENT_URI;
        Cursor cursor = null;
        List<Program> programs = new ArrayList<>();
        try {
            // TvProvider returns programs chronological order by default.
            cursor = mContentResolver.query(uri, null, TvContract.Programs.COLUMN_INTERNAL_PROVIDER_FLAG1 + "=?", new String[]{"1"}, null);
            if (cursor == null || cursor.getCount() == 0) {
                return programs;
            }
            while (cursor.moveToNext()) {
                programs.add(Program.fromCursor(cursor));
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to get appointed programs ", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return programs;
    }

    public Program getProgram(long programId) {
        Uri uri = TvContract.buildProgramUri(programId);

        return getProgram(uri);
    }

    public Program getProgram(Uri uri) {
        Cursor cursor = null;
        Program program = null;

        try {
            cursor = mContentResolver.query(uri, null, null, null, null);
            if (cursor != null) {
                cursor.moveToNext();
                program = Program.fromCursor(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get program from TvProvider.", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return program;
    }

    public Program getProgram(Uri channelUri, long nowtime) {
        Uri uri = TvContract.buildProgramsUriForChannel(channelUri);
        List<Program> channel_programs = getPrograms(uri);
        Program program = null;
        int j = 0;
        for (j = 0; j < channel_programs.size(); j++) {
            program = channel_programs.get(j);
            if (nowtime >= program.getStartTimeUtcMillis() && nowtime < program.getEndTimeUtcMillis())
                break;
        }
        if (j == channel_programs.size())
            program = null;

        return program;
    }

    public int deleteProgram(ChannelInfo channel) {
        return deleteProgram(channel.getId());
    }

    public int deleteProgram(Long channelId) {
        int deleteCount = mContentResolver.delete(
                TvContract.Programs.CONTENT_URI,
                TvContract.Programs.COLUMN_CHANNEL_ID + "=?",
                new String[] { String.valueOf(channelId)});
        if (deleteCount > 0) {
            Log.d(TAG, "Deleted " + deleteCount + " programs");
        }
        return deleteCount;
    }

    public int deleteProgram(Long channelId, String versionNotEqual, String eitExt) {
        int deleteCount = mContentResolver.delete(
                TvContract.Programs.CONTENT_URI,
                TvContract.Programs.COLUMN_CHANNEL_ID + "=? AND "
                + TvContract.Programs.COLUMN_VERSION_NUMBER + "!=? AND "
                + TvContract.Programs.COLUMN_INTERNAL_PROVIDER_FLAG3 + "=?",
                new String[] { String.valueOf(channelId), versionNotEqual, eitExt});
        if (deleteCount > 0) {
            Log.d(TAG, "Deleted " + deleteCount + " programs");
        }
        return deleteCount;
    }

    public int deletePrograms(Long channelId, String versionNotEqual) {
        int deleteCount = mContentResolver.delete(
                TvContract.Programs.CONTENT_URI,
                TvContract.Programs.COLUMN_CHANNEL_ID + "=? AND "
                + TvContract.Programs.COLUMN_VERSION_NUMBER + "!=?",
                new String[] { String.valueOf(channelId), versionNotEqual});
        if (deleteCount > 0) {
            Log.d(TAG, "Deleted " + deleteCount + " programs");
        }
        return deleteCount;
    }
}
