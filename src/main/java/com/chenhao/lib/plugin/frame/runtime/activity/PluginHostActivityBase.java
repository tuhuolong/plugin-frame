
package com.chenhao.lib.plugin.frame.runtime.activity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.chenhao.lib.plugin.frame.debug.PluginErrorInfoActivity;
import com.chenhao.lib.plugin.sdk.PluginLoadedInfo;
import com.chenhao.lib.plugin.sdk.page.PluginBaseActivity;

import java.util.Iterator;
import java.util.Set;

/**
 * Created by chenhao on 16/12/24.
 */

public class PluginHostActivityBase extends FragmentActivity {
    public static final String EXTRA_DEVICE_DID = "extra_device_did";
    public static final String EXTRA_DEVICE_MODEL = "extra_device_model";
    public static final int ACTIVITY_REQUEST_VERIFY_PINCODE = 9999;
    protected static final int MSG_EXIT_PROCESS = 1;
    static final String FRAGMENTS_TAG = "android:support:fragments";
    private static final int PROCESS_KILL_DELAY = 30 * 1000;
    // private static ArrayList<WeakReference<PluginHostActivity>> mPluginHostActivityRefStack = new
    // ArrayList<>();

    private static Handler sHandle = null;

    String mModel;
    Resources.Theme mTheme;
    boolean mEnableVerifyPincode = false;
    boolean mIsVerifyed = false;
    boolean mIsSupportAd = false;
    private PluginLoadedInfo mLoadedInfo;
    private String mClass;
    private String mPackageName;
    private ActivityInfo mActivityInfo;

    // private PluginBaseActivity mPluginActivity;
    private PluginBaseActivity mXmPluginActivity;
    private int mOnResumeTimestamp;
    private String mPageName;

