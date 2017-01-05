
package app.lib.plugin.frame;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;

import app.lib.plugin.frame.common.MessageHandlerThread;
import app.lib.plugin.frame.util.ByteUtil;
import app.lib.plugin.frame.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by chenhao on 16/12/24.
 */

public class PluginManager {

    public static final boolean ENABLE_SIGNATURE_VALIDATE = false;

    private static final String META_KEY_PLUGIN_ID = "PluginId";
    private static final String META_KEY_MIN_API_LEVEL = "PluginMinApiLevel";
    private static final String META_KEY_DEVELOPER_ID = "PluginDeveloperId";

    private static final String PREF_PLUGIN_INFO_PREF = PluginManager.class.getName()
            + ".plugin_info_pref";

    private static final Object sLock = new Object();
    private static final String ASSETS_PLUGIN_DIR = "plugin";

    private static final String TMP_DIR = "tmp";

    public static Context sAppContext;
    private static PluginManager sInstance;
    MessageHandlerThread mWorkerThread;
    Handler mWorkerHandler;

    private boolean mIsInitialized = false;

    private SharedPreferences mPluginInfoPref;
    private Map<String, PluginInfo> mPluginInfoMap = new HashMap();
    private Map<String, PluginDeveloperInfo> mDeveloperInfoMap = new HashMap<>();

