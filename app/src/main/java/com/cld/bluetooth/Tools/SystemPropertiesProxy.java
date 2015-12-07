package com.cld.bluetooth.tools;

/**
 * Created by taojian on 2015/12/7.
 */
import android.content.Context;
import java.lang.reflect.Method;

public class SystemPropertiesProxy {
    public static final String MTK_PLATFORM_KEY = "ro.mediatek.platform";
    public static final String MTK_VERSION_BRANCH_KEY = "ro.mediatek.version.branch";
    public static final String MTK_VERSION_RELEASE_KEY = "ro.mediatek.version.release";

    private SystemPropertiesProxy() {
    }

    public static String get(String key) throws IllegalArgumentException {
        String ret = "";

        try {
            Class e = Class.forName("android.os.SystemProperties");
            Class[] paramTypes = new Class[]{String.class};
            Method get = e.getMethod("get", paramTypes);
            Object[] params = new Object[]{new String(key)};
            ret = (String)get.invoke(e, params);
        } catch (IllegalArgumentException var6) {
            throw var6;
        } catch (Exception var7) {
            ret = "";
        }

        return ret;
    }

    public static String get(String key, String def) throws IllegalArgumentException {
        String ret;
        try {
            Class e = Class.forName("android.os.SystemProperties");
            Class[] paramTypes = new Class[]{String.class, String.class};
            Method get = e.getMethod("get", paramTypes);
            Object[] params = new Object[]{new String(key), new String(def)};
            ret = (String)get.invoke(e, params);
        } catch (IllegalArgumentException var7) {
            throw var7;
        } catch (Exception var8) {
            ret = def;
        }

        return ret;
    }

    public static Integer getInt(String key, int def) throws IllegalArgumentException {
        Integer ret = Integer.valueOf(def);

        try {
            Class e = Class.forName("android.os.SystemProperties");
            Class[] paramTypes = new Class[]{String.class, Integer.TYPE};
            Method getInt = e.getMethod("getInt", paramTypes);
            Object[] params = new Object[]{new String(key), new Integer(def)};
            ret = (Integer)getInt.invoke(e, params);
        } catch (IllegalArgumentException var7) {
            throw var7;
        } catch (Exception var8) {
            ret = Integer.valueOf(def);
        }

        return ret;
    }

    public static Long getLong(String key, long def) throws IllegalArgumentException {
        Long ret = Long.valueOf(def);

        try {
            Class e = Class.forName("android.os.SystemProperties");
            Class[] paramTypes = new Class[]{String.class, Long.TYPE};
            Method getLong = e.getMethod("getLong", paramTypes);
            Object[] params = new Object[]{new String(key), new Long(def)};
            ret = (Long)getLong.invoke(e, params);
        } catch (IllegalArgumentException var8) {
            throw var8;
        } catch (Exception var9) {
            ret = Long.valueOf(def);
        }

        return ret;
    }

    public static Boolean getBoolean(String key, boolean def) throws IllegalArgumentException {
        Boolean ret = Boolean.valueOf(def);

        try {
            Class e = Class.forName("android.os.SystemProperties");
            Class[] paramTypes = new Class[]{String.class, Boolean.TYPE};
            Method getBoolean = e.getMethod("getBoolean", paramTypes);
            Object[] params = new Object[]{new String(key), new Boolean(def)};
            ret = (Boolean)getBoolean.invoke(e, params);
        } catch (IllegalArgumentException var7) {
            throw var7;
        } catch (Exception var8) {
            ret = Boolean.valueOf(def);
        }

        return ret;
    }

    public static void set(Context context, String key, String val) throws IllegalArgumentException {
        try {
            Class e = Class.forName("android.os.SystemProperties");
            Class[] paramTypes = new Class[]{String.class, String.class};
            Method set = e.getMethod("set", paramTypes);
            Object[] params = new Object[]{new String(key), new String(val)};
            set.invoke(e, params);
        } catch (IllegalArgumentException var7) {
            throw var7;
        } catch (Exception var8) {
        }

    }

    public static boolean isMediatekPlatform() {
        String platform = get("ro.mediatek.platform");
        return platform != null && (platform.startsWith("MT") || platform.startsWith("mt"));
    }
}
