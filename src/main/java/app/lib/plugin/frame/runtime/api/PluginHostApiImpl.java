
package app.lib.plugin.frame.runtime.api;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import java.io.File;
import java.lang.reflect.Method;

import app.lib.plugin.frame.BuildConfig;
import app.lib.plugin.frame.Plugin;
import app.lib.plugin.frame.PluginRuntimeManager;
import app.lib.plugin.frame.PluginSetting;
import app.lib.plugin.sdk.PluginContext;
import app.lib.plugin.sdk.PluginHostApi;
import app.lib.plugin.sdk.activity.PluginBaseActivity;

import static app.lib.plugin.frame.runtime.activity.PluginHostActivityBase.KEY_ACTIVITY_CLASS;
import static app.lib.plugin.frame.runtime.activity.PluginHostActivityBase.KEY_PLUGIN_ID;
import static app.lib.plugin.frame.runtime.activity.PluginHostActivityBase.KEY_VERSION_CODE;

/**
 * Created by chenhao on 16/12/24.
 */

public class PluginHostApiImpl extends PluginHostApi {

    public PluginHostApiImpl() {
        super();
    }

    @Override
    public int getApiLevel() {
        return PluginSetting.API_LEVEL;
    }

    @Override
    public Application getApplication() {
        return Plugin.getInstance().getApplication();
    }

    @Override
    public Context getAppContext() {
        return Plugin.getInstance().getAppContext();
    }

    @Override
    public int getAppVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    @Override
    public String getAppVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void startActivity(Context context, PluginContext pluginContext,
            Class<? extends PluginBaseActivity> activityClass, Intent intent) {
        int requestCode = -1;
        if (intent != null) {
            requestCode = intent.getIntExtra("requestCode", -1);
        }
        startActivityForResult(context, pluginContext, activityClass, intent, requestCode);
    }

    @Override
    public void startActivityForResult(Context context, PluginContext pluginContext,
            Class<? extends PluginBaseActivity> activityClass, Intent intent, int requestCode) {
        PluginRuntimeManager.PluginProcess process = PluginRuntimeManager.getInstance()
                .chooseProcess(pluginContext.getPluginId());

        Class hostActivityClass = PluginRuntimeManager.getInstance().getHostActivityClass(process);

        if (hostActivityClass == null) {
            return;
        }
        Intent startIntent = new Intent(getAppContext(), hostActivityClass);

        startIntent.putExtra(KEY_PLUGIN_ID, pluginContext.getPluginId());
        startIntent.putExtra(KEY_VERSION_CODE, pluginContext.getVersionCode());
        startIntent.putExtra(KEY_ACTIVITY_CLASS, activityClass.getName());

        if (intent != null) {
            startIntent.setData(intent.getData());
            startIntent.putExtras(intent);
            startIntent.setFlags(intent.getFlags());
        }
        if (context instanceof PluginBaseActivity) {
            PluginBaseActivity baseActivity = (PluginBaseActivity) context;
            baseActivity.activity().startActivityForResult(startIntent, requestCode);
        } else if (context instanceof Activity) {
            ((Activity) context).startActivityForResult(startIntent, requestCode);
        } else {
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startIntent);
        }
    }

    @Override
    public void loadLibrary(PluginContext pluginContext, String libName) {
        if (pluginContext == null || TextUtils.isEmpty(libName)) {
            return;
        }

        try {
            String libPath = PluginSetting.getSoDir(getAppContext(), pluginContext.getPluginId(),
                    pluginContext.getVersionCode()) + File.separator + "lib" + libName + ".so";
            Method method = Runtime.getRuntime().getClass().getDeclaredMethod("load", String.class,
                    ClassLoader.class);
            method.setAccessible(true);
            method.invoke(Runtime.getRuntime(), libPath, pluginContext.getClassLoader());
        } catch (Throwable e) {
            int h = hashCode();
        }
    }
}
