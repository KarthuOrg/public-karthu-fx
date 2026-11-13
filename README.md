# Karthu FX

JavaFX components with a dark theme of their own, and a showcase application that shows them all.

> **Under development. Please do not build on it yet.**
>
> **Created with AI.** The code, the tests and the documentation were written with an AI assistant
> (Claude), directed by a human. Today Karthu FX is a demonstration of components, not a finished
> library.

## What it is

- **A demonstration.** It shows what JavaFX components built with an AI assistant look like:
  themed dialogs, a settings dialog, form inputs, a Markdown view and a few custom controls, all in
  one theme, Nebula Dark.
- **A showcase application.** Every component and every themed standard control has a page to
  click through.
- **Free to read and to copy from.** The licence is MIT-0: take what is useful, no attribution
  needed.

## What it is not

- **Not a finished library.** It is still changing. An API may change or disappear from one
  version to the next, without notice and without a migration path.
- **Not for production.** It has automated tests, but it has not been proven in other people's
  applications.
- **Not supported.** No issues, no pull requests, no answers, and no promise that the work goes on.
- **Not a general toolkit.** One dark theme and no light one, and it needs JDK 25 and JavaFX 25.

## Have a look

With JDK 25 and Maven 3.9 or newer:

```bash
mvn clean install                      # build the library and the showcase
mvn javafx:run -pl karthu-fx-showcase  # start the showcase
```

## More

- [What the library holds](docs/components.md), component by component
- [A guide for coding agents](docs/agent-guide.md): setup, rules and a tested sample for each
  component, for building with karthu-fx in another project
- [Getting, building and running it](docs/getting-started.md): modules, Maven coordinates, the
  Windows executable
- [The theme](STYLEGUIDE.md): colours, sizes, style classes
- [What changed](CHANGELOG.md), version by version
- [How a version is released](docs/releasing.md)

## Licence

[MIT-0](LICENSE). Provided as is, without warranty: use it at your own risk.
