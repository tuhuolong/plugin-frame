
package com.chenhao.lib.plugin.frame;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;

import com.chenhao.lib.plugin.frame.common.MessageHandlerThread;

/**
 * Created by chenhao on 16/12/24.
 */

public class PluginManager {

    private static final String KEY_MIN_API_LEVEL = "PluginMinApiLevel";
    private static final String KEY_DEVELOPER_ID = "PluginDeveloperId";

    private static final Object sLock = new Object();

    private static PluginManager sInstance;

    Handler mUiHandler;
    MessageHandlerThread mWorkerThread;
    Handler mWorkerHandler;

    private PluginManager() {
        init();
    }

    public static PluginManager getInstance() {
        if (sInstance == null) {
            synchronized (sLock) {
                // 有可能在其他线程已创建
                if (sInstance == null) {
                    sInstance = new PluginManager();
                }
            }
        }
        return sInstance;
    }

    private void init() {
        mWorkerThread = new MessageHandlerThread("PluginWorker");
        mWorkerThread.start();
        mWorkerHandler = new Handler(mWorkerThread.getLooper());

        installAssetPlugin();
    }

    private boolean installPlugin(final PluginRecord pluginRecord,
            PluginPackageInfo installingPackageInfo) {
        // if (!checkPlugin(plugFile)) {
        // Log.e(TAG, "checkPlugin failed");
        // FileUtils.deleteFile(plugFile);
        // return false;
        // }
        // File file = new File(plugFile);
        // if (!file.exists()) {
        // Log.e(TAG, "plugin file not exist:" + plugFile);
        // return false;
        // }
        // PackageRawInfo packageRawInfo = PluginRuntimeManager.loadPackageInfo(mAppContext,
        // plugFile);
        // if (packageRawInfo == null) {
        // Log.e(TAG, "loadPackageInfo failed");
        // return false;
        // }
        // if (isDebug) {
        // FileUtils.deleteDirectory(getInstallPluginRootPath(packageRawInfo));
        // }
        // String path = getInstallPluginPath(packageRawInfo);
        // FileUtils.createDirIfNotExists(path);
        // String installFilePath = getInstallPluginFilePath(packageRawInfo);
        // FileUtils.copyFileToFile(plugFile, installFilePath);
        // // 安装libs 文件
        // PackageUtils.installApkSo(plugFile, path);
        // Log.d(TAG, "installPlugin success:" + plugFile);
        // mPluginInfoLocal.addPluginInfo(installFilePath, packageRawInfo, isDebug);
        // loadPlugin(installFilePath, packageRawInfo);
        return true;
    }

    private PackageRawInfo getPackageRawInfo(Context context, String packagePath) {
        PackageRawInfo rawInfo = null;

        if (TextUtils.isEmpty(packagePath)) {
            return null;
        }

        PackageInfo packageInfo = context.getPackageManager().getPackageArchiveInfo(
                packagePath, PackageManager.GET_META_DATA);

        if (packageInfo != null) {
            rawInfo = new PackageRawInfo();
            rawInfo.mVersion = packageInfo.versionCode;
            rawInfo.mPackageName = packageInfo.packageName;

            ApplicationInfo appInfo = packageInfo.applicationInfo;

            if (appInfo != null) {
                Bundle bundle = appInfo.metaData;
                if (bundle != null) {
                    rawInfo.mMinApiLevel = bundle.getInt(KEY_MIN_API_LEVEL, 0);
                    rawInfo.mDeveloperId = bundle.getString(KEY_DEVELOPER_ID);
                }
            }
        }

        return rawInfo;
    }

    private void installAssetPlugin() {
//        String tmpPath = getPackageTempPath(packageId, packageType);
//
//        boolean unpackSuccess = true;
//
//        try {
//            InputStream input = assetManager.open(builtinPackagePath);
//            FileUtils.createFileIfNotExists(tmpPath);
//
//            FileOutputStream output = new FileOutputStream(tmpPath);
//            int length = -1;
//            byte[] buffer = new byte[1024];
//            while ((length = input.read(buffer)) != -1) {
//                output.write(buffer, 0, length);
//            }
//            output.flush();
//            input.close();
//            output.close();
//        } catch (Exception e) {
//            unpackSuccess = false;
//        }
//
//        if (!unpackSuccess) {
//            FileUtils.deleteFile(tmpPath);
//            return;
//        }
    }

    public void sendMessage(final Context context, final String pluginId, final int msgType,
            final Bundle msgArg) {

    }
}
