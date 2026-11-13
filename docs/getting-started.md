# Getting, building and running karthu-fx

Read the [README](../README.md) first: karthu-fx is under development and not meant to be built
on yet. This page is for looking at it and trying it out.

## Modules

| Maven module | JPMS module | Purpose |
|--------------|-------------|---------|
| `karthu-fx-core` | `org.karthu.fx` | The component library: theming, configuration, dialogs, settings, custom controls |
| `karthu-fx-showcase` | `org.karthu.fx.showcase` | Demo application showcasing all themed JavaFX controls |

What the library holds, package by package: [`components.md`](components.md). Setup, rules and a
tested code sample for each component, written for a coding agent that builds with karthu-fx in
another project: [`agent-guide.md`](agent-guide.md).

## Requirements

- JDK 25
- Maven 3.9+

## Build and run the showcase

```bash
mvn clean install                      # build the library and the showcase
mvn javafx:run -pl karthu-fx-showcase  # launch the showcase app (needs the install above once)
```

## Using the library in a project

`karthu-fx-core` and its parent POM are published to Maven Central, with `-sources` and
`-javadoc` jars, from `1.1.0-RC1` on. No repository and no token to configure:

```xml
<dependency>
    <groupId>org.karthu</groupId>
    <artifactId>karthu-fx-core</artifactId>
    <version>1.1.0-RC1</version>
</dependency>
```

- `karthu-fx-core` ships only `slf4j-api`; provide your own SLF4J binding in the application
  (the showcase uses Log4j 2).
- Full JPMS: add `requires org.karthu.fx;` to your `module-info.java`.
- Apply the theme to every scene with `ThemeManager.apply(scene)` before the scene is shown.

## Windows executable (jpackage)

The showcase can be packaged as a standalone Windows application (bundled runtime, no Java
required on the target machine):

```bash
mvn clean package -pl karthu-fx-showcase -am -Pshowcase-exe
```

Output: `karthu-fx-showcase/target/dist/KarthuFxShowcase/KarthuFxShowcase.exe`. Zip the
`KarthuFxShowcase/` folder to pass it on.

Requires a JDK that ships `jpackage` (any standard JDK 25 distribution) on the build machine.

## Releasing

How a version is built, signed and published: [`releasing.md`](releasing.md).
