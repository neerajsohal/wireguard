package com.wirevpn.rnwireguard.util;

import android.os.AsyncTask;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.URI;

public class Resolver extends AsyncTask<String, Void, String> {
    protected String doInBackground(String... params) {
        try{
            return resolveDomain(params[0]);
        } catch(Exception e) {
            return null;
        }
    }

    // Resolve domain to IP address prefering IPv4 over IPv6
    private static String resolveDomain(String endpoint) throws Exception {
        URI uri = new URI("wg://" + endpoint);
        if(uri.getPort() < 0 || uri.getPort() > 65535) {
            throw new Exception("Config file can't be parsed");
        }
        InetAddress addrs[] = InetAddress.getAllByName(uri.getHost());
        switch(addrs.length) {
            case 0:
                throw new Exception("Can't resolve domain name");
            case 1:
                return addrs[0].getHostAddress() + ":" + uri.getPort();
            default:
                if(addrs[0] instanceof Inet4Address) {
                    return addrs[0].getHostAddress() + ":" + uri.getPort();
                }
                return addrs[1].getHostAddress() + ":" + uri.getPort();
        }
    }
}