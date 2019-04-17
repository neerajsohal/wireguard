
# react-native-wireguard

## Getting started

`$ npm install react-native-wireguard --save`

### Mostly automatic installation

`$ react-native link react-native-wireguard`

### Extra steps

#### iOS

Doesn't work on iOS yet. SOON...

#### Android

1. Insert the following service lines to your `AndroidManifest.xml`:
    ```xml
    <application>

        ....

        <service
            android:name="com.wirevpn.rnwireguard.WGVpnService"
            android:permission="android.permission.BIND_VPN_SERVICE">
            <intent-filter>
                <action android:name="android.net.VpnService" />
            </intent-filter>
        </service>

        ....

    </application>
  	```
2. Insert your notification area icon(s) to `res/drawable` or to its multiple hidpi counterparts.
3. Insert your `applicationId` to the top level `build.gradle` to have it accessible.
    ```
    buildscript {

        ....

        ext {
            minSdkVersion = 21
            targetSdkVersion = 26
            compileSdkVersion = 28

            ....

            applicationId = "com.wirevpn.android"
        }

        ....
    }
    ```


## Usage
```javascript
import WireGuard from 'react-native-wireguard';

// Gets the version of the underying wireguard-go
WireGuard.Version().then((v) => this.setState{version: v});

// Config is a list of key-values
// Each key-value is separated by newline
// MTU is optional and defaults to 1420
// If endpoint resolves to multiple IP addresses,
// IPv4 is preferred over IPv6
var config = `
    private_key=AChhQs3VpzOTnlCmKLYir59q1ul7vvfqRySXHT3Ru1E=\n
    address=192.168.0.1/32\n
    address=c0:a8::1/128\n
    dns=192.168.0.0\n
    dns=c0:a8::\n
    mtu=1420\n
    public_key=r1nsEJbtGiuORXk9EjbeVSXcDfdIdC7f47PCWLmrr1o=\n
    allowed_ip=0.0.0.0/0\n
    allowed_ip=::/0\n
    endpoint=someserver.com:51820\n`;

// A name for your session
var session = 'MyVPNSession';

// After a successfull connection, application is brought to
// foreground and needs a notification
var notif = {
    icon: 'ic_notif_icon', // Name of the icon in /res directory
    title: 'My VPN',
    text: 'Connected to ' + country;
}

// Starts the VPN connection
// After successfull connection you will receive an event
WireGuard.Connect(config, session, notif).catch(
    (e) => console.warn(e.message));

// Check if VPN service is online
WireGuard.Status().then((b) => {
    if(b){
        // Update state
    }
});

// Terminates the connection
// After successfull termination of the connection you will
// receive an event
WireGuard.Disconnect()

componentDidMount() {
    DeviceEventEmitter.addListener(WireGuard.EV_TYPE_SYSTEM, () => {
		if(e === WireGuard.EV_STARTED_BY_SYSTEM) {
			// This event is emitted when VPN service is started
            // by the system. For example if a user enables Always-On
            // in settings, system will try to bring the VPN online but
            // since it doesn't have any config it will fail and send
            // this event instead so that you can start it correctly
            // here...
		}
    });

    // If any exceptions occur after calling the Connect()
    // method you can catch them here. e is of type string
    DeviceEventEmitter.addListener(WireGuard.EV_TYPE_EXCEPTION, () => {
		console.log(e);
    });

    DeviceEventEmitter.addListener(WireGuard.EV_TYPE_REGULAR, () => {
        if(e === WireGuard.EV_STOPPED) {
            // Update state
		} else if(e === WireGuard.EV_STARTED) {
			// Update state
		}
    });
}

componentWillUnmount() {
    // You will receive the same event multiple times
    // if this not set
    DeviceEventEmitter.removeAllListeners();
}

```