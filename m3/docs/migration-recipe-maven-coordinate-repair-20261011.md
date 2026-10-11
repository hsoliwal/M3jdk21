# Migration recipe Maven dependency-resolution repair — 2026-10-11

## Observed failure

The foundation-contracts run 38124027510 and release-admission run 38124027513
fail before Java recipe compilation because Maven Central cannot resolve:
- org.openrewrite:rewrite-java:8.17.1-m3-source-1
- org.openrewrite:rewrite-java-21:8.17.1-m3-source-1
- org.openrewrite:rewrite-test:8.17.1-m3-source-1

The `m3/tooling/migration-recipes/pom.xml` has no repository declaration for
these custom coordinates and the workflow does not build/install a matching
fork first. A version suffix is not evidence that an artifact is published.

## Proposed correction

Use the upstream-published OpenRewrite 8.17.1 coordinates in this standalone
Maven module. The M3 recipe implementations remain the source owners in this
module; only their third-party dependency coordinates change. This does not
change target JDK source or public APIs.

If a custom OpenRewrite fork is a hard requirement, it must instead be built
from a pinned source revision and installed/resolved by an explicit, verifiable
workflow. Silently depending on missing Maven Central artifacts is not a
portable build contract.

## Verification

Run the module's full `mvn clean verify` with Java 21. Compile/tests are the
arbiter: if the upstream API is insufficient, retain the failing evidence and
design an explicit fork-provisioning path rather than suppressing tests.
