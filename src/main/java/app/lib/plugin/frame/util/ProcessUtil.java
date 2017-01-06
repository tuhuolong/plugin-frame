
package app.lib.plugin.frame.util;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

/**
 * Created by chenhao on 17/1/6.
 */

public class ProcessUtil {
    public static String sProcessName = null;

    public static void initProcessName(Context context) {
        if (sProcessName == null) {
            ActivityManager am = ((ActivityManager) context
                    .getSystemService(Context.ACTIVITY_SERVICE));
            List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();
            int myPid = android.os.Process.myPid();
            for (ActivityManager.RunningAppProcessInfo info : processInfos) {
                if (info.pid == myPid) {
                    sProcessName = info.processName;
                }
            }
        }
    }

    public static String getCurrentProcessName(Context context) {
        initProcessName(context);

        return sProcessName;
    }

    public static boolean isMainProcess(Context context) {
        initProcessName(context);

        String packageName = context.getPackageName();

        return sProcessName.equalsIgnoreCase(packageName);
    }

    public static int getProcessIdByName(Context context, String processName) {
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);

        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager
                .getRunningAppProcesses();
        if (appProcesses == null)
            return 0;

        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equalsIgnoreCase(processName)) {
                return appProcess.pid;
            }
        }

        return 0;
    }
}
