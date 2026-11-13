<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK 21 — 21.0.14 m3.1 technical preview candidate

**Preparation status: HOLD for release admission.** These notes describe the
candidate and its test scope; they do not announce a published release.

Tag: `m3jdk-21.0.14-m3.1-preview` · Branch: `release/m3jdk-21.0.14-m3.1-preview` ·
Tier: `P0_TECHNICAL_PREVIEW` ([release contract](release-contract.json)).

## Source and naming identity

| Item | Preparation identity |
| --- | --- |
| Product | M3JDK 21; machine ID `M3JDK21` |
| Inspected repository | `hsoliwal/M3jdk21` |
| Inspected master baseline | `b9cc756f726de31af10652b5583d9bba58ba27b2` |
| Baseline tree | `9b5fa265be790996a7ed665992b618f642f6f454` |
| Configured version | `21.0.14` in `make/conf/version-numbers.conf` |
| Contract upstream baseline | `openjdk/jdk21` at `890adb6410dab4606a4f26a942aed02fb2f55387` |
| Final receiving/release commit | Must be bound separately by the executed receiving and release evidence; the inspected baseline is not that final commit |

The verifier now checks the display name, version, preview tag and release
branch against the actual version configuration. Naming metadata follows the
current `jdk.m3.*` source packages and module descriptors. Standard JDK names,
JNI surfaces and the exact compatibility alias `Object.toAdpater(Class<T>)`
remain unchanged, alongside `Object.toAdapter(Class<T>)`.

The same canonical naming map retains the original stable capability IDs,
prior source hashes and target lineage. It restores the historically recorded
`synexia.lexicons.related-source-range` capability with its original
`implemented-unverified` status. Four queued naming candidates remain queued.
The [passive naming receipt](../tooling/name-contract/receipts/release-preparation/source-identity.json)
records the inspected source and restoration provenance. Naming presence is not
runtime qualification.

## Preparation changes and regression scope

| Area | Preparation change | What the tests establish |
| --- | --- | --- |
| Current naming | Exact `jdk.m3.` package/module acceptance; stale source map symbols corrected using inspected declarations | Known owner accepted; similar/arbitrary namespaces, comment-only declarations and unsafe companion boundaries refused |
| Mapping schema/history | Required signal metadata normalized; original stable-ID fixtures restored; historical related-range record preserved | Full authority validates without dropping schema requirements or renaming historical capability IDs |
| Arrays lineage | Current Arrays source pinned; two exact historical patch reversals retained as test data | Source-chain and patch mutations refused; no repository Java source is transformed by this test |
| Release identity | Display name, configured version, preview tag and branch validated together | Version drift, missing/duplicate version fields and mismatched preview identity refused |
| Evidence ownership | Canonical recipe execution belongs to Synexia; target acceptance remains M3JDK21-owned | Wrong owner, stale/malformed receipt, artifact-path and hash failures remain refused |
| Packaging and dependency checks | Selected build artifacts and the narrow passive receipt exception tested | Missing/extra platform artifact and executable external dependency paths are not silently admitted |

These are target-side **release-tooling regression tests**, not product code
coverage percentages. They execute fixtures and read pinned source. The Java API guard additionally
executes synthetic module fixtures on stock Java 21. These checks do not build
the JDK, execute a native product library, run the full jtreg suite, or
qualify any receiving phase. Canonical recipe execution is a distinct receipt.

## Current-source coverage and admission matrix

All eleven gates in `mandatory_release_gates` need a receipt bound to the release commit and tree in
the `M3_RELEASE_EVIDENCE_V1` manifest. The GitHub Release lists each gate's state, PASS or PENDING,
with its producer.
The scope below applies to this preparation. Older successful or failed runs
retain their original source pins and cannot fill these current-source rows.

| Gate | Producer |
| --- | --- |
| `RIGHTS_AND_LICENSE_BOUNDARY`, `SOURCE_TREE_AND_UPSTREAM_BASELINE`, `RELEASE_CLAIMS_VALIDATION`, `PROOF_PACK_SHA256` | `verify_release_admission.py` (local or `m3-first-release-admission.yml` job `contract`) |
| `M3_FOUNDATION_TESTS` | `bash ./make/m3/build.sh` (job `foundation`) |
| `PRECOMPUTE_JAVA_DIFFERENTIAL` | `mvn -B -ntp -f m3/ports/precompute/pom.xml test` (job `precompute`) |
| `PRECOMPUTE_JNI_NATIVE_PARITY` | `mvn -B -ntp -f m3/ports/precompute/pom.xml -Pnative test` (job `precompute`) |
| `OPENJDK_SANITY_MATRIX` | `.github/workflows/main.yml` (OpenJDK GHA Sanity Checks), CI only |
| `M3_MAVEN_CONTROL_REACTOR` | **PENDING — receipt produced inside M3jdk21 required.** Under the 2026-10-31 standalone rule `release-contract.json` assigns this gate to `hsoliwal/M3jdk21`; the receipt records its exact commit and recipe ID. This table records no execution or product admission. |
| `STRING_UTF16_CONTRACT` | no workflow job or command is bound to this gate yet |
| `M3_CHANGED_ATOM_COVERAGE` | **PENDING** — instrumented, source-hash-checked >=90% line and branch coverage of changed contract-bearing M3 atoms, with complete inventory and raw report hashes; does not assert whole-JDK coverage |

