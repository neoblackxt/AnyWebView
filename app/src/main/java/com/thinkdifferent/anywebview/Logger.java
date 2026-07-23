package com.thinkdifferent.anywebview;

/**
 * Log sink supplied by whichever entry point is active.
 *
 * The two frameworks do not share a log channel. Vector surfaces android.util.Log from de.robv
 * modules in logcat, but not from libxposed modules -- those are expected to use
 * XposedInterface.log(), which routes into the framework's own module log. Hardcoding
 * android.util.Log therefore left the module completely silent under libxposed: working, but with
 * no diagnostics and no error reporting.
 *
 * WebViewProviderInjector logs through this instead, so each adapter can route to the channel its
 * framework actually reads.
 */
public interface Logger {

    void d(String message);

    void w(String message);

    void e(String message, Throwable t);
}
