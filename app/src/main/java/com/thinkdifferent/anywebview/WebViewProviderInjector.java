package com.thinkdifferent.anywebview;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Framework-agnostic core of the module: given the WebViewProviderInfo[] that
 * SystemImpl.getWebViewPackages() returned, produce a longer array with every installed WebView
 * provider appended.
 *
 * This class deliberately has no Xposed dependency of any kind, so the same logic backs both the
 * de.robv (assets/xposed_init) and libxposed (META-INF/xposed/java_init.list) entry points. Each
 * entry point owns exactly one instance, and therefore one cache.
 */
public final class WebViewProviderInjector {

    private static final String WEBVIEW_LIBRARY_META_DATA = "com.android.webview.WebViewLibrary";

    /** Which entry point owns this instance, for log attribution. */
    private final String via;
    /** Framework-appropriate log sink, supplied by the entry point. */
    private final Logger logger;

    private Class<?> classWebViewProviderInfo;
    private Object extraProviders;

    public WebViewProviderInjector(String via, Logger logger) {
        this.via = via;
        this.logger = logger;
    }

    /**
     * Returns a WebViewProviderInfo[] consisting of stockProviders plus every installed provider
     * not already present, or null when there is nothing to add or anything went wrong. Callers
     * substitute the returned array for the hooked method's result; returning null means "leave
     * the original result alone".
     *
     * @param systemImpl  the SystemImpl instance the hooked call was made on
     * @param classLoader the system_server class loader
     */
    public Object appendTo(Object systemImpl, Object stockProviders, ClassLoader classLoader) {
        try {
            if (stockProviders == null) {
                return null;
            }
            int stockLen = Array.getLength(stockProviders);

            // getWebViewPackages() is called repeatedly (provider selection, package-change
            // handling, shell commands). Enumerating every installed package on each call would
            // run a full PackageManager sweep in system_server, so this is computed once.
            if (extraProviders == null) {
                Context context = findSystemContext(systemImpl, classLoader);
                if (context == null) {
                    logger.w(via + ": no system Context, leaving provider list untouched");
                    return null;
                }
                PackageManager pm = context.getPackageManager();
                if (pm == null) {
                    logger.w(via + ": no PackageManager, leaving provider list untouched");
                    return null;
                }
                classWebViewProviderInfo = classLoader.loadClass("android.webkit.WebViewProviderInfo");
                Constructor<?> constructor = classWebViewProviderInfo.getConstructor(
                        /*packageName*/ String.class, /*description*/ String.class,
                        /*availableByDefault*/ boolean.class, /*isFallback*/ boolean.class,
                        /*signatures*/ String[].class);
                extraProviders = buildExtraProviders(stockProviders, stockLen, pm, constructor);
            }

            int extraLen = Array.getLength(extraProviders);
            if (extraLen == 0) {
                return null;
            }

            Object merged = Array.newInstance(classWebViewProviderInfo, stockLen + extraLen);
            System.arraycopy(stockProviders, 0, merged, 0, stockLen);
            System.arraycopy(extraProviders, 0, merged, stockLen, extraLen);
            logger.d(via + ": appended " + extraLen + " provider(s) to " + stockLen + " stock entries");
            return merged;
        } catch (Throwable t) {
            logger.e(via + ": failed to append WebView providers", t);
            return null;
        }
    }

    /**
     * Resolves a system_server Context, which yields a PackageManager privileged enough to
     * enumerate every installed package with signatures.
     *
     * On Android 15+ WebViewUpdateService constructs SystemImpl per service and it holds the
     * Context in mContext, so the receiver is the most direct source and is exactly the
     * PackageManager SystemImpl itself uses in getPackageInfoForProvider(). On API 24-34
     * SystemImpl is a LazyHolder singleton with a no-arg constructor and no Context field at all
     * (its SystemInterface methods take a Context parameter instead), so fall back to
     * system_server's own ActivityThread, which is present on every release.
     */
    private Context findSystemContext(Object systemImpl, ClassLoader classLoader) {
        if (systemImpl != null) {
            try {
                Field field = systemImpl.getClass().getDeclaredField("mContext");
                field.setAccessible(true);
                Context context = (Context) field.get(systemImpl);
                if (context != null) {
                    return context;
                }
            } catch (NoSuchFieldException expectedOnOlderReleases) {
                // Pre-Android-15 SystemImpl has no Context field; handled by the fallback below.
            } catch (Throwable t) {
                logger.e(via + ": could not read SystemImpl.mContext", t);
            }
        }
        try {
            Class<?> classActivityThread = Class.forName(
                    "android.app.ActivityThread", false, classLoader);
            Object activityThread = classActivityThread.getMethod("currentActivityThread")
                    .invoke(null);
            if (activityThread != null) {
                return (Context) classActivityThread.getMethod("getSystemContext")
                        .invoke(activityThread);
            }
        } catch (Throwable t) {
            logger.e(via + ": could not reach ActivityThread system context", t);
        }
        return null;
    }

