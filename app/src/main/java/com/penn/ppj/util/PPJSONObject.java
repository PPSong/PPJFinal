package com.penn.ppj.util;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by penn on 08/04/2017.
 */

public class PPJSONObject {
    private JSONObject jsonObject;

    public PPJSONObject() {
        jsonObject = new JSONObject();
    }

    public PPJSONObject put(String name, String string) {
        try {
            jsonObject.put(name, string);
        } catch (JSONException e) {
            Log.v("ppLog", "PPJSONObject error:" + e);
        }

        return this;
    }

    public PPJSONObject put(String name, int i) {
        try {
            jsonObject.put(name, i);
        } catch (JSONException e) {
            Log.v("ppLog", "PPJSONObject error:" + e);
        }

        return this;
    }

    public PPJSONObject put(String name, long i) {
        try {
            jsonObject.put(name, i);
        } catch (JSONException e) {
            Log.v("ppLog", "PPJSONObject error:" + e);
        }

        return this;
    }

    public PPJSONObject put(String name, JSONArray ja) {
        try {
            jsonObject.put(name, ja);
        } catch (JSONException e) {
            Log.v("ppLog", "PPJSONObject error:" + e);
        }

        return this;
    }

    public JSONObject getJSONObject() {
        return jsonObject;
    }
}
