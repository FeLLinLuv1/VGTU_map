package com.example.vgtu_map;

import android.util.Log;
import android.webkit.JavascriptInterface;

public class WebAppInterface {
    private final map mContext;
    private static final String TAG = "WebAppInterface";

    WebAppInterface(map context) {
        mContext = context;
    }

    @JavascriptInterface
    public void fetchAuditoriumDetails(String auditoriumId) {
        Log.d(TAG, "fetchAuditoriumDetails вызван с ID: " + auditoriumId);
        mContext.fetchAuditoriumDetailsFromDB(auditoriumId);
    }
}