
package com.chenhao.lib.plugin.frame.runtime.api;

import android.app.Application;
import android.content.Context;

import com.chenhao.lib.plugin.sdk.PluginApi;

/**
 * Created by chenhao on 16/12/24.
 */

public class PluginApiImpl extends PluginApi {

    public PluginApiImpl() {
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
