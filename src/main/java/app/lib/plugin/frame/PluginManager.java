
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

import app.lib.asynccallback.AsyncCallback;
import app.lib.asynccallback.Error;
import app.lib.plugin.frame.common.MessageHandlerThread;
import app.lib.plugin.frame.entity.PluginInfo;
import app.lib.plugin.frame.entity.PluginPackageInfo;
import app.lib.plugin.frame.util.ByteUtil;
import app.lib.plugin.frame.util.FileUtil;

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
    private static final String DOWNLOADED_DIR = "download";
    private static final String INSTALLED_DIR = "install";

    private static PluginManager sInstance;
    MessageHandlerThread mWorkerThread;
    Handler mWorkerHandler;

    private boolean mIsInitialized = false;

    private SharedPreferences mPluginInfoPref;
    private Map<String, PluginInfo> mPluginInfoMap = new HashMap();
    private Map<String, PluginDeveloperInfo> mDeveloperInfoMap = new HashMap<>();
    // private Map<String, Map<Integer, PluginPackageInfo>> mDownloadedPackageInfoMap = new
    // HashMap<>();

    private PluginManager() {
        mWorkerThread = new MessageHandlerThread("PluginWorker");
        mWorkerThread.start();
        mWorkerHandler = new Handler(mWorkerThread.getLooper());

        mWorkerHandler.post(new Runnable() {
            @Override
            public void run() {
                init();

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

    public boolean isInitialized() {
        synchronized (this) {
            return mIsInitialized;
        }
    }

    private void init() {
        mPluginInfoPref = getAppContext().getSharedPreferences(PREF_PLUGIN_INFO_PREF,
                Context.MODE_PRIVATE);

        PluginInfo pluginInfo = new PluginInfo();
        pluginInfo.setPluginId("id_1");
        pluginInfo.setName("Demo");
        pluginInfo.setIcon("http://test.png");
        mPluginInfoMap.put(pluginInfo.getPluginId(), pluginInfo);
        //
        // PluginDeveloperInfo developerInfo = new PluginDeveloperInfo();
        // developerInfo.setDeveloperId("id_894148746");
        // developerInfo.setCert("");
        // mDeveloperInfoMap.put(developerInfo.getDeveloperId(), developerInfo);

        synchronized (this) {
            mIsInitialized = true;
        }
    }

    public void downloadPlugin(PluginInfo pluginInfo) {

    }

    public void installPlugin(final PluginInfo pluginInfo, PluginPackageInfo installingPackageInfo,
            AsyncCallback<Void, Error> callback) {
        if (pluginInfo == null || installingPackageInfo == null) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(-1, " "));
            }
            return;
        }

        if (pluginInfo.isInstalled() && pluginInfo.getInstalledPackageInfo()
                .getVersionCode() >= installingPackageInfo.getVersionCode()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(-1, " "));
            }
            return;
        }

        String installedPluginId = installingPackageInfo.getPluginId();
        int installedVersionCode = installingPackageInfo.getVersionCode();
        String installedPackagePath = getInstalledPath(installedPluginId, installedVersionCode);

        FileUtil.copyFileToFile(installingPackageInfo.getPackagePath(), installedPackagePath);

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

            pluginInfo.setInstalledPackageInfo(installedPackageInfo);

            if (callback != null) {
                callback.sendSuccessMessage(null);
            }

        } else {
            FileUtil.deleteFile(installedPackagePath);

            if (callback != null) {
                callback.sendFailureMessage(new Error(-1, " "));
            }
        }
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

                    String downloadedPath = getDownloadedPath(tmpRawInfo.mPluginId,
                            tmpRawInfo.mVersionCode);

                    boolean isCopySuccess = FileUtil.copyFileToFile(tmpPath, downloadedPath);

                    FileUtil.deleteFile(tmpPath);

                    if (isCopySuccess) {
                        PackageRawInfo rawInfo = getPackageRawInfo(downloadedPath);
                        if (rawInfo != null) {

                            PluginInfo pluginInfo = getPluginInfo(rawInfo.mPluginId);

                            if (pluginInfo != null) {
                                boolean installedLower = pluginInfo.isInstalled()
                                        && pluginInfo.getInstalledPackageInfo()
                                                .getVersionCode() < rawInfo.mVersionCode;
                                boolean downloadedLower = !pluginInfo.isInstalled()
                                        && pluginInfo.isDownloaded()
                                        && pluginInfo.getDownloadedPackageInfo()
                                                .getVersionCode() < rawInfo.mVersionCode;
                                boolean none = !pluginInfo.isInstalled()
                                        && !pluginInfo.isDownloaded();

                                if (installedLower || downloadedLower || none) {
                                    PluginPackageInfo downloadedPackageInfo = new PluginPackageInfo();
                                    downloadedPackageInfo.setPluginId(rawInfo.mPluginId);
                                    downloadedPackageInfo.setVersionCode(rawInfo.mVersionCode);
                                    downloadedPackageInfo.setDeveloperId(rawInfo.mDeveloperId);
                                    downloadedPackageInfo.setMinApiLevel(rawInfo.mMinApiLevel);
                                    downloadedPackageInfo.setPackageName(rawInfo.mPackageName);
                                    downloadedPackageInfo.setPackagePath(downloadedPath);
                                    pluginInfo.setDownloadedPackageInfo(downloadedPackageInfo);
                                } else {
                                    FileUtil.deleteFile(downloadedPath);
                                }

                            } else {
                                FileUtil.deleteFile(downloadedPath);
                            }
                        } else {
                            FileUtil.deleteFile(downloadedPath);
                        }
                    } else {
                        FileUtil.deleteFile(downloadedPath);
                    }

                } else {
                    FileUtil.deleteFile(tmpPath);
                }
            }
        } catch (Exception e) {
        }
    }

    private String getDownloadedPath(String pluginId, int version) {
        return getPluginBaseDir() + File.separator + DOWNLOADED_DIR + File.separator + pluginId
                + File.separator + version + ".apk";
    }

    private String getInstalledPath(String pluginId, int version) {
        return getPluginBaseDir() + File.separator + INSTALLED_DIR + File.separator + pluginId
                + File.separator + version + ".apk";
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

    public synchronized PluginInfo getPluginInfo(String pluginId) {
        if (TextUtils.isEmpty(pluginId)) {
            return null;
        }
        return mPluginInfoMap.get(pluginId);
    }

    // private synchronized void addDownloadedPackageInfo(PluginPackageInfo packageInfo) {
    // Map<Integer, PluginPackageInfo> pluginPackageInfoMap = mDownloadedPackageInfoMap
    // .get(packageInfo.getPluginId());
    // if (pluginPackageInfoMap == null) {
    // pluginPackageInfoMap = new HashMap<>();
    // mDownloadedPackageInfoMap.put(packageInfo.getPluginId(), pluginPackageInfoMap);
    // }
    // pluginPackageInfoMap.put(packageInfo.getVersionCode(), packageInfo);
    // }

}
