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
| Foundation build environment | `M3JDK=/absolute/path/to/JDK21`, the variable actually read by `make/m3/build.sh` |
| Additional public M3 surface | Preserve declared `jdk.m3.*` companion packages and their separately reviewed API/module boundaries; a source declaration is not installed-image admission |
| Mapping authority | [`doc/m3/name-mapping.json`](../../../doc/m3/name-mapping.json) and [naming admission policy](../../../doc/m3/m3jdk21-naming-admission-workflow.md) |
| Source transformation | Follow [repository receiving/recipe policy](../../../AGENTS.md); document exact donor provenance and preserve upstream license headers |

This fixes the earlier README quickstart confusion between `M3_JDK` and `M3JDK`,
and between `com.m3.text` and the actual `jdk.m3.text` build artifact.
A product rename or internal implementation optimization never licenses a public
JDK ABI change or a broad search-and-replace.

## Verified scope of the foundation test runner

The `m3/core/test/` directory contains 22 **top-level executable `*Test.java`**
classes at the reviewed source tree. The historical, exact-source-sealed
[`make/m3/build.sh`](../build.sh) remains **unchanged** and explicitly runs 18.
The separate additive
[`run_extra_core_contracts.sh`](run_extra_core_contracts.sh) compiles and
starts the remaining four after `make/m3/build.sh` has built the module JAR;
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
python3 -m unittest discover -s make/m3/release -p 'test_*.py' -v
python3 make/m3/release/verify_test_wiring.py --root . --strict \
  --json-out make/m3/release/queue/current/core-test-wiring.json
python3 make/m3/tooling/name-contract/check.py
python3 make/m3/release/audit_release_identity.py --root . \
  --json-out make/m3/release/queue/current/release-identity.json