    /**
     * Builds the WebViewProviderInfo[] to append: every installed package declaring
     * com.android.webview.WebViewLibrary meta-data that is not already in the stock list.
     *
     * Packages without that meta-data are skipped because validityResult() rejects them with
     * VALIDITY_NO_LIBRARY_FLAG regardless, so including them would only cost a PackageManager
     * lookup each. Entries are marked availableByDefault so provider selection considers them,
     * and carry their own signature so providerHasValidSignature() passes without relying on a
     * debuggable build. Actual suitability is still decided downstream by validityResult().
     */
    private Object buildExtraProviders(Object stockProviders, int stockLen, PackageManager pm,
                                       Constructor<?> constructorWebViewProviderInfo)
            throws Exception {
        List<String> stockPkgNames = new ArrayList<>();
        Field packageNameField = classWebViewProviderInfo.getField("packageName");
        for (int i = 0; i < stockLen; i++) {
            stockPkgNames.add((String) packageNameField.get(Array.get(stockProviders, i)));
        }
        logger.d(via + ": stock providers:" + stockPkgNames);

        List<PackageInfo> installedPackageInfoList = pm.getInstalledPackages(
                PackageManager.MATCH_ALL | PackageManager.GET_META_DATA
                        | PackageManager.GET_SIGNATURES);
        if (installedPackageInfoList == null) {
            logger.w(via + ": installedPackageInfoList is null");
            return Array.newInstance(classWebViewProviderInfo, 0);
        }
        logger.d(via + ": installedPackageInfoList length:" + installedPackageInfoList.size());

        List<Object> extras = new ArrayList<>();
        for (PackageInfo packageInfo : installedPackageInfoList) {
            if (packageInfo.applicationInfo == null) {
                continue;
            }
            Bundle metaData = packageInfo.applicationInfo.metaData;
            if (metaData == null) {
                continue;
            }
            String awLib = metaData.getString(WEBVIEW_LIBRARY_META_DATA);
            if (awLib == null || awLib.isEmpty()) {
                continue;
            }
            if (stockPkgNames.contains(packageInfo.packageName)) {
                logger.d(via + ": skipping already-declared provider:" + packageInfo.packageName);
                continue;
            }
            if (packageInfo.signatures == null || packageInfo.signatures.length == 0) {
                logger.d(via + ": skipping unsigned provider:" + packageInfo.packageName);
                continue;
            }

            String label = pm.getApplicationLabel(packageInfo.applicationInfo).toString();
            String[] signatures = encodeSignatures(packageInfo);
            logger.d(via + ": adding provider:" + packageInfo.packageName + " label:" + label
                    + " signatures:" + signatures.length);
            extras.add(constructorWebViewProviderInfo.newInstance(
                    packageInfo.packageName, label,
                    /*availableByDefault*/ true, /*isFallback*/ false, signatures));
        }

        Object result = Array.newInstance(classWebViewProviderInfo, extras.size());
        for (int i = 0; i < extras.size(); i++) {
            Array.set(result, i, extras.get(i));
        }
        return result;
    }

    /**
     * WebViewProviderInfo takes base64-encoded DER certificates, matching the format the
     * framework parses out of config_webview_packages.xml.
     */
    private static String[] encodeSignatures(PackageInfo packageInfo) throws Exception {
        int signsLen = packageInfo.signatures.length;
        String[] signatures = new String[signsLen];
        CertificateFactory certFactory = CertificateFactory.getInstance("X509");
        for (int i = 0; i < signsLen; i++) {
            InputStream certStream = new ByteArrayInputStream(packageInfo.signatures[i].toByteArray());
            X509Certificate x509Cert = (X509Certificate) certFactory.generateCertificate(certStream);
            signatures[i] = Base64.encodeToString(x509Cert.getEncoded(), Base64.NO_WRAP);
        }
        return signatures;
    }
}
