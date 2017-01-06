
package app.lib.plugin.frame.runtime.api;

import android.app.Application;
import android.content.Context;

import app.lib.plugin.sdk.PluginHostApi;

/**
 * Created by chenhao on 16/12/24.
 */

public class PluginHostApiImpl extends PluginHostApi {

    public PluginHostApiImpl() {
        super();
    }

    @Override
    public int getApiLevel() {
        return 0;
    }

    @Override
    public Application getApplication() {
        return null;
    }

    @Override
    public Context getAppContext() {
        return null;
    }
}
