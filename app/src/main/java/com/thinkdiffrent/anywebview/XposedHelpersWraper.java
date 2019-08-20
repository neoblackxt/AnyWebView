package com.thinkdiffrent.anywebview;

import android.content.res.Resources;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class XposedHelpersWraper {
    //TODO global prevent force close

    private static String TAG = "Xposed";

    public static String getTAG() {
        return TAG;
    }

    public static void setTAG(String TAG) {
        XposedHelpersWraper.TAG = TAG;
    }

    public static Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> hookClass, XC_MethodHook callback) {
        try {
            return XposedBridge.hookAllConstructors(hookClass, callback);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        try {
            return XposedBridge.hookAllMethods(hookClass, methodName, callback);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static XC_MethodHook.Unhook hookMethod(Member hookMethod, XC_MethodHook callback) {
        try {
            return XposedBridge.hookMethod(hookMethod, callback);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Object invokeOriginalMethod(Member method, Object thisObject, Object[] args) {
        try {
            return XposedBridge.invokeOriginalMethod(method, thisObject, args);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public synchronized static void log(String text) {
        Log.d(TAG, text);
    }

    public synchronized static void log(String TAG, String text) {
        Log.d(TAG, text);
    }

    public synchronized static void log(Throwable t) {
        Log.e(TAG, Log.getStackTraceString(t));
    }

    public synchronized static void log(String TAG, Throwable t) {
        Log.e(TAG, Log.getStackTraceString(t));
    }

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        try {
            return XposedHelpers.findClass(className, classLoader);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Field findField(Class<?> clazz, String fieldName) {
        try {
            return XposedHelpers.findField(clazz, fieldName);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Field findFirstFieldByExactType(Class<?> clazz, Class<?> type) {
        try {
            return XposedHelpers.findFirstFieldByExactType(clazz, type);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static XC_MethodHook.Unhook findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        try {
            return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static XC_MethodHook.Unhook findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        try {
            return XposedHelpers.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Method findMethodExact(Class<?> clazz, String methodName, Object... parameterTypes) {
        try {
            return XposedHelpers.findMethodExact(clazz, methodName, parameterTypes);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Method findMethodExact(String className, ClassLoader classLoader, String methodName, Object... parameterTypes) {
        try {
            return XposedHelpers.findMethodExact(className, classLoader, methodName, parameterTypes);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Method findMethodExact(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            return XposedHelpers.findMethodExact(clazz, methodName, parameterTypes);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Method[] findMethodsByExactParameters(Class<?> clazz, Class<?> returnType, Class<?>... parameterTypes) {
        try {
            return XposedHelpers.findMethodsByExactParameters(clazz, returnType, parameterTypes);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            return XposedHelpers.findMethodBestMatch(clazz, methodName, parameterTypes);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Object... args) {
        try {
            return XposedHelpers.findMethodBestMatch(clazz, methodName, args);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object[] args) {
        try {
            return XposedHelpers.findMethodBestMatch(clazz, methodName, parameterTypes, args);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Class<?>[] getParameterTypes(Object... args) {
        try {
            return XposedHelpers.getParameterTypes(args);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Class<?>[] getClassesAsArray(Class<?>... clazzes) {
        try {
            return XposedHelpers.getClassesAsArray(clazzes);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Constructor<?> findConstructorExact(Class<?> clazz, Object... parameterTypes) {
        try {
            return XposedHelpers.findConstructorExact(clazz, parameterTypes);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Constructor<?> findConstructorExact(String className, ClassLoader classLoader, Object... parameterTypes) {
        try {
            return XposedHelpers.findConstructorExact(className, classLoader, parameterTypes);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Constructor<?> findConstructorExact(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            return XposedHelpers.findConstructorExact(clazz, parameterTypes);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static XC_MethodHook.Unhook findAndHookConstructor(Class<?> clazz, Object... parameterTypesAndCallback) {
        try {
            return XposedHelpers.findAndHookConstructor(clazz, parameterTypesAndCallback);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static XC_MethodHook.Unhook findAndHookConstructor(String className, ClassLoader classLoader, Object... parameterTypesAndCallback) {
        try {
            return XposedHelpers.findAndHookConstructor(className, classLoader, parameterTypesAndCallback);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            return XposedHelpers.findConstructorBestMatch(clazz, parameterTypes);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Object... args) {
        try {
            return XposedHelpers.findConstructorBestMatch(clazz, args);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Class<?>[] parameterTypes, Object[] args) {
        try {
            return XposedHelpers.findConstructorBestMatch(clazz, parameterTypes, args);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static void setObjectField(Object obj, String fieldName, Object value) {
        try {
            XposedHelpers.setObjectField(obj, fieldName, value);
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void setBooleanField(Object obj, String fieldName, boolean value) {
        try {
            XposedHelpers.setBooleanField(obj, fieldName, value);
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void setByteField(Object obj, String fieldName, byte value) {
        try {
            XposedHelpers.setByteField(obj, fieldName, value);
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void setCharField(Object obj, String fieldName, char value) {
        try {
            XposedHelpers.setCharField(obj, fieldName, value);
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void setDoubleField(Object obj, String fieldName, double value) {
        try {
            XposedHelpers.setDoubleField(obj, fieldName, value);
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void setFloatField(Object obj, String fieldName, float value) {
        try {
            XposedHelpers.setFloatField(obj, fieldName, value);
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void setIntField(Object obj, String fieldName, int value) {
        try {
            XposedHelpers.setIntField(obj, fieldName, value);
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void setLongField(Object obj, String fieldName, long value) {
        try {
            XposedHelpers.setLongField(obj, fieldName, value);
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void setShortField(Object obj, String fieldName, short value) {
        try {
            XposedHelpers.setShortField(obj, fieldName, value);
        } catch (Throwable t) {
            log(t);
        }
    }

    public static Object getObjectField(Object obj, String fieldName) {
        try {
            return XposedHelpers.getObjectField(obj, fieldName);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Object getSurroundingThis(Object obj) {
        try {
            return XposedHelpers.getSurroundingThis(obj);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static boolean getBooleanField(Object obj, String fieldName) {
        try {
            return XposedHelpers.getBooleanField(obj, fieldName);
        } catch (Throwable t) {
            log(t);
        }
        return false;
    }

    public static byte getByteField(Object obj, String fieldName) {
        try {
            return XposedHelpers.getByteField(obj, fieldName);
        } catch (Throwable t) {
            log(t);
        }
        return 0;
    }

    public static char getCharField(Object obj, String fieldName) {
        try {
            return XposedHelpers.getCharField(obj, fieldName);
        } catch (Throwable t) {
            log(t);
        }
        return 0;
    }

    public static double getDoubleField(Object obj, String fieldName) {
        try {
            return XposedHelpers.getDoubleField(obj, fieldName);
        } catch (Throwable t) {
            log(t);
        }
        return 0;
    }

    public static float getFloatField(Object obj, String fieldName) {
        try {
            return XposedHelpers.getFloatField(obj, fieldName);
        } catch (Throwable t) {
            log(t);
        }
        return 0;
    }

    public static int getIntField(Object obj, String fieldName) {
        try {
            return XposedHelpers.getIntField(obj, fieldName);
        } catch (Throwable t) {
            log(t);
        }
        return 0;
    }

    public static long getLongField(Object obj, String fieldName) {
        try {
            return XposedHelpers.getLongField(obj, fieldName);
        } catch (Throwable t) {
            log(t);
        }
        return 0;
    }

    public static short getShortField(Object obj, String fieldName) {
        try {
            return XposedHelpers.getShortField(obj, fieldName);
        } catch (Throwable t) {
            log(t);
        }
        return 0;
    }

    public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) {
        try {
            XposedHelpers.setStaticObjectField(clazz, fieldName, value);
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void setStaticBooleanField(Class<?> clazz, String fieldName, boolean value) {
        try {
            XposedHelpers.setStaticBooleanField(clazz, fieldName, value);
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void setStaticByteField(Class<?> clazz, String fieldName, byte value) {
        try {
            XposedHelpers.setStaticByteField(clazz, fieldName, value);
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void setStaticCharField(Class<?> clazz, String fieldName, char value) {
        try {
            XposedHelpers.setStaticCharField(clazz, fieldName, value);
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void setStaticDoubleField(Class<?> clazz, String fieldName, double value) {
        try {
            XposedHelpers.setStaticDoubleField(clazz, fieldName, value);
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void setStaticFloatField(Class<?> clazz, String fieldName, float value) {
        try {
            XposedHelpers.setStaticFloatField(clazz, fieldName, value);
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void setStaticIntField(Class<?> clazz, String fieldName, int value) {
        try {
            XposedHelpers.setStaticIntField(clazz, fieldName, value);
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void setStaticLongField(Class<?> clazz, String fieldName, long value) {
        try {
            XposedHelpers.setStaticLongField(clazz, fieldName, value);
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void setStaticShortField(Class<?> clazz, String fieldName, short value) {
        try {
            XposedHelpers.setStaticShortField(clazz, fieldName, value);
        } catch (Throwable t) {
            log(t);
        }
    }

    public static Object getStaticObjectField(Class<?> clazz, String fieldName) {
        try {
            return XposedHelpers.getStaticObjectField(clazz, fieldName);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static boolean getStaticBooleanField(Class<?> clazz, String fieldName) {
        try {
            return XposedHelpers.getStaticBooleanField(clazz, fieldName);
        } catch (Throwable t) {
            log(t);
        }
        return false;
    }

    public static byte getStaticByteField(Class<?> clazz, String fieldName) {
        try {
            return XposedHelpers.getStaticByteField(clazz, fieldName);
        } catch (Throwable t) {
            log(t);
        }
        return 0;
    }

    public static char getStaticCharField(Class<?> clazz, String fieldName) {
        try {
            return XposedHelpers.getStaticCharField(clazz, fieldName);
        } catch (Throwable t) {
            log(t);
        }
        return 0;
    }

    public static double getStaticDoubleField(Class<?> clazz, String fieldName) {
        try {
            return XposedHelpers.getStaticDoubleField(clazz, fieldName);
        } catch (Throwable t) {
            log(t);
        }
        return 0;
    }

    public static float getStaticFloatField(Class<?> clazz, String fieldName) {
        try {
            return XposedHelpers.getStaticFloatField(clazz, fieldName);
        } catch (Throwable t) {
            log(t);
        }
        return 0;
    }

    public static int getStaticIntField(Class<?> clazz, String fieldName) {
        try {
            return XposedHelpers.getStaticIntField(clazz, fieldName);
        } catch (Throwable t) {
            log(t);
        }
        return 0;
    }

    public static long getStaticLongField(Class<?> clazz, String fieldName) {
        try {
            return XposedHelpers.getStaticLongField(clazz, fieldName);
        } catch (Throwable t) {
            log(t);
        }
        return 0;
    }

    public static short getStaticShortField(Class<?> clazz, String fieldName) {
        try {
            return XposedHelpers.getStaticShortField(clazz, fieldName);
        } catch (Throwable t) {
            log(t);
        }
        return 0;
    }

    public static Object callMethod(Object obj, String methodName, Object... args) {
        try {
            return XposedHelpers.callMethod(obj, methodName, args);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Object callMethod(Object obj, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            return XposedHelpers.callMethod(obj, methodName, parameterTypes, args);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        try {
            return XposedHelpers.callMethod(clazz, methodName, args);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            return XposedHelpers.callStaticMethod(clazz, methodName, parameterTypes, args);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Object newInstance(Class<?> clazz, Object... args) {
        try {
            return XposedHelpers.newInstance(clazz, args);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Object newInstance(Class<?> clazz, Class<?>[] parameterTypes, Object... args) {
        try {
            return XposedHelpers.newInstance(clazz, parameterTypes, args);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Object setAdditionalInstanceField(Object obj, String key, Object value) {
        try {
            return XposedHelpers.setAdditionalInstanceField(obj, key, value);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Object getAdditionalInstanceField(Object obj, String key) {
        try {
            return XposedHelpers.getAdditionalInstanceField(obj, key);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Object removeAdditionalInstanceField(Object obj, String key) {
        try {
            return XposedHelpers.removeAdditionalInstanceField(obj, key);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Object setAdditionalStaticField(Object obj, String key, Object value) {
        try {
            return XposedHelpers.setAdditionalStaticField(obj, key, value);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Object getAdditionalStaticField(Object obj, String key) {
        try {
            return XposedHelpers.getAdditionalStaticField(obj, key);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Object removeAdditionalStaticField(Object obj, String key) {
        try {
            return XposedHelpers.removeAdditionalStaticField(obj, key);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Object setAdditionalStaticField(Class<?> clazz, String key, Object value) {
        try {
            return XposedHelpers.setAdditionalStaticField(clazz, key, value);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Object getAdditionalStaticField(Class<?> clazz, String key) {
        try {
            return XposedHelpers.getAdditionalStaticField(clazz, key);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static Object removeAdditionalStaticField(Class<?> clazz, String key) {
        try {
            return XposedHelpers.removeAdditionalStaticField(clazz, key);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static byte[] assetAsByteArray(Resources res, String path) {
        try {
            return XposedHelpers.assetAsByteArray(res, path);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

    public static String getMD5Sum(String file) {
        try {
            return XposedHelpers.getMD5Sum(file);
        } catch (Throwable t) {
            log(t);
        }
        return null;
    }

}
