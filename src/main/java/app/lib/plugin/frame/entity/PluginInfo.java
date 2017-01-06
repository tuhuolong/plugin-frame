
package app.lib.plugin.frame.entity;

import android.os.Parcel;
import android.os.Parcelable;

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
    private PluginPackageInfo mInstalledPackageInfo;
    // 已下载信息
    private PluginPackageInfo mDownloadedPackageInfo;

    public PluginInfo() {
    }

    protected PluginInfo(Parcel in) {
        this.mPluginId = in.readString();
        this.mName = in.readString();
        this.mIcon = in.readString();
        this.mInstalledPackageInfo = in.readParcelable(PluginPackageInfo.class.getClassLoader());
        this.mDownloadedPackageInfo = in.readParcelable(PluginPackageInfo.class.getClassLoader());
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

    public synchronized void setInstalledPackageInfo(PluginPackageInfo installedPackageInfo) {
        mInstalledPackageInfo = installedPackageInfo;
    }

    public synchronized PluginPackageInfo getDownloadedPackageInfo() {
        return mDownloadedPackageInfo;
    }

    public synchronized void setDownloadedPackageInfo(PluginPackageInfo downloadedPackageInfo) {
        mDownloadedPackageInfo = downloadedPackageInfo;
    }

    public synchronized boolean isDownloaded() {
        return mDownloadedPackageInfo != null;
    }

    public synchronized boolean isInstalled() {
        return mInstalledPackageInfo != null;
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
        dest.writeParcelable(this.mInstalledPackageInfo, flags);
        dest.writeParcelable(this.mDownloadedPackageInfo, flags);
    }
}
