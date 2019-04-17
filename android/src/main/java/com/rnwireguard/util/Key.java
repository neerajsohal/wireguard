/*
 * Copyright © 2017-2019 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 * Copied from wireguard-android source code
 */

package com.wirevpn.rnwireguard.util;

import java.util.Arrays;

public class Key {
    public static String toHex(final String strBase64) throws Exception {
        final char[] input = strBase64.toCharArray();
        if(input.length != Format.BASE64.length || input[Format.BASE64.length - 1] != '=') {
            throw new Exception("Not a BASE64 encoded key");
        }

        final byte[] _key = new byte[Format.BINARY.length];
        int ret = 0;
        int i;
        for(i = 0; i < _key.length / 3; ++i) {
            final int val = decodeBase64(input, i * 4);
            ret |= val >>> 31;
            _key[i * 3] = (byte) ((val >>> 16) & 0xff);
            _key[i * 3 + 1] = (byte) ((val >>> 8) & 0xff);
            _key[i * 3 + 2] = (byte) (val & 0xff);
        }

        final char[] endSegment = {
                input[i * 4],
                input[i * 4 + 1],
                input[i * 4 + 2],
                'A',
        };

        final int val = decodeBase64(endSegment, 0);
        ret |= (val >>> 31) | (val & 0xff);
        _key[i * 3] = (byte) ((val >>> 16) & 0xff);
        _key[i * 3 + 1] = (byte) ((val >>> 8) & 0xff);

        if(ret != 0) {
            throw new Exception("Not a BASE64 encoded key");
        }

        final char[] output = new char[Format.HEX.length];
        for (i = 0; i < _key.length; ++i) {
            output[i * 2] = (char) (87 + (_key[i] >> 4 & 0xf)
                    + ((((_key[i] >> 4 & 0xf) - 10) >> 8) & ~38));
            output[i * 2 + 1] = (char) (87 + (_key[i] & 0xf)
                    + ((((_key[i] & 0xf) - 10) >> 8) & ~38));
        }
        return new String(output);
    }

    private static int decodeBase64(final char[] src, final int srcOffset) {
        int val = 0;
        for(int i = 0; i < 4; ++i) {
            final char c = src[i + srcOffset];
            val |= (-1
                    + ((((('A' - 1) - c) & (c - ('Z' + 1))) >>> 8) & (c - 64))
                    + ((((('a' - 1) - c) & (c - ('z' + 1))) >>> 8) & (c - 70))
                    + ((((('0' - 1) - c) & (c - ('9' + 1))) >>> 8) & (c + 5))
                    + ((((('+' - 1) - c) & (c - ('+' + 1))) >>> 8) & 63)
                    + ((((('/' - 1) - c) & (c - ('/' + 1))) >>> 8) & 64)
            ) << (18 - 6 * i);
        }
        return val;
    }

    private enum Format {
        BASE64(44),
        BINARY(32),
        HEX(64);

        private final int length;

        Format(final int length) {
            this.length = length;
        }

        public int getLength() {
            return length;
        }
    }
}