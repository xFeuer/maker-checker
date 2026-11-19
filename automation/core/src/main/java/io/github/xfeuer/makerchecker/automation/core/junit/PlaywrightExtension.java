package io.github.xfeuer.makerchecker.automation.core.junit;

import com.microsoft.playwright.APIRequestContext;
import io.github.xfeuer.makerchecker.automation.core.config.Configuration;
import io.github.xfeuer.makerchecker.automation.core.playwright.PlaywrightSession;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

/**
 * Binds the Playwright lifecycle to JUnit: one session per test class and one request context per test.
 *
 * <p>Playwright is not thread-safe, so the tests of one class must run on one thread. Classes may run in parallel.
 *
 * <p>Other extensions build on this one: they ask it for the request context of the current test.
 */
public final class PlaywrightExtension implements BeforeAllCallback {

    private static final Namespace NAMESPACE = Namespace.create(PlaywrightExtension.class);

    @Override
    public void beforeAll(ExtensionContext context) {
        session(context);
    }

    /** The configuration of this test run. It is read once. */
    public static Configuration configuration(ExtensionContext context) {
        return context.getRoot()
                .getStore(NAMESPACE)
                .computeIfAbsent(Configuration.class, key -> Configuration.load(), Configuration.class);
    }

    /** The request context of the current test. It is disposed when the test ends. */
    public static APIRequestContext requestContext(ExtensionContext context) {
        return context.getStore(NAMESPACE)
                .computeIfAbsent(
                        RequestContextResource.class,
                        key -> new RequestContextResource(session(context).newRequestContext()),
                        RequestContextResource.class)
                .requestContext();
    }

    private static PlaywrightSession session(ExtensionContext context) {
        return context.getStore(NAMESPACE)
                .computeIfAbsent(PlaywrightSession.class, key -> PlaywrightSession.start(), PlaywrightSession.class);
    }

    /** Makes a request context closeable, so that the store disposes it together with its test. */
    private record RequestContextResource(APIRequestContext requestContext) implements AutoCloseable {

        @Override
        public void close() {
            requestContext.dispose();
        }
    }
}
