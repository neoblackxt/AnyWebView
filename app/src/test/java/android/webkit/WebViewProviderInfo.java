package android.webkit;

/** JVM test double for the hidden framework class used reflectively by the injector. */
public final class WebViewProviderInfo {
    public final String packageName;
    public final String description;
    public final boolean availableByDefault;
    public final boolean isFallback;
    public final String[] signatures;

    public WebViewProviderInfo(String packageName, String description,
            boolean availableByDefault, boolean isFallback, String[] signatures) {
        this.packageName = packageName;
        this.description = description;
        this.availableByDefault = availableByDefault;
        this.isFallback = isFallback;
        this.signatures = signatures;
    }
}
