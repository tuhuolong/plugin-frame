
package app.lib.plugin.frame;

import android.content.Context;

import java.io.File;

/**
 * Created by chenhao on 16/12/24.
 */

public class PluginSetting {
    public static final int API_LEVEL = 1;
    // 调试插件Id起始值
    public static final long PLUGIN_ID_DEBUG_START = 1;
    // 调试插件Id结束值
    public static final long PLUGIN_ID_DEBUG_END = 100;
    // 调试包Id起始值
    public static final long PACKAGE_ID_DEBUG_START = 1;
    // 调试包Id结束值
    public static final long PACKAGE_ID_DEBUG_END = 100;

    public static boolean IS_DEBUG = false;

    public static boolean isValidatePluginId(long pluginId) {
        if (pluginId > 0) {
            return true;
        } else {
            return false;
        }

    }

    public static boolean isValidatePackageId(long packageId) {
        if (packageId > 0) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isDebugPackageId(long packageId) {
        if ((PACKAGE_ID_DEBUG_START <= packageId) && (packageId <= PACKAGE_ID_DEBUG_END)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isValidateDeveloperId(long developerId) {
        if (developerId > 0) {
            return true;
        } else {
            return false;
        }
    }

    public static String getDexOptimizedBaseDir(Context context) {
        File dexOutputDir = context.getDir("dex", Context.MODE_PRIVATE);
        String dir = dexOutputDir.getAbsolutePath() + File.separator + "plugin";
        return dir;
    }

    public static String getDexOptimizedDir(Context context, String pluginId) {
        File dexOutputDir = context.getDir("dex", Context.MODE_PRIVATE);
        String dir = dexOutputDir.getAbsolutePath() + File.separator + "plugin" + File.separator
                + pluginId;
        return dir;
    }

    public static String getSoBaseDirOld(Context context) {
        String SO_INSTALL_DIR_OLD = context.getFilesDir().getPath() + File.separator + "plugin"
                + File.separator + "install" + File.separator + "libs";
        return SO_INSTALL_DIR_OLD;
    }

    public static String getSoBaseDir(Context context) {
        String SO_INSTALL_DIR = PluginManager.getInstance().getInstalledBasePath() + File.separator
                + "lib";
        return SO_INSTALL_DIR;
    }

    public static String getSoDir(Context context, String pluginId, int versionCode) {
        String soInstallDir = getSoBaseDir(context) + File.separator + pluginId + File.separator
                + versionCode;
        return soInstallDir;
    }
}
