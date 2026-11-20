package io.github.xfeuer.makerchecker.automation.api;

/**
 * Hears of every exchange that a transport completes.
 *
 * <p>This is how a reporting tool gets its evidence while the clients know nothing of the tool. The JUnit binding
 * finds the implementations with {@link java.util.ServiceLoader}, so a listener joins a test run by being on its
 * classpath.
 *
 * <p>A listener is called on the thread of the test that made the request.
 */
public interface ExchangeListener {

    void onExchange(Exchange exchange);
}
