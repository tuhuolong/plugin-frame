
package app.lib.plugin.frame.entity;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by chenhao on 17/1/6.
 */

public class PluginInfo implements Parcelable {
    public static final Creator<PluginInfo> CREATOR = new Creator<PluginInfo>() {
        @Override
        public PluginInfo createFromParcel(Parcel source) {
            return new PluginInfo(source);
        }

        @Override
        public PluginInfo[] newArray(int size) {
            return new PluginInfo[size];
        }
    };
    private String mPluginId;
    private String mName;
    private String mIcon;
    // 已安装信息
    private int mInstalledVersionCode;
    private PluginPackageInfo mInstalledPackageInfo;
    // 已下载信息
    private int mDownloadedVersionCode;
    private PluginPackageInfo mDownloadedPackageInfo;

    public PluginInfo() {
    }

    protected PluginInfo(Parcel in) {
        this.mPluginId = in.readString();
        this.mName = in.readString();
        this.mIcon = in.readString();
        this.mInstalledVersionCode = in.readInt();
        this.mInstalledPackageInfo = in.readParcelable(PluginPackageInfo.class.getClassLoader());
        this.mDownloadedVersionCode = in.readInt();
        this.mDownloadedPackageInfo = in.readParcelable(PluginPackageInfo.class.getClassLoader());
    }

    public static PluginInfo buildFromPref(String pluginId, String prefJsonStr) {
        PluginInfo pluginInfo = new PluginInfo();
        try {
            JSONObject configJson = new JSONObject(prefJsonStr);
            pluginInfo.mPluginId = pluginId;
            pluginInfo.mName = configJson.optString("name");
            pluginInfo.mIcon = configJson.optString("icon");
            pluginInfo.mInstalledVersionCode = configJson.optInt("installed");
            pluginInfo.mDownloadedVersionCode = configJson.optInt("downloaded");
        } catch (JSONException e) {
            return null;
        }
        return pluginInfo;
    }

    public synchronized String toPrefJsonStr() {
        String jsonStr = "";
        try {
            JSONObject json = new JSONObject();
            json.put("name", mName);
            json.put("icon", mIcon);
            json.put("installed", mInstalledVersionCode);
            json.put("downloaded", mDownloadedVersionCode);
            jsonStr = json.toString();
        } catch (JSONException e) {
        }
        return jsonStr;
    }

    public synchronized String getPluginId() {
        return mPluginId;
    }

    public synchronized void setPluginId(String pluginId) {
        mPluginId = pluginId;
    }

    public synchronized String getName() {
        return mName;
    }

    public synchronized void setName(String name) {
        mName = name;
    }

    public synchronized String getIcon() {
        return mIcon;
    }

    public synchronized void setIcon(String icon) {
        mIcon = icon;
    }

    public synchronized PluginPackageInfo getInstalledPackageInfo() {
        return mInstalledPackageInfo;
    }

    public synchronized void setInstalledPackageInfo(int installedVersionCode,
            PluginPackageInfo installedPackageInfo) {
        mInstalledVersionCode = installedVersionCode;
        mInstalledPackageInfo = installedPackageInfo;
    }

    public synchronized PluginPackageInfo getDownloadedPackageInfo() {
        return mDownloadedPackageInfo;
    }

    public synchronized void setDownloadedPackageInfo(int downloadedVersionCode,
            PluginPackageInfo downloadedPackageInfo) {
        mDownloadedVersionCode = downloadedVersionCode;
        mDownloadedPackageInfo = downloadedPackageInfo;
    }

    public synchronized boolean isDownloaded() {
        return mDownloadedPackageInfo != null;
    }

    public synchronized boolean isInstalled() {
        return mInstalledPackageInfo != null;
    }

    public synchronized int getInstalledVersionCode() {
        return mInstalledVersionCode;
    }

    public synchronized int getDownloadedVersionCode() {
        return mDownloadedVersionCode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mPluginId);
        dest.writeString(this.mName);
        dest.writeString(this.mIcon);
        dest.writeInt(this.mInstalledVersionCode);
        dest.writeParcelable(this.mInstalledPackageInfo, flags);
        dest.writeInt(this.mDownloadedVersionCode);
        dest.writeParcelable(this.mDownloadedPackageInfo, flags);
    }
}
