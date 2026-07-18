package com.thinkdifferent.anywebview;

import static com.thinkdifferent.anywebview.XposedHelpersWraper.findAndHookMethod;
import static com.thinkdifferent.anywebview.XposedHelpersWraper.findClass;
import static com.thinkdifferent.anywebview.XposedHelpersWraper.log;

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
        log(VIA + ": entry point loaded, hooking getWebViewPackages");
        findAndHookMethod(classSystemImpl, "getWebViewPackages", new XC_MethodHook() {
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

    @Override
    public void initZygote(StartupParam startupParam) {
    }
}