    private PluginManager() {
        mWorkerThread = new MessageHandlerThread("PluginWorker");
        mWorkerThread.start();
        mWorkerHandler = new Handler(mWorkerThread.getLooper());

        mWorkerHandler.post(new Runnable() {
            @Override
            public void run() {
                init();

                installAssetPlugin();
            }
        });
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

    private static String generateSignatureMD5(Signature[] signatures) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            if (signatures != null) {
                for (Signature s : signatures)
                    digest.update(s.toByteArray());
            }
            return ByteUtil.toHexString(digest.digest());
        } catch (Exception e) {
            return "";
        }
    }

    private Context getAppContext() {
        return sAppContext;
    }

    public boolean isInitialized() {
        synchronized (this) {
            return mIsInitialized;
        }
    }

    private void init() {
        mPluginInfoPref = getAppContext().getSharedPreferences(PREF_PLUGIN_INFO_PREF,
                Context.MODE_PRIVATE);

        synchronized (this) {
            mIsInitialized = true;
        }
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
                    rawInfo.mPluginId = bundle.getString(META_KEY_PLUGIN_ID);
                    rawInfo.mMinApiLevel = bundle.getInt(META_KEY_MIN_API_LEVEL, 0);
                    rawInfo.mDeveloperId = bundle.getString(META_KEY_DEVELOPER_ID);
                }
            }
        }

        return rawInfo;
    }

    private String getPluginBaseDir() {
        return getAppContext().getFilesDir().getPath() + File.separator + "plugin";
    }

    private PluginDeveloperInfo getPluginDeveloperInfo(String developerId) {
        synchronized (this) {
            return mDeveloperInfoMap.get(developerId);
        }
    }

    private void installAssetPlugin() {
        AssetManager assetManager = getAppContext().getAssets();

        try {
            String[] assetsPaths = assetManager.list(ASSETS_PLUGIN_DIR);
            for (int i = 0, len = assetsPaths.length; i < len; i++) {

                String assetsPath = assetsPaths[i];

                String assetsPackagePath = ASSETS_PLUGIN_DIR + File.separator + assetsPath;

                String tmpPath = getPluginBaseDir() + File.separator + TMP_DIR + File.separator
                        + "tmp.apk";

                boolean unpackSuccess = true;

                try {
                    InputStream input = assetManager.open(assetsPackagePath);
                    FileUtil.createFileIfNotExists(tmpPath);

                    FileOutputStream output = new FileOutputStream(tmpPath);
                    int length;
                    byte[] buffer = new byte[1024];
                    while ((length = input.read(buffer)) != -1) {
                        output.write(buffer, 0, length);
                    }
                    output.flush();
                    input.close();
                    output.close();
                } catch (Exception e) {
                    unpackSuccess = false;
                }

                if (!unpackSuccess) {
                    FileUtil.deleteFile(tmpPath);
                    continue;
                }

                PackageRawInfo tmpRawInfo = getPackageRawInfo(getAppContext(), tmpPath);

                if (tmpRawInfo != null) {
                    PluginDeveloperInfo developerInfo = getPluginDeveloperInfo(
                            tmpRawInfo.mDeveloperId);
                    boolean isSignatureValid = validateSignature(developerInfo, tmpPath);
                    if (!isSignatureValid) {
                        FileUtil.deleteFile(tmpPath);
                        continue;
                    }

                    boolean isMinApiLevelValid = validateMinApiLevel(tmpRawInfo.mMinApiLevel);
                    if (!isMinApiLevelValid) {
                        FileUtil.deleteFile(tmpPath);
                        continue;
                    }

                    // String downloadedPath = getPackageDownloadedPath(pluginId, packageId,
                    // packageType);
                    //
                    // boolean isCopySuccess = FileUtils.copyFileToFile(tmpPath, downloadedPath);
                    //
                    // FileUtils.deleteFile(tmpPath);
                    //
                    // if (isCopySuccess) {
                    // PackageRawInfo rawInfo = getPackageRawInfo(downloadedPath, packageType);
                    // if (rawInfo != null
                    // && mDeveloperInfoMap.containsKey(rawInfo.mDeveloperId)) {
                    //
                    // PluginPackageInfo downloadedPackageInfo = new PluginPackageInfo();
                    //
                    // downloadedPackageInfo.setPluginId(pluginId);
                    // downloadedPackageInfo.setPackageId(packageId);
                    // downloadedPackageInfo.setPackagePath(downloadedPath);
                    // downloadedPackageInfo.setDeveloperId(rawInfo.mDeveloperId);
                    // downloadedPackageInfo.setMinApiLevel(rawInfo.mMinApiLevel);
                    // downloadedPackageInfo.setPlatform(rawInfo.mPlatform);
                    // downloadedPackageInfo.setVersion(rawInfo.mVersion);
                    // downloadedPackageInfo.setPackageType(packageType);
                    // downloadedPackageInfo.setPackageName(rawInfo.mPackageName);
                    // downloadedPackageInfo.setModelList(rawInfo.mModelList);
                    // downloadedPackageInfo.setIsSupportWidget(rawInfo.mIsSupportWidget);
                    //
                    // addDownloadedPackageInfoInternal(downloadedPackageInfo);
                    // } else {
                    // FileUtils.deleteFile(downloadedPath);
                    // }
                    // } else {
                    // FileUtils.deleteFile(downloadedPath);
                    // }

                } else {
                    // FileUtils.deleteFile(tmpPath);
                    continue;
                }
            }
        } catch (Exception e) {
        }
    }

    private boolean validateSignature(PluginDeveloperInfo developerInfo, String path) {
        if (!ENABLE_SIGNATURE_VALIDATE) {
            return true;
        }

        if (developerInfo == null || TextUtils.isEmpty(developerInfo.getCert())
                || TextUtils.isEmpty(path)) {
            return false;
        }

        boolean result = false;

        PackageInfo packageInfo = getAppContext().getPackageManager().getPackageArchiveInfo(
                path, PackageManager.GET_SIGNATURES);

        if (packageInfo != null) {
            if (developerInfo.getCert()
                    .equalsIgnoreCase(generateSignatureMD5(packageInfo.signatures))) {
                result = true;
            }
        }

        return result;
    }

    private boolean validateMinApiLevel(int minApiLevel) {
        if (0 < minApiLevel && minApiLevel <= PluginSetting.API_LEVEL) {
            return true;
        } else {
            return false;
        }
    }

    public void sendMessage(final Context context, final String pluginId, final int msgType,
            final Bundle msgArg) {

    }
}
