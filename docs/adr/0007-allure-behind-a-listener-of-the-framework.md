# 0007. Allure behind a listener of the framework

- Status: Accepted
- Date: 2026-11-20

## Context

A test run against a financial system has to leave evidence: which test ran, what it sent and what came back. The report is also the first thing that a reader of this repository looks at.

The suites run on the JUnit Platform, with Jupiter now and Cucumber later ([ADR 0005](0005-junit-platform-with-cucumber-on-top.md)). The API tests send their requests through Playwright ([ADR 0004](0004-playwright-for-java-on-the-api-and-ui-layers.md)). A Playwright response does not expose the request that it answers, so only the code that sends a request can record the whole exchange.

Such a report can hold account data. It must not load anything from another host, and no credential may reach it.

## Decision

Allure is the reporting tool. Four choices shape how it is used.

- **Results.** Allure Java 3 writes them. Its adapter for JUnit Jupiter adds one result for every test, and its API for HTTP exchanges holds a request with its response.
- **Evidence.** Every typed client sends its requests through `ApiTransport`. The transport publishes each exchange to the `ExchangeListener`s on the classpath. The module `automation/allure` holds the one listener that knows Allure, and it attaches the exchange to the result of the running test. No other module of the framework imports Allure.
- **Report.** Allure Report 2 generates it. It is a Java program in one zip on Maven Central, and a task of this build runs it on the Java toolchain. Every run of the system tests ends with the report, also when a test fails.
- **One file, no other host.** The report is one HTML file. The build turns off the analytics of Allure and fails when the report would load anything from another host.

## Options considered

The tool:

- **Allure.** Chosen. Its results are plain files and its report is a static page. It shows an HTTP exchange and a Playwright trace as such, and it has adapters for Jupiter and for Cucumber.
- **The test reports of Gradle.** They need nothing. They hold the outcome and the output of a test, and no evidence beyond that.
- **ReportPortal.** It adds history and analysis. It is a server that has to be hosted, and a static site cannot show it.
- **A Playwright trace for each test.** A trace records the exact traffic. It is one archive for each test, and it opens in the trace viewer, not in a report. Traces belong in this report as attachments once the UI layer exists.

The generator of the report:

- **Allure Report 2.** Chosen. It needs only the JDK, Gradle resolves and caches it like any dependency, and one environment variable turns off its analytics.
- **Allure Report 3.** It is the current line, with a newer design. It runs on Node and installs about 160 npm packages. Version 3.33.0 puts a Google Tag Manager script into every report of its default design and has no switch for that.

The place of Allure in the framework:

- **Behind a listener.** Chosen. The framework says what happened, and a module of its own turns that into Allure. This is how the framework already treats the test engine ([ADR 0006](0006-a-playwright-lifecycle-owned-by-the-framework.md)).
- **Called by the transport.** One module less. Every client would then pull in the reporting tool, and no other tool could take its place without a change to the transport.

The task that generates the report:

- **A task of this build.** Chosen. It starts the program with the Java launcher of the toolchain, so it needs no script, no `JAVA_HOME` and nothing on the path.
- **The Allure Gradle plugin.** It is the official way. Version 4.3.1 worked here with the configuration cache, but on Gradle 9.9.1 it raised two deprecation warnings, one of them an error from Gradle 10 on. It also starts a script that needs `JAVA_HOME` or `java` on the path. For Allure Report 3 it adds a repository to the project, which this build forbids.

## Consequences

- The report shows every request with its response, and neither a client nor a test holds a line of reporting code.
- Another reporting tool is one new module. The clients and the suites stay as they are.
- The generator can change without a change to a test, because Allure Report 3 reads the same results. That is worth another look once its report can be built without the analytics script.
- Cucumber can join the same report. Allure has an adapter for it, and the adapter for the JUnit Platform leaves the Cucumber engine to that one.
- The report shows a request as the client built it. Headers that Playwright adds are not in it. A request that gets no response is not attached: the failure of its test names the URL.
- Allure hides the value of a header that carries a credential, and the listener has it hide the value of every cookie. A body is attached as it is. A rule for sensitive fields has to exist before the first test handles one.
- The build owns the task that runs Allure, and it has to follow Allure when the layout of that zip changes.
- One file embeds every attachment. That has to be weighed again when traces arrive, and when CI publishes the report.
