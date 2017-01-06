
package app.lib.plugin.frame.server;

import android.content.Context;

import org.json.JSONArray;

import app.lib.asynccallback.AsyncCallback;
import app.lib.asynccallback.Error;
import app.lib.plugin.frame.server.result.UpdatePluginResult;

/**
 * Created by chenhao on 17/1/6.
 */

public class PluginServerApi {

    public void updatePlugin(Context context, JSONArray pluginInfoArray, int apiLevel,
            int appVersion, AsyncCallback<UpdatePluginResult, Error> callback) {

        UpdatePluginResult result = new UpdatePluginResult();

        if (callback != null) {

        }
    }
}
