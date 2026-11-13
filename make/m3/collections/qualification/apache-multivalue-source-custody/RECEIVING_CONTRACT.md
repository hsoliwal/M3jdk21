<!-- SPDX-License-Identifier: Apache-2.0 -->
# Original M3JDK21 Apache multivalue V5 current-JNI source receiving

This document precedes source transfer on this **isolated** branch, stacked on open M3JDK21 compiler qualification #2308 at exact source commit `25ae6e7e7f209f67d807ef20d53ef9499d322d62`. Canonical Synexia source authority is draft [#15483](https://github.com/hsoliwal/com.synexia/pull/15483), which reuses prior original Synexia recipes #15126, #15135, #15148. Do not alter product Java/JNI classes, original V3/V4 history or `master`.

## Scope: one workflow, three additive passive V5 source files

The exact existing workflow `.github/workflows/m3-owned-apache-multivalue.yml` preimage Git blob `6970a9bcfdc9c5e0d846400689a73e812bb7e3c2` (SHA256 `6ddd49fc642dca308ea06505b6c2188ee4eb651160f0a664e38626d634762423`) is to be replaced only by the qualified canonical Synexia template.

The stable undated directory `m3/collections/qualification/apache-multivalue-source-custody/` is the receiving output for three previously absent verifier/receipt leaves. This avoids the correctly enforced public JDK naming policy's rejection of date-stamped *new* paths; no policy bypass is authorized.

The original 3397-byte V5 JSON receipt is **byte-identical** to historical #2265 source, Git blob `ac361caf86326a25185d917c26822656fce53819`, SHA256 `5498404216725b2a8742599a83f820867a615edf000b4b39d34f845ca4301249`. The original Python V5 verifier and negative probe are reused, only module/import paths migrated from the historical V4 dated directory to the stable successor directory. Original V4 receipt and verifier, V3/V2 history, first-party source ownership, original source checks and negative tamper/bom/crlf/revert tests remain intact.

## Actual historical source identity, not inferred runtime equivalence

Three product source postimages exactly match historical V5:
- `M3ArrayList.java` — `3c6e1b12a40d5516d8b1ca0c8ed9fc86c99003f1` (LC27 stable compaction category).
- `M3LongRangeSet.java` — `9bb208d014ce7e5d6a02577e40bf021338de8fa4` (LC704 bounded lower-bound category).
- `m3_index_mask_compactor.c` — `5afeb41575e758b06882b3e3d8104b0c275f50ca` (GeeksforGeeks mask/rank category).
All 7 other V4 successor Git blobs matched current source; the entire original 31-class Apache production directory and Apache test directories matched the historical V5 branch Git blob identities. Contest solution code was never copied. HackerRank frequency queries were rejected as a non-equivalent problem contract.

## Required native source lifecycle

Build the **existing** `m3/arrays/src/main/native/CMakeLists.txt` C11 JNI library `libm3_arrays.so` with unchanged source and retained `m3/native/include/m3_jni_core.h`. The original `M3ArrayNative.allocateObjectsRequired` is intentionally required-native and must throw `UnsatisfiedLinkError` without `m3.arrays.native.path`; never add a fallback. Provide the absolute compiled path only for the original full factory and Apache contract tests. Keep original two optional/absent-mask SDK runs unmodified. Retain original strict javac, C17 JNI mask build, Maven source receiving, no test suppression and negative custody probes. The original source `M3ArrayNative.load(Path)` already exists; do not replace it.

## Execution gates

1. Source receipt and exact before/after Git blobs.
2. Python strict original V5 verification and **57 negative / two positive** probe cases (no source modifications in checkout).
3. Strict actual Java21 compiler and real C17 native compactor build.
4. Original CMake `m3_arrays` native compile and required-owner selftest in default and `-Xint` under `-Xcheck:jni`.
5. Original Apache counter/factory/SDK tests (no skipped tests), and Maven/OpenRewrite SDK first-apply/fixed-point/drift gate.
6. Hosted exact-head outcomes and release admission separately.

The earlier historical V5 job passed source custody and two 1798-check SDK modes but then failed with `UnsatisfiedLinkError` from the *missing* arrays JNI library. Do not call this receiver green merely because source hashes match. Keep **DRAFT/HOLD** until real terminal CI. Independent Guava MinMaxPriorityQueue source custody, full JVM/jtreg/JCK and broad hsoliwal/* all remain separate.

## Public-name-safe filename refinement

The same strict M3JDK21 naming policy also rejects newly introduced `_V5` file **basenames**. Newly created product files must use stable, undated AND unversioned filenames: `SOURCE_BINDING_SUCCESSOR.json`, `probe_source_binding_successor.py`, and `verify_source_binding_successor.py`. The JSON *schema* remains V5, and its **3397 raw bytes / original Git blob do not change**. The verifier's `RECEIPT_NAME` and probe's explicitly joined successor paths change only to locate these files. Historical V4/V5 provenance identifiers and original historic scripts retain their proper original names. This correction does not waive, downgrade or amend `check_public_names.py`.
