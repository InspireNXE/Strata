# Installation

## Requirements

| What | Version |
|---|---|
| Java | 25 or newer, for building and at run time |
| Gradle | 9.7.1, through the wrapper in the repository |
| Minecraft (optional) | 26.3 exactly; the mod metadata accepts `[26.3, 26.4)` |
| NeoForge (optional) | 26.3.0.83 or newer |
| Fabric Loader (optional) | 0.19.5 or newer. Fabric API is not required at run time. |

Strata itself has no other dependencies. The Swing adapter uses only the JDK.

## Get the artifacts

Strata is not published to a public Maven repository yet. Until it is, build it from source.

```bash
git clone https://github.com/InspireNXE/Strata.git
cd Strata
./gradlew build
```

The build compiles every module, runs the tests, checks that every public type and member is documented, and compiles every example on this site. The jars land in each module's `build/libs`:

| Jar | Contents | Use it for |
|---|---|---|
| `strata-api` | The API | Compiling screens and components |
| `strata-spi` | The SPI | Writing an engine adapter |
| `strata-core` | The implementation | Run time only, through an adapter |
| `strata-canvas-swing` | The Swing adapter | Swing applications and tests |
| `strata-mc-neoforge` | `api`, `spi`, `core` and the Minecraft adapter in one jar | NeoForge mods |
| `strata-mc-fabric` | The same for Fabric | Fabric mods |

To use these from another Gradle project, publish them to your local Maven repository:

```bash
./gradlew publishToMavenLocal
```

They are then available as `org.inspirenxe:strata-<module>:0.1.0-SNAPSHOT` from `mavenLocal()`.

!!! warning "Not yet tried from outside the repository"
    The published metadata has not been consumed from a separate mod project yet. The snippets below follow the build's own setup. If one fails for you, please open an issue with the error.

## Swing applications

Depend on the Swing adapter. It brings `strata-api` and `strata-spi` with it and keeps `strata-core` on the runtime classpath only, so your code compiles against the API alone.

```groovy
repositories {
    mavenLocal()
}

dependencies {
    implementation 'org.inspirenxe:strata-canvas-swing:0.1.0-SNAPSHOT'
}
```

Then continue with [Your first screen](first-screen.md), or read the [Swing platform page](../platforms/swing.md).

## Minecraft mods

Strata ships as its own client-side mod, with the mod id `strata`. Players install the Strata jar for their loader next to your mod, and your mod compiles against the same jar.

=== "NeoForge"

    In a ModDevGradle project:

    ```groovy
    repositories {
        mavenLocal()
    }

    dependencies {
        implementation 'org.inspirenxe:strata-mc-neoforge:0.1.0-SNAPSHOT'
    }
    ```

    Declare the dependency in `META-INF/neoforge.mods.toml`, so the game refuses to start without Strata instead of failing later:

    ```toml
    [[dependencies.examplemod]]
    modId = "strata"
    type = "required"
    versionRange = "*"
    ordering = "NONE"
    side = "CLIENT"
    ```

=== "Fabric"

    In a Loom project. Loom 1.18 does not remap, so use `implementation`, not `modImplementation`:

    ```groovy
    repositories {
        mavenLocal()
    }

    dependencies {
        implementation 'org.inspirenxe:strata-mc-fabric:0.1.0-SNAPSHOT'
    }
    ```

    Declare the dependency in `fabric.mod.json`:

    ```json
    "depends": {
        "strata": "*"
    }
    ```

The examples accept any Strata version, because both loaders sort `0.1.0-SNAPSHOT` before `0.1.0`, so a range such as `[0.1.0,)` would reject the current build. Pin a real range once releases exist.

Only use the `org.inspirenxe.strata.api` packages and the `org.inspirenxe.strata.minecraft` package from your mod. The loader jar also contains `spi` and `core`, because the loader needs them at run time, but they are not part of the API. They change without notice.

!!! note "Client only"
    Strata is a client-side mod (`side = "CLIENT"` on NeoForge, `"environment": "client"` on Fabric). Keep every class that touches Strata out of code that runs on a dedicated server, the same as you would for vanilla screen classes.

### Bundling instead of a separate jar

Both loaders can nest one mod jar inside another: NeoForge with `jarJar`, Fabric with `include`. Nesting means players do not have to install Strata themselves. This has not been tried with Strata yet. If several mods nest different Strata versions, the loader picks one, so keep your version range honest.

## Trying it without a project

The repository's sandbox opens a demo screen in a window:

```bash
./gradlew :sandbox:run
```

On a machine without a display, render one frame to a PNG instead:

```bash
./gradlew :sandbox:run --args="--snapshot sandbox.png"
```

To start Minecraft with Strata loaded, use the loader projects' run configurations: `./gradlew :mc-neoforge:runClient` or `./gradlew :mc-fabric:runClient`. Strata on its own opens no screens. You need a mod, or a small test of your own, to open one.
