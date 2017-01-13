
package app.lib.plugin.frame;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

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
            final Bundle msgArg, final SendMessageCallback callback) {

        PluginInfo pluginInfo = PluginManager.getInstance().getPluginInfo(pluginId);

        if (pluginInfo == null) {
            if (callback != null) {
                callback.sendSendFailureMessage(new Error(-1, "can not found PluginInfo"));
            }
            return;
        }

        if (!pluginInfo.isDownloaded() && !pluginInfo.isInstalled()) {

        } else if (pluginInfo.isDownloaded() && !pluginInfo.isInstalled()) {
            installPlugin(pluginInfo, pluginInfo.getDownloadedPackageInfo(),
                    new AsyncCallback<Void, Error>() {
                        @Override
                        public void onSuccess(Void result) {
                            PluginRuntimeManager.getInstance().sendMessage(context, pluginId,
                                    msgType, msgArg, callback);
                        }

                        @Override
                        public void onFailure(Error error) {

                        }
                    });
        } else if (!pluginInfo.isDownloaded() && pluginInfo.isInstalled()) {
            PluginRuntimeManager.getInstance().sendMessage(context, pluginId, msgType, msgArg,
                    callback);
        } else {
            PluginRuntimeManager.getInstance().sendMessage(context, pluginId, msgType, msgArg,
                    callback);
        }

    }

    public static class SendMessageCallback {
        private static final int MSG_DOWNLOAD_START = 1;
        private static final int MSG_DOWNLOAD_PROGRESS = 2;
        private static final int MSG_DOWNLOAD_SUCCESS = 3;
        private static final int MSG_DOWNLOAD_FAILURE = 4;
        private static final int MSG_SEND_SUCCESS = 5;
        private static final int MSG_SEND_FAILURE = 6;

        private Handler mDispatcher;

        public SendMessageCallback() {
            Looper looper = Looper.myLooper();

            if (looper == null) {
                throw new RuntimeException("async callback must have looper");
            } else {
                mDispatcher = new SendMessageCallback.Dispatcher(this, looper);
            }
        }

        public void onDownloadStart() {
        }

        public void onDownloadProgress(float percent) {
        }

        public void onDownloadSuccess() {
        }

        public void onDownloadFailure(Error error) {
        }

        public void onSendSuccess(boolean handled) {

        }

        public void onSendFailure(Error error) {

        }

        public void sendDownloadStartMessage() {
            mDispatcher.sendEmptyMessage(MSG_DOWNLOAD_START);
        }

        public void sendDownloadProgressMessage(float progress) {
            mDispatcher.sendMessage(mDispatcher.obtainMessage(MSG_DOWNLOAD_PROGRESS, progress));
        }

        public void sendDownloadSuccessMessage() {
            mDispatcher.sendEmptyMessage(MSG_DOWNLOAD_SUCCESS);
        }

        public void sendDownloadFailureMessage(Error error) {
            mDispatcher.sendMessage(mDispatcher.obtainMessage(MSG_DOWNLOAD_FAILURE, error));
        }

        public void sendSendSuccessMessage(boolean handled) {
            mDispatcher.sendMessage(mDispatcher.obtainMessage(MSG_SEND_SUCCESS, handled));
        }

        public void sendSendFailureMessage(Error error) {
            mDispatcher.sendMessage(mDispatcher.obtainMessage(MSG_SEND_FAILURE, error));
        }

        private static class Dispatcher extends Handler {
            private SendMessageCallback mCallback;

            Dispatcher(SendMessageCallback callback, Looper looper) {
                super(looper);

                mCallback = callback;
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_DOWNLOAD_START:
                        mCallback.onDownloadStart();
                        break;
                    case MSG_DOWNLOAD_PROGRESS:
                        float percent = (float) msg.obj;
                        mCallback.onDownloadProgress(percent);
                        break;
                    case MSG_DOWNLOAD_SUCCESS:
                        mCallback.onDownloadSuccess();
                        break;
                    case MSG_DOWNLOAD_FAILURE:
                        if (msg.obj instanceof Error) {
                            Error error = (Error) msg.obj;
                            mCallback.onDownloadFailure(error);
                        }
                        break;
                    case MSG_SEND_SUCCESS:
                        if (msg.obj instanceof Boolean) {
                            boolean handled = (boolean) msg.obj;
                            mCallback.onSendSuccess(handled);
                        }
                        break;
                    case MSG_SEND_FAILURE:
                        if (msg.obj instanceof Error) {
                            Error error = (Error) msg.obj;
                            mCallback.onSendFailure(error);
                        }
                        break;
                }
            }
        }
    }
}
