
package app.lib.plugin.frame.debug;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import app.lib.plugin.frame.PluginSetting;
import app.lib.plugin.frame.R;
import app.lib.plugin.sdk.PluginContext;

/**
 * Created by chenhao on 16/12/24.
 */

public class PluginErrorInfoActivity extends FragmentActivity {
    public static void showErrorInfo(Context context, PluginContext pluginContext,
            Throwable throwable) {
        if (PluginSetting.IS_DEBUG) {
            Intent intent = new Intent(context, PluginErrorInfoActivity.class);
            Writer writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);
            throwable.printStackTrace(pw);
            pw.close();
            String error = writer.toString();
            intent.putExtra("info", error);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            // if (PluginSettings.sExceptionHandler != null) {
            // PluginSettings.sExceptionHandler.handleException(loadedInfo, throwable);
            // }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plugin_error_info_activity);
        String info = getIntent().getStringExtra("info");
        TextView textView = (TextView) findViewById(R.id.info);
        textView.setText(info);
    }
}
