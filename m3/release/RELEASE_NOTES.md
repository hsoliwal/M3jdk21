<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3JDK21 21.0.14 m3.1 technical preview

Tag: `m3jdk-21.0.14-m3.1-preview` · Branch: `release/m3jdk-21.0.14-m3.1-preview` ·
Tier: `P0_TECHNICAL_PREVIEW` ([release contract](release-contract.json))

This is a source technical preview of M3JDK21, an OpenJDK 21-derived research runtime. It is
admitted by evidence, not by size or PR count ([release pack](README.md)). Every statement below is
bounded by `release-contract.json`. A claim is publishable only when the proof pack attached to the
GitHub Release contains its exact command, environment, raw result and SHA-256.

## Source identity

| Item | Value |
| --- | --- |
| Upstream repository | `openjdk/jdk21` |
| Upstream baseline (parent of the release commit) | `890adb6410dab4606a4f26a942aed02fb2f55387` (tag `jdk-21+35`, `jdk-21-ga`) |
| Release history | one fork commit above the baseline (`single_fork_commit_required`); immutable after the tag |
| Version in the tree | `make/conf/version-numbers.conf`: 21.0.14 (`DEFAULT_VERSION_UPDATE=14`); the jdk21u updates are part of the tree, not of the ancestry |
| Release tree | the master tree after the release-preview PR. The release commit, the tree and the evidence manifest are listed in the GitHub Release. |

Verify a checkout:

```sh
git rev-parse HEAD^ HEAD^{tree}          # parent must be 890adb6410dab4606a4f26a942aed02fb2f55387
python3 -I m3/release/verify_release_admission.py --root . --release \
  --evidence /absolute/path/to/evidence.json --json-out admission.json
```

## Claims

| Contract claim | State |
| --- | --- |
| `immutable_text_foundation` | `IMPLEMENTED_WITH_REPOSITORY_TEST_SURFACE`. This means: an independently authored immutable-text and precompute foundation (`m3/core`, built and tested by `m3/build.sh` in JIT, `-Xint`, `-XX:-CompactStrings` and C2-only modes); product verification machinery; Java precompute differential tests and JNI/native parity tests (`m3/ports/precompute`); a deterministic release contract, a rights and provenance record and a proof-pack checksum process. |

Rights: first-party M3/Synexia material is under [LICENSE-M3-APACHE-2.0.txt](../../LICENSE-M3-APACHE-2.0.txt)
with the boundary in [M3-SYNEXIA-NOTICE.md](../../M3-SYNEXIA-NOTICE.md) and
[M3-SYNEXIA-RIGHTS.md](../../M3-SYNEXIA-RIGHTS.md). OpenJDK and JDK-derived source keeps its
GPL-2.0 with Classpath Exception terms and is not relicensed.

## Non-claims

Each of these is `NOT_CLAIMED` in the contract, and this preview does not make it. Its
upgrade gates are listed in `claim_upgrade_gates`.

| Contract claim | State | Upgrade requires |
| --- | --- | --- |
| `java_lang_string_backed_by_m3_string` | NOT_CLAIMED. `m3/README.md`: the P0 foundation does not modify `java.lang.String`. | product build, relevant jtreg, interpreter/C1/C2, GC matrix, CDS, UTF-16 and unpaired-surrogate contract, substring/equals/hash/compare/intern contract |
| `full_regex_precompute` | NOT_CLAIMED | 10,000+ regex cases against the `java.util.regex` oracle, cold vs warm, sealed cache keys, invalidation and corruption refusal, JNI parity where used |
| `full_jtreg_compatibility` | NOT_CLAIMED | a recorded jtreg run on a built image |
| `performance_superiority_over_openjdk21` | NOT_CLAIMED | same-host A/B against the exact baseline with raw data ([spec](BENCHMARK_AND_COMPATIBILITY_SPEC.md)) |
| `arrays_qualified`, `collections_qualified`, `swt_eclipse_ide_qualified` | NOT_CLAIMED | their own target evidence after the String and regex gates |
| TornadoVM optional runtime | `CONDITIONAL_ON_EXACT_MATCHED_IMAGE_RECEIPT`; no matched image receipt exists | the ten `optional_runtime_capabilities.tornadovm.required_gates` |
| Android and iOS JDKs | no JDK build lane exists (audit blocker 14) | a mobile build lane; the donor survey is in `audit-20261030/` |

No binary JDK image is a release claim of this preview. JDK bundles are attached only if the
OpenJDK sanity workflow (`main.yml`) completes green at the release commit.

## Mandatory gates

All ten gates in `mandatory_release_gates` need a receipt bound to the release commit and tree in
the `M3_RELEASE_EVIDENCE_V1` manifest. The GitHub Release lists each gate's state, PASS or PENDING,
with its producer.

