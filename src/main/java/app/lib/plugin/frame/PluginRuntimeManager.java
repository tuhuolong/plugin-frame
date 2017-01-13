
package app.lib.plugin.frame;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import app.lib.asynccallback.Error;
import app.lib.plugin.frame.entity.PluginPackageInfo;
import app.lib.plugin.frame.runtime.activity.PluginHostActivityMain;
import app.lib.plugin.frame.runtime.activity.PluginHostActivityPlugin1;
import app.lib.plugin.frame.runtime.activity.PluginHostActivityPlugin2;
import app.lib.plugin.frame.runtime.bridge.BridgeError;
import app.lib.plugin.frame.runtime.bridge.IBridgeSendMessageCallback;
import app.lib.plugin.frame.runtime.bridge.IBridgeServiceApi;
import app.lib.plugin.frame.runtime.bridge.PluginBridgeServiceMain;
import app.lib.plugin.frame.runtime.bridge.PluginBridgeServicePlugin1;
import app.lib.plugin.frame.runtime.bridge.PluginBridgeServicePlugin2;
import app.lib.plugin.frame.util.FileUtil;
import app.lib.plugin.sdk.IMessageReceiver;
import app.lib.plugin.sdk.PluginContext;
import dalvik.system.DexClassLoader;

import static app.lib.plugin.frame.runtime.bridge.PluginBridgeServiceBase.KEY_SEND_MESSAGE_RESULT_HANDLED;

/**
 * Created by chenhao on 16/12/24.
 */

public class PluginRuntimeManager {
    private static final Object sLock = new Object();

    private static PluginRuntimeManager sInstance;
    private final ConcurrentHashMap<String, PluginContext> mPackageContextMap = new ConcurrentHashMap<>();

    private Context mAppContext;

    private Map<PluginProcess, IBridgeServiceApi> mBridgeApiProxyMap = new ConcurrentHashMap<>();
    private IBridgeServiceApi mMainBridgeApiProxy;
    private IBridgeServiceApi mPlugin1BridgeApiProxy;
    private IBridgeServiceApi mPlugin2BridgeApiProxy;

    private PluginRuntimeManager() {
        mAppContext = Plugin.getInstance().getAppContext();
    }

    public static PluginRuntimeManager getInstance() {
        if (sInstance == null) {
            synchronized (sLock) {
                // 有可能在其他线程已创建
                if (sInstance == null) {
                    sInstance = new PluginRuntimeManager();
                }
            }
        }
        return sInstance;
    }

    // private static void applyLanguage(Resources resources, Locale locale) {
    // if (resources == null)
    // return;
    // Log.d("LanguageUtil", "applyLanguage:" + locale.toString());
    // Configuration config = resources.getConfiguration();
    // DisplayMetrics dm = resources.getDisplayMetrics();
    // config.locale = locale;
    // resources.updateConfiguration(config, dm);
    // }

    // static XmPluginPackage mLastXmPluginPackage = null;
    // public static void clearViewBuffer(XmPluginPackage xmPluginPackage) {
    // if(mLastXmPluginPackage==xmPluginPackage)
    // return;
    // mLastXmPluginPackage = xmPluginPackage;
    // try {
    // Field field = LayoutInflater.class.getDeclaredField("sConstructorMap");
    // boolean isStatic = Modifier.isStatic(field.getModifiers());
    // if (isStatic) {
    // field.setAccessible(true);
    // HashMap constructorMap = (HashMap) field.get(null);
    // constructorMap.clear();
    // }
    // } catch (Exception e) {
    //
    // }
    // }

    // public Class getPluginHostActivityClass(String packagePath) {
    // return PluginHostActivityMain.class;
    // }

    // public XmPluginPackage getXmPluginPackageByUrl(String url) {
    // return mUrlPackages.get(url);
    // }
    //
    // public XmPluginPackage getXmPluginPackageByPackagePath(String path) {
    // return mPackagePathPackages.get(path);
    // }
    //
    // public void applyLanguage(Locale locale) {
    // Collection<XmPluginPackage> packages = mPackagePathPackages.values();
    // for (XmPluginPackage xmPluginPackage : packages) {
    // applyLanguage(xmPluginPackage.getResources(), locale);
    // }
    // }
    //

