# Pinned GitHub recipe boot for Maven

SPDX-License-Identifier: Apache-2.0  
Copyright 2026 Hitesh Soliwal and contributors

The `m3-recipe-boot` profile checks a reviewed GitHub PR manifest during Maven
`validate`. Its shared config is `.m3/recipe-boot.properties` at the Maven
multi-module root. `.mvn/README.md` establishes that root for nested invocations.
The checked-in example is inactive until its exact pins are supplied.

## Install and configure

The reusable implementation stays in `hsoliwal/com.synexia`:

- Source owner: `synexia-openrewrite-recipes/crates/m3-pr-stack-preservation`.
- Recipe: `com.synexia.m3.bootstrap.M3InstallBootRecipe`.
- Artifact: `com.synexia.m3:m3-pr-stack-preservation:1.0.0-m3boot20261021`.
- Canonical task/proofs: `synexia-openrewrite-recipes/recipe-crates/m3-recipe-boot-20261021`.

With Java21 and Maven3.9+, run `mvn -B -ntp -f
synexia-openrewrite-recipes/crates/m3-pr-stack-preservation/pom.xml install` from
the canonical checkout. This coordinate is not assumed to be on Maven Central.
Copy `.m3/recipe-boot-example.properties` to `.m3/recipe-boot.properties`, replace
all placeholder pins with the reviewed target/base/head commits and original
patch SHA-256, and set the PR list. A private repository may use the named token
environment variable. Never put the token in the properties file.

Now ordinary Maven builds check the pinned manifest. The inherited hook chooses
one actual project from the Maven session, including standalone child, `-pl` and
`-rf` builds. Other inherited invocations return before reading configuration or
making network requests. `-Dm3.recipe.boot.skip=true` disables the optional hook.
The coverage ledger distinguishes real parent inheritance from module aggregation;
independent Maven roots have their own source-sealed profile.

## Fetch, apply and build

`check` validates pinned GitHub metadata and patch envelopes. It does not apply a
patch or certify Git applicability, GitHub checks or JDK compatibility.

For application, use a separate properties file with `mode=apply` or `mode=build`.
For `build`, also supply an absolute `mavenExecutable`. Start the canonical runner
before Maven loads its reactor:

```sh
java -jar /path/to/m3-pr-stack-preservation-1.0.0-m3boot20261021.jar /path/to/reviewed-boot.properties
```

The runner fetches the original pinned PR patch, checks metadata before and after
download, validates exact complete hunk context, applies the mbox in a fresh
candidate clone, and optionally runs a new Maven `verify` on its `pomPath`. A
module introduced by a patch is therefore part of the new model. Inherited
Maven invocation overrides are cleared. The original checkout and remote refs
are not modified. A held PR is rolled back inside the candidate; earlier staged
PRs remain available for review. Evidence and the real build log are written to
the configured output directory outside the source tree.

The Maven lifecycle hook only supports `check`; it refuses apply/build because
its reactor model has already been read. Use the separate runner as a strict
gate before any work in parallel `-T` builds. A lifecycle hook cannot impose a
global barrier on unrelated Maven worker threads.

## Hunk resolver and Chrome AI

Deterministic hunk resolution requires exact old context and a unique admitted
location. Ambiguity remains a hold. The canonical candidate's `.git/m3-proof`
receipts contain bounded `advicePackets` for the existing Chrome AI Studio
Prompt client or native ChromeML advisor. The model may suggest a supplied
candidate ID or abstain. `M3HunkAdvice` validates the response against the
original resolver receipt and current source; it does not make a held patch
applicable. Source-changing recipes and product tests remain authoritative.

The canonical tests use actual Git, Maven, OpenRewrite and JUnit, with controlled
model responses. They do not claim live Chrome inference or complete JDK image,
jtreg, JNI/ABI or native-runtime qualification. Public product changes here are
thin Maven/configuration/evidence receivers, not copies of the reusable algorithm.

## Coverage and custody

The installation census is pinned to product commit
`3055b76e294423da047c0a2ee030045bca97ef00`. It accounts for **635 POMs**:
**487 eligible entry points**, covered by **457 independent profile owners** and
**30 inherited children**, plus **148 excluded sealed upstream/custody/fixture
POMs**. Newly introduced entry points require a refreshed census and recipe run;
these counts describe the pinned revision rather than floating future modules.

The reusable source is reconciled to Synexia commit
`c48869338072df8373b2da07ba4a4b1765674667`. Its bounded SDK execution covers the
466 receiving files in 19 disjoint parts, using the unchanged five-test owner
suite for every part and checking all receiving preconditions before writes.
The full product inventory itself is not presented as a single qualified recipe.

See `m3/qualification/recipe-boot-20261021/` for all-POM coverage, original and
result hashes, exclusions, inheritance roots and application receipts. Historical
upstream metadata, exact custody mirrors and before/after fixtures retain their
original bytes. The recipe refuses a conflicting pre-existing boot profile.
