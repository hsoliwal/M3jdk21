<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Executed receiving evidence

This directory retains byte-exact executed receipts. [EVIDENCE_INDEX.tsv](EVIDENCE_INDEX.tsv) gives every copied artifact's SHA-256 and original location. The metadata summaries are derived from those receipts; no gate is inferred from a plan.

## Primary records

| Record | Executed result | Scope |
| --- | --- | --- |
| [SDK_ACCEPTANCE.json](SDK_ACCEPTANCE.json) | 5 SDK tests, 8 sealing cases, 9 isolated receiver cases per dataset | R2: 28/0; bridge: 1/0; proof2: 3/0 actual first/repeat changes |
| [received/RECEIPT.json](received/RECEIPT.json) | PASS_SCOPED; 41 successful commands | Actual 2f receiver, 337 sealed input rows |
| [received/MAVEN_TESTS.json](received/MAVEN_TESTS.json) | 255 tests; zero failures/errors/skips | Complete collections module test goal; executed owners and XML hashes retained |
| [received/API_COMPARISON.json](received/API_COMPARISON.json) | No existing public/protected member removed or changed; no new type | Four classified member additions; before/after descriptor dumps retained |
| [received/JNI_ABI.json](received/JNI_ABI.json) | All old prototypes retained; long path entry additive | Generated headers, exported symbols and two local C17/UBSan libraries |
| [long-selftest/actual-long-selftest.json](long-selftest/actual-long-selftest.json) | 7,127 checks / 1,500 randomized cases | Existing long facade self-test on actual compiled receiver; source/class/toolchain recheck |
| [long-bridge/proof/GATES.json](long-bridge/proof/GATES.json) | Existing test passes before/after; 1,259 identical behavior observations; identical public API | Separate bridge candidate qualification before actual receiving |
| [performance/RESULTS.json](performance/RESULTS.json) | 262,064 → 131,048 invoking-thread allocated bytes/call | Exact 32,750-entry sorted-direction workload; allocation only |
| [historical/HISTORICAL_DONATION_PREFLIGHT.json](historical/HISTORICAL_DONATION_PREFLIGHT.json) | Static admission refuses baseline and receiver; manifest seal separately passes | Full historical verifier and exact JUnit 1.12.2 NOT EXECUTED |

## Maven and runtime scope

The module summary records 255 tests across 89 executed JUnit owners, with zero failures, errors and skips. The [complete Maven log](received/015-collections-full-module-tests.stdout.log) and report hashes identify what executed. The actual module used its source-built JUnit Jupiter 5.10.2 / Platform 1.10.2 dependencies, bound by artifact hashes in MAVEN_TESTS.json. This does not execute the distinct historical verifier's exact JUnit 1.12.2 requirement.

Standalone owners without dedicated JUnit wrappers are explicitly invoked below. Native runs use `-Xcheck:jni`; the two libraries are built with C17/UBSan, and missing-library refusal is an independent Java process. Before/after source and compiled API dumps, generated JNI headers, full source manifests and final input rechecks remain separately recorded.

## Actual command log index

