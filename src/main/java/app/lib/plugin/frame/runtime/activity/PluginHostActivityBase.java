
package app.lib.plugin.frame.runtime.activity;

import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

import java.lang.reflect.Constructor;

import app.lib.plugin.frame.PluginRuntimeManager;
import app.lib.plugin.frame.debug.PluginErrorInfoActivity;
import app.lib.plugin.sdk.PluginContext;
import app.lib.plugin.sdk.activity.PluginBaseActivity;

/**
 * Created by chenhao on 16/12/24.
 */

public class PluginHostActivityBase extends FragmentActivity {

    public static final String KEY_PLUGIN_ID = "key_plugin_id";
    public static final String KEY_VERSION_CODE = "key_version_code";
    public static final String KEY_ACTIVITY_CLASS = "key_activity_class";

    static final String FRAGMENTS_TAG = "android:support:fragments";

    Resources.Theme mTheme;
    private PluginContext mPluginContext;
    private String mActivityClass;

    private PluginBaseActivity mPluginActivity;

    @Override
    public Resources getResources() {
        if (mPluginContext == null) {
            return super.getResources();
        } else {
            return mPluginContext.getResources();
        }
    }

    @Override
    public AssetManager getAssets() {
        if (mPluginContext == null) {
            return super.getAssets();
        } else {
            return mPluginContext.getAssetManager();
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
        if (mPluginContext == null) {
            return super.getClassLoader();
        } else {
            return mPluginContext.getClassLoader();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // 防止切到后台被kill掉后，再次启动初始化Fragment crash
        if (savedInstanceState != null) {
            savedInstanceState.remove(FRAGMENTS_TAG);
        }
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Intent intent = getIntent();

        String pluginId = intent.getStringExtra(KEY_PLUGIN_ID);
        int versionCode = intent.getIntExtra(KEY_VERSION_CODE, 0);
        mActivityClass = intent.getStringExtra(KEY_ACTIVITY_CLASS);

        mPluginContext = PluginRuntimeManager.getInstance().getPluginContextRuntime(pluginId,
                versionCode);

        if (mPluginContext == null) {
            finish();
            return;
        }

        // mPluginContext = PluginRuntimeManager.getInstance().getXmPluginPackage(mModel);
        // if (mPluginContext == null) {
        // setResult(RESULT_CANCELED);
        // finish();
        // return;
        // }
        // }
        /// 注意，不要在此之前调用intent.getExtra之类接口
        intent.setExtrasClassLoader(mPluginContext.getClassLoader());
        // mPackageName = intent.getStringExtra(IXmPluginActivity.EXTRA_PACKAGE);
        // mClass = intent.getStringExtra(IXmPluginActivity.EXTRA_CLASS);

        handleActivityInfo();
        launchActivity(intent);

    }

    private void handleActivityInfo() {
        Resources.Theme superTheme = super.getTheme();
        mTheme = mPluginContext.getResources().newTheme();
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
        if (mPluginContext != null) {
            try {
                Class<?> localClass = mPluginContext.getClassLoader().loadClass(mActivityClass);
                Constructor<?> localConstructor = localClass.getConstructor(new Class[] {});
                Object instance = localConstructor.newInstance(new Object[] {});
                mPluginActivity = (PluginBaseActivity) instance;
                mPluginActivity.attach(this, mPluginContext);
                mPluginActivity.setIntent(intent);
                mPluginActivity.onCreate(intent.getExtras());
            } catch (Throwable e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        } else {
            try {
                Class<?> localClass = getClassLoader().loadClass(mActivityClass);
                Constructor<?> localConstructor = localClass
                        .getConstructor(new Class[] {});
                Object instance = localConstructor.newInstance(new Object[] {});
                mPluginActivity = (PluginBaseActivity) instance;
                mPluginActivity.attach(this, mPluginContext);
                mPluginActivity.setIntent(intent);
                mPluginActivity.onCreate(intent.getExtras());
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }

    }

    @Override
    public void recreate() {
        super.recreate();

        if (mPluginActivity != null) {
            try {
                mPluginActivity.recreate();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (mPluginActivity != null) {
            try {
                mPluginActivity.onPostCreate(savedInstanceState);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mPluginActivity != null) {
            try {
                mPluginActivity.onStart();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mPluginActivity != null) {
            try {
                mPluginActivity.onResume();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if (mPluginActivity != null) {
            try {
                mPluginActivity.onPostResume();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mPluginActivity != null) {
            try {
                mPluginActivity.onPause();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mPluginActivity != null) {
            try {
                mPluginActivity.onStop();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mPluginActivity != null) {
            try {
                mPluginActivity.onDestroy();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);

        if (mPluginActivity != null) {
            try {
                mPluginActivity.onSaveInstanceState(outState);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (mPluginActivity != null) {
            try {
                mPluginActivity.onRestoreInstanceState(savedInstanceState);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mPluginActivity != null) {
            try {
                mPluginActivity.onActivityResult(requestCode, resultCode, data);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (mPluginActivity != null) {
            try {
                mPluginActivity.onRequestPermissionsResult(requestCode, permissions,
                        grantResults);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (mPluginActivity != null) {
            try {
                mPluginActivity.onNewIntent(intent);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();

        if (mPluginActivity != null) {
            try {
                mPluginActivity.onBackPressed();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mPluginActivity != null) {
            try {
                if (mPluginActivity.onTouchEvent(event))
                    return true;
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return false;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mPluginActivity != null) {
            try {
                if (mPluginActivity.dispatchTouchEvent(ev))
                    return true;
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return false;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mPluginActivity != null) {
            try {
                if (mPluginActivity.onKeyDown(keyCode, event))
                    return true;
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return false;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mPluginActivity != null) {
            try {
                if (mPluginActivity.onKeyUp(keyCode, event))
                    return true;
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return false;
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (mPluginActivity != null) {
            try {
                if (mPluginActivity.onKeyLongPress(keyCode, event))
                    return true;
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return false;
            }
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        if (mPluginActivity != null) {
            try {
                if (mPluginActivity.onKeyMultiple(keyCode, repeatCount, event))
                    return true;
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return false;
            }
        }
        return super.onKeyMultiple(keyCode, repeatCount, event);
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        if (mPluginActivity != null) {
            try {
                if (mPluginActivity.onKeyShortcut(keyCode, event))
                    return true;
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return false;
            }
        }
        return super.onKeyShortcut(keyCode, event);
    }

    @Override
    public void onWindowAttributesChanged(WindowManager.LayoutParams params) {
        if (mPluginActivity != null)
            try {
                mPluginActivity.onWindowAttributesChanged(params);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        super.onWindowAttributesChanged(params);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (mPluginActivity != null)
            try {
                mPluginActivity.onWindowFocusChanged(hasFocus);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mPluginActivity != null)
            try {
                mPluginActivity.onCreateOptionsMenu(menu);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return false;
            }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mPluginActivity != null)
            try {
                mPluginActivity.onOptionsItemSelected(item);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return false;
            }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onContentChanged() {
        // super.onContentChanged();

        if (mPluginActivity != null) {
            try {
                mPluginActivity.onContentChanged();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mPluginActivity != null) {
            try {
                mPluginActivity.onAttachedToWindow();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mPluginActivity != null) {
            try {
                mPluginActivity.onDetachedFromWindow();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        if (mPluginActivity != null) {
            try {
                mPluginActivity.onLowMemory();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (mPluginActivity != null) {
            try {
                mPluginActivity.onTrimMemory(level);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        if (mPluginActivity != null) {
            try {
                if (mPluginActivity.onTrackballEvent(event))
                    return true;
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return false;
            }
        }

        return super.onTrackballEvent(event);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();

        if (mPluginActivity != null) {
            try {
                mPluginActivity.onUserInteraction();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();

        if (mPluginActivity != null) {
            try {
                mPluginActivity.onUserLeaveHint();
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mPluginActivity != null) {
            try {
                mPluginActivity.onConfigurationChanged(newConfig);
            } catch (Exception e) {
                PluginErrorInfoActivity.showErrorInfo(this, mPluginContext, e);
                finish();
                return;
            }
        }
    }
}
