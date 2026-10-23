package com.thinkdifferent.anywebview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.webkit.WebViewProviderInfo;

import org.junit.Test;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebViewProviderInjectorTest {
    private static final ClassLoader CLASS_LOADER = WebViewProviderInfo.class.getClassLoader();

    private final List<String> warnings = new ArrayList<>();
    private final WebViewProviderInjector injector = new WebViewProviderInjector("test",
            new Logger() {
                @Override
                public void d(String message) {
                }

                @Override
                public void w(String message) {
                    warnings.add(message);
                }

                @Override
                public void e(String message, Throwable t) {
                    throw new AssertionError(message, t);
                }
            });

    @Test
    public void findProviderPackagesFieldPrefersAospName() throws Exception {
        WebViewProviderInfo[] providers = {provider("stock")};
        NamedSystemImpl systemImpl = new NamedSystemImpl(providers);

        Field field = injector.findProviderPackagesField(NamedSystemImpl.class, CLASS_LOADER);

        assertEquals("mWebViewProviderPackages", field.getName());
        assertSame(providers, field.get(systemImpl));
    }

    @Test
    public void findProviderPackagesFieldFindsUniqueRenamedField() throws Exception {
        WebViewProviderInfo[] providers = {provider("stock")};
        RenamedSystemImpl systemImpl = new RenamedSystemImpl(providers);

        Field field = injector.findProviderPackagesField(RenamedSystemImpl.class, CLASS_LOADER);

        assertEquals("renamedProviders", field.getName());
        assertSame(providers, field.get(systemImpl));
    }

    @Test
    public void findProviderPackagesFieldRejectsAmbiguousTypeLookup() {
        Field field = injector.findProviderPackagesField(AmbiguousSystemImpl.class, CLASS_LOADER);

        assertNull(field);
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("multiple WebViewProviderInfo[] fields"));
    }

    @Test
    public void appendToFieldDoesNotDuplicateProvidersWhenCalledAgain() throws Exception {
        WebViewProviderInfo stock = provider("stock");
        WebViewProviderInfo extra = provider("extra");
        seedCache(new WebViewProviderInfo[]{extra}, Collections.emptyMap());
        NamedSystemImpl systemImpl = new NamedSystemImpl(new WebViewProviderInfo[]{stock});
        Field field = injector.findProviderPackagesField(NamedSystemImpl.class, CLASS_LOADER);

        injector.appendToField(systemImpl, field, CLASS_LOADER);
        Object firstMerged = field.get(systemImpl);
        assertEquals(2, Array.getLength(firstMerged));
        assertSame(stock, Array.get(firstMerged, 0));
        assertSame(extra, Array.get(firstMerged, 1));

        injector.appendToField(systemImpl, field, CLASS_LOADER);
        assertSame(firstMerged, field.get(systemImpl));
    }

    @Test
    public void appendToFieldDoesNotRepeatSignatureOverrideWhenCalledAgain() throws Exception {
        WebViewProviderInfo stock = provider("stock");
        WebViewProviderInfo override = provider("stock");
        seedCache(new WebViewProviderInfo[0], Collections.singletonMap("stock", override));
        NamedSystemImpl systemImpl = new NamedSystemImpl(new WebViewProviderInfo[]{stock});
        Field field = injector.findProviderPackagesField(NamedSystemImpl.class, CLASS_LOADER);

        injector.appendToField(systemImpl, field, CLASS_LOADER);
        Object firstMerged = field.get(systemImpl);
        assertEquals(1, Array.getLength(firstMerged));
        assertSame(override, Array.get(firstMerged, 0));

        injector.appendToField(systemImpl, field, CLASS_LOADER);
        assertSame(firstMerged, field.get(systemImpl));
    }

    private void seedCache(WebViewProviderInfo[] extras, Map<String, Object> overrides)
            throws Exception {
        setInjectorField("classWebViewProviderInfo", WebViewProviderInfo.class);
        setInjectorField("providerPackageNameField",
                WebViewProviderInfo.class.getField("packageName"));
        setInjectorField("stockSignatureOverrides", new HashMap<>(overrides));
        setInjectorField("extraProviders", extras);
    }

    private void setInjectorField(String name, Object value) throws Exception {
        Field field = WebViewProviderInjector.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(injector, value);
    }

    private static WebViewProviderInfo provider(String packageName) {
        return new WebViewProviderInfo(packageName, packageName, true, false, new String[0]);
    }

    private static final class NamedSystemImpl {
        private final WebViewProviderInfo[] mWebViewProviderPackages;
        private final WebViewProviderInfo[] unrelated = null;

        NamedSystemImpl(WebViewProviderInfo[] providers) {
            mWebViewProviderPackages = providers;
        }
    }

    private static final class RenamedSystemImpl {
        private final WebViewProviderInfo[] renamedProviders;

        RenamedSystemImpl(WebViewProviderInfo[] providers) {
            renamedProviders = providers;
        }
    }

    private static final class AmbiguousSystemImpl {
        private WebViewProviderInfo[] firstProviders;
        private WebViewProviderInfo[] secondProviders;
    }
}
