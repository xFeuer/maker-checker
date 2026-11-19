package io.github.xfeuer.makerchecker.automation.api.junit;

import io.github.xfeuer.makerchecker.automation.api.account.AccountServiceClient;
import io.github.xfeuer.makerchecker.automation.core.junit.PlaywrightExtension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/** Builds the typed clients that test methods ask for as parameters. */
final class ApiClients implements ParameterResolver {

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == AccountServiceClient.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return new AccountServiceClient(
                PlaywrightExtension.requestContext(extensionContext),
                PlaywrightExtension.configuration(extensionContext).accountServiceUrl());
    }
}
