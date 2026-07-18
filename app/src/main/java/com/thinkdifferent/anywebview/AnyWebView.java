package com.thinkdifferent.anywebview;

import static com.thinkdifferent.anywebview.XposedHelpersWraper.findAndHookMethod;
import static com.thinkdifferent.anywebview.XposedHelpersWraper.findClass;
import static com.thinkdifferent.anywebview.XposedHelpersWraper.findConstructorBestMatch;
import static com.thinkdifferent.anywebview.XposedHelpersWraper.getObjectField;
import static com.thinkdifferent.anywebview.XposedHelpersWraper.log;

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

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class AnyWebView extends BaseXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        //services.jar
        if (lpparam.packageName.equals("android")) {
            Initialize initialize = new Initialize(lpparam).invoke();
            Class<?> classWebViewProviderInfo = initialize.getClassWebViewProviderInfo();
            Class<?> classSystemImpl = initialize.getClassSystemImpl();
            Constructor<?> constructorWebViewProviderInfo = initialize.getConstructorWebViewProviderInfo();

            // getWebViewPackages() is called repeatedly (provider selection, package-change
            // handling, shell commands). Enumerating every installed package on each call would
            // run a full PackageManager sweep in system_server, so the appended entries are
            // computed once and reused.
            final Object[] extraProvidersCache = new Object[1];

            findAndHookMethod(classSystemImpl, "getWebViewPackages", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Object stockProviders = param.getResult();
                        if (stockProviders == null) {
                            return;
                        }
                        int stockLen = Array.getLength(stockProviders);
                        if (extraProvidersCache[0] == null) {
                            Context context = findSystemContext(param.thisObject, lpparam.classLoader);
                            if (context == null) {
                                log("no system Context, leaving provider list untouched");
                                return;
                            }
                            PackageManager pm = context.getPackageManager();
                            if (pm == null) {
                                log("no PackageManager, leaving provider list untouched");
                                return;
                            }
                            extraProvidersCache[0] = buildExtraProviders(
                                    stockProviders, stockLen, pm,
                                    classWebViewProviderInfo, constructorWebViewProviderInfo);
                        }
                        Object extraProviders = extraProvidersCache[0];
                        int extraLen = Array.getLength(extraProviders);
                        if (extraLen == 0) {
                            return;
                        }

                        Object merged = Array.newInstance(classWebViewProviderInfo, stockLen + extraLen);
                        System.arraycopy(stockProviders, 0, merged, 0, stockLen);
                        System.arraycopy(extraProviders, 0, merged, stockLen, extraLen);
                        log("appended " + extraLen + " provider(s) to " + stockLen + " stock entries");
                        param.setResult(merged);
                    } catch (Throwable t) {
                        log(t);
                    }
                }
            });
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
    private static Context findSystemContext(Object systemImpl, ClassLoader classLoader) {
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
            log(t);
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
            log(t);
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
    private static Object buildExtraProviders(Object stockProviders, int stockLen,
                                              PackageManager pm,
                                              Class<?> classWebViewProviderInfo,
                                              Constructor<?> constructorWebViewProviderInfo) throws Exception {
        List<String> stockPkgNames = new ArrayList<>();
        for (int i = 0; i < stockLen; i++) {
            Object provider = Array.get(stockProviders, i);
            stockPkgNames.add((String) getObjectField(provider, "packageName"));
        }
        log("stock providers:" + stockPkgNames);

        List<PackageInfo> installedPackageInfoList = pm.getInstalledPackages(
                PackageManager.MATCH_ALL | PackageManager.GET_META_DATA | PackageManager.GET_SIGNATURES);
        if (installedPackageInfoList == null) {
            log("installedPackageInfoList is null");
            return Array.newInstance(classWebViewProviderInfo, 0);
        }
        log("installedPackageInfoList length:" + installedPackageInfoList.size());

        List<Object> extras = new ArrayList<>();
        for (PackageInfo packageInfo : installedPackageInfoList) {
            if (packageInfo.applicationInfo == null) {
                continue;
            }
            Bundle metaData = packageInfo.applicationInfo.metaData;
            if (metaData == null) {
                continue;
            }
            String awLib = metaData.getString("com.android.webview.WebViewLibrary");
            if (awLib == null || awLib.isEmpty()) {
                continue;
            }
            if (stockPkgNames.contains(packageInfo.packageName)) {
                log("skipping already-declared provider:" + packageInfo.packageName);
                continue;
            }
            if (packageInfo.signatures == null || packageInfo.signatures.length == 0) {
                log("skipping unsigned provider:" + packageInfo.packageName);
                continue;
            }

            String label = pm.getApplicationLabel(packageInfo.applicationInfo).toString();
            String[] signatures = encodeSignatures(packageInfo);
            log("adding provider:" + packageInfo.packageName + " label:" + label
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


    @Override
    public void initZygote(StartupParam startupParam) {
    }

    private static class Initialize {
        private XC_LoadPackage.LoadPackageParam lpparam;
        private Class<?> classWebViewProviderInfo;
        private Class<?> classSystemImpl;
        private Constructor<?> constructorWebViewProviderInfo;

        public Initialize(XC_LoadPackage.LoadPackageParam lpparam) {
            this.lpparam = lpparam;
        }

        public Class<?> getClassWebViewProviderInfo() {
            return classWebViewProviderInfo;
        }

        public Class<?> getClassSystemImpl() {
            return classSystemImpl;
        }

        public Constructor<?> getConstructorWebViewProviderInfo() {
            return constructorWebViewProviderInfo;
        }


        public Initialize invoke() {
            log("invoke init");
            classWebViewProviderInfo = findClass(
                    "android.webkit.WebViewProviderInfo", lpparam.classLoader);
            constructorWebViewProviderInfo = findConstructorBestMatch(
                    classWebViewProviderInfo,/*packageName*/String.class,
                    /*description*/String.class,/*TAG_AVAILABILITY*/boolean.class,
                    /*TAG_FALLBACK*/boolean.class,/*readSignatures*/String[].class);

            classSystemImpl = findClass("com.android.server.webkit.SystemImpl", lpparam.classLoader);

            return this;
        }
    }
}
