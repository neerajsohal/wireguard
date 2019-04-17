package com.wirevpn.rnwireguard.config;

import java.util.ArrayList;

import com.wirevpn.rnwireguard.util.CIDR;

public class WGInterface {
    private ArrayList<CIDR> addresses;
    private ArrayList<CIDR> dnsServers;
    private String privateKey;
    private String listenPort;
    private int mtu;
    private boolean replacePeers;

    public WGInterface() {
        addresses = new ArrayList<CIDR>();
        dnsServers = new ArrayList<CIDR>();
        privateKey = null;
        listenPort = null;
        mtu = 1420;
        replacePeers = true;
    }

    public void AddAddress(String addr) throws Exception {
        addresses.add(new CIDR(addr));
    }

    public void AddAddressArray(String[] addrs) throws Exception {
        for (int i = 0; i < addrs.length; i++) {
            AddAddress(addrs[i]);
        }
    }

    public void AddDNS(String addr) throws Exception {
        dnsServers.add(new CIDR(addr));
    }

    public void AddDNSArray(String[] addrs) throws Exception {
        for (int i = 0; i < addrs.length; i++) {
            AddDNS(addrs[i]);
        }
    }

    public void SetPrivateKey(String pk) {
        privateKey = pk;
    }

    public void SetListenPort(String lp) {
        listenPort = lp;
    }

    public void SetMTU(int m) {
        // Android Bug #70916
        if(m < 1280) {
            mtu = 1280;
        } else {
            mtu = m;
        }
    }

    public ArrayList<CIDR> GetAddresses() {
        return addresses;
    }

    public ArrayList<CIDR> GetDNSServers() {
        return dnsServers;
    }

    public String GetPrivateKey() {
        return privateKey;
    }

    public String GetListenPort() {
        return listenPort;
    }

    public int GetMTU() {
        return mtu;
    }

    // We generate only the relevant part for wg configuration protocol
    // See https://www.wireguard.com/xplatform/#cross-platform-userspace-implementation
    public String toString() {
        StringBuilder s = new StringBuilder();
        if(privateKey != null) {
            s.append("private_key=");
            s.append(privateKey);
            s.append("\n");
        }
        if(listenPort != null) {
            s.append("listen_port=");
            s.append(listenPort);
            s.append("\n");
        }
        if(replacePeers) {
            s.append("replace_peers=true\n");
        }
        return s.toString();
    }

    // For full serialization
    public String toFullString() {
        StringBuilder s = new StringBuilder();
        if(privateKey != null) {
            s.append("private_key=");
            s.append(privateKey);
            s.append("\n");
        }
        if(listenPort != null) {
            s.append("listen_port=");
            s.append(listenPort);
            s.append("\n");
        }
        for(CIDR addr : addresses) {
            s.append("address=");
            s.append(addr.toString());
            s.append("\n");
        }
        for(CIDR addr : dnsServers) {
            s.append("dns=");
            s.append(addr.toShortString());
            s.append("\n");
        }
        s.append("mtu=");
        s.append(Integer.toString(mtu));
        s.append("\n");
        return s.toString();
    }
}
