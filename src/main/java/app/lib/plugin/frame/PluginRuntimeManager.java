
package app.lib.plugin.frame;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import app.lib.plugin.frame.runtime.bridge.IBridgeServiceApi;
import app.lib.plugin.frame.runtime.bridge.PluginBridgeServiceMain;
import app.lib.plugin.frame.runtime.bridge.PluginBridgeServicePlugin1;
import app.lib.plugin.frame.runtime.bridge.PluginBridgeServicePlugin2;
import app.lib.plugin.sdk.PluginContext;

/**
 * Created by chenhao on 16/12/24.
 */

public class PluginRuntimeManager {
    private static final Object sLock = new Object();

    private static PluginRuntimeManager sInstance;

    private IBridgeServiceApi mMainBridgeApiProxy;
    private IBridgeServiceApi mPlugin1BridgeApiProxy;
    private IBridgeServiceApi mPlugin2BridgeApiProxy;

    // model对应的hash
    // private final ConcurrentHashMap<String, XmPluginPackage> mUrlPackages = new
    // ConcurrentHashMap<String, XmPluginPackage>();
    // private final ConcurrentHashMap<String, XmPluginPackage> mPackagePathPackages = new
    // ConcurrentHashMap<String, XmPluginPackage>();
    // private Context mAppContext;

