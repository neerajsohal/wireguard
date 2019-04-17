/*
 * Copyright © 2017-2019 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 * Copied from wireguard-android source code
 */

package com.wirevpn.rnwireguard.wrapper;

import android.content.Context;

import com.wirevpn.rnwireguard.util.SOLoader;

public final class WGWrapper {
    public WGWrapper(Context context) {
        SOLoader.loadSharedLibrary(context, "wg-go");
    }

    // Turn WireGuard on for given tun and config
    // Implementation at src/tools/libwg-go
    public static native int wgTurnOn(String ifName, int tunFd, String settings);

    // Turn WireGuard off
    // Implementation at src/tools/libwg-go
    public static native void wgTurnOff(int handle);

    // Get IPv4 socket
    // Implementation at src/tools/libwg-go
    public static native int wgGetSocketV4(int handle);

    // Get IPv6 socket
    // Implementation at src/tools/libwg-go
    public static native int wgGetSocketV6(int handle);

    // Get wireguard-go version
    // Implementation at src/tools/libwg-go
    public static native String wgVersion();
}