| Gate | Producer |
| --- | --- |
| `RIGHTS_AND_LICENSE_BOUNDARY`, `SOURCE_TREE_AND_UPSTREAM_BASELINE`, `RELEASE_CLAIMS_VALIDATION`, `PROOF_PACK_SHA256` | `verify_release_admission.py` (local or `m3-first-release-admission.yml` job `contract`) |
| `M3_FOUNDATION_TESTS` | `bash ./m3/build.sh` (job `foundation`) |
| `PRECOMPUTE_JAVA_DIFFERENTIAL` | `mvn -B -ntp -f m3/ports/precompute/pom.xml test` (job `precompute`) |
| `PRECOMPUTE_JNI_NATIVE_PARITY` | `mvn -B -ntp -f m3/ports/precompute/pom.xml -Pnative test` (job `precompute`) |
| `OPENJDK_SANITY_MATRIX` | `.github/workflows/main.yml` (OpenJDK GHA Sanity Checks), CI only |
| `M3_MAVEN_CONTROL_REACTOR` | `hsoliwal/com.synexia` canonical recipe execution (owner per contract); imported receipt |
| `STRING_UTF16_CONTRACT` | no workflow job or command is bound to this gate yet |

## Platform matrix

These are the CI facts as of 2026-10-31 for master `bb66a8c16642`. "Pending" means no completed
run exists at the release commit.

| Platform | JDK build lane | Latest evidence | Preview state |
| --- | --- | --- | --- |
| linux-x64 | `main.yml` -> `build-linux.yml`, tier1 `test.yml` | Last green build: run 39250083857 (`bb6b279495ce`, 2026-10-19, another branch). `main.yml` creates jobs again (run 41153588755 at `bb66a8c16642`: 17 jobs, cancelled by concurrency). | build pending |
| linux-x86 | `build-linux.yml` (hs) | run 39250083857 | build pending |
| linux-aarch64, ppc64le, riscv64, s390x, arm | `build-cross-compile.yml` (cross build only, no tests) | run 39250083857 | cross build pending |
| windows-x64 | `build-windows.yml`, tier1 | run 39250083857 | build pending |
| windows-aarch64 | `build-windows.yml` (HotSpot only, cross) | run 39250083857 | build pending |
| macos-x64 | `build-macos.yml`, tier1 | run 39250083857 | build pending |
| macos-aarch64 | `build-macos.yml` (cross from macos-15-intel) | run 39250083857 | build pending |
| android-arm64 | none (only the C11/JNI header compile in `m3-jni-common.yml`) | run 41019153845: success | not claimed |
| ios-arm64 | none (only the C11 source cross-compile in `m3-jni-common.yml`) | run 41019153845: success | not claimed |
| C11 JNI contract (windows-2022, ubuntu-24.04, ubuntu-24.04-arm, macos-15, macos-15-intel) | `m3-jni-common.yml` | run 41019153845: all jobs success | header contract only |

## Known issues

- **Platform builds:** no completed OpenJDK sanity run exists since 2026-10-19. The Actions queue
  held 305 queued runs on 2026-10-31, so `OPENJDK_SANITY_MATRIX` is PENDING.
- **Legacy recipe custody:** `legacy_recipe_custody` is HOLD. 811 executable candidates remain
  under the legacy recipe roots. Their deletion depends on the Synexia importer repoint. Until it
  lands, `verify_release_admission.py --release` cannot report `release_ready: true`.
- **Synexia-owned gates:** the `M3_MAVEN_CONTROL_REACTOR` receipt is executed only in
  `hsoliwal/com.synexia`. `m3-a3.yml` needs the `SYNEXIA_READ_TOKEN` repository secret to read
  the private pin.
- **`STRING_UTF16_CONTRACT`:** this gate has no bound producer.
- **Tokenizer image workflow:** `M3JDK21 tokenizer source-built image and jtreg` stops before the
  build, at the `get-jtreg` step: the artifact `bundles-jtreg-8.2.1+1` is not found (last run 40992710079,
  2026-10-30). The earlier doclint failure in `TokenMatcher.java`
  (blocker 7) is fixed and checked with `javac -Xdoclint:all/protected -Werror`.
- **Generated-mirror drift (audit blocker 13 a, b):** `M3StateMaskHistogram.java` and
  `M3SemanticHasher.java` pins still drift. Successor patches are prepared and not merged.
- **Admission workflow trigger (audit blocker 5):** `m3-first-release-admission.yml` runs on
  `release/**` pushes, pull requests and dispatch, but not on master pushes.
- **Compile audit:** `javac --release 21 -Xlint:all,-module -Werror` over the 27 audited `m3/**`
  modules gives 16 PASS. The other 10 fail only on dependencies that are not on the probe
  classpath:
  - Jackson, Lucene/OpenNLP, OpenRewrite, JavaFX and TornadoVM API;
  - `jdk.internal.vm.parallel`, which needs the built image;
  - intra-repository parser closure.

  None of the 27 audited modules has a source-level compile error.
- **Swing viewport serialization:** `M3PackedTableModel` throws `NotSerializableException` when
  it holds a column (`M3PackedColumn` is not `Serializable`). This was already true before this
  release.
- **Contract field:** `release-contract.json` `release.release_branch` still names
  `release/first-clean-boundary-20261007`. That branch was never created, and the verifier does
  not read the field.
