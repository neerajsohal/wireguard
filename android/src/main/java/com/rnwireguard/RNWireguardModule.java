package com.wirevpn.rnwireguard;

import android.os.ParcelFileDescriptor;
import android.net.VpnService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.util.Log;
import android.app.Activity;
import android.app.PendingIntent;
import android.os.Build;
import android.os.IBinder;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.wirevpn.rnwireguard.RNWireguardCallbacks;
import com.wirevpn.rnwireguard.WGVpnServiceCallbacks;
import com.wirevpn.rnwireguard.WGVpnService.LocalBinder;
import com.wirevpn.rnwireguard.config.WGPeer;
import com.wirevpn.rnwireguard.wrapper.WGWrapper;
import com.wirevpn.rnwireguard.util.CIDR;
import com.wirevpn.rnwireguard.Constants;
import com.wirevpn.rnwireguard.BaseLifecycleEventListener;

import java.util.HashMap;
import java.util.Map;

public class RNWireguardModule extends ReactContextBaseJavaModule implements RNWireguardCallbacks {
	private final ReactApplicationContext reactContext;
	private final WGWrapper wireguard;
	private String config;
	private Promise connectPromise;
	private String sessionName;
	private int notifIcon;
	private String notifTitle;
	private String notifText;
	private String pkg;
	private WGVpnServiceCallbacks vpnService;
	private ServiceConnection connection;

	// Handler for the onActivityResult
	private final ActivityEventListener vpnActivity = new BaseActivityEventListener() {
		@Override
		public void onActivityResult(
			Activity activity, int requestCode, int resultCode, Intent intent) {
			if (connectPromise != null && requestCode == Constants.VpnStartIntent) {
				if (resultCode == Activity.RESULT_OK) {
					try {
						startVpnService();
					} catch(Exception e) {
						connectPromise.reject(e);
						connectPromise = null;
					}
				} else {
					connectPromise.reject("Authorization not granted");
					connectPromise = null;
				}
			}
		}
	};

	// Handler for life cycle events
	private final LifecycleEventListener lifecycle = new BaseLifecycleEventListener() {
		@Override
		public void onHostDestroy() {
			emit(Constants.EventTypeRegular, Constants.EventDestroyed);
		}
        @Override
        public void onHostResume() {
            emit(Constants.EventTypeRegular, Constants.EventResumed);
        }
        @Override
        public void onHostPause() {
            emit(Constants.EventTypeRegular, Constants.EventPaused);
        }
	};

	// Callback for binder
	public void emit(String type, String event) {
		reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
			.emit(type, event);
	}

	// Callback for binder
	public ReactApplicationContext getContext() {
		return reactContext;
	}

	// Callback for binder
	public Activity getActivity() {
		return getCurrentActivity();
	}

	// Callback for binder
	public Promise getConnectPromise() {
		return connectPromise;
	}

	public RNWireguardModule(final ReactApplicationContext reactContext) {
		super(reactContext);
		this.reactContext = reactContext;
		pkg = reactContext.getPackageName();
		wireguard = new WGWrapper(reactContext);
		connection = new ServiceConnection() {
			@Override
			public void onServiceDisconnected(ComponentName name) {
				Log.i("WG_INFO", "VPN_SERVICE_UNBIND");
			}
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				LocalBinder local = (LocalBinder) service;
				vpnService = local.getCallbacks();
				local.setCallbacks(RNWireguardModule.this);
				Log.i("WG_INFO", "VPN_SERVICE_BIND");
			}
		};
		reactContext.addActivityEventListener(vpnActivity);
		reactContext.addLifecycleEventListener(lifecycle);
		reactContext.bindService(new Intent(reactContext, WGVpnService.class),
			connection, Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT | Context.BIND_IMPORTANT);
	}

	private void startVpnService() throws Exception {
		Intent intent = new Intent(reactContext, WGVpnService.class);
		intent.putExtra(pkg + ".CONFIG", config);
		intent.putExtra(pkg + ".SESSION", sessionName);
		intent.putExtra(pkg + ".TITLE", notifTitle);
		intent.putExtra(pkg + ".ICON", notifIcon);
		intent.putExtra(pkg + ".TEXT", notifText);
		if (Build.VERSION.SDK_INT >= 26) {
			reactContext.startForegroundService(intent);
		} else {
			reactContext.startService(intent);
		}
	}

	@Override
	public String getName() {
		return "RNWireguard";
	}

	@Override
	public Map<String, Object> getConstants() {
		final Map<String, Object> constants = new HashMap<>();
		constants.put(Constants.EventTypeException, Constants.EventTypeException);
		constants.put(Constants.EventTypeRegular, Constants.EventTypeRegular);
		constants.put(Constants.EventRevoked, Constants.EventRevoked);
		constants.put(Constants.EventDestroyed, Constants.EventDestroyed);
        constants.put(Constants.EventResumed, Constants.EventResumed);
		return constants;
	}

	@ReactMethod
	public void _connect(
		String confStr, String session, String icon, String title, String text, Promise promise) {
		try {
			sessionName = session;
			config = confStr;
			notifTitle = title;
			notifText = text;
			connectPromise = promise;
			try {
				notifIcon = reactContext.getResources().getIdentifier(icon, "drawable", pkg);
				if(notifIcon == 0) {
					throw new Exception("");
				}
			} catch(Exception e) {
				// Better exception message
				throw new Exception("Notification icon not found");
			}
			Intent intent = VpnService.prepare(reactContext);
			if (intent != null) {
				// Need authorization from user
				// We can't override onActivityResult directly
				// Listener above will catch this. React way of doing things I guess
				getCurrentActivity().startActivityForResult(intent, Constants.VpnStartIntent);
			} else {
				// Everything is set, we can start the service
				startVpnService();
			}
		} catch(Exception e) {
			connectPromise = null;
			promise.reject(e);
		}
	}

	@ReactMethod
	public void _disconnect(Promise promise) {
		if (vpnService != null) {
			vpnService.stop();
			promise.resolve(true);
		} else {
			promise.resolve(false);
		}
	}

	@ReactMethod
	public void _status(Promise promise) {
		if (vpnService != null) {
			promise.resolve(vpnService.getStatus());
		} else {
			promise.resolve(false);
		}
	}

	@ReactMethod
	public void _version(Promise promise) {
		promise.resolve(wireguard.wgVersion());
	}
}