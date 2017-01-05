
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

/**
 * Created by chenhao on 16/12/24.
 */

public class PluginBridgeServiceBase extends Service {

    HandlerThread mWorkerThread;
    Handler mWorkerHandler;

    Handler mUiHandler;

    Context mAppContext;

    IBridgeServiceApi.Stub mStub = new IBridgeServiceApi.Stub() {
        @Override
        public void sendMessage(final int pluginId, int msgType, Bundle msgArg,
                IBridgeCallback bridgeCallback) throws RemoteException {
            mWorkerHandler.post(new Runnable() {
                @Override
                public void run() {
                    PluginRuntimeManager.getInstance().loadApk(pluginId);
                }
            });
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

        mWorkerThread = new MessageHandlerThread("PluginBridgeServiceWorker");
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
