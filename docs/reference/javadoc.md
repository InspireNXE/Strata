# Javadoc

The Javadoc is the contract. Every public and protected type and member in the library modules is documented, and the build fails on a missing comment. The API and SPI docs state what implementors must guarantee: threading, null handling, units, coordinate spaces and what happens on bad input.

No parameter accepts null unless its documentation says so, and passing null where it isn't allowed throws `NullPointerException`. The Javadoc doesn't repeat this on every method.

| Module | For | Javadoc |
|---|---|---|
| `api` | Everyone writing screens and components | [Open](../javadoc/api/index.html) |
| `spi` | Adapter authors | [Open](../javadoc/spi/index.html) |
| `canvas-swing` | Swing applications | [Open](../javadoc/canvas-swing/index.html) |
| `mc-common` | Minecraft mods | [Open](../javadoc/mc-common/index.html) |
| `core` | Contributors. Not part of the API. | [Open](../javadoc/core/index.html) |

Good starting points in the API Javadoc:

- `org.inspirenxe.strata.api.component.Component`: everything every component has.
- `org.inspirenxe.strata.api.event` (the package page): the order handlers and default behavior run in.
- `org.inspirenxe.strata.api.event.EventTypes`: every event type.
- `org.inspirenxe.strata.api.extension.CustomComponent`: the rules for writing components.
- `org.inspirenxe.strata.spi` (the package page): the steps for writing an adapter.

To build the Javadoc locally, run `./gradlew javadoc`. Each module's output is in its `build/docs/javadoc` directory.
