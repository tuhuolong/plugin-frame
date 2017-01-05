
package app.lib.plugin.frame.runtime.bridge;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by chenhao on 16/12/24.
 */
public class BridgeError implements Parcelable {

    public static final Creator<BridgeError> CREATOR = new Creator<BridgeError>() {
        @Override
        public BridgeError createFromParcel(Parcel source) {
            return new BridgeError(source);
        }

        @Override
        public BridgeError[] newArray(int size) {
            return new BridgeError[size];
        }
    };
    private int mCode;
    private String mDetail;

    public BridgeError(int code, String detail) {
        mCode = code;
        mDetail = detail;
    }

    protected BridgeError(Parcel in) {
        this.mCode = in.readInt();
        this.mDetail = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mCode);
        dest.writeString(this.mDetail);
    }

    final public int getCode() {
        return mCode;
    }

    final public String getDetail() {
        return mDetail;
    }
}
