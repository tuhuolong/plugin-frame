
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import app.lib.asynccallback.AsyncCallback;
import app.lib.asynccallback.Error;
import app.lib.plugin.frame.common.MessageHandlerThread;
import app.lib.plugin.frame.entity.PluginDeveloperInfo;
import app.lib.plugin.frame.entity.PluginInfo;
import app.lib.plugin.frame.entity.PluginPackageInfo;
import app.lib.plugin.frame.util.ByteUtil;
import app.lib.plugin.frame.util.FileUtil;
import app.lib.plugin.frame.util.PreferenceUtil;

import static app.lib.plugin.frame.util.FileUtil.copyFileToFile;

/**
 * Created by chenhao on 16/12/24.
 */

public class PluginManager {

    private static final String META_KEY_PLUGIN_ID = "PluginId";
    private static final String META_KEY_MIN_API_LEVEL = "PluginMinApiLevel";
    private static final String META_KEY_DEVELOPER_ID = "PluginDeveloperId";

    private static final String PREF_PLUGIN_INFO_PREF = PluginManager.class.getName()
            + ".plugin_info_pref";
    private static final String PREF_DEVELOPER_INFO_PREF = PluginManager.class.getName()
            + ".developer_info_pref";

    private static final Object sLock = new Object();
    private static final String ASSETS_PLUGIN_DIR = "plugin";

    private static final String TMP_DIR = "tmp";
    private static final String DOWNLOADED_DIR = "download";
    private static final String INSTALLED_DIR = "install";

    private static PluginManager sInstance;
    MessageHandlerThread mWorkerThread;
    Handler mWorkerHandler;

    private boolean mIsInitialized = false;

    private SharedPreferences mPluginInfoPref;
    private SharedPreferences mDeveloperInfoPref;
    private Map<String, PluginInfo> mPluginInfoMap = new ConcurrentHashMap<>();
    private Map<String, PluginDeveloperInfo> mDeveloperInfoMap = new ConcurrentHashMap<>();
    private Map<String, Map<Integer, PluginPackageInfo>> mDownloadedPackageInfoMap = new ConcurrentHashMap<>();
    private Map<String, Map<Integer, PluginPackageInfo>> mInstalledPackageInfoMap = new ConcurrentHashMap<>();