    private PluginRuntimeManager() {
        // mAppContext = PluginSettings.sContext;
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

    private PluginProcess chooseProcess(int pluginId) {
        return PluginProcess.MAIN;
    }

    public void sendMessage(final Context context, final int pluginId, final int msgType,
            final Bundle msgArg) {

        final PluginProcess process = chooseProcess(pluginId);

        IBridgeServiceApi apiProxy = getBridgeApiProxy(process);
        if (apiProxy == null) {

            Class clazz = getPluginBridgeServiceClass(process);
            if (clazz == null) {
                return;
            }
            Intent intent = new Intent(context, clazz);
            context.bindService(intent, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder service) {
                    IBridgeServiceApi apiProxy = IBridgeServiceApi.Stub.asInterface(service);

                    setBridgeApiProxy(process, apiProxy);

                    realSendMessage(apiProxy, pluginId, msgType, msgArg);
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    setBridgeApiProxy(process, null);

                    context.unbindService(this);
                }
            }, Context.BIND_AUTO_CREATE);

        } else {
            realSendMessage(apiProxy, pluginId, msgType, msgArg);
        }
    }

    void realSendMessage(IBridgeServiceApi apiProxy, int pluginId, int msgType, Bundle msgArg) {
        try {
            apiProxy.sendMessage(pluginId, msgType, msgArg, null);
        } catch (RemoteException e) {
        }
    }

    public IBridgeServiceApi getBridgeApiProxy(PluginProcess process) {
        if (process == null) {
            return null;
        }

        IBridgeServiceApi apiProxy = null;

        synchronized (sLock) {
            if (process == PluginProcess.MAIN) {
                apiProxy = mMainBridgeApiProxy;
            } else if (process == PluginProcess.PLUGIN1) {
                apiProxy = mPlugin1BridgeApiProxy;
            } else if (process == PluginProcess.PLUGIN2) {
                apiProxy = mPlugin2BridgeApiProxy;
            }
        }

        return apiProxy;
    }

    public void setBridgeApiProxy(PluginProcess process, IBridgeServiceApi apiProxy) {
        if (process == null) {
            return;
        }

        synchronized (sLock) {
            if (process == PluginProcess.MAIN) {
                mMainBridgeApiProxy = apiProxy;
            } else if (process == PluginProcess.PLUGIN1) {
                mPlugin1BridgeApiProxy = apiProxy;
            } else if (process == PluginProcess.PLUGIN2) {
                mPlugin2BridgeApiProxy = apiProxy;
            }
        }
    }

    private Class getPluginBridgeServiceClass(PluginProcess runningProcess) {
        if (runningProcess == PluginProcess.MAIN) {
            return PluginBridgeServiceMain.class;
        } else if (runningProcess == PluginProcess.PLUGIN1) {
            return PluginBridgeServicePlugin1.class;
        } else if (runningProcess == PluginProcess.PLUGIN2) {
            return PluginBridgeServicePlugin2.class;
        } else {
            return null;
        }
    }

    public PluginContext loadApk(int pluginId) {
        PluginContext pluginContext = null;
        return pluginContext;
    }

    private enum PluginProcess {
        MAIN("main"), PLUGIN1("plugin1"), PLUGIN2("plugin2");

        private String mValue;

        PluginProcess(String value) {
            mValue = value;
        }

        public String getValue() {
            return mValue;
        }
    }

    // public static PackageRawInfo loadPackageInfo(Context context, String packagePath) {
    // PackageRawInfo rawInfo = null;
    //
    // if (TextUtils.isEmpty(packagePath)) {
    // return null;
    // }
    //
    // PackageInfo packageInfo = context.getPackageManager().getPackageArchiveInfo(
    // packagePath, PackageManager.GET_META_DATA);
    //
    // if (packageInfo != null) {
    // rawInfo = new PackageRawInfo();
    // rawInfo.mVersion = packageInfo.versionCode;
    // rawInfo.mPackageName = packageInfo.packageName;
    // ApplicationInfo appInfo = packageInfo.applicationInfo;
    // if (appInfo != null) {
    // Bundle bundle = appInfo.metaData;
    // if (bundle != null) {
    // String urls = bundle.getString("urls", "");
    // rawInfo.mUrlList = Arrays.asList(urls.split("\\|"));
    // rawInfo.mMinApiLevel = bundle.getInt("minPluginSdkApiVersion", 0);
    // rawInfo.mDeveloperId = getDeveloperIdFromBundleHelper(bundle);
    // rawInfo.mPlatform = bundle.getString("MiHomePlatform", "");
    // rawInfo.mMessageHandleName = bundle.getString("message_handler", "");
    // }
    // }
    // }
    //
    // return rawInfo;
    // }

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

    /**
     * 加载apk文件，获取apk XmPluginPackage 信息
     *
     * @param
     * @return
     */
    // public PluginLoadedInfo loadApk(String packagePath, PackageRawInfo packageRawInfo) {
    // XmPluginPackage loadedInfo = mPackagePathPackages.get(packagePath);
    // if (loadedInfo != null)
    // return loadedInfo;
    //
    // if (packageRawInfo == null) {
    // packageRawInfo = loadPackageInfo(mAppContext, packagePath);
    // }
    // if (packageRawInfo == null) {
    // return null;
    // }
    //
    // DexClassLoader dexClassLoader = createDexClassLoader(packageRawInfo, packagePath);
    // AssetManager assetManager = createAssetManager(packagePath);
    // Resources resources = createResources(assetManager);
    // IXmPluginMessageReceiver xmPluginMessageReceiver = null;
    // if (!TextUtils.isEmpty(packageRawInfo.mMessageHandleName)) {
    // try {
    // Class<?> localClass = dexClassLoader.loadClass(packageRawInfo.mMessageHandleName);
    // Constructor<?> localConstructor = localClass
    // .getConstructor(new Class[]{});
    // Object instance = localConstructor.newInstance(new Object[]{});
    // xmPluginMessageReceiver = (IXmPluginMessageReceiver) instance;
    // } catch (Exception e) {
    // Log.e(TAG, "load apk", e);
    // return null;
    // }
    // }
    //
    // loadedInfo = new XmPluginPackage(packagePath, packageRawInfo, dexClassLoader, assetManager,
    // resources, xmPluginMessageReceiver);
    //// ApplicationInfo appInfo = rawPackageInfo.applicationInfo;
    //// if (Build.VERSION.SDK_INT >= 8) {
    //// appInfo.sourceDir = packagePath;
    //// appInfo.publicSourceDir = packagePath;
    //// }
    //
    // for (String url : packageRawInfo.mUrlList) {
    // mUrlPackages.put(url, loadedInfo);
    // }
    // mPackagePathPackages.put(packagePath, loadedInfo);
    // return loadedInfo;
    // }

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
