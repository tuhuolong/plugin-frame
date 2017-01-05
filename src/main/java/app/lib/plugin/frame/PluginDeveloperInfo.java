
package app.lib.plugin.frame;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by chenhao on 16/12/24.
 */

public class PluginDeveloperInfo {
    private String mDeveloperId;
    private String mCert;

    public static PluginDeveloperInfo buildFromPref(String developerId, String prefJsonStr) {
        PluginDeveloperInfo developerInfo = new PluginDeveloperInfo();
        try {
            JSONObject configJson = new JSONObject(prefJsonStr);
            developerInfo.mDeveloperId = developerId;
            developerInfo.mCert = configJson.optString("cert");
        } catch (JSONException e) {
            return null;
        }
        return developerInfo;
    }

    public synchronized String toPrefJsonStr() {
        String jsonStr = "";
        try {
            JSONObject json = new JSONObject();
            json.put("cert", mCert);
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

    public synchronized String getCert() {
        if (TextUtils.isEmpty(mCert)) {
            return "";
        }
        return mCert;
    }

    public synchronized void setCert(String cert) {
        mCert = cert;
    }
}
