<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 release preparation: naming, documentation and coverage

Status: **RELEASE HOLD — preparation, not admission**. Reviewed 2026-11-07.
The authoritative release decision is the output of
[`verify_release_admission.py`](verify_release_admission.py) against an exact,
clean release source commit/tree and its checksum-sealed evidence manifest.
This page does not promote any claim marked `NOT_CLAIMED` in
[`release-contract.json`](release-contract.json).

## Product and naming contract

| Surface | Release spelling and owner |
| --- | --- |
| Product | **M3JDK21**, an OpenJDK 21-derived SDK; GitHub repository `hsoliwal/M3jdk21` |
| Public Java SE packages and classes | Preserve `java.*`, `javax.*`, class names, module exports, constructors, descriptors, reflection and serialization contracts |
| Internal implementation | Established `M3*` owners; do not automatically replace `MIndex*` with `M3*` |
| Independent foundation module | `jdk.m3.text`; output `m3/build/jdk.m3.text.jar` |
| Foundation build environment | `M3JDK=/absolute/path/to/JDK21`, the variable actually read by `m3/build.sh` |
| Additional public M3 surface | Preserve declared `jdk.m3.*` companion packages and their separately reviewed API/module boundaries; a source declaration is not installed-image admission |
| Mapping authority | [`m3/docs/name-mapping.json`](../docs/name-mapping.json) and [naming admission policy](../docs/m3jdk21-naming-admission-workflow.md) |
| Source transformation | Follow [repository receiving/recipe policy](../../AGENTS.md); document exact donor provenance and preserve upstream license headers |

This fixes the earlier README quickstart confusion between `M3_JDK` and `M3JDK`,
and between `com.m3.text` and the actual `jdk.m3.text` build artifact.
A product rename or internal implementation optimization never licenses a public
JDK ABI change or a broad search-and-replace.

## Verified scope of the foundation test runner

The `m3/core/test/` directory contains 22 **top-level executable `*Test.java`**
classes at the reviewed source tree. The historical, exact-source-sealed
[`m3/build.sh`](../build.sh) remains **unchanged** and explicitly runs 18.
The separate additive
[`run_extra_core_contracts.sh`](run_extra_core_contracts.sh) compiles and
starts the remaining four after `m3/build.sh` has built the module JAR;
the release workflow invokes both steps in order. The additional entry points:

| Test | Execution lane | Contract exercised |
| --- | --- | --- |
| `M3LanguageCorpusManifestTest` | JIT, interpreted, compact-strings-off and C2-only | Corpus generation/digest and malformed manifest refusal |
| `M3RopeTextIndexTest` | Same four JVM modes | UTF-16 seams, substring/window behavior, regex parity, long offsets |
| `M3PrecomputePayloadFieldPatternTest` | Default JVM mode with module opened for test reflection | Compiled-pattern sharing, 131,072 UTF-16 field-name inputs, concurrent readers |
| `M3WordRowsContractTest` | Default JVM mode, module JAR on classpath for its subprocess | Mapped row format, direct-address/Java fallback parity, corrupt-image refusal and bounded worker cleanup |

The word-row standalone test does **not** demonstrate JNI parity without a loaded
native library. Separate JNI/native acceptance remains mandatory. The four new
test entries do not substitute for OpenJDK `jtreg`, GC/JIT, installed-image
execution, or measurement against the upstream baseline.

The [static wiring gate](verify_test_wiring.py) checks the union of the
historically pinned build and the additive release runner. It fails if any
standalone `*Test.java` class lacks a compile or launch reference, if an
unknown class is referenced, or if CI does not invoke both scripts. It checks
**wiring, not line coverage, branch coverage, or actual test success**. Its
synthetic refusal tests also prove missing-runner and missing-invocation
cases fail. The release workflow publishes `core-test-wiring.json` and
source-bound logs; runtime results must independently pass.

## Reproduce and retain evidence

From a complete checkout of the candidate revision:

```sh
git rev-parse HEAD 'HEAD^{tree}'
python3 -m unittest discover -s m3/release -p 'test_*.py' -v
python3 m3/release/verify_test_wiring.py --root . --strict \
  --json-out m3/release/queue/current/core-test-wiring.json
python3 m3/tooling/name-contract/check.py
python3 m3/release/audit_release_identity.py --root . \
  --json-out m3/release/queue/current/release-identity.json
export M3JDK=/absolute/path/to/a/JDK-21-installation
bash m3/build.sh
bash m3/release/run_extra_core_contracts.sh
mvn -B -ntp -f m3/ports/precompute/pom.xml test
mvn -B -ntp -f m3/ports/precompute/pom.xml -Pnative test
python3 m3/release/verify_release_admission.py --root .
```

Save the exact Git commit and tree, Java/toolchain/OS/architecture, compile
logs, VM-mode test logs, native parity logs, platform/installed-image checks,
test-case counts, failures/skips and SHA-256 digests. Static test discovery
counts are not execution totals. For a release attempt invoke the admission
verifier with `--release` and a complete exact-source evidence manifest as
documented in [the release pack](README.md).

## Remaining release blockers

- An exact release candidate still needs a green **OpenJDK sanity/jtreg
  matrix**, source-built matched JDK image, Java/JNI parity and complete
  all-mandatory-gates manifest; do not rely on prior-branch successes.
- Legacy recipe custody and cross-repository dependency concerns remain
  governed by the existing release verifier and standalone audit. No recipe,
  donor catalogue or obsolete source is silently deleted here.
- The revised [release checklist](RELEASE_CHECKLIST.md),
  [release notes](RELEASE_NOTES.md) and updated
  [machine contract](release-contract.json) now agree on
  `release/m3jdk-21.0.14-m3.1-preview` in current `master`.
  The [release identity audit](audit_release_identity.py) prevents a future
  name split; it does not assert that a release branch or tag is qualified.
- The new `M3_CHANGED_ATOM_COVERAGE` mandatory contract gate requires
  reviewed changed-atom method inventory, raw JaCoCo XML, source SHA-256,
  and measured >=90% line **and branch** coverage for each changed
  contract-bearing atom. See the upstream
  [naming/documentation/coverage specification](NAMING_DOCUMENTATION_COVERAGE.md).
  No instrumented result is claimed in this PR; the gate remains PENDING.
- [`AGENTS.md`](../../AGENTS.md) and the current
  [machine release contract](release-contract.json) agree that Synexia is
  the exclusive canonical Maven/OpenRewrite recipe-authoring and execution
  owner. M3JDK21 consumes an exact, hash-verified
  `M3_MAVEN_CONTROL_REACTOR` receipt as passive data; its runtime, JDK,
  JNI and installed-image gates remain M3JDK21-owned. The custody policy
  agreement **does not supply a matching execution receipt**. That gate
  and legacy recipe-source migration debt remain HOLD until proven.
- The updated admission workflow now uploads raw
  `m3/build/logs/` foundation logs and their SHA-256 list, the
  `core-test-wiring.json` audit, and Maven precompute Surefire/native
  reports. Artifacts are evidence inputs, not automatic PASS statuses.
- The [SWT fork](https://github.com/hsoliwal/eclipse.platform.swt) has its
  own native, UI, platform and coverage gates. SWT qualification is not
  inherited from a successful M3 foundation test.

Do not label this change `production ready`, publish a release tag, or
upgrade `NOT_CLAIMED` states based on source presence or this checklist.