    private PluginManager() {
        mWorkerThread = new MessageHandlerThread("PluginWorker");
        mWorkerThread.start();
        mWorkerHandler = new Handler(mWorkerThread.getLooper());

        init();

        mWorkerHandler.post(new Runnable() {
            @Override
            public void run() {
                unpackAssetPlugin();
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
        return Plugin.getInstance().getAppContext();
    }

    Handler getWorkerHandler() {
        return mWorkerHandler;
    }

    public boolean isInitialized() {
        synchronized (this) {
            return mIsInitialized;
        }
    }

    private void init() {
        mPluginInfoPref = getAppContext().getSharedPreferences(PREF_PLUGIN_INFO_PREF,
                Context.MODE_PRIVATE);
        mDeveloperInfoPref = getAppContext().getSharedPreferences(PREF_DEVELOPER_INFO_PREF,
                Context.MODE_PRIVATE);

        scanInstalledPackage();
        scanDownloadedPackage();
        readDeveloperInfoPref();
        readPluginInfoPref();

        PluginInfo pluginInfo = mPluginInfoMap.get("demo");
        if (pluginInfo == null) {
            pluginInfo = new PluginInfo();
            pluginInfo.setPluginId("demo");
            pluginInfo.setName("Demo");
            pluginInfo.setIcon("http://test.png");
            mPluginInfoMap.put(pluginInfo.getPluginId(), pluginInfo);

            savePluginInfoPref(pluginInfo);
        }

        PluginDeveloperInfo developerInfo = mDeveloperInfoMap.get("dev_1");
        if (developerInfo == null) {
            developerInfo = new PluginDeveloperInfo();
            developerInfo.setDeveloperId("dev_1");
            developerInfo.setCert("608E839C638661F881B9E25ACC301E9A");
            mDeveloperInfoMap.put(developerInfo.getDeveloperId(), developerInfo);

            saveDeveloperInfoPref(developerInfo);
        }

        synchronized (this) {
            mIsInitialized = true;
        }
    }

    private void readPluginInfoPref() {
        Map<String, ?> pluginInfoPrefMap = mPluginInfoPref.getAll();
        if (pluginInfoPrefMap != null && pluginInfoPrefMap.size() > 0) {
            for (Map.Entry<String, ?> record : pluginInfoPrefMap.entrySet()) {
                String key = record.getKey();
                String prefStr = (String) record.getValue();
                PluginInfo pluginInfo = PluginInfo.buildFromPref(key, prefStr);
                if (pluginInfo != null) {
                    boolean infoChanged = false;

                    int installedVersionCode = pluginInfo.getInstalledVersionCode();
                    if (installedVersionCode > 0) {
                        Map<Integer, PluginPackageInfo> packageInfoMap = mInstalledPackageInfoMap
                                .get(pluginInfo.getPluginId());
                        if (packageInfoMap != null) {
                            PluginPackageInfo packageInfo = packageInfoMap
                                    .get(installedVersionCode);
                            if (packageInfo != null) {
                                pluginInfo.setInstalledPackageInfo(installedVersionCode,
                                        packageInfo);
                            } else {
                                infoChanged = true;
                                pluginInfo.setInstalledPackageInfo(0, null);
                            }
                        } else {
                            infoChanged = true;
                            pluginInfo.setInstalledPackageInfo(0, null);
                        }
                    }

                    int downloadedVersionCode = pluginInfo.getDownloadedVersionCode();
                    if (downloadedVersionCode > 0) {
                        Map<Integer, PluginPackageInfo> packageInfoMap = mDownloadedPackageInfoMap
                                .get(pluginInfo.getPluginId());
                        if (packageInfoMap != null) {
                            PluginPackageInfo packageInfo = packageInfoMap
                                    .get(downloadedVersionCode);
                            if (packageInfo != null) {
                                pluginInfo.setDownloadedPackageInfo(downloadedVersionCode,
                                        packageInfo);
                            } else {
                                infoChanged = true;
                                pluginInfo.setDownloadedPackageInfo(0, null);
                            }
                        } else {
                            infoChanged = true;
                            pluginInfo.setDownloadedPackageInfo(0, null);
                        }
                    }

                    if (infoChanged) {
                        savePluginInfoPref(pluginInfo);
                    }

                    mPluginInfoMap.put(pluginInfo.getPluginId(), pluginInfo);
                }
            }
        }
    }

    private void readDeveloperInfoPref() {
        Map<String, ?> developerInfoPrefMap = mDeveloperInfoPref.getAll();
        if (developerInfoPrefMap != null && developerInfoPrefMap.size() > 0) {
            for (Map.Entry<String, ?> record : developerInfoPrefMap.entrySet()) {
                String key = record.getKey();
                String prefStr = (String) record.getValue();
                PluginDeveloperInfo developerInfo = PluginDeveloperInfo.buildFromPref(key, prefStr);
                if (developerInfo != null) {
                    mDeveloperInfoMap.put(developerInfo.getDeveloperId(), developerInfo);
                }
            }
        }
    }

    private void scanInstalledPackage() {
        File apkInstallDir = new File(getInstalledApkBasePath());
        if (apkInstallDir.isDirectory()) {
            String[] pluginPaths = apkInstallDir.list();
            if (pluginPaths != null) {
                for (int i = 0, len_1 = pluginPaths.length; i < len_1; i++) {

                    String pluginId = pluginPaths[i];

                    String pluginPath = apkInstallDir + File.separator + pluginId;

                    File pluginIdDir = new File(pluginPath);

                    String[] packagePaths = pluginIdDir.list();
                    if (packagePaths == null || packagePaths.length < 1) {
                        continue;
                    }

                    for (int j = 0, len_2 = packagePaths.length; j < len_2; j++) {
                        String fileName = packagePaths[j];
                        String packagePath = pluginPath + File.separator + fileName;
                        int versionCode;
                        try {
                            versionCode = Integer.parseInt(
                                    fileName.substring(0, fileName.length() - ".apk".length()));
                        } catch (Exception e) {
                            versionCode = 0;
                        }

                        if (versionCode <= 0) {
                            FileUtil.deleteFile(packagePath);
                            continue;
                        }

                        PackageRawInfo rawInfo = getPackageRawInfo(packagePath);
                        if (rawInfo == null) {
                            FileUtil.deleteFile(packagePath);
                            continue;
                        }

                        // PluginDeveloperInfo developerInfo =
                        // mDeveloperInfoMap.get(rawInfo.mDeveloperId);
                        // boolean isSignatureValid = validateSignature(developerInfo, packagePath);
                        // if (!isSignatureValid) {
                        // FileUtil.deleteFile(packagePath);
                        // continue;
                        // }

                        // boolean isMinApiLevelValid = validateMinApiLevel(rawInfo.mMinApiLevel);
                        // if (!isMinApiLevelValid) {
                        // FileUtil.deleteFile(packagePath);
                        // continue;
                        // }
                        //
                        PluginPackageInfo installedPackageInfo = new PluginPackageInfo();

                        installedPackageInfo.setPluginId(rawInfo.mPluginId);
                        installedPackageInfo.setVersionCode(rawInfo.mVersionCode);
                        installedPackageInfo.setDeveloperId(rawInfo.mDeveloperId);
                        installedPackageInfo.setMinApiLevel(rawInfo.mMinApiLevel);
                        installedPackageInfo.setPackageName(rawInfo.mPackageName);
                        installedPackageInfo.setPackagePath(packagePath);

                        addInstalledPluginPackageInfo(installedPackageInfo);
                    }

                }
            }
        }
    }

    private void scanDownloadedPackage() {
        File apkDownloadDir = new File(getDownloadedBasePath());
        if (apkDownloadDir.isDirectory()) {
            String[] pluginPaths = apkDownloadDir.list();
            for (int i = 0, len_1 = pluginPaths.length; i < len_1; i++) {

                String pluginIdStr = pluginPaths[i];

                String pluginPath = getDownloadedBasePath() + File.separator + pluginIdStr;

                File pluginIdDir = new File(pluginPath);

                String[] packagePaths = pluginIdDir.list();
                if (packagePaths == null || packagePaths.length < 1) {
                    continue;
                }

                for (int j = 0, len_2 = packagePaths.length; j < len_2; j++) {
                    String fileName = packagePaths[j];

                    String packagePath = pluginPath + File.separator + fileName;

                    int versionCode;
                    try {
                        versionCode = Integer.parseInt(
                                fileName.substring(0, fileName.length() - ".apk".length()));
                    } catch (Exception e) {
                        versionCode = 0;
                    }

                    if (versionCode <= 0) {
                        FileUtil.deleteFile(packagePath);
                        continue;
                    }

                    PackageRawInfo rawInfo = getPackageRawInfo(packagePath);
                    if (rawInfo == null) {
                        FileUtil.deleteFile(packagePath);
                        continue;
                    }

                    // PluginDeveloperInfo developerInfo =
                    // mDeveloperInfoMap.get(rawInfo.mDeveloperId);
                    // boolean isSignatureValid = validateSignature(developerInfo, packagePath);
                    // if (!isSignatureValid) {
                    // FileUtil.deleteFile(packagePath);
                    // continue;
                    // }

                    // boolean isMinApiLevelValid = validateMinApiLevel(rawInfo.mMinApiLevel);
                    // if (!isMinApiLevelValid) {
                    // FileUtil.deleteFile(packagePath);
                    // continue;
                    // }

                    PluginPackageInfo downloadedPackageInfo = new PluginPackageInfo();

                    downloadedPackageInfo.setPluginId(rawInfo.mPluginId);
                    downloadedPackageInfo.setVersionCode(rawInfo.mVersionCode);
                    downloadedPackageInfo.setDeveloperId(rawInfo.mDeveloperId);
                    downloadedPackageInfo.setMinApiLevel(rawInfo.mMinApiLevel);
                    downloadedPackageInfo.setPackageName(rawInfo.mPackageName);
                    downloadedPackageInfo.setPackagePath(packagePath);

                    addDownloadedPluginPackageInfo(downloadedPackageInfo);
                }

            }
        }
    }

    void downloadPlugin(final PluginInfo pluginInfo, PluginApi.DownloadPluginCallback callback) {
        if (pluginInfo == null) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(-1, ""));
            }
            return;
        }
    }

