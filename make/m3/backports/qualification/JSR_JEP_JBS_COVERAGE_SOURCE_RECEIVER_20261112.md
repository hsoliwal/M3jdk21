<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# JSR/JEP/JBS 22–27 coverage: source-exact two-file receiving candidate (2026-11-12)

**DRAFT / NOT ADMITTED / DO NOT MERGE WITHOUT EXECUTION EVIDENCE.**

M3JDK21 is the integrated JDK 21 product. This receiver contains only two
target-source outputs and this passive source/acceptance receipt, **not** the
canonical Maven/OpenRewrite recipe, source templates or recipe executor.

## Frozen source evidence

- Target baseline: `hsoliwal/M3jdk21 master@b98bf8c065818aac3447be165909e2a013325d9a`.
- Canonical private source: `hsoliwal/com.synexia`;
  merged [#14996](https://github.com/hsoliwal/com.synexia/pull/14996).
- Existing recipe descriptor:
  `synexia-openrewrite-recipes/src/main/resources/META-INF/rewrite/m3jdk21-jsr-jep-jbs-full-coverage-20261031.yml`
  Git blob `00721ce99b0fe320c37001d64d3a6de5f9d9d7ba`.
- Existing two-path manifest Git blob:
  `ece7314fe5bbf3f8137c28cf2214a9b2e01813c7`.
- The four existing canonical JBS parser/test postimages
  (`compatibility_queue.py`, `inventory.py`, `test_compatibility_queue.py`,
  `test_inventory.py`) were independently compared byte-for-byte to M3JDK21
  master at this baseline: **4/4 already present; no re-import**.

| Target source (absent on baseline) | Expected SHA-256 | Git blob verified on receiver branch |
| --- | --- | --- |
| `make/m3/backports/jsr_jep_jbs_coverage.py` | `e3c1f8828a04d42e6a262d8e60a5b37ad20ef684c17ae009084cd52599653cf0` | `4a968eb57739cf1da0470bcc255c54ba49c22666` |
| `make/m3/backports/test_jsr_jep_jbs_coverage.py` | `425f5308faa4dc5a3d199620ef87ab4c10345b47933bc2b1ac927d280ef75129` | `37fe81de37eb1b50089ed65dfdb7b38a16ba456d` |

Both receiving Git blobs and complete UTF-8 contents matched the exact
canonical source postimage resources upon independent GitHub readback.

## What the source does (NOT a blanket backport)

The new coverage script joins existing platform JSR 397–402, 85 catalogued
JDK 22–27 JEPs, the GA commit candidate denominator and source-subject JBS
candidate index. It reuses existing `inventory`, `compatibility_queue`,
`jep_source_witness` and `release_jep_authority` owners, preserves Java 21
grammar/class-file 65 and never promotes any feature or JBS fix. A commit
subject is not a verified JBS issue.

## Mandatory open gates

| Gate | Current evidence |
| --- | --- |
| Six original/current input file Git blob comparisons | 4/4 original present; two absent-before outputs exact on branch |
| Exact canonical Maven/OpenRewrite apply, fixed point and drift refusal | **NOT PASSED / HOLD** |
| Java 21 Maven/JUnit and source-integrated Python tests | **NOT PASSED / HOLD** |
| Complete authentic upstream Git GA-tag inventory replay | **NOT RUN / HOLD** |
| JBS issue database identity and feature-level backports | **NOT VERIFIED / HOLD** |
| Current M3JDK21 source-built image, Java 21 ABI, jtreg, JNI, HotSpot and jlink | **NOT RUN / HOLD** |

The dedicated source workflow [Synexia #17195](https://github.com/hsoliwal/com.synexia/pull/17195)
had its first [Actions run](https://github.com/hsoliwal/com.synexia/actions/runs/43230405724)
fail with zero executed steps. A rerun was explicitly requested; its eventual
result must be checked independently. A queued or zero-step run is **not** a PASS.

**Admission rule:** this candidate PR is for review of the exact generated
source bytes and nothing more. Do not merge, tag, release or describe this
source as qualified until the canonical Maven/OpenRewrite recipe *actually*
executes and the independent target-required tests and product acceptance
gates pass. Reconcile all changed paths and attach per-gate evidence.
