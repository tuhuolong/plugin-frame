
package com.chenhao.lib.plugin.frame;

/**
 * Created by chenhao on 16/12/24.
 */

public class PluginRecord {

    // 已安装信息
    private String mInstalledPluginId;
    private long mInstalledPluginVersion;
    private PluginPackageInfo mInstalledPackageInfo;
    private PluginDeveloperInfo mInstalledDeveloperInfo;

    // 已下载信息
    private String mDownloadedPluginId;
    private long mDownloadedPluginVersion;
    private PluginPackageInfo mDownloadedPackageInfo;
    private PluginDeveloperInfo mDownloadedDeveloperInfo;
}
