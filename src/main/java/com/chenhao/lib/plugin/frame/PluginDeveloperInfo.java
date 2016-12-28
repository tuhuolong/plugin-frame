
package com.chenhao.lib.plugin.frame;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by chenhao on 16/12/24.
 */

public class PluginDeveloperInfo {
    private String mDeveloperId;
    private String mKey;

    public static PluginDeveloperInfo buildFromPref(String developerId, String prefJsonStr) {
        PluginDeveloperInfo developerInfo = new PluginDeveloperInfo();
        try {
            JSONObject configJson = new JSONObject(prefJsonStr);
            developerInfo.mDeveloperId = developerId;
            developerInfo.mKey = configJson.optString("key");
        } catch (JSONException e) {
            return null;
        }
        return developerInfo;
    }

    public synchronized String toPrefJsonStr() {
        String jsonStr = "";
        try {
            JSONObject json = new JSONObject();
            json.put("key", mKey);
            jsonStr = json.toString();
        } catch (JSONException e) {
        }
        return jsonStr;
    }

    public synchronized String getDeveloperId() {
        return mDeveloperId;
    }

    public synchronized void setDeveloperId(String developerId) {
        mDeveloperId = developerId;
    }

    public synchronized String getKey() {
        if (TextUtils.isEmpty(mKey)) {
            return "";
        }
        return mKey;
    }

    public synchronized void setKey(String key) {
        mKey = key;
    }
}
