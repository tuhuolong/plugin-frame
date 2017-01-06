
package app.lib.plugin.frame.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by chenhao on 17/1/6.
 */

public class PluginPackageInfo implements Parcelable {
    public static final Creator<PluginPackageInfo> CREATOR = new Creator<PluginPackageInfo>() {
        @Override
        public PluginPackageInfo createFromParcel(Parcel source) {
            return new PluginPackageInfo(source);
        }

        @Override
        public PluginPackageInfo[] newArray(int size) {
            return new PluginPackageInfo[size];
        }
    };
    private String mPluginId;
    private int mVersionCode;
    private String mPackagePath;
    private int mMinApiLevel;
    private String mDeveloperId;
    private String mPackageName;

    public PluginPackageInfo() {
    }

    protected PluginPackageInfo(Parcel in) {
        this.mPluginId = in.readString();
        this.mVersionCode = in.readInt();
        this.mPackagePath = in.readString();
        this.mMinApiLevel = in.readInt();
        this.mDeveloperId = in.readString();
        this.mPackageName = in.readString();
    }

    public synchronized String getPluginId() {
        return mPluginId;
    }

    public synchronized void setPluginId(String pluginId) {
        mPluginId = pluginId;
    }

    public synchronized int getVersionCode() {
        return mVersionCode;
    }

    public synchronized void setVersionCode(int versionCode) {
        mVersionCode = versionCode;
    }

    public synchronized String getPackagePath() {
        return mPackagePath;
    }

    public synchronized void setPackagePath(String packagePath) {
        mPackagePath = packagePath;
    }

    public synchronized int getMinApiLevel() {
        return mMinApiLevel;
    }

    public synchronized void setMinApiLevel(int minApiLevel) {
        mMinApiLevel = minApiLevel;
    }

    public synchronized String getDeveloperId() {
        return mDeveloperId;
    }

    public synchronized void setDeveloperId(String developerId) {
        mDeveloperId = developerId;
    }

    public synchronized String getPackageName() {
        return mPackageName;
    }

    public synchronized void setPackageName(String packageName) {
        mPackageName = packageName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mPluginId);
        dest.writeInt(this.mVersionCode);
        dest.writeString(this.mPackagePath);
        dest.writeInt(this.mMinApiLevel);
        dest.writeString(this.mDeveloperId);
        dest.writeString(this.mPackageName);
    }
}
