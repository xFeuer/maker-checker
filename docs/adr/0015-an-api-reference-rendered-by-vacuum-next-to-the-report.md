# 0015. An API reference rendered by vacuum, next to the report

- Status: Accepted
- Date: 2026-11-26

## Context

The contracts are the source of truth for both sides of the repository ([ADR 0002](0002-one-repository-with-a-black-box-boundary.md)). They are YAML: seven specifications and the parts that they share. A reader should see their operations, their schemas and their examples as pages, without reading YAML.

The type of a problem is an address under the site of the repository, and [ADR 0010](0010-problem-details-for-every-error-response.md) promised a page at each of those addresses. Until now they led nowhere.

GitHub Pages already serves the report of the last run on `main`, at the root of that site ([ADR 0007](0007-allure-behind-a-listener-of-the-framework.md)). The report has to keep its address.

Two rules of earlier decisions hold here too. The build needs only a JDK ([ADR 0009](0009-openapi-3-1-contracts-linted-by-vacuum-in-the-build.md)). And a page that the build publishes loads nothing from another host (ADR 0007).

## Decision

- **vacuum renders the reference.** Its `docs` command writes pages for every specification, with a catalog in front: one entry for each service and one for the shared parts. The pages are files that any web server can serve, and they carry their scripts, their style sheets and their fonts themselves. vacuum is the program that already lints the contracts, so the build gets no new tool.
- **The reference is HTML and nothing else.** vacuum can also write a page of lint findings, files for programs to read and a copy of the specifications. The build switches the three off. It also switches off the footer of vacuum, which names its maker as the holder of a copyright.
- **The build changes one thing about those pages.** It adds a rule to one style sheet, so that the pages of the catalog scroll.
- **The build writes the pages of the problem types.** It reads every example of an error response from the specifications and writes one page for each type: its status, its title, what the examples say and the operations that answer with it. One more page lists the types. The specifications are the only source, and the build fails when they give one type two titles or two status codes.
- **The site has three parts.** The report stays at the root. The reference is under `api/`. The problem types are under `problems/`, where their addresses point.
- **Every page keeps the rule of the report.** The build fails when a page of the reference or of the problem types would load a script, a style sheet, an image or a frame from another host.
- **The build makes all of it, and CI publishes it.** `build` renders the reference and writes the problem pages. The task `site` puts both next to the report. CI publishes the site of every run on `main` that has a report, as it published the report alone before.

## Options considered

The renderer:

- **vacuum.** Chosen. It takes no new download, no page of our own and no setting: its pages load nothing from another host as they come, and it opens no connection while it renders. It reads the shared parts and links each service to them. Its look is its own, and its `docs` command is from 2026.
- **Redoc, Scalar or Swagger UI.** Each is a script that draws the reference in the browser, from the specification. Their looks are the familiar ones. Each would be one more download for the build, with a page of our own around it. By their own documents each one talks to another host until a setting stops it: Redoc 3 reports usage data, Scalar loads its fonts from a CDN, and Swagger UI asks an online validator. They were weighed from those documents and not rendered.
- **Redocly CLI,** which writes one HTML file for a specification. It runs on Node (ADR 0009).

The pages of the problem types:

- **Generated from the specifications.** Chosen. The page of a type cannot say something else than the contracts, and a new type gets its page with its first example.
- **Written by hand,** one file for each type. The text could say more, such as what a client should do next. It would be a second source next to the contracts, and nothing would hold the two together.
- **A custom page of vacuum.** vacuum renders Markdown files that lie next to a specification, under ten fixed names such as `errors.md`. One page could list the types. It could not give each type the address that ADR 0010 names.

The mode of vacuum:

- **The default mode,** which writes relative links only. Chosen. The pages work under any address, and from disk.
- **The mode for hosting,** `--publish`. In version 0.43.0 its deeper pages link to a catalog page that is not there. With the full address of the site, every script and every style sheet gets the host in its address, and a local copy would load them from the published site.

The catalog, which cannot scroll in vacuum 0.43.0:

- **One rule, added by the build.** Chosen. A reader reaches every entry of the catalog in any window. The build now relies on the name of a style sheet of vacuum and on the name of a class.
- **The pages as vacuum writes them.** The build would rely on nothing inside those pages. A reader with a small window could not reach the last entries.
- **A first page of our own in place of the catalog.** It would link to pages whose addresses belong to vacuum. The page that lists the specifications of a service has the same fault, and it would keep it.

The place of the report:

- **The root, as before.** Chosen. Its address is in the README, and a reader of an old pull request still finds it.
- **A first page of our own** that links to the report, the reference and the problem types. It would lead a reader better. The report would have to move.

## Consequences

- The look of the reference is the one of vacuum: dark first, with a switch to light. The outline of a page lies over the ends of its first lines until the reader folds it.
- In vacuum 0.43.0 the pages of the catalog cannot scroll: the style of vacuum stops every page from scrolling as a whole, and those pages have no panel that scrolls instead. The build adds one rule to their style sheet, for the class that those pages have on their body. It fails when the first page of the catalog no longer loads that style sheet or no longer has that class. It sees no other change of those pages, so scrolling the catalog is a step of raising vacuum. The rule goes when vacuum lets its catalog scroll.
- The addresses under `api/` belong to vacuum, such as `api/services/<service>/versions/<version>/specs/<name>/`. Nothing may link to a page below `api/index.html`: a later vacuum can move it. That is why a page of a problem type names an operation in words.
- The addresses under `problems/` are ours, and they are the identifiers of ADR 0010. The page of a type is the file `problems/<name>.html`. GitHub Pages also serves it without the ending.
- Raising vacuum now also means looking at the reference. `contracts/README.md` has the steps.
- vacuum does not write the same bytes twice, so the build never takes the reference from its cache.
- A description in a specification is Markdown, and vacuum renders it. An image from another host in a description fails the build.
- The check for other hosts reads the tags of every page, as it does for the report. It does not see what a script asks for while it runs. That was checked once in a browser, for the version in use.
- The site is about 27 MiB in 1,100 files, and the reference is most of it. CI uploads it on every run on `main`.
- The report is still the first page, and it does not link to the reference. The README does.
- The services of phase 3 answer with problems whose types now lead to a page.
