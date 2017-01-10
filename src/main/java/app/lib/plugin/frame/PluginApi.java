
package app.lib.plugin.frame;

import android.content.Context;
import android.os.Bundle;

import app.lib.asynccallback.AsyncCallback;
import app.lib.asynccallback.Error;
import app.lib.plugin.frame.entity.PluginInfo;
import app.lib.plugin.frame.entity.PluginPackageInfo;

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

    public void installPlugin(final PluginInfo pluginInfo,
            final PluginPackageInfo installingPackageInfo,
            final AsyncCallback<Void, Error> callback) {
        PluginManager.getInstance().getWorkerHandler().post(new Runnable() {
            @Override
            public void run() {
                PluginManager.getInstance().installPlugin(pluginInfo, installingPackageInfo,
                        callback);
            }
        });
    }

    public void installDebugPlugin(final String apkPath,
            final AsyncCallback<Void, Error> callback) {
        PluginManager.getInstance().getWorkerHandler().post(new Runnable() {
            @Override
            public void run() {
                PluginManager.getInstance().installDebugPlugin(apkPath, callback);
            }
        });
    }

    public void sendMessage(final Context context, final String pluginId, final int msgType,
            final Bundle msgArg) {

        PluginInfo pluginInfo = PluginManager.getInstance().getPluginInfo(pluginId);

        if (pluginInfo == null) {
            return;
        }

        if (!pluginInfo.isDownloaded() && !pluginInfo.isInstalled()) {

        } else if (pluginInfo.isDownloaded() && !pluginInfo.isInstalled()) {
            installPlugin(pluginInfo, pluginInfo.getDownloadedPackageInfo(),
                    new AsyncCallback<Void, Error>() {
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
            PluginRuntimeManager.getInstance().sendMessage(context, pluginId, msgType, msgArg);
        } else {
            PluginRuntimeManager.getInstance().sendMessage(context, pluginId, msgType, msgArg);
        }

    }
}
