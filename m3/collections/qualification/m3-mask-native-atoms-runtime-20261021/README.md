<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Native position atoms: target receiving scope

**PENDING full target acceptance.** This receipt records a bounded refinement of
the existing M3JDK21 native collection owner and its Java regression test.
M3JDK21 remains the integrated SDK. Synexia owns the reusable recipe, source
fixtures, catalogue and convergence evidence.

## Exact runtime change

The receiving base is M3JDK21 PR #1603, commit
`7c0d2493edbf93ac5cb726667ce83046d3519e3e`. The target files are:

| File | Before SHA-256 | After SHA-256 |
| --- | --- | --- |
| `m3/collections/src/main/native/collections/m3_index_mask_compactor.c` | `fb907640aa9615ce28c61bb802da39e6cb16030b9f95aad266aa6c68531b4074` | `58f403c3b2677ecb8c7c36d9e04233760c1477f4d770f431efddc5318edf9ad2` |
| `m3/collections/src/test/java/com/m3/collections/M3MaskPositionAtomsTest.java` | `ABSENT` | `53d22b261f4376f41e735dc5121d3d8f97ee08f4eafdf99e3f615d0330b22548` |

The unchanged Java owner is `com.m3.collections.M3IndexMaskCompactor`, Git blob
`b81b069fb4815f0c78b17119390d7b204f6179d1`. The zero-mask branches fill stable
positions from the existing chunk offset. The required/excluded branch prepares
one tested mask behind the existing overlap refusal. Public Java APIs, all nine
JNI exports, validation, exception/allocation order and the JNI Region schedule
retain their existing contracts. The filtered-wrapper contract repair is excluded.

This target-local mapping refers to the existing
`m3/docs/name-mapping.json` entries `/family_name_mapping/2` and
`/family_name_mapping/3`. Its root document and receiving sequence are unchanged:
STRING is active; subsequent family acceptance remains pending.

## Canonical recipe and application

The recipe is
[`com.synexia.m3.MaskNativeAtomsRuntime20261021`](https://github.com/hsoliwal/com.synexia/blob/ad4ff6084a3b95c9e5dc993eff96efef03ea8f2c/synexia-openrewrite-recipes/recipe-crates/m3-mask-native-atoms-runtime-20261021/README.md),
published through [Synexia #12650](https://github.com/hsoliwal/com.synexia/pull/12650).
Its [two-path target manifest](https://github.com/hsoliwal/com.synexia/blob/ad4ff6084a3b95c9e5dc993eff96efef03ea8f2c/synexia-openrewrite-recipes/recipe-crates/m3-mask-native-atoms-runtime-20261021/TARGETS.json)
and actual source-replay proof bind this scope.

- Canonical immutable revision: `ad4ff6084a3b95c9e5dc993eff96efef03ea8f2c`
- Native-only SDK proof SHA-256: `8b96ff9f844e6c238d2105c55af7b4c37f4628737e8232ca8115a5942dd14bd0`
- Two-file receiver root: `a9af2623aea50708e6506c43ffb484858d5f1740fa550466ea08307a34c733bf`
- Target application: `m3/collections/qualification/m3-mask-native-atoms-runtime-20261021/evidence/copier-apply.receipt.json`
- Exact replay: `m3/collections/qualification/m3-mask-native-atoms-runtime-20261021/evidence/copier-replay.receipt.json`
- GitHub source-blob readback: `m3/collections/qualification/m3-mask-native-atoms-runtime-20261021/source-blobs-readback.json`

These bindings identify the actual canonical publication, target application,
exact replay and GitHub source-blob readback. The blob receipt verifies content
objects before branch publication; final PR file-set verification is a separate
publication check. Full target acceptance remains PENDING.

## Evidence and known inherited failures

The canonical native-only packet executed actual Maven/OpenRewrite source
replay, parser-owner attribution, eight isolated copier cases and exact evidence
reuse. Its original Java/JNI counts are historical execution evidence, bound by
the [native evidence reuse receipt](https://github.com/hsoliwal/com.synexia/blob/ad4ff6084a3b95c9e5dc993eff96efef03ea8f2c/synexia-openrewrite-recipes/recipe-crates/m3-mask-native-atoms-runtime-20261021/evidence/NATIVE_EVIDENCE_REUSE.json);
they are not a fresh target-wide matrix.

### Fresh target Java/JNI execution

After materialization, twelve target-side commands passed against the actual
receiving files: copier check/apply/replay, toolchain observations, strict
Java 21 and C17 builds, export verification and three real JVM runs with
`-Xcheck:jni`. The generated header matches the existing owner and all nine
JNI symbols remain unchanged. Each JVM stderr is empty and each stdout exactly
matches the sealed baseline.

| Fresh target test | Result |
| --- | --- |
| Mask-position oracle | 16,777,216 truth cases; 66,196 vectors; 17,241,106 checks |
| Existing range regression | 7,680 cases; 53,782 checks |
| Existing required/excluded regression | 512 checks |

`evidence/results.json`, `evidence/commands.json` and `evidence/input-custody.json`
retain the execution and exact inputs. `VERIFICATION.md` and
`verification/verify_target_native.py` reproduce this selected-source test.
The prior 899-case JNI effect tape is reused by exact source/receipt hashes;
it was not rerun during this fresh target execution. A selected-source build
does not qualify the full module or an installed M3JDK image.

The two unchanged historical source-seal gates were actually run against their
complete pinned #1603 file inputs:

| Gate | Result before this continuation |
| --- | --- |
| `qualification/verify_mask_selection.py` | Exit 1 at the Java compactor; five of six source pins differ |
| `qualification/m3-index-mask-position-selection-20261018/verify.py` | Exit 1 at the Java compactor; two of three source pins differ |

`baseline-gates/results.json` retains exact commands, diagnostics and every
input comparison. These are bounded source-seal executions, not full repository
or module runs. The historical manifests and verifiers remain unchanged.

The installed donation verifier requires its exact historical source inventory,
qualified successor overlays and complete Java source-set equality. This receipt
does not expand that census or grant admission to unlisted sources. Full collection
Java/JNI/module execution, full reactor, applicable M3JDK image/jtreg,
platform/lifetime/concurrency and CPU/retained-heap/native/process-memory matrices
remain separately pending. The generic Synexia task admission and unresolved donor
prerequisite also remain open. No speedup or global acceptance is claimed.

## Catalogue, license and attribution

The [existing canonical catalogue](https://github.com/hsoliwal/com.synexia/blob/99fe40f05b8c0594427e7b9a062f5fd4791a8734/synexia-openrewrite-recipes/recipe-crates/m3-mask-range-atoms-20261021/catalogue.tsv)
retains the native mask comparison, the existing Java identity-fill donor,
stable-compaction/JNI schedule constraints and rejected challenge assumptions.
Its wrapper-repair rows are outside this target change. Existing target search
records remain under `m3/collections/algorithm-catalog/`; the earlier
fourteen-row review remains [M3JDK21 #1605](https://github.com/hsoliwal/M3jdk21/pull/1605).

Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
This separate Apache-2.0 collection module retains its existing LICENSE, NOTICE
and per-file notices. Its intake is `INDEPENDENT_APACHE_MODULE`; Synexia recipes
are authoring/proof tooling. No Synexia runtime dependency is introduced.
Challenge/editorial pages are problem references, with no solution-body copying
permission inferred. OpenJDK and third-party rights remain per file.

`RECEIVING.json` is target mapping/evidence. It is not a second recipe owner,
an automatic admission mechanism or a modification of the global receiving plan.
