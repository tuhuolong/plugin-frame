
package app.lib.plugin.frame.runtime.bridge;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import app.lib.plugin.frame.PluginRuntimeManager;
import app.lib.plugin.frame.common.MessageHandlerThread;
import app.lib.plugin.frame.entity.PluginInfo;
import app.lib.plugin.frame.runtime.api.PluginCoreApi;
import app.lib.plugin.sdk.PluginContext;

/**
 * Created by chenhao on 16/12/24.
 */

public class PluginBridgeServiceBase extends Service {

    public static final String KEY_SEND_MESSAGE_RESULT_HANDLED = "handled";

    HandlerThread mWorkerThread;
    Handler mWorkerHandler;

    Handler mUiHandler;

    Context mAppContext;

    IBridgeServiceApi.Stub mStub = new IBridgeServiceApi.Stub() {
        @Override
        public void sendMessage(final String pluginId, final int msgType, final Bundle msgArg,
                final IBridgeSendMessageCallback callback) throws RemoteException {
            mWorkerHandler.post(new Runnable() {
                @Override
                public void run() {
                    PluginInfo pluginInfo = PluginCoreApi.getInstance().getPluginInfo(pluginId);

                    if (pluginInfo == null) {
                        if (callback != null) {
                            try {
                                callback.onFailure(new BridgeError(-1, "not found PluginInfo"));
                            } catch (RemoteException e) {
                            }
                        }
                        return;
                    }

                    if (!pluginInfo.isInstalled()) {
                        if (callback != null) {
                            try {
                                callback.onFailure(new BridgeError(-1, "not installed"));
                            } catch (RemoteException e) {
                            }
                        }
                        return;
                    }

                    PluginContext pluginContext = PluginRuntimeManager.getInstance()
                            .loadApkRuntime(pluginInfo.getInstalledPackageInfo());

                    if (pluginContext != null && pluginContext.getMessageReceiver() != null) {
                        try {
                            if (msgArg != null) {
                                msgArg.setClassLoader(pluginContext.getClassLoader());
                            }

                            boolean handled = pluginContext.getMessageReceiver().handleMessage(
                                    mAppContext, pluginContext, msgType, msgArg);

                            Bundle result = new Bundle();
                            result.putBoolean(KEY_SEND_MESSAGE_RESULT_HANDLED, handled);

                            if (callback != null) {
                                callback.onSuccess(result);
                            }

                        } catch (Exception e) {
                            if (callback != null) {
                                try {
                                    callback.onFailure(
                                            new BridgeError(-1, "Plugin handle Exception"));
                                } catch (RemoteException re) {
                                }
                            }
                        }
                    } else {
                        if (callback != null) {
                            try {
                                callback.onFailure(new BridgeError(-1,
                                        "not found PluginContext or MessageReceiver"));
                            } catch (RemoteException e) {
                            }
                        }
                    }
                }
            });
        }

        @Override
        public void removePluginContext(String pluginId, int versionCode, IBridgeCallback callback)
                throws RemoteException {
            PluginRuntimeManager.getInstance().removePluginContextRuntime(pluginId, versionCode);
        }

        @Override
        public void startService(final String targetPluginHostServiceClassName, long pluginId,
                final long packageId, final Intent startIntent, final String pluginServiceClassName)
                throws RemoteException {
        }

        @Override
        public void exitProcess() throws RemoteException {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    System.exit(0);
                }
            });
        }
    };

    @Override
    public void onCreate() {
        mAppContext = getApplicationContext();

        mWorkerThread = new MessageHandlerThread(this.getClass().getSimpleName() + "Worker");
        mWorkerThread.start();
        mWorkerHandler = new Handler(mWorkerThread.getLooper());

        mUiHandler = new Handler(Looper.getMainLooper());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mStub;
    }

}
