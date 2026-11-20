package io.github.xfeuer.makerchecker.automation.api.junit;

import io.github.xfeuer.makerchecker.automation.api.ApiTransport;
import io.github.xfeuer.makerchecker.automation.api.ExchangeListener;
import io.github.xfeuer.makerchecker.automation.api.account.AccountServiceClient;
import io.github.xfeuer.makerchecker.automation.core.junit.PlaywrightExtension;
import java.util.List;
import java.util.ServiceLoader;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/** Builds the typed clients that test methods ask for as parameters. */
final class ApiClients implements ParameterResolver {

    private static final Namespace NAMESPACE = Namespace.create(ApiClients.class);

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == AccountServiceClient.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return new AccountServiceClient(new ApiTransport(
                PlaywrightExtension.requestContext(extensionContext),
                PlaywrightExtension.configuration(extensionContext).accountServiceUrl(),
                listeners(extensionContext)));
    }

    /** The listeners on the classpath of this test run. They are looked up once. */
    private static List<ExchangeListener> listeners(ExtensionContext context) {
        return context.getRoot()
                .getStore(NAMESPACE)
                .computeIfAbsent(Listeners.class, key -> Listeners.load(), Listeners.class)
                .all();
    }

    private record Listeners(List<ExchangeListener> all) {

        static Listeners load() {
            return new Listeners(ServiceLoader.load(ExchangeListener.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .toList());
        }
    }
}