    protected static Handler getHandler() {
        if (sHandle == null) {
            sHandle = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_EXIT_PROCESS: {
                            System.exit(0);
                            break;
                        }
                    }
                }
            };
        }
        return sHandle;
    }

    // public XmPluginPackage getXmPluginPackage() {
    // return this.mLoadedInfo;
    // }

    // ---------------------------- Android 原生接口 start ------------------------------

    @Override
    public Resources getResources() {
        if (mLoadedInfo == null) {
            return super.getResources();
        } else {
            return mLoadedInfo.getResources();
        }
    }

    @Override
    public AssetManager getAssets() {
        if (mLoadedInfo == null) {
            return super.getAssets();
        } else {
            return mLoadedInfo.getAssetManager();
        }
    }

    @Override
    public Resources.Theme getTheme() {
        if (mTheme == null) {
            return super.getTheme();
        } else {
            return mTheme;
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        if (mLoadedInfo == null) {
            return super.getClassLoader();
        } else {
            return mLoadedInfo.getClassLoader();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // 防止切到后台被kill掉后，再次启动初始化Fragment crash
        if (savedInstanceState != null) {
            savedInstanceState.remove(FRAGMENTS_TAG);
        }
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        String did = "";
        // 设置intent classloader
        Set<String> categories = intent.getCategories();
        if (categories != null) {
            Iterator<String> iterator = categories.iterator();
            while (iterator.hasNext()) {
                String category = iterator.next();
                if (category.startsWith("did:")) {
                    did = category.substring("did:".length());
                } else if (category.startsWith("model:")) {
                    mModel = category.substring("model:".length());
                }
            }
        }

        if (TextUtils.isEmpty(did) && TextUtils.isEmpty(mModel)) {
            did = getIntent().getStringExtra(EXTRA_DEVICE_DID);
        }

        // mLoadedInfo = PluginRuntimeManager.getInstance().getXmPluginPackage(mModel);
        // if (mLoadedInfo == null) {
        // setResult(RESULT_CANCELED);
        // finish();
        // return;
        // }
        // }
        /// 注意，不要在此之前调用intent.getExtra之类接口
        intent.setExtrasClassLoader(mLoadedInfo.getClassLoader());
        // mPackageName = intent.getStringExtra(IXmPluginActivity.EXTRA_PACKAGE);
        // mClass = intent.getStringExtra(IXmPluginActivity.EXTRA_CLASS);

        // getActivityInfo();
        handleActivityInfo();
        launchActivity(intent);

    }

    // private void getActivityInfo() {
    // if (mLoadedInfo == null)
    // return;
    // PackageInfo packageInfo = mLoadedInfo.packageInfo;
    // if ((packageInfo.activities != null)
    // && (packageInfo.activities.length > 0)) {
    // if (mClass == null) {
    // mClass = packageInfo.activities[0].name;
    // }
    // for (ActivityInfo a : packageInfo.activities) {
    // if (a.name.equals(mClass)) {
    // mActivityInfo = a;
    // }
    // }
    // }
    // }

    private void handleActivityInfo() {
        Resources.Theme superTheme = super.getTheme();
        mTheme = mLoadedInfo.getResources().newTheme();
        mTheme.setTo(superTheme);

        int defaultTheme = 0;
        if (Build.VERSION.SDK_INT >= 14) {
            defaultTheme = android.R.style.Theme_DeviceDefault;
        } else {
            defaultTheme = android.R.style.Theme;
        }
        // Finals适配三星以及部分加载XML出现异常BUG
        try {
            mTheme.applyStyle(defaultTheme, true);
        } catch (Exception e) {

        }
    }

    private void launchActivity(Intent intent) {
        // if (mLoadedInfo != null) {
        // try {
        // Class<?> localClass = mLoadedInfo.classLoader.loadClass(mClass);
        // Constructor<?> localConstructor = localClass.getConstructor(new Class[] {});
        // Object instance = localConstructor.newInstance(new Object[] {});
        // mXmPluginActivity = (XmPluginBaseActivity) instance;
        // mXmPluginActivity.attach(this, mLoadedInfo, mDevice);
        // mXmPluginActivity.setIntent(intent);
        // mXmPluginActivity.onCreate(intent.getExtras());
        // } catch (Throwable e) {
        // PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
        // finish();
        // return;
        // }
        // } else {
        // try {
        // Class<?> localClass = getClassLoader().loadClass(mClass);
        // Constructor<?> localConstructor = localClass
        // .getConstructor(new Class[] {});
        // Object instance = localConstructor.newInstance(new Object[] {});
        // mXmPluginActivity = (XmPluginBaseActivity) instance;
        // mXmPluginActivity.attach(this, mLoadedInfo, mDevice);
        // mXmPluginActivity.setIntent(intent);
        // mXmPluginActivity.onCreate(intent.getExtras());
        // } catch (Exception e) {
        // PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
        // finish();
        // return;
        // }
        // }

    }

    @Override
    public void recreate() {
        super.recreate();

        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.recreate();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.onPostCreate(savedInstanceState);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.onStart();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.onResume();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.onPostResume();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.onPause();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.onStop();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.onDestroy();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);

        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.onSaveInstanceState(outState);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.onRestoreInstanceState(savedInstanceState);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (ACTIVITY_REQUEST_VERIFY_PINCODE == requestCode) {
            if (resultCode == RESULT_CANCELED) {
                finish();
            }
            return;
        }
        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.onActivityResult(requestCode, resultCode, data);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.onRequestPermissionsResult(requestCode, permissions,
                        grantResults);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.onNewIntent(intent);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();

        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.onBackPressed();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mXmPluginActivity != null) {
            try {
                if (mXmPluginActivity.onTouchEvent(event))
                    return true;
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return false;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mXmPluginActivity != null) {
            try {
                if (mXmPluginActivity.dispatchTouchEvent(ev))
                    return true;
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return false;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mXmPluginActivity != null) {
            try {
                if (mXmPluginActivity.onKeyDown(keyCode, event))
                    return true;
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return false;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mXmPluginActivity != null) {
            try {
                if (mXmPluginActivity.onKeyUp(keyCode, event))
                    return true;
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return false;
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (mXmPluginActivity != null) {
            try {
                if (mXmPluginActivity.onKeyLongPress(keyCode, event))
                    return true;
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return false;
            }
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        if (mXmPluginActivity != null) {
            try {
                if (mXmPluginActivity.onKeyMultiple(keyCode, repeatCount, event))
                    return true;
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return false;
            }
        }
        return super.onKeyMultiple(keyCode, repeatCount, event);
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        if (mXmPluginActivity != null) {
            try {
                if (mXmPluginActivity.onKeyShortcut(keyCode, event))
                    return true;
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return false;
            }
        }
        return super.onKeyShortcut(keyCode, event);
    }

    @Override
    public void onWindowAttributesChanged(WindowManager.LayoutParams params) {
        if (mXmPluginActivity != null)
            try {
                mXmPluginActivity.onWindowAttributesChanged(params);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        super.onWindowAttributesChanged(params);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (mXmPluginActivity != null)
            try {
                mXmPluginActivity.onWindowFocusChanged(hasFocus);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mXmPluginActivity != null)
            try {
                mXmPluginActivity.onCreateOptionsMenu(menu);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return false;
            }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mXmPluginActivity != null)
            try {
                mXmPluginActivity.onOptionsItemSelected(item);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return false;
            }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onContentChanged() {
        // super.onContentChanged();

        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.onContentChanged();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.onAttachedToWindow();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.onDetachedFromWindow();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.onLowMemory();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.onTrimMemory(level);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        if (mXmPluginActivity != null) {
            try {
                if (mXmPluginActivity.onTrackballEvent(event))
                    return true;
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return false;
            }
        }

        return super.onTrackballEvent(event);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();

        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.onUserInteraction();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();

        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.onUserLeaveHint();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mXmPluginActivity != null) {
            try {
                mXmPluginActivity.onConfigurationChanged(newConfig);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mLoadedInfo, e);
                finish();
                return;
            }
        }
    }
}
