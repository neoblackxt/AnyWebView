package com.thinkdifferent.anywebview;

import android.annotation.SuppressLint;
import android.os.SystemClock;
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
            sink("D " + message);
        }

        @Override
        public void w(String message) {
            log(Log.WARN, TAG, message);
            sink("W " + message);
        }

        @Override
        public void e(String message, Throwable t) {
            log(Log.ERROR, TAG, message, t);
            sink("E " + message + " " + t);
        }
    };

    /**
     * Diagnostic build only (see the [diag] lines below): appends every module log line to a file
     * that survives logcat rotation at boot and does not depend on how the framework routes logs.
     */
    private static void sink(String line) {
        try (java.io.FileWriter writer = new java.io.FileWriter(
                "/data/system/anywebview-diag.log", true)) {
            writer.write(SystemClock.uptimeMillis() + " " + line + "\n");
        } catch (Throwable ignored) {
            // diagnostic sink only
        }
    }

    private final WebViewProviderInjector injector = new WebViewProviderInjector(VIA, logger);
    private boolean hooksInstalled;

    // Diagnostic build only: records the lifecycle order explicitly, so that a boot log states
    // which path installed the hooks and whether onSystemServerStarting arrived, instead of
    // leaving either to be inferred. Hook behaviour is unchanged.
    private boolean systemServerStartingDelivered;
    private boolean getterHookReported;
    private boolean constructorHookReported;
    private boolean onStartHookReported;

    private void diag(String message) {
        String line = "[diag] uptime=" + SystemClock.uptimeMillis()
                + " thread=" + Thread.currentThread().getName() + " " + message;
        Log.i(TAG, line);
        logger.d(line);
    }

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        if (param.isSystemServer()) {
            ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
            diag("onModuleLoaded process=" + param.getProcessName() + " framework="
                    + getFrameworkName() + " " + getFrameworkVersion() + " api=" + getApiVersion()
                    + " threadClassLoader=" + threadClassLoader);
            installHooks(threadClassLoader);
        }
    }

    // hook() takes java.lang.reflect.Executable (API 26+), but this suppression is safe:
    // libxposed frameworks only exist on API 26+ (the api AAR declares minSdkVersion 26),
    // so this class is never loaded on API 24-25, where the de.robv entry point applies.
    @SuppressLint("NewApi")
    @Override
    public void onSystemServerStarting(XposedModuleInterface.SystemServerStartingParam param) {
        systemServerStartingDelivered = true;
        ClassLoader callbackClassLoader = param.getClassLoader();
        diag("onSystemServerStarting delivered, hooksInstalled=" + hooksInstalled
                + " callbackClassLoader=" + callbackClassLoader + " sameAsThreadClassLoader="
                + (callbackClassLoader == Thread.currentThread().getContextClassLoader()));
        installHooks(callbackClassLoader);
    }

    @SuppressLint("NewApi")
    private synchronized void installHooks(final ClassLoader classLoader) {
        if (hooksInstalled || classLoader == null) {
            return;
        }
        try {
            logger.d(VIA + ": entry point loaded (framework=" + getFrameworkName()
                    + " " + getFrameworkVersion() + ", api=" + getApiVersion() + ")");
            Class<?> classSystemImpl = classLoader.loadClass(
                    "com.android.server.webkit.SystemImpl");
            Method getWebViewPackages;
            try {
                getWebViewPackages = classSystemImpl.getDeclaredMethod("getWebViewPackages");
            } catch (NoSuchMethodException missingGetter) {
                diag("getWebViewPackages is absent, taking the constructor fallback");
                hookProviderFieldAfterConstruction(classSystemImpl, classLoader);
                hooksInstalled = true;
                diag("hooks installed via constructor fallback");
                return;
            }
            logger.d(VIA + ": hooking getWebViewPackages");
            hook(getWebViewPackages).intercept(new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Object stockProviders = chain.proceed();
                    if (!getterHookReported) {
                        getterHookReported = true;
                        // Diagnostic code must never throw into system_server.
                        try {
                            diag("getWebViewPackages first call, onSystemServerStartingDelivered="
                                    + systemServerStartingDelivered + " stockLength="
                                    + (stockProviders == null ? -1
                                            : java.lang.reflect.Array.getLength(stockProviders)));
                        } catch (Throwable t) {
                            sink("E getter diagnostic failed: " + t);
                        }
                    }
                    Object merged = injector.appendTo(
                            chain.getThisObject(), stockProviders, classLoader);
                    return merged != null ? merged : stockProviders;
                }
            });
            hooksInstalled = true;
            diag("hooks installed via getWebViewPackages hook");
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
            Class<?> classSystemImpl, ClassLoader classLoader) throws ReflectiveOperationException {
        final Field providerField = injector.findProviderPackagesField(
                classSystemImpl, classLoader);
        if (providerField == null) {
            logger.w(VIA + ": getWebViewPackages missing and no unambiguous provider field"
                    + " to rewrite, nothing hooked");
            return;
        }
        logger.d(VIA + ": getWebViewPackages missing, hooking SystemImpl constructors,"
                + " rewriting " + providerField.getName() + " after each returns");
        Constructor<?>[] constructors = classSystemImpl.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            hook(constructor).intercept(new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Object result = chain.proceed();
                    if (!constructorHookReported) {
                        constructorHookReported = true;
                        // Diagnostic code must never throw into system_server.
                        try {
                            Object current = providerField.get(chain.getThisObject());
                            diag("SystemImpl constructor returned, field=" + providerField.getName()
                                    + " fieldValue=" + (current == null ? "null"
                                            : "length "
                                                    + java.lang.reflect.Array.getLength(current))
                                    + " onSystemServerStartingDelivered="
                                    + systemServerStartingDelivered);
                        } catch (Throwable t) {
                            sink("E constructor diagnostic failed: " + t);
                        }
                    }
                    injector.appendToField(chain.getThisObject(), providerField, classLoader);
                    return result;
                }
            });
        }
        diag("hooked " + constructors.length + " SystemImpl constructor(s)");
        // Samsung Android 17 also inlines SystemImpl's initialization into the service
        // constructor. Its empty constructor returns before the provider field is assigned.
        Method onStart = classLoader.loadClass("com.android.server.webkit.WebViewUpdateService")
                .getDeclaredMethod("onStart");
        hook(onStart).intercept(new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                if (!onStartHookReported) {
                    onStartHookReported = true;
                    // Diagnostic code must never throw into system_server.
                    try {
                        diag("WebViewUpdateService.onStart entered, onSystemServerStartingDelivered="
                                + systemServerStartingDelivered);
                    } catch (Throwable t) {
                        sink("E onStart diagnostic failed: " + t);
                    }
                }
                injector.appendToService(chain.getThisObject(), classLoader);
                return chain.proceed();
            }
        });
        logger.d(VIA + ": hooked WebViewUpdateService.onStart before provider registration");
    }
}
