package com.wirevpn.rnwireguard;

import android.app.PendingIntent;

public interface WGVpnServiceCallbacks {
    void stop();
    boolean getStatus();
}