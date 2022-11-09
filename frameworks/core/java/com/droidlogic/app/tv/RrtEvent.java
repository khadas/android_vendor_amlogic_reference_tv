package com.droidlogic.app.tv;

import android.os.Parcel;
import android.util.Log;
import vendor.amlogic.hardware.tvserver.V1_0.TvHidlParcel;



public class RrtEvent {
    private static final String TAG = "RrtEvent";
    public int      tableId;
    public int      ratingRegion;
    public int      versionNumber;
    public String   ratingRegionName;
    public int      dimensionDefined;
    public String   dimensionName;
    public int      graduatedScale;
    public int      valuesDefined;
    public String   abbrevRatingValue;
    public String   ratingValue;

    public void printRrtEventInfo(){
        Log.i(TAG,"[RrtEventInfo]"+
            "\n tableId = "+tableId+
            "\n ratingRegion = "+ratingRegion+
            "\n versionNumber = "+versionNumber+
            "\n dimensionDefined = "+dimensionDefined);
    }

    public void readRrtEvent(TvHidlParcel p) {
        Log.i(TAG,"readRrtEvent");
        tableId = p.bodyInt.get(1);
        ratingRegion = p.bodyInt.get(2);
        versionNumber = p.bodyInt.get(3);
        ratingRegionName = p.bodyString.get(0);
        dimensionDefined = p.bodyInt.get(4);
        dimensionName = p.bodyString.get(1);
        graduatedScale = p.bodyInt.get(5);
        valuesDefined = p.bodyInt.get(6);;
        abbrevRatingValue = p.bodyString.get(2);
        ratingValue = p.bodyString.get(3);
    }
 }

