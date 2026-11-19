package io.github.xfeuer.makerchecker.automation.core.playwright;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Playwright;
import java.util.Map;

/**
 * One Playwright driver process.
 *
 * <p>Playwright is not thread-safe: use a session only on the thread that started it.
 */
public final class PlaywrightSession implements AutoCloseable {

    private final Playwright playwright;

    private PlaywrightSession(Playwright playwright) {
        this.playwright = playwright;
    }

    public static PlaywrightSession start() {
        // API tests need no browser. Without this, the driver downloads every browser on its first start.
        Map<String, String> environment = Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");
        return new PlaywrightSession(Playwright.create(new Playwright.CreateOptions().setEnv(environment)));
    }

    /** Opens a request context with its own cookies. The caller disposes it. */
    public APIRequestContext newRequestContext() {
        return playwright.request().newContext();
    }

    @Override
    public void close() {
        playwright.close();
    }
}
