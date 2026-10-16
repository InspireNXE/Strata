# Contributing

## Building

You need JDK 25. The Gradle wrapper does the rest.

```bash
./gradlew build
```

This compiles every module, runs every test, runs Javadoc with every doclint check, and compiles and tests the documentation examples. It fails on any Javadoc warning, including a missing comment.

| Command | Does |
|---|---|
| `./gradlew :sandbox:run` | Opens the demo screen in a window |
| `./gradlew :sandbox:run --args="--snapshot out.png"` | Renders the demo screen to a PNG, without a display |
| `./gradlew :mc-neoforge:runClient` | Starts a NeoForge client with Strata loaded |
| `./gradlew :mc-fabric:runClient` | Starts a Fabric client with Strata loaded |
| `./gradlew javadoc` | Builds each module's Javadoc into its `build/docs/javadoc` |
| `./gradlew publishToMavenLocal` | Publishes every module to your local Maven repository |

## Project layout

| Module | Role |
|---|---|
| `api` | What developers compile against: interfaces, values and extension points |
| `spi` | What engine adapters implement and drive, plus the contract test fixtures |
| `core` | The implementation |
| `canvas-swing`, `sandbox` | The Java2D adapter, and a demo written against the API only |
| `mc-common`, `mc-neoforge`, `mc-fabric` | The Minecraft adapter and one mod jar per loader |
| `docs-examples` | The code this site shows, compiled and tested |
| `build-logic` | The `strata-java`, `strata-shared`, `strata-loader` and `strata-documented` convention plugins |

## Rules worth knowing before you change code

- `api`, `spi` and `core` never import an engine type. Everything engine-specific goes through the SPI.
- `api` never imports `spi` or `core`.
- New built-in components are written as `CustomComponent` or `CustomContainer` subclasses in `core`, the way a developer would write them.
- Nothing in `core` reads the wall clock. Use the platform's clock, so tests stay deterministic.
- Painting should not allocate. Cache spans and rectangles during layout.
- Clip and transform pushes are balanced in `try`/`finally`.
- A canceled event skips the component's default `on...` hook. Tests rely on this.
- Minecraft input codes are SDL scancodes. Always map them through the `InputConstants` names.
- Check every Minecraft 26.3 API against the vanilla sources before using it.

## Code style

Strata follows Wintergreen's style: 4-space indents, K&R braces, `final` on parameters and locals that never change, `this.` on every field and method access, records for value types, and self-typed setters that return `T`. Text is American English (color, behavior, canceled), except identifiers that follow JDK naming, such as `isCancelled`. Source files have no license headers.

## Documentation rules

- Every public and protected type, member and package in the library modules has Javadoc, and the build enforces it.
- API and SPI docs state requirements for implementors: what an implementation must do, what it may assume, threading, null handling, units and coordinate spaces, and what happens on bad input.
- Core and adapter docs state what the class actually does, including defaults and edge cases. Check each claim against the code.
- Interfaces that only Strata implements say so in a plain sentence: "Do not implement this interface."
- Write plainly: no em or en dashes, no bold labels, no dramatic contrasts or closing summaries.

## The documentation site

The site is built with [MkDocs](https://www.mkdocs.org/) and the [Material theme](https://squidfunk.github.io/mkdocs-material/) from the Markdown in `docs/`, and published to GitHub Pages by `.github/workflows/docs.yml` on every push to `main`.

### Building it locally

```bash
pip install -r docs/requirements.txt
./gradlew siteJavadoc
mkdocs serve
```

`mkdocs serve` serves the site at `http://127.0.0.1:8000` and rebuilds on every change. `siteJavadoc` copies each library's Javadoc into `docs/javadoc`, which is ignored by Git, so the Javadoc links work. The build is strict: a broken link or a missing snippet fails it.

### Code on the site

Java code on the site is not written in the Markdown. It lives in the `docs-examples` project, which the main build compiles and tests, and pages include it by section:

```markdown
    ```java
    ;--8<-- "layout/LayoutExamples.java:grid"
    ```
```

The path is relative to `docs-examples/src/main/java/org/inspirenxe/strata/examples` (or the matching test directory). The section is marked in the Java file with comments:

```java
// --8<-- [start:grid]
final GridPanel form = factory.gridPanel();
// --8<-- [end:grid]
```

So an API change that breaks an example breaks the build, and the site can never show code that does not compile. Short fragments that only name a method may stay inline in the Markdown; anything a reader might copy belongs in `docs-examples`.

### Images

The screenshots in `docs/assets/images` are rendered from the example screens by the Swing adapter. After changing an example, or anything that changes how things draw, render them again:

```bash
./gradlew :docs-examples:renderDocImages
```

`RenderDocImages` lists which screen goes to which image.

### Publishing

The workflow builds on every pull request, so a broken page shows up in review, and deploys from `main`. The repository's Pages source must be set to "GitHub Actions" once, under Settings, Pages.
