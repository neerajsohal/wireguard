package com.wirevpn.rnwireguard.util;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;

public class CIDR {
    private int mask;
    private InetAddress address;

    public CIDR(String ipAddr) throws Exception {
        if (ipAddr.contains("/")) {
            String tokens[] = ipAddr.split("\\/");
            String addressPart = tokens[0].trim();
            String networkPart = tokens[1].trim();
            address = InetAddress.getByName(addressPart);
            mask = Integer.parseInt(networkPart);
        } else {
            address = InetAddress.getByName(ipAddr);
            if (address instanceof Inet6Address) {
                mask = 128;
            } else {
                mask = 32;
            }
        }
    }

    public int GetMask() {
        return mask;
    }

    public InetAddress GetAddress() {
        return address;
    }

    public String toString() {
        return address.getHostAddress() + "/" + Integer.toString(mask);
    }

    public String toShortString() {
        return address.getHostAddress();
    }
}
