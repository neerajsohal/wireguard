package com.wirevpn.rnwireguard;

import android.app.Service;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.os.Build;
import android.os.IBinder;
import android.os.Binder;
import android.support.v4.content.LocalBroadcastManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.support.v4.app.NotificationCompat;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Parcel;

import com.wirevpn.rnwireguard.config.WGConfig;
import com.wirevpn.rnwireguard.config.WGPeer;
import com.wirevpn.rnwireguard.util.CIDR;
import com.wirevpn.rnwireguard.wrapper.WGWrapper;
import com.wirevpn.rnwireguard.Constants;
import com.wirevpn.rnwireguard.WGVpnServiceCallbacks;
import com.wirevpn.rnwireguard.RNWireguardCallbacks;
import com.facebook.react.bridge.Promise;

public class WGVpnService extends VpnService implements WGVpnServiceCallbacks {
    private int tunnelHandle;
    private ParcelFileDescriptor fdInterface;
    private WGConfig config;
    private String session;
    private WGWrapper wireguard;
    private RNWireguardCallbacks rnmodule;
    private boolean connected = false;
    private IBinder binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        // Check Android source code for details (core/java/android/net/VpnService.java)
        // It needs to be there otherwise when VPN connection is closed from Android system menus
        // onRevoke won't be called thus app won't have the knowledge that the tunnel should be closed.
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            if (code == IBinder.LAST_CALL_TRANSACTION) {
                onRevoke();
                return true;
            }
            return false;
        }
        public WGVpnServiceCallbacks getCallbacks() {
            return WGVpnService.this;
        }
        public void setCallbacks(RNWireguardCallbacks cb) {
            rnmodule = cb;
        }
    }

    // Returns true if connection is online. Please keep in mind that
    public boolean getStatus() {
        return tunnelHandle > -1 && fdInterface != null && connected;
    }

    public void stop() {
        wireguard.wgTurnOff(tunnelHandle);
        try {
            fdInterface.close();
        } catch (Exception e) {};
        config = null;
        session = null;
        connected = false;
        stopForegnd();
    }

    @Override
    public void onCreate() {
        tunnelHandle = -1;
        fdInterface = null;
        wireguard = new WGWrapper(this);
        super.onCreate();
        Log.i("WG_INFO", "VPN_SERVICE_ON_CREATE");
    }

    // onDestroy is not guaranteed to be called. System can close everything abruptly
    @Override
    public void onDestroy() {
        stop();
        super.onDestroy();
        Log.i("WG_INFO", "VPN_SERVICE_ON_DESTROY");
    }

    @Override
    public void onRevoke() {
        stop();
        if (rnmodule != null) rnmodule.emit(Constants.EventTypeRegular, Constants.EventRevoked);
        super.onRevoke();
        Log.i("WG_INFO", "VPN_SERVICE_ON_REVOKE");
    }

    @Override
    public int onStartCommand(Intent intent, final int flags, final int startId) {
        // An Android service is created once but onStartCommand is called everytime a new service
        // is tried to be started. So we take appropriate measures here to not mess up things
        try {
            String pkg = getPackageName();
            String _config = intent.getStringExtra(pkg + ".CONFIG");
            String _session = intent.getStringExtra(pkg + ".SESSION");
            String _title = intent.getStringExtra(pkg + ".TITLE");
            String _text = intent.getStringExtra(pkg + ".TEXT");
            int _icon = intent.getIntExtra(pkg + ".ICON", 0);

            // Configs exists only when service is started from within the app
            if(_config != null) {
                if(configure(_config, _session, _title)) {
                    tunnelHandle = wireguard.wgTurnOn(
                        session, fdInterface.detachFd(), config.toString());
                    if(tunnelHandle < 0) {
                        throw new Exception("Couldn't create WireGuard tunnel");
                    }

                    protect(wireguard.wgGetSocketV4(tunnelHandle));
                    protect(wireguard.wgGetSocketV6(tunnelHandle));

                    connected = true;
                    startForegnd(_icon, _title, _text);
                    if (rnmodule != null && rnmodule.getConnectPromise() != null) rnmodule.getConnectPromise().resolve(true);
                    Log.i("WG_INFO", "VPN_SERVICE_ON_START_USER");
                }
            } else {
                // So if Android system tries to get the connection on we stop the service
                // because correct config is not supplied
                if (rnmodule != null && rnmodule.getConnectPromise() != null) rnmodule.getConnectPromise().resolve(false);
                Log.i("WG_INFO", "VPN_SERVICE_ON_START_SYSTEM");
                stopSelf(startId);
            }
        }
        catch (Exception e) {
            // An exception here mostly means a wrong configuration
            // It's easy to fuck up my ultra simple parser
            if (rnmodule != null && rnmodule.getConnectPromise() != null) rnmodule.getConnectPromise().reject(e);
            Log.e("WG_ERR", "VPN_SERVICE_EXCEPTION", e);
            onRevoke();
        }

        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    private boolean configure(String parameters, String sessionName, String title) throws Exception {
        WGConfig newConfig = new WGConfig(parameters);

        // If we have a connection with exact same config don't do anything
        if (config != null && config.toFullString().equals(newConfig.toFullString())
            && tunnelHandle > -1
            && fdInterface != null) {
            return false;
        }

        Builder builder = new Builder();
        builder.setMtu(newConfig.GetInterface().GetMTU()); // Defaults to 1420
        builder.setBlocking(true);

        // I'm ashamed. Couldn't figure out a strange 'timeout' bug after VPN is turned on
        // I swear we will get rid of this once I figure it out
        builder.addDisallowedApplication(getPackageName());

        // Add interface address (local IP we will get once we're in the network)
        for(CIDR addr : newConfig.GetInterface().GetAddresses()) {
            builder.addAddress(addr.GetAddress(), addr.GetMask());
        }

        // Add DNS servers
        for(CIDR addr : newConfig.GetInterface().GetDNSServers()) {
            builder.addDnsServer(addr.GetAddress());
        }

        // Add allowed IPs
        for(WGPeer peer : newConfig.GetPeers()) {
            for (CIDR addr : peer.GetAllowedIPs()) {
                builder.addRoute(addr.GetAddress(), addr.GetMask());
            }
        }

        // Try to close old file descriptor before overwriting it for good
        try {
            fdInterface.close();
            if(tunnelHandle > -1) {
                wireguard.wgTurnOff(tunnelHandle);
            }
        } catch (Exception e) { };

        // Create Android VPN interface
        fdInterface = builder.setSession(title).establish();
        if(fdInterface == null) {
            throw new Exception("Can't create Android VPN interface");
        }

        config = newConfig;
        session = sessionName;

        return true;
    }

    // Removes notification
    private void stopForegnd() {
        // Remove notification
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
    }

    // Creates the clickable notification (needs to be called right after service is started)
    private void startForegnd(int icon, String title, String text) throws Exception {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                Constants.NotifChannelID,
                Constants.NotifChannelName,
                NotificationManager.IMPORTANCE_HIGH);
            chan.enableVibration(true);
            ((NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE))
                .createNotificationChannel(chan);
        }
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(
            this, Constants.NotifChannelID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true);
        if (rnmodule != null) {
            PendingIntent close = PendingIntent.getActivity(rnmodule.getContext(), 0,
			    new Intent(rnmodule.getContext(), rnmodule.getActivity().getClass()),
			    PendingIntent.FLAG_CANCEL_CURRENT);
            nBuilder.setContentIntent(close);
        }
        startForeground(1, nBuilder.build());
    }
}