export M3JDK=/absolute/path/to/a/JDK-21-installation
bash make/m3/build.sh
bash make/m3/release/run_extra_core_contracts.sh
mvn -B -ntp -f m3/ports/precompute/pom.xml test
mvn -B -ntp -f m3/ports/precompute/pom.xml -Pnative test
python3 make/m3/release/verify_release_admission.py --root .
```

Save the exact Git commit and tree, Java/toolchain/OS/architecture, compile
logs, VM-mode test logs, native parity logs, platform/installed-image checks,
test-case counts, failures/skips and SHA-256 digests. Static test discovery
counts are not execution totals. For a release attempt invoke the admission
verifier with `--release` and a complete exact-source evidence manifest as
documented in [the release pack](README.md).

## 2026-11-09 integration candidate

**Result: release admission HOLD; it moved from 6 to 7 passing gates.** `verify_release_admission.py --release` on the integration candidate reports `policy_valid: false` and `release_ready: false`.
- Gates: 7 PASS, 3 recorded FAIL (`OPENJDK_SANITY_MATRIX`, `STRING_UTF16_CONTRACT`, `M3_CHANGED_ATOM_COVERAGE`), 1 PENDING (`M3_MAVEN_CONTROL_REACTOR`).
- Policy: `standalone.no_external_repo_dependency` BLOCK; `legacy_recipe_custody` HOLD. `name_queue` now passes.
- Pack hashes (sha256): `admission-release.json` `3bcd3b03fcf7`, `evidence.json` `644cd89ab2b9`, `SHA256SUMS` `7a60adeea1a8`.
- Results gathered after admission (gtest and the direct `-Xint` runs) are listed in `ADDENDUM-SHA256SUMS`. The admission receipt does not include them.

| Item | Value |
| --- | --- |
| Integration candidate | `1528a255c4773a54d9d89f7448690113e8559f9e` on `release-candidate/m3jdk-21.0.14-m3.1-preview-integration-e14d2c9b`: one commit on `890adb6410da`, dated 2026-11-09 |
| Tree | `59ea4931c1c4f5f3782294c74552ad1e3d5fd18f` = `m3/integration-20261109` head `e14d2c9b39c7` |
| Integration content | `master` `e8e1e837a640` plus #2893, #2897, #2899, #2901, #2904, #2906, #2907, #2910 (first commit), #2909 and #2903 (DOC_BANNER_OPENER v1.1 rerun last); #2905 excluded (policy, user review) |
| Final cut | follows the user's merges; this candidate binds only its tree |
| Evidence pack | `F:/m3-scratch/m3jdk-release-20261109/integration/` |

| Gate | Status | Evidence |
| --- | --- | --- |
| `RIGHTS_AND_LICENSE_BOUNDARY` | PASS | `logs/policy/RIGHTS_AND_LICENSE_BOUNDARY.json` |
| `SOURCE_TREE_AND_UPSTREAM_BASELINE` | PASS: single commit, clean, merge base `890adb6410da`; build tree hashes to `59ea4931` | `logs/policy/SOURCE_TREE_AND_UPSTREAM_BASELINE.json` |
| `OPENJDK_SANITY_MATRIX` | **FAIL** (linux-x64 only). The source-built image now builds with javac warnings fatal and doclint on. jtreg tier1 subset 91/92: the one failure, `M3RegexShortCircuitTest`, is a stale white-box test, fixed in #2914 and passing on this image. hs `tier1_common` after reconfiguring with `--with-gtest` (googletest 1.14.0) and rebuilding the same product sources (only `test/` differs: tree `c7b095ef`, which adds the #2910 and #2914 test commits): 9 passed, 3 did not meet platform requirements; `GTestWrapper` passed all 979 gtests. The multi-platform matrix was not run | `logs/jdk-make-integration3-images.log`, `logs/jtreg-int/tier1-subset*`, `logs/jtreg-int/hs-tier1-common-gtest*`, `logs/gtest-build-tree.txt`, `ADDENDUM-SHA256SUMS` |
| `M3_FOUNDATION_TESTS` | PASS: `build.sh`, the extra contracts in 4 VM modes, `check-m3.py` and `verify-shared-mapping.py` | `logs/foundation-*.log` |
| `PRECOMPUTE_JAVA_DIFFERENTIAL` | PASS | `logs/precompute-java.log` |
| `PRECOMPUTE_JNI_NATIVE_PARITY` | PASS: precompute `-Pnative`, word-row JNI in 4 modes, collections native and default suites | `logs/precompute-native.log`, `logs/collections-*.log` |
| `M3_MAVEN_CONTROL_REACTOR` | **PENDING/HOLD**: policy conflict; #2905 is waiting for user review | - |
| `STRING_UTF16_CONTRACT` | **FAIL** (one timeout). Upstream `java/lang/String` with Character/StringBuilder/StringBuffer passes in default mode; `-XX:-CompactStrings` and C2-only pass. Under `-Xint`, 87/88: `UnicodeCasingTest` exceeds its 800 s timeout, and stock Temurin 21+35 under jtreg `-Xint` times out the same way. Run directly without jtreg, both pass: stock in 1238 s and this image in 1707 s; for 16 of those minutes a 16-job build was running on the same machine | `logs/jtreg-int/utf16-*`, `logs/jtreg-int/*xint-unicodecasing*` |
| `M3_CHANGED_ATOM_COVERAGE` | **FAIL**: below 90%; no reviewed inventory | `logs/coverage/jacoco.xml` |
| `RELEASE_CLAIMS_VALIDATION` | PASS | `logs/policy/RELEASE_CLAIMS_VALIDATION.json` |
| `PROOF_PACK_SHA256` | PASS: 2279 files, 0 mismatches | `SHA256SUMS` |

Other results on this candidate:
- Release tooling tests: 149 run, OK.
- Name-contract tests: 121 run, OK; `check.py` and `audit.py --strict` PASS.
- M3 String/Object jtreg on the image: all 10 M3Object tests pass once the #2910 test follow-up is applied.
- `M3CanonicalApiBoundaryTest` now flags `jdk.internal.misc.M3ObjectMemory`.
- Remaining M3 String white-box failures relate to the immediate-string owner and the pool (see #2910 and the triage notes).
- Experimental `-XX:+UseM3StringStorage`: upstream String passes 83/88. #2911 and #2912 fix the two correctness failures; the three timeouts are recorded in the release notes.

## 2026-11-09 measured candidate

**Result: release admission HOLD.** `verify_release_admission.py --release` against the evidence manifest reports `policy_valid: false` and `release_ready: false`. Exit 1. Of the 11 mandatory gates, 6 PASS with hash-verified artifacts, 3 have recorded FAIL receipts (`OPENJDK_SANITY_MATRIX`, `M3_FOUNDATION_TESTS`, `M3_CHANGED_ATOM_COVERAGE`) and 2 are PENDING without a receipt (`M3_MAVEN_CONTROL_REACTOR`, `STRING_UTF16_CONTRACT`). Policy BLOCKs: `name_queue` and `standalone.no_external_repo_dependency`; `legacy_recipe_custody` is HOLD; `git.single_release_commit` passes. Pack hashes (sha256): `admission-release.json` `319d92b73d95`, `evidence.json` `08f4a9cc0f07`, `SHA256SUMS` `1b99dc685364`.

| Item | Value |
| --- | --- |
| Source `master` | `809625db76e9eabdccc627ec25b5d6773a6fd0be` (includes #2884-#2889 and #2891) |
| Source tree | `30651922a8b3c8b7f80cb89dc04fc5ab5069f50d` |
| Candidate commit | `fe51a3c9ff2f09140be4348e24caa0b8bd9fd18c` on `release-candidate/m3jdk-21.0.14-m3.1-preview-809625db` |
| Candidate shape | single commit, parent `890adb6410dab4606a4f26a942aed02fb2f55387` (`jdk-21+35`), tree equal to the source tree; author and committer `Hitesh Soliwal <hsoliwal@gmail.com>`, both dates `2026-11-09T00:00:00+00:00` (the cut date, not backdated) |
| Superseded cut | `7607ffeee51557cc4695d25fc98fb2f48264284c` (`release-candidate/m3jdk-21.0.14-m3.1-preview-20261109`, from `master` `43ffa8f6016f`, before #2891); not used for evidence |
| `master` after the cut | #2892 (N7b) and later commits are **not** part of this candidate |
| Existing `release/m3jdk-21.0.14-m3.1-preview` | `5589b15ec870` (2026-10-31), stale; not moved, deleted or force-pushed |
| Source identity | the GitHub tarball of the candidate, extracted and hashed from raw bytes (`rawtree.py`), gives tree `30651922a8b3`; the clean checkout has `git status` empty |
| Environment | Windows 11 Home 10.0.26300, WSL2 Ubuntu 24.04.4, x86_64, 32 vCPU. Temurin `jdk-21+35` (the pinned workflow archive, sha256 `82f64c53acaa`), Maven 3.9.14, gcc 13.3.0, cmake 3.28, CPython 3.12.3 with `jsonschema==4.26.0`. JDK build: boot JDK 21.0.12.1, jtreg 8.2.1 |
| Evidence pack | `F:/m3-scratch/m3jdk-release-20261109/` (outside the checkout): `evidence.json` (`M3_RELEASE_EVIDENCE_V1`), `SHA256SUMS`, `admission-release.json`, raw `logs/`, producer `tools/` |

GitHub Actions runs were queued for the whole session. Queued or cancelled
runs count as PENDING, never PASS. Every result below comes from a local run;
log paths are relative to the pack.

| Gate | Status | Evidence |
| --- | --- | --- |
| `RIGHTS_AND_LICENSE_BOUNDARY` | PASS: 6 selected verifier checks (notice/rights/authors/Apache files, OpenJDK boundary text, rights reservation) | `logs/policy/RIGHTS_AND_LICENSE_BOUNDARY.json` |
| `SOURCE_TREE_AND_UPSTREAM_BASELINE` | PASS: 8 checks, including `git.single_release_commit`, `git.merge_base` = `890adb6410da`, `git.clean_source` and `contract.release_identity` | `logs/policy/SOURCE_TREE_AND_UPSTREAM_BASELINE.json`, `logs/extract-rawtree-identity-809625db.log` |
| `OPENJDK_SANITY_MATRIX` | **FAIL**: configure exits 0, but `make images` exits 2 in `java.base`. javac runs with `-encoding ascii` and reports 83 unmappable-character errors in 16 `jdk/internal/math/M3*.java` files; 53 M3 sources in total hold non-ASCII text. No candidate image means no jtreg run. The non-Linux matrix was not run | `logs/jdk-make-images.log`, `logs/jdk-nonascii-java-sources.txt` |
| `M3_FOUNDATION_TESTS` | **FAIL** (1 of 4 job steps). `make/m3/build.sh` exit 0; `run_extra_core_contracts.sh` exit 0 in the jit, int, nocompact and c2 modes; strict test wiring PASS; `verify-shared-mapping.py` exit 0. `compatibility/check-m3.py` exits 1 on a stale fixture path (#2893) | `logs/foundation-*.log`, `logs/foundation-raw-logs/` |
| `PRECOMPUTE_JAVA_DIFFERENTIAL` | PASS: 125 tests, 0 failures, 2 skipped | `logs/precompute-java.log` |
| `PRECOMPUTE_JNI_NATIVE_PARITY` | PASS: precompute `-Pnative` 125 tests, 0 failures; word-row JNI proof in 4 VM modes (`checks=96 JNI=true`, `-Xcheck:jni`); collections `-Pm3-collections-native` 2854 tests, 0 failures, 1 skipped (default suite 2756/0, 135 skipped) | `logs/precompute-native.log`, `logs/wordrows-native-jni.log`, `logs/collections-native-mvn.log` |
| `M3_MAVEN_CONTROL_REACTOR` | **HOLD**: no receipt. Owner decision required (below) | - |
| `STRING_UTF16_CONTRACT` | **PENDING**: no candidate image. The foundation UTF-16 oracles pass in 4 VM modes, but they are not this gate | - |
| `M3_CHANGED_ATOM_COVERAGE` | **FAIL** (measured, below threshold). JaCoCo 0.8.12 over the `jdk.m3.text` foundation suites: line 71.9% (2359/3279), branch 54.0% (1200/2224); 311 of 607 methods reach 90% on both. No reviewed changed-atom inventory exists | `logs/coverage/jacoco.xml` |
| `RELEASE_CLAIMS_VALIDATION` | PASS: 7 checks; none of the contract's forbidden phrases appears in `RELEASE_NOTES.md` | `logs/policy/RELEASE_CLAIMS_VALIDATION.json` |
| `PROOF_PACK_SHA256` | PASS: `SHA256SUMS` covers 2228 pack files; re-reading them gives 0 mismatches | `SHA256SUMS`, `logs/proof-pack-check.log` |

Development admission on the candidate (exit 1, `policy_valid: false`):
- `name_queue` BLOCK: `name-plan.json` pins `authority_blob` `3879a0f1fbc2`, but `9eb3a926ff`
  changed one target in `doc/m3/name-mapping.json`, which is now blob `f191f5772b1d`.
- `standalone.no_external_repo_dependency` BLOCK: counts grew past `STANDALONE_BASELINE.tsv`
  (`D_CHECKOUT` 4->5, `D_REPO_REF` 360->428, `N_PATH` 12->19, `N_NAME` 2011->2339). Release mode
  requires zero. 4 of the 5 checkouts, all 8 secrets and 152 of the 428 repository references are
  in archived `m3/qualification/master-landing-20261028/retained-variants/` copies.
- `legacy_recipe_custody` HOLD: 817 executable candidates under legacy recipe roots.

Other local results:
- Release tooling unit tests: 149 run, OK.
- `git diff --check`: clean.
- Release identity audit (`--release`): PASS.
- Name-contract: `check.py` and `audit.py --strict` refuse the same map drift. The unit tests ran
  121: 2 failures and 2 errors from that drift, and from mapping targets that still name pre-move
  `m3/arrays/...` and `m3/collections/...` paths.
- Strict retained-source queue: `ARCHIVE_PATH_MISSING_PR_ORIGINAL_PATH`.

Fix PRs opened from this measurement (draft, unmerged):

| PR | Blocker |
| --- | --- |
| #2893 | `check-m3.py` still points at `m3-reference/src/com/synexia/indexstring` after the rename. Changes 1 line; result exit 0, `M3_CONTENT_RANGE_PASS` |
| #2897 | The `wordrows-native` pin keeps `bytes: 958` while its resealed `gitBlob` and `sha256` name the 952-byte script. Changes 1 field; the job's own step then passes `PASS_ORIGINAL_WORDROWS_LOADED_JNI_FOUR_MODES` |
| #2899 | `java.base` does not compile: changes `-encoding ascii` to UTF-8 (the javac part of JDK-8301971) and clears 7 `-Werror` warnings. `make java.base-java` then exits 0 with 0 warnings |
| #2901 | `jdk.m3.collections` doclint: 42 errors to 0 (doc comments only) |

**Diagnostic image, indicative only (not a candidate receipt).** This image is the candidate tree with the #2899 and #2901 changes overlaid. It was built with `JAVA_WARNINGS_ARE_ERRORS=`, so javac warnings were non-fatal; `java -version` reports `21.0.14-internal`. With jtreg 8.2.1 on `test/jdk/java/lang/String`:

| Mode | Upstream tests | M3-added tests |
| --- | --- | --- |
| default | 88/89 pass | 59/82 pass (15 failed, 8 errors) |
| `-XX:-CompactStrings` | 87/88 pass | 62/82 pass |

The one upstream error is `StringPlatformChars`, which needs `-nativepath`; the test image was not built. Notable M3 failures:
- `M3StringConcatTupleTest` and `M3StringConcatReferenceDagTest`: a `null` operand is rendered as an empty string in concatenation.
- `M3CanonicalApiBoundaryTest`: `exported JDK package exposes M3 class: java.lang.M3Object`.
- `M3StringInternTest`, and `M3FlatNeedleGateTest` (timed out after 480 s).

The `-Xint`, C1, C2, StringBuilder/Character and `test/jdk/jdk/internal/m3` runs were stopped before they finished. Logs are in `logs/jtreg-diag/` (not in `SHA256SUMS`). `STRING_UTF16_CONTRACT` therefore stays PENDING for this candidate, and these M3 String failures would also block it.

### HOLDs and the owner action each needs

1. **`M3_MAVEN_CONTROL_REACTOR`, policy conflict.** The 2026-10-31 rule says M3JDK21 is standalone,
   with no build, CI or release dependency on `com.synexia`. The contract and `AGENTS.md` require a
   Synexia-owned receipt for this gate. The owner must decide one of two things: keep the gate and
   supply a receipt from `hsoliwal/com.synexia` (repository, commit, recipe id), or change the
   contract through a reviewed policy PR. Release preparation does not edit policy.
2. **Naming authority pin.** Re-pin `name-plan.json` `authority_blob` to `f191f5772b1d` through
   the documented authority-pin recipe (receipt `M3JDK21_NAME_AUTHORITY_PIN_RECEIPT_V1`), or
   approve a target-side one-field re-pin. Then update the map targets for the moved `m3/arrays`
   and `m3/collections` owners.
3. **Standalone ratchet.** Decide whether archived `retained-variants` copies count as executable
   dependencies, or relocate them; then remove the live references (about 276 `D_REPO_REF` rows).
   Do not lower the baseline without that decision.
4. **JDK build lint.** After #2899 and #2901, `jdk.m3.collections` stops on javac's
   dangling-documentation-comment warnings under `-Werror` (javac stops reporting at 100;
   743 files in the module open with a `/*****` license header, which javac reads as a doc comment). Decide between disabling that lint for the M3 modules in
   `Java.gmk` and rewriting the headers. Then rebuild the image and run tier1 plus the
   String/UTF-16 jtreg set on the new candidate.
5. **Coverage.** Review and approve a changed-atom inventory, then raise the line and branch
   coverage of those atoms to at least 90%.
6. **Legacy recipe custody (817) and the retained-source queue.** These need the existing
   reviewed migration.
7. **Re-cut.** Merging any fix PR changes the tree. Cut a new single-commit candidate from the
   merged `master` and rerun this pack; these receipts bind only `fe51a3c9ff2f`.

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
- Under the 2026-10-31 standalone rule the
  [machine release contract](release-contract.json) assigns the
  `M3_MAVEN_CONTROL_REACTOR` receipt to `hsoliwal/M3jdk21`: it is produced
  inside this repository from its own sources, with an exact, hash-verified
  record of commit and recipe ID. Runtime, JDK, JNI and installed-image gates
  remain M3JDK21-owned. [`AGENTS.md`](../../../AGENTS.md) still names Synexia as
  the recipe owner and needs the same decision. The custody policy
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