### Integration candidate `1528a255c477` (2026-11-09)

This candidate is `master` `e8e1e837a640` plus the open fix PRs, in tree `59ea4931c1c4`.
- PASS (7 gates): rights, source, foundation, precompute Java and JNI, claims, proof pack.
- FAIL: OpenJDK sanity. Only linux-x64 ran: tier1 subset 91/92, and hs `tier1_common` passed, including all 979 gtests.
- FAIL: STRING_UTF16. All upstream String modes pass except one `-Xint` timeout. Stock 21+35 has the same timeout, and run directly both pass (1238 s stock, 1707 s M3, measured under build load).
- FAIL: changed-atom coverage.
- PENDING: the Maven control reactor (#2905).
- The source-built image now builds with javac warnings fatal and doclint on.

Admission remains **HOLD**; details are in [RELEASE_PREPARATION.md](RELEASE_PREPARATION.md#2026-11-09-integration-candidate).

### Measured on candidate `fe51a3c9ff2f` (2026-11-09)

The candidate is one commit over `890adb6410da` with the tree of `master` `809625db76e9` (`30651922a8b3`). It was measured locally on Windows 11 with WSL2 Ubuntu 24.04, x86_64; GitHub Actions runs were queued. Release admission is **HOLD**. The full table, log names and owner actions are in [RELEASE_PREPARATION.md](RELEASE_PREPARATION.md#2026-11-09-measured-candidate).

| Gate | State |
| --- | --- |
| `RIGHTS_AND_LICENSE_BOUNDARY`, `SOURCE_TREE_AND_UPSTREAM_BASELINE`, `RELEASE_CLAIMS_VALIDATION`, `PROOF_PACK_SHA256` | PASS |
| `PRECOMPUTE_JAVA_DIFFERENTIAL` | PASS: 125 tests, 0 failures |
| `PRECOMPUTE_JNI_NATIVE_PARITY` | PASS: precompute `-Pnative` 125/0; word-row JNI in 4 VM modes; collections native 2854 tests, 0 failures, 1 skipped |
| `M3_FOUNDATION_TESTS` | FAIL: `check-m3.py` stale fixture path (fix #2893); `build.sh` and the extra contracts pass |
| `OPENJDK_SANITY_MATRIX` | FAIL: the linux-x64 source build stops in `java.base` (fixes #2899, #2901; one owner decision remains) |
| `STRING_UTF16_CONTRACT` | PENDING: no candidate image. On a diagnostic image, upstream `java/lang/String` passes 88/89 and the M3-added String tests pass 59/82 |
| `M3_CHANGED_ATOM_COVERAGE` | FAIL: `jdk.m3.text` line 71.9%, branch 54.0%; no reviewed inventory |
| `M3_MAVEN_CONTROL_REACTOR` | HOLD: owner decision (standalone rule vs Synexia-owned receipt) |

## Platform matrix

**Preview platform selection:** this preview ships **linux-x64 only**
(`platform_selection` in [release-contract.json](release-contract.json); the
`platforms` input of `m3-release.yml`). Every other platform is `NOT_CLAIMED`:
macos-x64, macos-aarch64, windows-x64, windows-aarch64, linux-x86,
linux-aarch64, linux-arm, linux-ppc64le, linux-riscv64, linux-s390x,
android-arm64 and ios-arm64. No gate is dropped: `OPENJDK_SANITY_MATRIX`
passes only when the linux-x64 runs pass on the exact final candidate.

**Historical CI snapshot only:** the entries below describe 2026-10-31 observations
at master `bb66a8c16642`, not 2026-11-07 current `master` and not a release candidate.
Recompute build, test and coverage evidence against the final candidate commit and tree.
`Pending` means no qualifying run was recorded at the then-proposed release commit.

| Platform | JDK build lane | Latest evidence | Preview state |
| --- | --- | --- | --- |
| linux-x64 | `main.yml` -> `build-linux.yml`, tier1 `test.yml` | Last green build: run 39250083857 (`bb6b279495ce`, 2026-10-19, another branch). `main.yml` creates jobs again (run 41153588755 at `bb66a8c16642`: 17 jobs created, run cancelled). | selected; build pending |
| linux-x86 | `build-linux.yml` (hs) | run 39250083857 | NOT_CLAIMED |
| linux-aarch64, ppc64le, riscv64, s390x, arm | `build-cross-compile.yml` (cross build only, no tests) | run 39250083857 | NOT_CLAIMED |
| windows-x64 | `build-windows.yml`, tier1 | run 39250083857 | NOT_CLAIMED |
| windows-aarch64 | `build-windows.yml` (HotSpot only, cross) | run 39250083857 | NOT_CLAIMED |
| macos-x64 | `build-macos.yml`, tier1 | run 39250083857 | NOT_CLAIMED |
| macos-aarch64 | `build-macos.yml` (cross from macos-15-intel) | run 39250083857 | NOT_CLAIMED |
| android-arm64 | none (only the C11/JNI header compile in `m3-jni-common.yml`) | run 41019153845: success | NOT_CLAIMED |
| ios-arm64 | none (only the C11 source cross-compile in `m3-jni-common.yml`) | run 41019153845: success | NOT_CLAIMED |
| C11 JNI contract (windows-2022, ubuntu-24.04, ubuntu-24.04-arm, macos-15, macos-15-intel) | `m3-jni-common.yml` | run 41019153845: all jobs success | header contract only |

## Known issues

- **M3Object observation API: internal, pending user admission as public API.** `java.lang.M3Object`
  (with `M3Object.Field`), `ObjectEvent`, `ObjectSubscription` and `ObjectChangeListener` are
  package-private once #2910 lands. `Object.toAdapter` keeps its capability case, and its public
  Javadoc no longer names the type.
- **Experimental `-XX:+UseM3StringStorage` (off by default; String backed by M3 is NOT_CLAIMED).**
  - Measured 2026-11-09, upstream `java/lang/String` jtreg with the flag: 83 of 88 pass.
  - Correctness fixes are open: #2904 (null concat operand), #2911 (String length limit
    `OutOfMemoryError`) and #2912 (racy `char[]`/`int[]` constructors).
  - Known performance limitation: search reads one code unit at a time
    (`M3StringSearchPrecompute` -> `CodeUnitCursor.at` -> `M3StringAtom.unitAt`).
    `M3FlatNeedleGateTest` takes 19.6 s in default mode but exceeds its 480 s timeout with the
    flag, more than 24x slower. Upstream `LiteralReplace`, `StringRepeat#id1` and
    `UnicodeCasingTest` time out with the flag (480 s, 480 s and 800 s).

- **Source-built JDK image (measured 2026-11-09):** `make images` on the candidate stops in
  `java.base`. The build compiles with `-encoding ascii`, and 53 M3 Java sources contain
  non-ASCII text. Next come 7 `-Werror` warnings in `java.base` and then 42 doclint errors
  in `jdk.m3.collections`; the fixes are #2899 and #2901. After those, `jdk.m3.collections`
  stops on javac's dangling-documentation-comment lint: 743 module files open with a
  `/*****` license header, which javac reads as a doc comment. That last one needs an
  owner decision.
- **Naming authority pin (measured 2026-11-09):** `name-plan.json` pins `name-mapping.json`
  blob `3879a0f1fbc2`, but `9eb3a926ff` moved it to `f191f5772b1d`. The documented re-pin
  path is the Synexia authority-pin recipe.
- **Standalone ratchet (measured 2026-11-09):** the dependency counts grew past
  `STANDALONE_BASELINE.tsv`. 152 of the 428 `D_REPO_REF` rows, and all 8 `D_SECRET` rows,
  are in archived `retained-variants` copies.

- **Platform builds (snapshot 2026-10-31):** the last recorded completed OpenJDK
  sanity run in this audit was from 2026-10-19; 305 Action runs were queued
  on 2026-10-31. This is historical evidence, not a statement about the live
  Actions queue. The final candidate's `OPENJDK_SANITY_MATRIX` remains unqualified
  until current-revision results are included in the release evidence manifest.
- **Legacy recipe custody (2026-10-31 inventory):** `legacy_recipe_custody` was HOLD
  with 811 executable candidates across historical recipe roots. Re-run the current
  verifier and inventory at the final source tree; do not assume that count remains
  current, remove candidates without sealing provenance/callers, or waive the gate.
- **Maven control-reactor evidence:** **PENDING.** Under the 2026-10-31 standalone
  rule `M3_MAVEN_CONTROL_REACTOR` requires an exact source-bound, hash-verified
  receipt produced inside `hsoliwal/M3jdk21`. M3JDK21 owns this gate and the
  product build, runtime and target acceptance gates. The target still requires
  zero executable dependencies on another repository. The actual receipt must
  match the final receiving bytes; this release note is not that receipt.
- **`STRING_UTF16_CONTRACT`:** this gate has no bound producer.
- **Changed atom test coverage:** `M3_CHANGED_ATOM_COVERAGE` is PENDING until
  source-pinned instrumented counts and the complete contract-atom inventory
  satisfy the 90% minimum line/branch gate; 99% remains a stricter objective.
  No whole-JDK line/branch percentage is claimed.
- **Tokenizer image workflow:** `M3JDK21 tokenizer source-built image and jtreg` stops before the
  build, at the `get-jtreg` step: the artifact `bundles-jtreg-8.2.1+1` is not found (last run 40992710079,
  2026-10-30). The earlier doclint failure in `TokenMatcher.java`
  (blocker 7) is fixed and checked with `javac -Xdoclint:all/protected -Werror`.
- **Generated-mirror drift (audit blocker 13 a, b):** `M3StateMaskHistogram.java` and
  `M3SemanticHasher.java` pins still drift. Successor patches are prepared and not merged.
- **Admission workflow trigger (audit blocker 5):** `m3-first-release-admission.yml` runs on
  `release/**` pushes, pull requests and dispatch, but not on master pushes.
- **Compile audit:** `javac --release 21 -Xlint:all,-module -Werror` over the 27 audited `m3/**`
  modules gives 16 PASS, 9 FAIL, 1 ABSENT (`module-packs/build-support`: sources live under
  vendor subtrees) and 1 NO_SOURCES (`module-packs/collections`: module-info only). Each of the 9
  failures is a dependency that is not on the probe classpath:
  - Jackson, Lucene/OpenNLP, OpenRewrite, JavaFX, TornadoVM API and Synexia MIndex;
  - `jdk.internal.vm.parallel`, which needs the built image;
  - intra-repository parser closure.

  None of the 27 audited modules has a source-level compile error.
- **Swing viewport serialization:** `M3PackedTableModel` throws `NotSerializableException` when
  it holds a column (`M3PackedColumn` is not `Serializable`). This was already true before this
  release.
- **Release branch identity:** the contract, this note and the checklist now agree on
  `release/m3jdk-21.0.14-m3.1-preview`. This naming check does not establish a built or
  tested release image.
- **Recipe custody/owner policy:** current target namespace and map policy
  agree on declared `jdk.m3.*` companions, stable lineage IDs and queued naming
  candidates. Synexia owns canonical recipe authoring/execution; M3JDK21 owns
  product acceptance and consumes its receipt as passive data. The exact canonical
  qualification and execution receipt, whole-tree naming fixed point, and
  installed-image/JNI/runtime qualification remain separate pending gates.
  Consistent target policy does not establish those execution results.
| Surface | Required producer / evidence | Preparation disposition |
| --- | --- | --- |
| Naming and canonical target census | `name-contract/check.py`, `audit.py --strict`, and all `test_*.py` in that directory | Executed on the candidate text/source snapshot; no installed-image claim |
| Release tooling | All `make/m3/release/test_*.py` | Executed on candidate tooling and isolated fixture repositories |
| Canonical transformation | Existing Synexia source-sealed recipe and real SDK replay/fixed-point/refusal tests | Separate execution receipt required for final receiving bytes |
| Immutable text foundation | `bash ./make/m3/build.sh` at the exact final candidate | NOT RUN by this preparation; current-source receipt required |
| Precompute Java parity | `mvn -B -ntp -f m3/ports/precompute/pom.xml test` | NOT RUN by this preparation; current-source receipt required |
| Precompute JNI/native parity | Same POM with `-Pnative test`, matched OS/architecture/toolchain | NOT RUN by this preparation; platform-specific receipt required |
| String UTF-16 contract | Source-built M3JDK image, affected String/jtreg/VM tests | PENDING; no dedicated first-release workflow producer bound to `STRING_UTF16_CONTRACT` |
| API guard mechanism | Existing Java 21 patched-module workflow fixtures; current/legacy namespace self-tests | Executed: one positive, two deliberate internality violations refused; no product-image claim |
| Public API/module boundary | `make run-test TEST=test/jdk/java/lang/String/M3CanonicalApiBoundaryTest.java` on the matching image | NOT RUN by this preparation; synthetic stock-JDK fixtures do not satisfy it |
| SHA-256 Object adapter/JNI | `test/jdk/java/lang/Object/SHA256ObjectAdapterTest.java` and its native library on the matching image | NOT RUN by this preparation; naming inspection preserves both adapter spellings |
| Linux x64, Windows x64, macOS x64 and macOS aarch64 bundles | Selected `m3-release.yml` build jobs and exact-run artifacts; OpenJDK sanity evidence | PENDING for final candidate; a packaging test is not a platform build |
| Other architecture/mobile claims | Explicit source-built runtime and target execution receipts for each claimed platform | No qualification asserted by this preparation |
| Performance | Same-host baseline/candidate measurements with raw CPU/heap/native-memory results | NOT RUN; no superiority claim |

## Non-claims

Each of these is `NOT_CLAIMED` in the contract, and this preview does not make it. Its
upgrade gates are listed in `claim_upgrade_gates`.

| Contract claim | State | Upgrade requires |
| --- | --- | --- |
| `java_lang_string_backed_by_m3_string` | NOT_CLAIMED. `make/m3/README.md`: the P0 foundation does not modify `java.lang.String`. | product build, relevant jtreg, interpreter/C1/C2, GC matrix, CDS, UTF-16 and unpaired-surrogate contract, substring/equals/hash/compare/intern contract |
| `full_regex_precompute` | NOT_CLAIMED | 10,000+ regex cases against the `java.util.regex` oracle, cold vs warm, sealed cache keys, invalidation and corruption refusal, JNI parity where used |
| `full_jtreg_compatibility` | NOT_CLAIMED | a recorded jtreg run on a built image |
| `performance_superiority_over_openjdk21` | NOT_CLAIMED | same-host A/B against the exact baseline with raw data ([spec](BENCHMARK_AND_COMPATIBILITY_SPEC.md)) |
| `arrays_qualified`, `collections_qualified`, `swt_eclipse_ide_qualified` | NOT_CLAIMED | their own target evidence after the String and regex gates |
| TornadoVM optional runtime | `CONDITIONAL_ON_EXACT_MATCHED_IMAGE_RECEIPT`; no matched image receipt exists | the ten `optional_runtime_capabilities.tornadovm.required_gates` |
| Android and iOS JDKs | no JDK build lane exists (audit blocker 14) | a mobile build lane; the donor survey is in `audit-20261030/` |

No binary JDK image is a release claim of this preview. JDK bundles are attached only if the
OpenJDK sanity workflow (`main.yml`) completes green at the release commit.

## Claims and mandatory gates

`immutable_text_foundation` remains
`IMPLEMENTED_WITH_REPOSITORY_TEST_SURFACE`. Full String backing, full regex
precompute, full jtreg compatibility, performance superiority, qualified arrays,
qualified collections and SWT/Eclipse integration remain `NOT_CLAIMED`.
Optional TornadoVM remains conditional on its exact matched-image gates.

All eleven `mandatory_release_gates` need PASS receipts for the final commit and
tree: rights/license; source/upstream; OpenJDK sanity; foundation; Java parity;
JNI/native parity; canonical Maven recipe execution; String UTF-16; changed-atom
instrumented line/branch coverage; release claims; and proof-pack SHA-256. The verifier checks artifact identity and
manifest structure; reviewers must confirm the command's contract coverage.
A gate's existence or unit test is not its execution evidence.

Release remains on HOLD until every gate, source cleanliness, retained-source
review, standalone dependency boundary and legacy-recipe custody requirement
passes. The existing single-fork-commit history requirement is preserved and
must be resolved through explicit policy/ancestry review, not history rewriting
performed by this preparation. No pending-gate publication is admitted here.

## Rights and historical evidence

First-party M3/Synexia expression retains Apache-2.0 and
Copyright 2026 Hitesh Soliwal and contributors. See
[LICENSE-M3-APACHE-2.0.txt](../../../LICENSE-M3-APACHE-2.0.txt),
[M3-SYNEXIA-NOTICE.md](../../../M3-SYNEXIA-NOTICE.md), and
[M3-SYNEXIA-RIGHTS.md](../../../M3-SYNEXIA-RIGHTS.md). OpenJDK-derived source and
patch data retain the applicable GPL-2.0/Classpath Exception terms; recipe
licensing does not relicense their output.

Dated audit and queue receipts remain historical evidence at their recorded
source revisions. Old PR numbers, queued-run counts, platform failures and
secret setup instructions are not current release prerequisites. The
[current candidate checklist](RELEASE_CHECKLIST_CANDIDATE_2756.md) defines preparation and
admission steps without assuming those historical observations still apply.
