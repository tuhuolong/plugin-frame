
package app.lib.plugin.frame.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by chenhao on 17/1/10.
 */

public class PluginDeveloperInfo implements Parcelable {
    public static final Creator<PluginDeveloperInfo> CREATOR = new Creator<PluginDeveloperInfo>() {
        @Override
        public PluginDeveloperInfo createFromParcel(Parcel source) {
            return new PluginDeveloperInfo(source);
        }

        @Override
        public PluginDeveloperInfo[] newArray(int size) {
            return new PluginDeveloperInfo[size];
        }
    };
    private String mDeveloperId;
    private String mCert;

    public PluginDeveloperInfo() {
    }

    protected PluginDeveloperInfo(Parcel in) {
        this.mDeveloperId = in.readString();
        this.mCert = in.readString();
    }

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mDeveloperId);
        dest.writeString(this.mCert);
    }
}
