package com.wirevpn.rnwireguard.config;

import java.util.ArrayList;

import com.wirevpn.rnwireguard.util.CIDR;

public class WGPeer {
    private ArrayList<CIDR> allowedIPs;
    private String endpoint;
    private String publicKey;
    private int keepalive;

    public WGPeer() {
        allowedIPs = new ArrayList<CIDR>();
        publicKey = null;
        endpoint = null;
        keepalive = 0;
    }

    public void AddAllowedIP(String ip) throws Exception {
        allowedIPs.add(new CIDR(ip));
    }

    public void AddAllowedIPArray(String[] addrs) throws Exception {
        for (int i = 0; i < addrs.length; i++) {
            AddAllowedIP(addrs[i]);
        }
    }

    public void SetEndpoint(String end) {
        endpoint = end;
    }

    public void SetPublicKey(String pk) {
        publicKey = pk;
    }

    public void SetKeepalive(int interval) {
        keepalive = interval;
    }

    public ArrayList<CIDR> GetAllowedIPs() {
        return allowedIPs;
    }

    public String GetEndpoint() {
        return endpoint;
    }

    public String GetPublicKey() {
        return publicKey;
    }

    public int GetKeepalive() {
        return keepalive;
    }

    // We generate only the relevant part for wg configuration protocol
    // See https://www.wireguard.com/xplatform/#cross-platform-userspace-implementation
    public String toString() {
        StringBuilder s = new StringBuilder();
        if(publicKey != null) {
            s.append("public_key=");
            s.append(publicKey);
            s.append("\n");
        }
        for(CIDR i : allowedIPs) {
            s.append("allowed_ip=");
            s.append(i.toString());
            s.append("\n");
        }
        if(endpoint != null) {
            s.append("endpoint=");
            s.append(endpoint);
            s.append("\n");
        }
        s.append("persistent_keepalive_interval=");
        s.append(Integer.toString(keepalive));
        s.append("\n");
        return s.toString();
    }

    // For full serialization
    public String toFullString() {
        return toString();
    }
}
