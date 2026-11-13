# karthu-fx

Standalone JavaFX component library (`karthu-fx-core`, JPMS module `org.karthu.fx`) with its own
theme and controls, plus a showcase application (`karthu-fx-showcase`). See `README.md` for what
the project is and is not, `docs/components.md` for what the library holds,
`docs/getting-started.md` for the module overview and the build, and `STYLEGUIDE.md` for the
normative styling rules. The README stays short: details go into `docs/` and are linked from it.

## Code rules

- JavaDoc on every exported type and member of `karthu-fx-core`. Every `package` runs javadoc with
  every doclint group and fails on any warning, so a gap stops the release build; `clean test`
  does not catch it. Publishing and releasing: `docs/releasing.md`.
- Every package has a `package-info.java` that says what the package is for. API added after 1.1.0
  carries `@since` with the version it first ships in, and a method that rejects an argument names
  the exception with `@throws`.
- A change someone who uses the library notices gets a line in `CHANGELOG.md` under
  `## [Unreleased]`, in the commit that makes it: what changed for them, in one of the sections
  *Added*, *Changed*, *Fixed* and *Removed*. A release turns the heading into the version's
  (`docs/releasing.md`).
- `docs/agent-guide.md` tells a coding agent in another project how to use the library. Its code
  samples are the methods of `AgentGuideSamplesTest` (`karthu-fx-showcase`), which the build
  compiles and runs and holds the guide to, line by line. A change of the exported API changes
  the guide and that class with it; a new component gets its row, its rules and its sample there.
