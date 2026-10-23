package com.thinkdifferent.anywebview;

import static com.thinkdifferent.anywebview.XposedHelpersWraper.findClass;
import static com.thinkdifferent.anywebview.XposedHelpersWraper.findMethodExact;
import static com.thinkdifferent.anywebview.XposedHelpersWraper.hookAllConstructors;
import static com.thinkdifferent.anywebview.XposedHelpersWraper.hookMethod;
import static com.thinkdifferent.anywebview.XposedHelpersWraper.log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Entry point for the original de.robv Xposed API, declared in assets/xposed_init.
 *
 * Kept alongside the libxposed entry point in AnyWebViewModule so the module keeps working on
 * frameworks that do not implement libxposed, and on API 24-25, which libxposed does not support
 * (its AAR declares minSdkVersion 26). Both adapters delegate to WebViewProviderInjector, which
 * holds all of the actual logic.
 */
public class AnyWebView extends BaseXposedHookLoadPackage {

    private static final String VIA = "de.robv";

    /** Vector surfaces android.util.Log from de.robv modules in logcat. */
    private static final Logger LOGGER = new Logger() {
        @Override
        public void d(String message) {
            log(message);
        }

        @Override
        public void w(String message) {
            log(message);
        }

        @Override
        public void e(String message, Throwable t) {
            log(message);
            log(t);
        }
    };

    private final WebViewProviderInjector injector = new WebViewProviderInjector(VIA, LOGGER);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        //services.jar
        if (!lpparam.packageName.equals("android")) {
            return;
        }
        final ClassLoader classLoader = lpparam.classLoader;
        Class<?> classSystemImpl = findClass(
                "com.android.server.webkit.SystemImpl", classLoader);
        if (classSystemImpl == null) {
            log(VIA + ": SystemImpl not found, nothing hooked");
            return;
        }
        log(VIA + ": entry point loaded");
        Method getWebViewPackages = findMethodExact(classSystemImpl, "getWebViewPackages");
        if (getWebViewPackages == null) {
            hookProviderFieldWrites(classSystemImpl, classLoader);
            return;
        }
        log(VIA + ": hooking getWebViewPackages");
        hookMethod(getWebViewPackages, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Object merged = injector.appendTo(
                        param.thisObject, param.getResult(), classLoader);
                if (merged != null) {
                    param.setResult(merged);
                }
            }
        });
    }

    /**
     * Fallback for builds (seen on Samsung One UI) where R8 inlined getWebViewPackages()
     * away and every caller reads the provider field directly: rewriting the field right
     * after each SystemImpl constructor returns covers every reader.
     */
    private void hookProviderFieldWrites(Class<?> classSystemImpl, final ClassLoader classLoader) {
        final Field providerField = injector.findProviderPackagesField(
                classSystemImpl, classLoader);
        if (providerField == null) {
            log(VIA + ": getWebViewPackages missing and no provider field to rewrite,"
                    + " nothing hooked");
            return;
        }
        log(VIA + ": getWebViewPackages inlined away, hooking SystemImpl constructors,"
                + " rewriting " + providerField.getName() + " after each returns");
        hookAllConstructors(classSystemImpl, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                injector.appendToField(param.thisObject, providerField, classLoader);
            }
        });
    }

    @Override
    public void initZygote(StartupParam startupParam) {
    }
}
