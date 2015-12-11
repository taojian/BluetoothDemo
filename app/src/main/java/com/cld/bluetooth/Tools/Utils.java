package com.cld.bluetooth.tools;

/**
 * Created by taojian on 2015/12/7.
 */
public class Utils {
    public Utils() {
    }

    public static boolean int2ByteArray(int intValue, byte[] b, int pos, int length) {
        boolean result = false;
        if((double)intValue < Math.pow(2.0D, (double)(length * 8))) {
            for(int i = pos; i < length; ++i) {
                b[i] = (byte)(intValue >> 8 * (length - 1 - i) & 255);
            }
            result = true;
        }

        return result;
    }

    public static int byteArray2Int(byte[] b, int pos, int length) {
        int intValue = 0;

        for(int i = pos; i < length; ++i) {
            intValue += (b[i] & 255) << 8 * i;
        }

        return intValue;
    }
}
