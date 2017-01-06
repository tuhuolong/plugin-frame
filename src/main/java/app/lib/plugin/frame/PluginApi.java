
package app.lib.plugin.frame;

import android.content.Context;
import android.os.Bundle;

import app.lib.asynccallback.AsyncCallback;
import app.lib.asynccallback.Error;
import app.lib.plugin.frame.entity.PluginInfo;

/**
 * Created by chenhao on 17/1/6.
 */

public class PluginApi {

    private static final Object sLock = new Object();
    private static PluginApi sInstance;

    private PluginApi() {
    }

    public static PluginApi getInstance() {
        if (sInstance == null) {
            synchronized (sLock) {
                // 有可能在其他线程已创建
                if (sInstance == null) {
                    sInstance = new PluginApi();
                }
            }
        }
        return sInstance;
    }

    public void sendMessage(final Context context, final String pluginId, final int msgType,
            final Bundle msgArg) {

        PluginInfo pluginInfo = PluginManager.getInstance().getPluginInfo(pluginId);

        if (pluginInfo == null) {
            return;
        }

        if (!pluginInfo.isDownloaded() && !pluginInfo.isInstalled()) {

        } else if (pluginInfo.isDownloaded() && !pluginInfo.isInstalled()) {
            PluginManager.getInstance().installPlugin(pluginInfo,
                    pluginInfo.getDownloadedPackageInfo(), new AsyncCallback<Void, Error>() {
                        @Override
                        public void onSuccess(Void result) {
                            PluginRuntimeManager.getInstance().sendMessage(context, pluginId,
                                    msgType, msgArg);
                        }

                        @Override
                        public void onFailure(Error error) {

                        }
                    });
        } else if (!pluginInfo.isDownloaded() && pluginInfo.isInstalled()) {

        } else {

        }

    }
}
