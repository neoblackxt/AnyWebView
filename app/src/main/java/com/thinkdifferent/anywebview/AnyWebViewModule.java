package com.thinkdifferent.anywebview;

import android.annotation.SuppressLint;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * Entry point for the libxposed API, declared in META-INF/xposed/java_init.list.
 *
 * The framework instantiates this through the no-arg constructor and then calls
 * attachFramework(), so hook() is available from onSystemServerStarting() onwards.
 *
 * Compared with the de.robv adapter in AnyWebView this needs no package-name filtering:
 * onSystemServerStarting() fires only for system_server, which is the sole process this module
 * cares about. On the usual build the interceptor model expresses the append directly --
 * proceed(), then return a longer array -- but when the getter is absent (see
 * hookProviderFieldAfterConstruction) the backing field is rewritten after construction.
 */
public class AnyWebViewModule extends XposedModule {

    private static final String TAG = "anywebview";
    private static final String VIA = "libxposed";

    /**
     * Routes through XposedInterface.log() rather than android.util.Log: Vector does not surface
     * android.util.Log from libxposed modules, so the latter is silently dropped.
     */
    private final Logger logger = new Logger() {
        @Override
        public void d(String message) {
            log(Log.DEBUG, TAG, message);
        }

        @Override
        public void w(String message) {
            log(Log.WARN, TAG, message);
        }

        @Override
        public void e(String message, Throwable t) {
            log(Log.ERROR, TAG, message, t);
        }
    };

    private final WebViewProviderInjector injector = new WebViewProviderInjector(VIA, logger);

    // hook() takes java.lang.reflect.Executable (API 26+), but this suppression is safe:
    // libxposed frameworks only exist on API 26+ (the api AAR declares minSdkVersion 26),
    // so this class is never loaded on API 24-25, where the de.robv entry point applies.
    @SuppressLint("NewApi")
    @Override
    public void onSystemServerStarting(XposedModuleInterface.SystemServerStartingParam param) {
        final ClassLoader classLoader = param.getClassLoader();
        try {
            logger.d(VIA + ": entry point loaded (framework=" + getFrameworkName()
                    + " " + getFrameworkVersion() + ", api=" + getApiVersion() + ")");
            Class<?> classSystemImpl = classLoader.loadClass(
                    "com.android.server.webkit.SystemImpl");
            Method getWebViewPackages;
            try {
                getWebViewPackages = classSystemImpl.getDeclaredMethod("getWebViewPackages");
            } catch (NoSuchMethodException missingGetter) {
                hookProviderFieldAfterConstruction(classSystemImpl, classLoader);
                return;
            }
            logger.d(VIA + ": hooking getWebViewPackages");
            hook(getWebViewPackages).intercept(new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Object stockProviders = chain.proceed();
                    Object merged = injector.appendTo(
                            chain.getThisObject(), stockProviders, classLoader);
                    return merged != null ? merged : stockProviders;
                }
            });
        } catch (Throwable t) {
            logger.e(VIA + ": failed to hook SystemImpl", t);
        }
    }

    /**
     * Fallback for builds without getWebViewPackages(): rewriting the provider field after
     * each SystemImpl constructor returns makes the merged array visible to subsequent readers.
     * All constructors are hooked rather than assuming the AOSP signature.
     */
    // Same API-26-only reasoning as onSystemServerStarting: this class is never loaded on
    // API 24-25, where the de.robv entry point applies.
    @SuppressLint("NewApi")
    private void hookProviderFieldAfterConstruction(
            Class<?> classSystemImpl, ClassLoader classLoader) {
        final Field providerField = injector.findProviderPackagesField(
                classSystemImpl, classLoader);
        if (providerField == null) {
            logger.w(VIA + ": getWebViewPackages missing and no unambiguous provider field"
                    + " to rewrite, nothing hooked");
            return;
        }
        logger.d(VIA + ": getWebViewPackages missing, hooking SystemImpl constructors,"
                + " rewriting " + providerField.getName() + " after each returns");
        for (Constructor<?> constructor : classSystemImpl.getDeclaredConstructors()) {
            hook(constructor).intercept(new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Object result = chain.proceed();
                    injector.appendToField(chain.getThisObject(), providerField, classLoader);
                    return result;
                }
            });
        }
    }
}
