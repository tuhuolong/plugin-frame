// IBridgeCallback.aidl
package com.chenhao.lib.plugin.frame.runtime.bridge;

import com.chenhao.lib.plugin.frame.runtime.bridge.BridgeError;

interface IBridgeCallback {
    void onSendSuccess(in Bundle result);

    void onHandle(boolean handled);

    void onMessageSuccess(in Bundle result);

    void onMessageFailure(in BridgeError error);

    void onFailure(in BridgeError error);
}
