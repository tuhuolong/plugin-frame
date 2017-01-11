
package app.lib.plugin.frame;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.format.DateUtils;

import app.lib.plugin.frame.runtime.api.PluginCoreApi;
import app.lib.plugin.frame.runtime.api.PluginHostApiImpl;

/**
 * Created by chenhao on 17/1/6.
 */

public class Plugin {
    private static Plugin sInstance;

    private static Object sLock = new Object();

    private Application mApplication;
    private Context mAppContext;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ICoreServiceApi coreServiceApi = ICoreServiceApi.Stub.asInterface(service);

            PluginCoreApi.getInstance().setCoreApiProxy(coreServiceApi);

            // try {
            // coreServiceApi.registerClientApi(mClientApiStub);
            // } catch (RemoteException e) {
            // }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            unbindCoreService();

            PluginCoreApi.getInstance().reset();

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    bindCoreService();
                }
            }, 30 * DateUtils.SECOND_IN_MILLIS);
        }
    };

    private Plugin() {

    }

    public static Plugin getInstance() {
        if (sInstance == null) {
            synchronized (sLock) {
                // 有可能在其他线程已创建
                if (sInstance == null) {
                    sInstance = new Plugin();
                }
            }
        }
        return sInstance;
    }

    public void start(Application application, boolean enableDebug) {
        new PluginHostApiImpl();

        mApplication = application;
        mAppContext = application;

        PluginManager.getInstance();

        bindCoreService();

        PluginSetting.IS_DEBUG = enableDebug;
    }

    public Application getApplication() {
        return mApplication;
    }

    public Context getAppContext() {
        return mAppContext;
    }

    private void bindCoreService() {
        Intent intent = new Intent(mAppContext, PluginCoreService.class);
        mAppContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void unbindCoreService() {
        try {
            mAppContext.unbindService(mServiceConnection);
        } catch (Exception e) {
        }
    }
}