    void installPlugin(final PluginInfo pluginInfo, PluginPackageInfo installingPackageInfo,
            AsyncCallback<Void, Error> callback) {
        if (pluginInfo == null || installingPackageInfo == null) {
            if (callback != null) {
                callback.sendFailureMessage(
                        new Error(-1, "PluginInfo or PluginPackageInfo is null"));
            }
            return;
        }

        if (pluginInfo.isInstalled() && pluginInfo.getInstalledPackageInfo()
                .getVersionCode() >= installingPackageInfo.getVersionCode()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(-1, "can not downgrade"));
            }
            return;
        }

        String installedPluginId = installingPackageInfo.getPluginId();
        int installedVersionCode = installingPackageInfo.getVersionCode();

        Map<Integer, PluginPackageInfo> pluginPackageInfoMap = mInstalledPackageInfoMap
                .get(installedPluginId);
        if (pluginPackageInfoMap != null) {
            PluginPackageInfo existPackageInfo = pluginPackageInfoMap.get(installedVersionCode);
            if (existPackageInfo != null) {
                if (callback != null) {
                    callback.sendFailureMessage(new Error(-1, "same package is installed"));
                }
                return;
            }
        }

        String installedPackagePath = getInstalledPath(installedPluginId, installedVersionCode);

        boolean copySuccess = FileUtil.copyFileToFile(installingPackageInfo.getPackagePath(),
                installedPackagePath);

        if (!copySuccess) {
            FileUtil.deleteFile(installedPackagePath);

            if (callback != null) {
                callback.sendFailureMessage(new Error(-1, "copy to installed dir fail"));
            }
            return;
        }

        PackageRawInfo rawInfo = getPackageRawInfo(installedPackagePath);

        if (rawInfo != null) {

            PluginSoManager.getInstance().install(getAppContext(), installedPluginId,
                    installedVersionCode, installedPackagePath);

            PluginPackageInfo installedPackageInfo = new PluginPackageInfo();
            installedPackageInfo.setPluginId(rawInfo.mPluginId);
            installedPackageInfo.setVersionCode(rawInfo.mVersionCode);
            installedPackageInfo.setDeveloperId(rawInfo.mDeveloperId);
            installedPackageInfo.setMinApiLevel(rawInfo.mMinApiLevel);
            installedPackageInfo.setPackageName(rawInfo.mPackageName);
            installedPackageInfo.setPackagePath(installedPackagePath);

            addInstalledPluginPackageInfo(installedPackageInfo);

            pluginInfo.setInstalledPackageInfo(installedPackageInfo.getVersionCode(),
                    installedPackageInfo);

            savePluginInfoPref(pluginInfo);

            if (callback != null) {
                callback.sendSuccessMessage(null);
            }

        } else {
            FileUtil.deleteFile(installedPackagePath);

            if (callback != null) {
                callback.sendFailureMessage(new Error(-1, "can not get PackageRawInfo"));
            }
        }
    }

    void installDebugPlugin(String apkPath, final AsyncCallback<Void, Error> callback) {
        if (!PluginSetting.IS_DEBUG) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(-1, ""));
            }
            return;
        }

        PackageRawInfo rawInfo = getPackageRawInfo(apkPath);
        if (rawInfo == null) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(-1, ""));
            }
            return;
        }

        PluginDeveloperInfo developerInfo = getPluginDeveloperInfo(rawInfo.mDeveloperId);
        boolean isSignatureValid = validateSignature(developerInfo, apkPath);
        if (!isSignatureValid) {
            FileUtil.deleteFile(apkPath);
            if (callback != null) {
                callback.sendFailureMessage(new Error(-1, "signature wrong"));
            }
            return;
        }

        boolean isMinApiLevelValid = validateMinApiLevel(rawInfo.mMinApiLevel);
        if (!isMinApiLevelValid) {
            FileUtil.deleteFile(apkPath);
            if (callback != null) {
                callback.sendFailureMessage(new Error(-1, "minApiLevel wrong"));
            }
            return;
        }

        PluginInfo pluginInfo = getPluginInfo(rawInfo.mPluginId);

        if (pluginInfo == null) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(-1, "can not find PluginInfo"));
            }
            return;
        }

        if (pluginInfo.isInstalled()
                && pluginInfo.getInstalledPackageInfo().getVersionCode() > rawInfo.mVersionCode) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(-1, "can not downgrade"));
            }
            return;
        }

        removeInstalledPackage(rawInfo.mPluginId, rawInfo.mVersionCode);
        removeDownloadedPackage(rawInfo.mPluginId, rawInfo.mVersionCode);

        PluginRuntimeManager.getInstance().removePluginContextAll(rawInfo.mPluginId,
                rawInfo.mVersionCode);

        String downloadedPath = getDownloadedPath(rawInfo.mPluginId, rawInfo.mVersionCode);

        boolean isCopySuccess = copyFileToFile(apkPath, downloadedPath);

        FileUtil.deleteFile(apkPath);

        if (isCopySuccess) {
            rawInfo = getPackageRawInfo(downloadedPath);
            if (rawInfo != null) {

                PluginPackageInfo downloadedPackageInfo = new PluginPackageInfo();
                downloadedPackageInfo.setPluginId(rawInfo.mPluginId);
                downloadedPackageInfo.setVersionCode(rawInfo.mVersionCode);
                downloadedPackageInfo.setDeveloperId(rawInfo.mDeveloperId);
                downloadedPackageInfo.setMinApiLevel(rawInfo.mMinApiLevel);
                downloadedPackageInfo.setPackageName(rawInfo.mPackageName);
                downloadedPackageInfo.setPackagePath(downloadedPath);

                addDownloadedPluginPackageInfo(downloadedPackageInfo);

                pluginInfo.setDownloadedPackageInfo(downloadedPackageInfo.getVersionCode(),
                        downloadedPackageInfo);

                savePluginInfoPref(pluginInfo);

                installPlugin(pluginInfo, downloadedPackageInfo,
                        new AsyncCallback<Void, Error>() {
                            @Override
                            public void onSuccess(Void result) {
                                if (callback != null) {
                                    callback.sendSuccessMessage(null);
                                }
                            }

                            @Override
                            public void onFailure(Error error) {
                                if (callback != null) {
                                    callback.sendFailureMessage(
                                            new Error(-1, "PluginInfo null"));
                                }
                            }
                        });

            } else {
                FileUtil.deleteFile(downloadedPath);

                if (callback != null) {
                    callback.sendFailureMessage(new Error(-1, "MinApiLevel wrong"));
                }
            }
        } else {
            if (callback != null) {
                callback.sendFailureMessage(new Error(-1, "copy wrong"));
            }
        }
    }

    private void removeInstalledPackage(String pluginId, int versionCode) {
        PluginInfo pluginInfo = mPluginInfoMap.get(pluginId);
        if (pluginInfo != null) {
            pluginInfo.setInstalledPackageInfo(0, null);
            savePluginInfoPref(pluginInfo);
        }

        Map<Integer, PluginPackageInfo> packageInfoMap = mInstalledPackageInfoMap.get(pluginId);
        if (packageInfoMap == null) {
            return;
        }

        PluginPackageInfo packageInfo = packageInfoMap.get(versionCode);

        if (packageInfo == null) {
            return;
        }

        // dex
        String dexOptimizedDir = PluginSetting.getDexOptimizedDir(getAppContext(), pluginId);
        String dexOptimizedPath = dexOptimizedDir + File.separator + versionCode + ".dex";
        FileUtil.deleteFile(dexOptimizedPath);

        // so
        String soDir = PluginSetting.getSoDir(getAppContext(), pluginId, versionCode);
        FileUtil.deleteDirectory(soDir);

        // mpk
        String apkPath = getInstalledPath(pluginId, versionCode);
        FileUtil.deleteFile(apkPath);

        removeInstalledPluginPackageInfo(pluginId, versionCode);
    }

    private void removeDownloadedPackage(String pluginId, int versionCode) {
        PluginInfo pluginInfo = mPluginInfoMap.get(pluginId);
        if (pluginInfo != null) {
            pluginInfo.setDownloadedPackageInfo(0, null);
            savePluginInfoPref(pluginInfo);
        }

        Map<Integer, PluginPackageInfo> packageInfoMap = mDownloadedPackageInfoMap.get(pluginId);
        if (packageInfoMap == null) {
            return;
        }

        PluginPackageInfo packageInfo = packageInfoMap.get(versionCode);

        if (packageInfo == null) {
            return;
        }

        FileUtil.deleteFile(packageInfo.getPackagePath());

        removeDownloadedPluginPackageInfo(pluginId, versionCode);
    }

    private void addInstalledPluginPackageInfo(PluginPackageInfo packageInfo) {
        Map<Integer, PluginPackageInfo> pkgInfoMap = mInstalledPackageInfoMap
                .get(packageInfo.getPluginId());
        if (pkgInfoMap == null) {
            pkgInfoMap = new HashMap<>();
            mInstalledPackageInfoMap.put(packageInfo.getPluginId(), pkgInfoMap);
        }
        pkgInfoMap.put(packageInfo.getVersionCode(), packageInfo);
    }

    private void removeInstalledPluginPackageInfo(String pluginId, int versionCode) {
        Map<Integer, PluginPackageInfo> packageInfoMap = mInstalledPackageInfoMap.get(pluginId);
        if (packageInfoMap == null) {
            return;
        }
        packageInfoMap.remove(versionCode);
    }

    private void addDownloadedPluginPackageInfo(PluginPackageInfo packageInfo) {
        Map<Integer, PluginPackageInfo> pkgInfoMap = mDownloadedPackageInfoMap
                .get(packageInfo.getPluginId());
        if (pkgInfoMap == null) {
            pkgInfoMap = new HashMap<>();
            mDownloadedPackageInfoMap.put(packageInfo.getPluginId(), pkgInfoMap);
        }
        pkgInfoMap.put(packageInfo.getVersionCode(), packageInfo);
    }

    private void removeDownloadedPluginPackageInfo(String pluginId, int versionCode) {
        Map<Integer, PluginPackageInfo> packageInfoMap = mDownloadedPackageInfoMap.get(pluginId);
        if (packageInfoMap == null) {
            return;
        }
        packageInfoMap.remove(versionCode);
    }

    private synchronized void savePluginInfoPref(PluginInfo pluginInfo) {
        PreferenceUtil.setSettingString(mPluginInfoPref, pluginInfo.getPluginId(),
                pluginInfo.toPrefJsonStr());
    }

    private synchronized void removePluginInfoPref(String pluginId) {
        PreferenceUtil.removePreference(mPluginInfoPref, pluginId);
    }

    private synchronized void saveDeveloperInfoPref(PluginDeveloperInfo developerInfo) {
        PreferenceUtil.setSettingString(mDeveloperInfoPref, developerInfo.getDeveloperId(),
                developerInfo.toPrefJsonStr());
    }

    private synchronized void removeDeveloperInfoPref(String developerId) {
        PreferenceUtil.removePreference(mDeveloperInfoPref, developerId);
    }

    private PackageRawInfo getPackageRawInfo(String packagePath) {
        PackageRawInfo rawInfo = null;

        if (TextUtils.isEmpty(packagePath)) {
            return null;
        }

        PackageInfo packageInfo = getAppContext().getPackageManager().getPackageArchiveInfo(
                packagePath, PackageManager.GET_META_DATA);

        if (packageInfo != null) {
            rawInfo = new PackageRawInfo();
            rawInfo.mVersionCode = packageInfo.versionCode;
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

    private synchronized PluginDeveloperInfo getPluginDeveloperInfo(String developerId) {
        return mDeveloperInfoMap.get(developerId);
    }

    private void unpackAssetPlugin() {
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

                PackageRawInfo tmpRawInfo = getPackageRawInfo(tmpPath);

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

                    PluginInfo pluginInfo = mPluginInfoMap.get(tmpRawInfo.mPluginId);
                    if (pluginInfo == null) {
                        FileUtil.deleteFile(tmpPath);
                        continue;
                    }

                    boolean installedLower = pluginInfo.isInstalled()
                            && pluginInfo.getInstalledVersionCode() < tmpRawInfo.mVersionCode;
                    boolean downloadedLower = !pluginInfo.isInstalled() && pluginInfo.isDownloaded()
                            && pluginInfo.getDownloadedVersionCode() < tmpRawInfo.mVersionCode;
                    boolean none = !pluginInfo.isInstalled() && !pluginInfo.isDownloaded();

                    if (installedLower || downloadedLower || none) {
                        String downloadedPath = getDownloadedPath(tmpRawInfo.mPluginId,
                                tmpRawInfo.mVersionCode);

                        boolean isCopySuccess = copyFileToFile(tmpPath, downloadedPath);

                        FileUtil.deleteFile(tmpPath);

                        if (!isCopySuccess) {
                            FileUtil.deleteFile(tmpPath);
                            continue;
                        }

                        PackageRawInfo rawInfo = getPackageRawInfo(downloadedPath);
                        if (rawInfo == null) {
                            FileUtil.deleteFile(tmpPath);
                            continue;
                        }

                        PluginPackageInfo downloadedPackageInfo = new PluginPackageInfo();
                        downloadedPackageInfo.setPluginId(rawInfo.mPluginId);
                        downloadedPackageInfo.setVersionCode(rawInfo.mVersionCode);
                        downloadedPackageInfo.setDeveloperId(rawInfo.mDeveloperId);
                        downloadedPackageInfo.setMinApiLevel(rawInfo.mMinApiLevel);
                        downloadedPackageInfo.setPackageName(rawInfo.mPackageName);
                        downloadedPackageInfo.setPackagePath(downloadedPath);

                        addDownloadedPluginPackageInfo(downloadedPackageInfo);

                        pluginInfo.setDownloadedPackageInfo(downloadedPackageInfo.getVersionCode(),
                                downloadedPackageInfo);

                        savePluginInfoPref(pluginInfo);
                    } else {
                        FileUtil.deleteFile(tmpPath);
                    }
                } else {
                    FileUtil.deleteFile(tmpPath);
                }
            }
        } catch (Exception e) {
        }
    }

    private String getDownloadedBasePath() {
        return getPluginBaseDir() + File.separator + DOWNLOADED_DIR;
    }

    private String getDownloadedPath(String pluginId, int version) {
        return getDownloadedBasePath() + File.separator + pluginId + File.separator + version
                + ".apk";
    }

    String getInstalledBasePath() {
        return getPluginBaseDir() + File.separator + INSTALLED_DIR;
    }

    private String getInstalledApkBasePath() {
        return getInstalledBasePath() + File.separator + "apk";
    }

    private String getInstalledPath(String pluginId, int version) {
        return getInstalledApkBasePath() + File.separator + pluginId + File.separator + version
                + ".apk";
    }

    private boolean validateSignature(PluginDeveloperInfo developerInfo, String path) {
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

    public synchronized PluginInfo getPluginInfo(String pluginId) {
        if (TextUtils.isEmpty(pluginId)) {
            return null;
        }
        return mPluginInfoMap.get(pluginId);
    }

}
