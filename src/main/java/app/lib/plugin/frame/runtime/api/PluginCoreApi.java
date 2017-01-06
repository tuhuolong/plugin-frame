
package app.lib.plugin.frame.runtime.api;

import app.lib.plugin.frame.ICoreServiceApi;
import app.lib.plugin.frame.entity.PluginInfo;

/**
 * Created by chenhao on 17/1/6.
 */

public class PluginCoreApi {
    private static PluginCoreApi sInstance;
    private static Object sLock = new Object();

    private ICoreServiceApi mCoreApiProxy;

    private boolean mIsCoreReady;

    private PluginCoreApi() {
    }

    public static PluginCoreApi getInstance() {
        if (sInstance == null) {
            synchronized (sLock) {
                // 有可能在其他线程已创建
                if (sInstance == null) {
                    sInstance = new PluginCoreApi();
                }
            }
        }
        return sInstance;
    }

    void onCoreReady() {
        synchronized (sLock) {
            mIsCoreReady = true;
        }
    }

    private ICoreServiceApi getCoreApiProxy() throws CoreNotReadyException {
        ICoreServiceApi apiProxy;
        boolean isCoreReady;

        synchronized (sLock) {
            apiProxy = mCoreApiProxy;
            isCoreReady = mIsCoreReady;
        }

        if (apiProxy == null) {
            throw new CoreNotReadyException("apiProxy null");
        }

        // if (!isCoreReady) {
        // throw new CoreNotReadyException("isCoreReady false");
        // }

        return apiProxy;
    }

    public void setCoreApiProxy(ICoreServiceApi coreApi) {
        synchronized (sLock) {
            mCoreApiProxy = coreApi;
        }
    }

    public void reset() {
        synchronized (sLock) {
            mCoreApiProxy = null;

            mIsCoreReady = false;
        }
    }

    public boolean isCoreReady() {
        boolean isCoreReady;
        synchronized (sLock) {
            if (mCoreApiProxy != null && mIsCoreReady) {
                isCoreReady = true;
            } else {
                isCoreReady = false;
            }
        }
        return isCoreReady;
    }

    public PluginInfo getPluginInfo(String pluginId) {
        PluginInfo result = null;

        try {
            result = getCoreApiProxy().getPluginInfo(pluginId);
        } catch (Exception e) {
        }

        return result;
    }

    private static class CoreNotReadyException extends Exception {
        public CoreNotReadyException(String detail) {
            super(detail);
        }
    }

}