    public static String getPluginContextId(String pluginId, int versionCode) {
        return pluginId + "_" + versionCode;
    }

    public PluginProcess chooseProcess(String pluginId) {
        return PluginProcess.PLUGIN1;
    }

    public void sendMessage(final Context context, final String pluginId, final int msgType,
            final Bundle msgArg, final PluginApi.SendMessageCallback callback) {

        final PluginProcess process = chooseProcess(pluginId);

        IBridgeServiceApi apiProxy = getBridgeApiProxy(process);
        if (apiProxy == null) {

            Class clazz = getBridgeServiceClass(process);
            if (clazz == null) {
                if (callback != null) {
                    callback.sendSendFailureMessage(new Error(-1, "not found BridgeServiceClass"));
                }
                return;
            }
            Intent intent = new Intent(context, clazz);
            context.bindService(intent, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder service) {
                    IBridgeServiceApi apiProxy = IBridgeServiceApi.Stub.asInterface(service);

                    setBridgeApiProxy(process, apiProxy);

                    doSendMessage(apiProxy, pluginId, msgType, msgArg, callback);
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    setBridgeApiProxy(process, null);

                    context.unbindService(this);
                }
            }, Context.BIND_AUTO_CREATE);

        } else {
            doSendMessage(apiProxy, pluginId, msgType, msgArg, callback);
        }
    }

    void doSendMessage(IBridgeServiceApi apiProxy, String pluginId, int msgType, Bundle msgArg,
            final PluginApi.SendMessageCallback callback) {
        try {
            apiProxy.sendMessage(pluginId, msgType, msgArg, new IBridgeSendMessageCallback.Stub() {
                @Override
                public void onSuccess(Bundle result) throws RemoteException {
                    boolean handled = false;
                    if (result != null) {
                        handled = result.getBoolean(KEY_SEND_MESSAGE_RESULT_HANDLED);
                    }
                    if (callback != null) {
                        callback.sendSendSuccessMessage(handled);
                    }
                }

                @Override
                public void onFailure(BridgeError error) throws RemoteException {
                    if (callback != null) {
                        callback.sendSendFailureMessage(
                                new Error(error.getCode(), error.getDetail()));
                    }
                }

            });
        } catch (RemoteException e) {
            if (callback != null) {
                callback.sendSendFailureMessage(new Error(-1, "RemoteException"));
            }
        }
    }

    public IBridgeServiceApi getBridgeApiProxy(PluginProcess process) {
        if (process == null) {
            return null;
        }

        IBridgeServiceApi apiProxy = mBridgeApiProxyMap.get(process);

        return apiProxy;
    }

    public void setBridgeApiProxy(PluginProcess process, IBridgeServiceApi apiProxy) {
        if (process == null) {
            return;
        }

        if (apiProxy == null) {
            mBridgeApiProxyMap.remove(process);
        } else {
            mBridgeApiProxyMap.put(process, apiProxy);
        }
    }

    private Class getBridgeServiceClass(PluginProcess process) {
        if (process == PluginProcess.MAIN) {
            return PluginBridgeServiceMain.class;
        } else if (process == PluginProcess.PLUGIN1) {
            return PluginBridgeServicePlugin1.class;
        } else if (process == PluginProcess.PLUGIN2) {
            return PluginBridgeServicePlugin2.class;
        } else {
            return null;
        }
    }

    public Class getHostActivityClass(PluginProcess process) {
        if (process == PluginProcess.MAIN) {
            return PluginHostActivityMain.class;
        } else if (process == PluginProcess.PLUGIN1) {
            return PluginHostActivityPlugin1.class;
        } else if (process == PluginProcess.PLUGIN2) {
            return PluginHostActivityPlugin2.class;
        } else {
            return null;
        }
    }

    public PluginContext loadApkRuntime(PluginPackageInfo packageInfo) {
        PluginContext pluginContext = mPackageContextMap
                .get(getPluginContextId(packageInfo.getPluginId(), packageInfo.getVersionCode()));
        if (pluginContext != null) {
            return pluginContext;
        }

        String packagePath = packageInfo.getPackagePath();

        PackageManager pm = mAppContext.getPackageManager();
        PackageInfo rawPackageInfo = pm.getPackageArchiveInfo(packagePath,
                PackageManager.GET_META_DATA);

        if (rawPackageInfo == null) {
            return null;
        }

        String messageReceiverClassName = "";
        if (rawPackageInfo.applicationInfo.metaData != null) {
            messageReceiverClassName = rawPackageInfo.applicationInfo.metaData
                    .getString("PluginMessageReceiver");
        }

        final String packageName = packageInfo.getPackageName();
        String pluginId = packageInfo.getPluginId();
        int versionCode = packageInfo.getVersionCode();
        DexClassLoader dexClassLoader = createDexClassLoader(pluginId, versionCode, packagePath);
        AssetManager assetManager = createAssetManager(packagePath);
        Resources resources = createResources(assetManager);
        IMessageReceiver messageReceiver = null;
        if (!TextUtils.isEmpty(messageReceiverClassName)) {
            try {
                Class<?> localClass = dexClassLoader.loadClass(messageReceiverClassName);
                Constructor<?> localConstructor = localClass.getConstructor(new Class[] {});
                Object instance = localConstructor.newInstance(new Object[] {});
                messageReceiver = (IMessageReceiver) instance;
            } catch (Exception e) {
                return null;
            }
        }

        pluginContext = new PluginContext(pluginId, versionCode, dexClassLoader, assetManager,
                resources, messageReceiver);

        mPackageContextMap.put(
                getPluginContextId(packageInfo.getPluginId(), packageInfo.getVersionCode()),
                pluginContext);

        return pluginContext;
    }

    public PluginContext getPluginContextRuntime(String pluginId, int versionCode) {
        return mPackageContextMap.get(getPluginContextId(pluginId, versionCode));
    }

    public void removePluginContextRuntime(String pluginId, int versionCode) {
        mPackageContextMap.remove(getPluginContextId(pluginId, versionCode));
    }

    public void removePluginContextAll(String pluginId, int versionCode) {
        for (IBridgeServiceApi bridgeApiProxy : mBridgeApiProxyMap.values()) {
            try {
                bridgeApiProxy.removePluginContext(pluginId, versionCode, null);
            } catch (RemoteException e) {
            }
        }
    }

    private DexClassLoader createDexClassLoader(String pluginId, int versionCode, String dexPath) {
        String dexOptimizedPath = PluginSetting.getDexOptimizedDir(mAppContext, pluginId);
        FileUtil.createDirIfNotExists(dexOptimizedPath);
        DexClassLoader loader = new DexClassLoader(dexPath, dexOptimizedPath, null,
                mAppContext.getClassLoader());
        return loader;
    }

    private AssetManager createAssetManager(String dexPath) {
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
            addAssetPath.invoke(assetManager, dexPath);
            return assetManager;
        } catch (Exception e) {
            return null;
        }
    }

    private Resources createResources(AssetManager assetManager) {
        Resources superRes = mAppContext.getResources();
        Resources resources = new Resources(assetManager, superRes.getDisplayMetrics(),
                superRes.getConfiguration());
        return resources;
    }

    public enum PluginProcess {
        MAIN("main"), PLUGIN1("plugin1"), PLUGIN2("plugin2");

        private String mValue;

        PluginProcess(String value) {
            mValue = value;
        }

        public String getValue() {
            return mValue;
        }
    }

    // private static long getDeveloperIdFromBundleHelper(Bundle bundle) {
    // if (bundle == null) {
    // return 0;
    // }
    // long developerId = 0;
    // try {
    // String rawDeveloperIdStr = bundle.getString(MPK_KEY_DEVELOPER_ID, "");
    // String developerIdStr = rawDeveloperIdStr.substring("id_".length(),
    // rawDeveloperIdStr.length());
    // developerId = Long.parseLong(developerIdStr);
    // } catch (Exception e) {
    // developerId = 0;
    // }
    // return developerId;
    // }
    //
    // private static final String MPK_KEY_DEVELOPER_ID = "MiHomeDeveloperId";

    // private DexClassLoader createDexClassLoader(PackageRawInfo packageRawInfo, String dexPath) {
    // String dexOptimizedPath = PluginSettings.getDexOptimizedDir(mAppContext, packageRawInfo);
    // FileUtils.createDirIfNotExists(dexOptimizedPath);
    // DexClassLoader loader = new DexClassLoader(dexPath, dexOptimizedPath, null,
    // mAppContext.getClassLoader());
    // return loader;
    // }
    //
    // private AssetManager createAssetManager(String dexPath) {
    // try {
    // AssetManager assetManager = AssetManager.class.newInstance();
    // Method addAssetPath = assetManager.getClass().getMethod(
    // "addAssetPath", String.class);
    // addAssetPath.invoke(assetManager, dexPath);
    // return assetManager;
    // } catch (Exception e) {
    //
    // return null;
    // }
    //
    // }

    // public XmPluginPackage getPackageFromPackage(String packageName) {
    // // Collection<XmPluginPackage> list = mPackages.values();
    // // for(XmPluginPackage packageitem:list){
    // // if(packageitem.packageName.equals(packageName))
    // // return packageitem;
    // // }
    // return null;
    // }
    //
    // public void removeDexFile(Context context, long pluginId, long packageId) {
    // if (PluginSettings.isValidatePackageId(packageId)) {
    // File dexOutputDir = context.getDir("dex", Context.MODE_PRIVATE);
    // String dexOutputPath = dexOutputDir.getAbsolutePath() + File.separator + "plugin"
    // + File.separator + pluginId + File.separator + packageId + ".dex";
    // File file = new File(dexOutputPath);
    // // FileUtil.safeDelSingleFile(file);
    // file.delete();
    // }
    // }
    //
    // private Resources createResources(AssetManager assetManager) {
    // Resources superRes = mAppContext.getResources();
    // Resources resources = new Resources(assetManager,
    // superRes.getDisplayMetrics(), superRes.getConfiguration());
    // return resources;
    // }
    //
    // public XmPluginPackage getXmPluginPackageByCrashClassName(List<String> classNameList) {
    // if (classNameList == null || classNameList.size() <= 0) {
    // return null;
    // }
    //
    // XmPluginPackage result = null;
    //
    // try {
    //
    // List<String> packageNameList = new ArrayList<String>();
    //
    // for (String className : classNameList) {
    // if (className.startsWith("android")
    // || className.startsWith("dalvik")
    // || className.startsWith("java")
    // || className.startsWith("javax")
    // || className.startsWith("com.xiaomi.smarthome")) {
    // continue;
    // }
    //
    // String[] fields = className.split("\\.");
    //
    // String pkgName1 = "";
    // String pkgName2 = "";
    // String pkgName3 = "";
    // if (fields.length >= 3) {
    // pkgName1 = fields[0] + "." + fields[1] + "." + fields[2];
    // packageNameList.add(pkgName1);
    // }
    //
    // if (fields.length >= 4) {
    // pkgName2 = fields[0] + "." + fields[1] + "." + fields[2] + "." + fields[3];
    // packageNameList.add(pkgName2);
    // }
    //
    // if (fields.length >= 5) {
    // pkgName3 = fields[0] + "." + fields[1] + "." + fields[2] + "." + fields[3]
    // + "."
    // + fields[4];
    // packageNameList.add(pkgName3);
    // }
    // }
    //
    // if (packageNameList.size() <= 0) {
    // return null;
    // }
    //
    // for (int i = 0; i < mPackagePathPackages.size(); i++) {
    // XmPluginPackage xmPluginPackage = mPackagePathPackages.get(i);
    // String packageName = xmPluginPackage.packageRawInfo.mPackageName;
    // if (TextUtils.isEmpty(packageName)) {
    // continue;
    // }
    //
    // boolean found = false;
    //
    // for (int j = 0, len = packageNameList.size(); j < len; j++) {
    // String pkgName = packageNameList.get(j);
    // if (packageName.equalsIgnoreCase(pkgName)) {
    // found = true;
    // result = xmPluginPackage;
    // break;
    // }
    // }
    //
    // if (found) {
    // break;
    // }
    // }
    // } catch (Exception e) {
    // result = null;
    // }
    //
    // return result;
    // }
    //
    //
    // public Class getPluginActivityClass() {
    // return PluginHostActivityMain.class;
    // }

}
