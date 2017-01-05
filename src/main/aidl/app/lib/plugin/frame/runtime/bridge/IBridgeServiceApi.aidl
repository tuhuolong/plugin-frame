// IBridgeServiceApi.aidl
package app.lib.plugin.frame.runtime.bridge;

import app.lib.plugin.frame.runtime.bridge.IBridgeCallback;

interface IBridgeServiceApi {
    void sendMessage(int pluginId, int msgType, in Bundle msgArg, in IBridgeCallback bridgeCallback);

    void startService(in String targetPluginHostServiceClassName, long pluginId, long packageId, in Intent startIntent, in String serviceClassName);

    void exitProcess();
}