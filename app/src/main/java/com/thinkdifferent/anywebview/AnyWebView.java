package com.thinkdifferent.anywebview;

import static com.thinkdifferent.anywebview.XposedHelpersWraper.findAndHookConstructor;
import static com.thinkdifferent.anywebview.XposedHelpersWraper.findAndHookMethod;
import static com.thinkdifferent.anywebview.XposedHelpersWraper.findClass;
import static com.thinkdifferent.anywebview.XposedHelpersWraper.findConstructorBestMatch;
import static com.thinkdifferent.anywebview.XposedHelpersWraper.findField;
import static com.thinkdifferent.anywebview.XposedHelpersWraper.findFirstFieldByExactType;
import static com.thinkdifferent.anywebview.XposedHelpersWraper.findMethodExact;
import static com.thinkdifferent.anywebview.XposedHelpersWraper.log;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
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
            Class<?> classWebViewProviderInfoArray = initialize.getClassWebViewProviderInfoArray();
            Class<?> classSystemImpl = initialize.getClassSystemImpl();
            Class<?> classWebViewUpdater = initialize.getClassWebViewUpdater();
            Constructor<?> constructorWebViewProviderInfo = initialize.getConstructorWebViewProviderInfo();

            Class<?> classSystemServer = findClass("com.android.server.SystemServer", lpparam.classLoader);
            Field mPackageManager = findField(classSystemServer, "mPackageManager");
            mPackageManager.setAccessible(true);
            final PackageManager[] mPm = new PackageManager[1];
            Class<?> classComtextImpl = findClass("android.app.ContextImpl", lpparam.classLoader);
            final boolean[] findFlag = new boolean[]{false};
            findAndHookMethod(classComtextImpl, "getPackageManager", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    StackTraceElement[] stackTrace = new Throwable().getStackTrace();
                    // TODO Need a better filter condition or determination logic to get a PackageManager Object with privilege permission
                    //  instead of (stackTrace.length<=20;i<=5)
                    if (!findFlag[0] && stackTrace.length <= 20) {
                        log("enumerate method");
                        for (int i = 0; i < stackTrace.length && i <= 5; i++) {
                            StackTraceElement ste = stackTrace[i];
                            if (ste.getMethodName().equals("startBootstrapServices")) {
                                log("setting mPm");
                                mPm[0] = (PackageManager) param.getResult();
                                findFlag[0] = true;
                                break;
                            }
                        }
                        log(Arrays.toString(stackTrace));
                    }
                }
            });

            Method getInstalledPackages = findMethodExact(PackageManager.class, "getInstalledPackages", int.class);

            findAndHookConstructor(classSystemImpl, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        log("installedPackageInfoList");
                        List<PackageInfo> installedPackageInfoList = (ArrayList<PackageInfo>) getInstalledPackages.invoke(mPm[0], PackageManager.MATCH_ALL | PackageManager.GET_META_DATA | PackageManager.GET_SIGNATURES);
                        if (installedPackageInfoList == null) {
                            log("installedPackageInfoList is null");
                        } else {
                            log("installedPackageInfoList length:" + installedPackageInfoList.toArray().length);
                        }
                        List<String> webviewPkgNameList = new ArrayList<>();
                        List<String> webviewLabelList = new ArrayList<>();
                        List<String[]> webviewSignaturesList = new ArrayList<>();
                        for (PackageInfo packageInfo : installedPackageInfoList) {
                            Bundle metaData = packageInfo.applicationInfo.metaData;
                            if (metaData == null) {
                                continue;
                            }
                            String awLib = metaData.getString("com.android.webview.WebViewLibrary");
                            if (awLib != null && awLib.length() > 0) {
                                webviewPkgNameList.add(packageInfo.packageName);
                                webviewLabelList.add(mPm[0].getApplicationLabel(packageInfo.applicationInfo).toString());
                                int signsLen = packageInfo.signatures.length;
                                log("signsLen:" + signsLen);
                                String[] signatures = new String[signsLen];
                                for (int i = 0; i < signsLen; i++) {
                                    byte[] rawCert = packageInfo.signatures[i].toByteArray();
                                    InputStream certStream = new ByteArrayInputStream(rawCert);
                                    CertificateFactory certFactory = CertificateFactory.getInstance("X509");
                                    X509Certificate x509Cert = (X509Certificate) certFactory.generateCertificate(certStream);
                                    signatures[i] = Base64.encodeToString(x509Cert.getEncoded(), Base64.DEFAULT);

                                }
                                webviewSignaturesList.add(signatures);
                            }
                        }
                        log("webviewPkgNameList:" + webviewPkgNameList.toString());
                        log("webviewLabelList:" + webviewLabelList.toString());
                        log("webviewSignaturesList:");
                        for (String[] sarr : webviewSignaturesList) {
                            log("sarr len:" + sarr.length);
                            for (String s : sarr) {
                                log("sign:" + s);
                            }
                        }

                        int size = webviewPkgNameList.size();
                        Object webViewProviders = Array.newInstance(classWebViewProviderInfo, size);
                        for (int i = 0; i < size; i++) {
                            Array.set(webViewProviders, i, constructorWebViewProviderInfo.newInstance(webviewPkgNameList.get(i), webviewLabelList.get(i), true, false, webviewSignaturesList.get(i)));
                        }

                        Field mWebViewProviderPackages = findFirstFieldByExactType(classSystemImpl, classWebViewProviderInfoArray);
                        mWebViewProviderPackages.setAccessible(true);
                        mWebViewProviderPackages.set(param.thisObject, webViewProviders);
                        param.setResult(param.thisObject);
                    } catch (Throwable t) {
                        log(t);
                    }
                }
            });
            // Disable signature validity check
            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                findAndHookMethod(classWebViewUpdater, "validityResult", classWebViewProviderInfo, PackageInfo.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(*//*VALIDITY_OK*//*0);
                    }
                });
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                findAndHookMethod(classWebViewUpdater, "isValidProvider", classWebViewProviderInfo, PackageInfo.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(true);
                    }
                });
            }*/
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) {
    }

    private static class Initialize {
        private XC_LoadPackage.LoadPackageParam lpparam;
        private Class<?> classWebViewProviderInfo;
        private Class<?> classWebViewProviderInfoArray;
        private Class<?> classSystemImpl;
        private Class<?> classWebViewUpdater;
        private Constructor<?> constructorWebViewProviderInfo;

        public Initialize(XC_LoadPackage.LoadPackageParam lpparam) {
            this.lpparam = lpparam;
        }

        public Class<?> getClassWebViewProviderInfo() {
            return classWebViewProviderInfo;
        }

        public Class<?> getClassWebViewProviderInfoArray() {
            return classWebViewProviderInfoArray;
        }

        public Class<?> getClassSystemImpl() {
            return classSystemImpl;
        }

        public Class<?> getClassWebViewUpdater() {
            return classWebViewUpdater;
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

            classWebViewProviderInfoArray = findClass(
                    "[Landroid.webkit.WebViewProviderInfo;", lpparam.classLoader);
            classSystemImpl = findClass("com.android.server.webkit.SystemImpl", lpparam.classLoader);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                classWebViewUpdater = findClass("com.android.server.webkit.WebViewUpdateServiceImpl", lpparam.classLoader);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                classWebViewUpdater = findClass("com.android.server.webkit.WebViewUpdater", lpparam.classLoader);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                classWebViewUpdater = findClass("com.android.server.webkit.WebViewUpdateServiceImpl$WebViewUpdater", lpparam.classLoader);
            }

            return this;
        }
    }
}
