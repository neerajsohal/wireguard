package com.wirevpn.rnwireguard.config;

import java.util.ArrayList;
import java.util.Locale;

import com.wirevpn.rnwireguard.config.WGInterface;
import com.wirevpn.rnwireguard.config.WGPeer;
import com.wirevpn.rnwireguard.util.Key;
import com.wirevpn.rnwireguard.util.Resolver;

public class WGConfig {
    private WGInterface intrf;
    private ArrayList<WGPeer> peers;
    private WGPeer lastPeer;

    public WGConfig(String conf) throws Exception {
        peers = new ArrayList<WGPeer>();
        String lines[] = conf.split("\\r?\\n");
        for (String l : lines) {
            String tokens[] = l.split("\\=", 2);
            String key = tokens[0].trim().toLowerCase(Locale.ENGLISH);
            String value = tokens.length > 1 ? tokens[1].trim() : "";
            switch(key) {
                case "[interface]":
                    if(intrf != null) { throw new Exception("Config file can't be parsed: Interface already defined"); }
                    intrf = new WGInterface();
                    break;
                case "[peer]":
                    lastPeer = new WGPeer();
                    peers.add(lastPeer);
                    break;
                case "privatekey":
                    if (intrf == null) { throw new Exception("Config file can't be parsed: No interface section"); }
                    intrf.SetPrivateKey(Key.toHex(value));
                    break;
                case "publickey":
                    if (intrf == null) { throw new Exception("Config file can't be parsed: No interface section"); }
                    lastPeer.SetPublicKey(Key.toHex(value));
                    break;
                case "address":
                    if (intrf == null) { throw new Exception("Config file can't be parsed: No interface section"); }
                    intrf.AddAddressArray(value.split("\\s*,\\s*"));
                    break;
                case "dns":
                    if (intrf == null) { throw new Exception("Config file can't be parsed: No interface section"); }
                    intrf.AddDNSArray(value.split("\\s*,\\s*"));
                    break;
                case "listenport":
                    if (intrf == null) { throw new Exception("Config file can't be parsed: No interface section"); }
                    intrf.SetListenPort(value);
                    break;
                case "mtu":
                    if (intrf == null) { throw new Exception("Config file can't be parsed: No interface section"); }
                    intrf.SetMTU(Integer.parseInt(value));
                    break;
                case "allowedips":
                    if (lastPeer == null) { throw new Exception("Config file can't be parsed: No peer section"); }
                    lastPeer.AddAllowedIPArray(value.split("\\s*,\\s*"));
                    break;
                case "persistentkeepalive":
                    if (lastPeer == null) { throw new Exception("Config file can't be parsed: No peer section"); }
                    lastPeer.SetKeepalive(Integer.parseInt(value));
                    break;
                case "endpoint":
                    if (lastPeer == null) { throw new Exception("Config file can't be parsed: No peer section"); }
                    Resolver r = new Resolver();
                    String e = r.execute(value).get();
                    if (e == null) { throw new Exception("Config file can't be parsed: Can't resolve endpoint"); }
                    lastPeer.SetEndpoint(e);
                    break;
            }
        }
    }

    public WGInterface GetInterface() {
        return intrf;
    }

    public ArrayList<WGPeer> GetPeers() {
        return peers;
    }

    // We generate only the relevant part for wg configuration protocol.
    // See https://www.wireguard.com/xplatform/#cross-platform-userspace-implementation
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(intrf.toString());
        for(WGPeer p : peers) {
            s.append(p.toString());
        }
        return s.toString();
    }

    // Full serialization for equality comparison
    public String toFullString() {
        StringBuilder s = new StringBuilder();
        s.append(intrf.toFullString());
        for(WGPeer p : peers) {
            s.append(p.toFullString());
        }
        return s.toString();
    }
}
