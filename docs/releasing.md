# Publishing and releasing karthu-fx

What karthu-fx publishes, where, and how a release is made.

## What is published

The parent POM `karthu-fx` and `karthu-fx-core`, group `org.karthu`, to Maven Central through the
Central Portal (`central-publishing-maven-plugin`, server id `central`). `karthu-fx-showcase` is
built and tested, never published (the parent POM's `excludeArtifacts`). The plugin collects the artifacts of the whole
run and uploads them as one deployment at the end, so nothing is uploaded unless every module has
built and passed its tests. `autoPublish` is off: a deployment waits in the portal until someone
checks it and presses *Publish*.

`karthu-fx-core` is published as its POM, its jar, a `-sources` jar and a `-javadoc` jar, each with
its `.asc` signature and checksums. The parent is a POM only. The two extra jars are built in the
`package` phase of every build of `karthu-fx-core`, with no profile or extra option. `clean test`
never reaches `package` and does not build them.

- The `-sources` jar holds every source of the module, `module-info.java` included.
- The `-javadoc` jar documents the packages the module exports: `components`, `config`, `dialog`,
  `form`, `markdown`, `services`, `settings` and `theme` of `org.karthu.fx`.
- The javadoc runs on the module path with every doclint group (`-Xdoclint:all`, missing comments
  included) and fails the build on any warning. JavaDoc on every exported type and member is a
  rule of the code base (`CLAUDE.md`), and a finding is fixed in the JavaDoc, not silenced.
- The javadoc plugin skips a run whose command line and output it has seen before, and an edited
  JavaDoc changes neither. Without `clean`, a JavaDoc broken since the last successful run would
  therefore pass unchecked. `karthu-fx-core` deletes the plugin's stale data
  (`target/maven-javadoc-plugin-stale-data.txt`) in `prepare-package`, so the check runs in every
  build that packages it, with or without `clean`.

Signing happens only in the profile `release` (`maven-gpg-plugin` with the BouncyCastle signer,
which reads the key from `MAVEN_GPG_KEY` and its passphrase from `MAVEN_GPG_PASSPHRASE`), so a
local build needs no key. Central wants the signing key on a public keyserver.

## The workflows

- `.github/workflows/build.yml` ("Build karthu-fx") builds and tests the whole reactor under
  `xvfb-run` on every push to `main` and on every pull request.
- `.github/workflows/publish.yml` ("Publish karthu-fx") runs on a tag `v*`, on Temurin 25. It
  refuses a tag that does not name the reactor version, and a tag whose version has no section in
  `CHANGELOG.md`, then runs `mvn -Prelease deploy` under `xvfb-run`. It needs four repository
  secrets: `CENTRAL_USERNAME` and `CENTRAL_TOKEN` (a user token of the Central Portal),
  `GPG_PRIVATE_KEY` (the armored private key) and `GPG_PASSPHRASE`. After the upload it drafts the
  release on GitHub, with the version's section of the changelog as its text (a release candidate
  as a pre-release). That needs no secret, only the run's own token.

karthu-fx depends on no other repository, and everything it needs comes from Maven Central. That
is also why the workflow has no snapshot check of its own: a release can depend on no snapshot,
since Maven Central hosts none and the run would fail to resolve it.

## The changelog

`CHANGELOG.md` says what changed for someone who uses the library, version by version, the newest
first. A change a user notices gets its line under `## [Unreleased]` in the commit that makes it,
in one of the sections *Added*, *Changed*, *Fixed* and *Removed*. A release turns that heading
into the version's (step 1 below), and the release on GitHub shows the section.

## Releasing

`X.Y.Z` is the release, for a release candidate `X.Y.Z-RCn`.

1. On the branch to release, with a clean tree, set the release version in every POM:
   `mvn versions:set -DnewVersion=X.Y.Z -DgenerateBackupPoms=false`. No test pins the version,
   so nothing else changes. (`showcase.app.version` of the `showcase-exe` profile is the
   showcase executable's own version and is not touched.) In `CHANGELOG.md`, change the heading
   `## [Unreleased]` into `## [X.Y.Z] - YYYY-MM-DD`, the day of the release, and put a new, empty
   `## [Unreleased]` above it.
2. Build the full reactor, and do not commit or tag unless it is green: `mvn clean verify`, on
   Linux under `xvfb-run -a`. `verify` packages too, so this build also checks the JavaDoc of
   `karthu-fx-core` and builds its `-sources` and `-javadoc` jars.
3. Check that only the three POMs and the changelog changed, commit them by name, tag and push.
   All of them have LF line endings in the index: `git diff --stat` shows a few lines per file,
   not a whole-file rewrite.
   ```
   git status --short
   git diff --stat
   git add pom.xml karthu-fx-core/pom.xml karthu-fx-showcase/pom.xml CHANGELOG.md
   git commit -m "Release X.Y.Z"
   git tag vX.Y.Z
   git push origin HEAD
   git push origin vX.Y.Z
   ```
4. Check that the run of "Publish karthu-fx" is green. In the Central Portal, under *Deployments*,
   wait for the validation, look at the content (`karthu-fx` as a POM, `karthu-fx-core` with its
   POM, jar, `-sources` jar and `-javadoc` jar, each signed), then press *Publish*. A bad
   deployment is dropped there instead, and nothing reaches Central. A published version can never
   be changed or removed.
5. On GitHub, under *Releases*, open the draft the run made for the tag, read its text and press
   *Publish release*. Had the deployment to be dropped, delete the draft and the tag instead.
6. Move to the next development version the same way, and build before you commit and push:
   `versions:set` to `X.Y.(Z+1)-SNAPSHOT` (or `X.(Y+1).0-SNAPSHOT`), then `mvn clean verify`
   (under `xvfb-run -a` on Linux), `git status --short`, the three POMs by name, commit and push.
   Snapshots are not published: `mvn install` puts one into the local Maven repository.

About half an hour after *Publish*, `central.sonatype.com` lists the version, and a project
resolves `org.karthu:karthu-fx-core` without any repository or token in its settings.
