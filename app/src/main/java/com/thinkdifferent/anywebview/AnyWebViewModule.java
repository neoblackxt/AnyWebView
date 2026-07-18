package com.thinkdifferent.anywebview;

import android.util.Log;

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
 * cares about. The interceptor model also expresses the append directly -- proceed(), then return
 * a longer array -- rather than mutating a result out of band.
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

    @Override
    public void onSystemServerStarting(XposedModuleInterface.SystemServerStartingParam param) {
        final ClassLoader classLoader = param.getClassLoader();
        try {
            logger.d(VIA + ": entry point loaded (framework=" + getFrameworkName()
                    + " " + getFrameworkVersion() + ", api=" + getApiVersion()
                    + "), hooking getWebViewPackages");
            Class<?> classSystemImpl = classLoader.loadClass(
                    "com.android.server.webkit.SystemImpl");
            Method getWebViewPackages = classSystemImpl.getDeclaredMethod("getWebViewPackages");
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
            logger.e(VIA + ": failed to hook SystemImpl.getWebViewPackages", t);
        }
    }
}
