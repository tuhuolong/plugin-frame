
package app.lib.plugin.frame.debug;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import app.lib.asynccallback.AsyncCallback;
import app.lib.asynccallback.Error;
import app.lib.plugin.frame.PluginApi;

/**
 * Created by chenhao on 17/1/9.
 */

public class OpenApiReceiver extends BroadcastReceiver {
    public static final String OPEN_API_ACTION = "app.lib.plugin.action.OPEN_API";

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return;
        }

        if (action.equals(OPEN_API_ACTION)) {
            String msgType = intent.getStringExtra("type");
            String msgSubType = intent.getStringExtra("sub_type");
            if (!TextUtils.isEmpty(msgType)) {
                if (TextUtils.isEmpty(msgSubType)) {
                    return;
                }

                if (msgType.equalsIgnoreCase("plugin_debug")) {
                    String apkPath = intent.getStringExtra("apk_path");

                    PluginApi.getInstance().installDebugPlugin(apkPath,
                            new AsyncCallback<Void, Error>() {
                                @Override
                                public void onSuccess(Void result) {

                                }

                                @Override
                                public void onFailure(Error error) {

                                }
                            });
                }
            }
        }
    }
}
