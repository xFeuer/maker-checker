package io.github.xfeuer.makerchecker.automation.api.junit;

import io.github.xfeuer.makerchecker.automation.core.junit.PlaywrightExtension;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/** Marks a class of API tests. Its test methods can take the typed clients as parameters. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith({PlaywrightExtension.class, ApiClients.class})
public @interface ApiTest {}