| Command | Status | Observed check counts | Logs |
| --- | --- | --- | --- |
| `java-version` | PASS | — | [stdout](received/001-java-version.stdout.log) · [stderr](received/001-java-version.stderr.log) |
| `javac-version` | PASS | — | [stdout](received/002-javac-version.stdout.log) · [stderr](received/002-javac-version.stderr.log) |
| `javap-version` | PASS | — | [stdout](received/003-javap-version.stdout.log) · [stderr](received/003-javap-version.stderr.log) |
| `maven-version` | PASS | — | [stdout](received/004-maven-version.stdout.log) · [stderr](received/004-maven-version.stderr.log) |
| `gcc-version` | PASS | — | [stdout](received/005-gcc-version.stdout.log) · [stderr](received/005-gcc-version.stderr.log) |
| `nm-version` | PASS | — | [stdout](received/006-nm-version.stdout.log) · [stderr](received/006-nm-version.stderr.log) |
| `after-strict-java21` | PASS | — | [stdout](received/007-after-strict-java21.stdout.log) · [stderr](received/007-after-strict-java21.stderr.log) |
| `after-javap` | PASS | — | [stdout](received/008-after-javap.stdout.log) · [stderr](received/008-after-javap.stderr.log) |
| `before-strict-java21` | PASS | — | [stdout](received/009-before-strict-java21.stdout.log) · [stderr](received/009-before-strict-java21.stderr.log) |
| `before-javap` | PASS | — | [stdout](received/010-before-javap.stdout.log) · [stderr](received/010-before-javap.stderr.log) |
| `libm3_collections_pathmask.so-gcc-c17-ubsan` | PASS | — | [stdout](received/011-libm3_collections_pathmask.so-gcc-c17-ubsan.stdout.log) · [stderr](received/011-libm3_collections_pathmask.so-gcc-c17-ubsan.stderr.log) |
| `libm3_collections_pathmask.so-nm` | PASS | — | [stdout](received/012-libm3_collections_pathmask.so-nm.stdout.log) · [stderr](received/012-libm3_collections_pathmask.so-nm.stderr.log) |
| `libm3_collections_mask.so-gcc-c17-ubsan` | PASS | — | [stdout](received/013-libm3_collections_mask.so-gcc-c17-ubsan.stdout.log) · [stderr](received/013-libm3_collections_mask.so-gcc-c17-ubsan.stderr.log) |
| `libm3_collections_mask.so-nm` | PASS | — | [stdout](received/014-libm3_collections_mask.so-nm.stdout.log) · [stderr](received/014-libm3_collections_mask.so-nm.stderr.log) |
| `collections-full-module-tests` | PASS | 255 JUnit tests; see complete module log for internal assertion counts | [stdout](received/015-collections-full-module-tests.stdout.log) · [stderr](received/015-collections-full-module-tests.stderr.log) |
| `M3IndexPathMasksSelfTest-java` | PASS | 1554 | [stdout](received/016-M3IndexPathMasksSelfTest-java.stdout.log) · [stderr](received/016-M3IndexPathMasksSelfTest-java.stderr.log) |
| `M3IndexPathMasksLongSelfTest-java` | PASS | 3088 | [stdout](received/017-M3IndexPathMasksLongSelfTest-java.stdout.log) · [stderr](received/017-M3IndexPathMasksLongSelfTest-java.stderr.log) |
| `M3IndexPathMaskAliasSelfTest-java` | PASS | 185 | [stdout](received/018-M3IndexPathMaskAliasSelfTest-java.stdout.log) · [stderr](received/018-M3IndexPathMaskAliasSelfTest-java.stderr.log) |
| `M3IndexPathImpossibleStageSelfTest-java` | PASS | 14738 | [stdout](received/019-M3IndexPathImpossibleStageSelfTest-java.stdout.log) · [stderr](received/019-M3IndexPathImpossibleStageSelfTest-java.stderr.log) |
| `M3IndexPathZeroStagesSelfTest-java` | PASS | 11102 | [stdout](received/020-M3IndexPathZeroStagesSelfTest-java.stdout.log) · [stderr](received/020-M3IndexPathZeroStagesSelfTest-java.stderr.log) |
| `M3IndexPathZeroWindowSelfTest-java` | PASS | 22258 | [stdout](received/021-M3IndexPathZeroWindowSelfTest-java.stdout.log) · [stderr](received/021-M3IndexPathZeroWindowSelfTest-java.stderr.log) |
| `M3IndexPathMaskWindowSelfTest-java` | PASS | 22462 | [stdout](received/022-M3IndexPathMaskWindowSelfTest-java.stdout.log) · [stderr](received/022-M3IndexPathMaskWindowSelfTest-java.stderr.log) |
| `M3IndexMaskWindowSelfTest-java` | PASS | 15298 | [stdout](received/023-M3IndexMaskWindowSelfTest-java.stdout.log) · [stderr](received/023-M3IndexMaskWindowSelfTest-java.stderr.log) |
| `M3OrderWindowProof-java-common` | PASS | 621981 | [stdout](received/024-M3OrderWindowProof-java-common.stdout.log) · [stderr](received/024-M3OrderWindowProof-java-common.stderr.log) |
| `M3IndexPathMasksSelfTest-native---native` | PASS | 2324 | [stdout](received/025-M3IndexPathMasksSelfTest-native---native.stdout.log) · [stderr](received/025-M3IndexPathMasksSelfTest-native---native.stderr.log) |
| `M3IndexPathMasksLongSelfTest-native---native` | PASS | 6162 | [stdout](received/026-M3IndexPathMasksLongSelfTest-native---native.stdout.log) · [stderr](received/026-M3IndexPathMasksLongSelfTest-native---native.stderr.log) |
| `M3IndexPathMaskAliasSelfTest-native---native` | PASS | 186 | [stdout](received/027-M3IndexPathMaskAliasSelfTest-native---native.stdout.log) · [stderr](received/027-M3IndexPathMaskAliasSelfTest-native---native.stderr.log) |
| `M3IndexPathImpossibleStageSelfTest-native---native` | PASS | 16792 | [stdout](received/028-M3IndexPathImpossibleStageSelfTest-native---native.stdout.log) · [stderr](received/028-M3IndexPathImpossibleStageSelfTest-native---native.stderr.log) |
| `M3IndexPathZeroStagesSelfTest-native---native` | PASS | 12645 | [stdout](received/029-M3IndexPathZeroStagesSelfTest-native---native.stdout.log) · [stderr](received/029-M3IndexPathZeroStagesSelfTest-native---native.stderr.log) |
| `M3IndexPathZeroWindowSelfTest-native---native` | PASS | 22258 | [stdout](received/030-M3IndexPathZeroWindowSelfTest-native---native.stdout.log) · [stderr](received/030-M3IndexPathZeroWindowSelfTest-native---native.stderr.log) |
| `M3IndexPathMaskWindowSelfTest-native---native` | PASS | 22462 | [stdout](received/031-M3IndexPathMaskWindowSelfTest-native---native.stdout.log) · [stderr](received/031-M3IndexPathMaskWindowSelfTest-native---native.stderr.log) |
| `M3IndexMaskWindowSelfTest-native---native` | PASS | 30596 | [stdout](received/032-M3IndexMaskWindowSelfTest-native---native.stdout.log) · [stderr](received/032-M3IndexMaskWindowSelfTest-native---native.stderr.log) |
| `M3OrderWindowProof-native-common-jni` | PASS | 758662 | [stdout](received/033-M3OrderWindowProof-native-common-jni.stdout.log) · [stderr](received/033-M3OrderWindowProof-native-common-jni.stderr.log) |
| `M3IndexPathMaskAliasSelfTest-java---missing` | PASS | 181 | [stdout](received/034-M3IndexPathMaskAliasSelfTest-java---missing.stdout.log) · [stderr](received/034-M3IndexPathMaskAliasSelfTest-java---missing.stderr.log) |
| `M3FilteredNavigationBoundarySelfTest-java` | PASS | 16429309 | [stdout](received/035-M3FilteredNavigationBoundarySelfTest-java.stdout.log) · [stderr](received/035-M3FilteredNavigationBoundarySelfTest-java.stderr.log) |
| `M3ReadOnlyViewBoundarySelfTest-java` | PASS | 32267 | [stdout](received/036-M3ReadOnlyViewBoundarySelfTest-java.stdout.log) · [stderr](received/036-M3ReadOnlyViewBoundarySelfTest-java.stderr.log) |
| `M3WrapperConvergenceSelfTest-java` | PASS | 35 | [stdout](received/037-M3WrapperConvergenceSelfTest-java.stdout.log) · [stderr](received/037-M3WrapperConvergenceSelfTest-java.stderr.log) |
| `M3DeepReadOnlyCollectionSelfTest-java` | PASS | 84 | [stdout](received/038-M3DeepReadOnlyCollectionSelfTest-java.stdout.log) · [stderr](received/038-M3DeepReadOnlyCollectionSelfTest-java.stderr.log) |
| `M3NavigationFailureContract-java` | PASS | 140 | [stdout](received/039-M3NavigationFailureContract-java.stdout.log) · [stderr](received/039-M3NavigationFailureContract-java.stderr.log) |
| `M3BoundedFilteredNavigationContract-java` | PASS | 90431 | [stdout](received/040-M3BoundedFilteredNavigationContract-java.stdout.log) · [stderr](received/040-M3BoundedFilteredNavigationContract-java.stderr.log) |
| `M3OrderWindowProof-java-nullable` | PASS | 13 | [stdout](received/041-M3OrderWindowProof-java-nullable.stdout.log) · [stderr](received/041-M3OrderWindowProof-java-nullable.stderr.log) |

## Interpretation

The original candidate records remain under `candidate-proof/` one directory above. Their exact callback tapes and protected comparator probe have a different source epoch from this actual receiving run; they are not silently relabelled. The unchanged receiver runner makes no performance claim. Allocation is established only by the independently frozen matched proof packet under `performance/`, whose own source/class seals and raw samples are retained.

Copies of the long-bridge and proof2 qualification metadata retain their original preintegration status wording. Current actual integration is resolved by the sibling `FINAL_RECEIVING_STATUS.json`, not by rewriting those earlier receipts. All source and recipe publication custody resolves through the externally bound `SOURCE_PIN.json`; no runtime service or dependency on Synexia is introduced.
