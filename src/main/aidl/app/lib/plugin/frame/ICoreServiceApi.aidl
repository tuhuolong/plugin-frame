// ICoreServiceApi.aidl
package app.lib.plugin.frame;

import app.lib.plugin.frame.entity.PluginInfo;

interface ICoreServiceApi {

    PluginInfo getPluginInfo(in String pluginId);
}
