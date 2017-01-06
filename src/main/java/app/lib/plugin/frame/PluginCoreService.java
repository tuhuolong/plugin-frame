
package app.lib.plugin.frame;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import app.lib.plugin.frame.entity.PluginInfo;

/**
 * Created by chenhao on 17/1/6.
 */

public class PluginCoreService extends Service {

    ICoreServiceApi.Stub mStub = new ICoreServiceApi.Stub() {
        @Override
        public PluginInfo getPluginInfo(String pluginId) throws RemoteException {
            return PluginManager.getInstance().getPluginInfo(pluginId);
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mStub;
    }
}
