<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/docs/PMEM_PATH_PROBE_BOUND_CONTRACT.md blob 90e888d8cf3b; recipe m3/optane/recipe/pmem-source-image-20261106/relocate.py v3 -->
# M3 PMEM bounded-probe insertion correction — exact original helper leaf

Canonical source: hsoliwal/com.synexia PR #15356 at 20de3ad8748255bd2cf1de30fd2d3deac25210e1.
Owner: src/main/java/jdk/m3/optane/PmemSourcePathIndex.java (donor m3-pmem-sdk/src/main/java/com/synexia/m3/pmem/PmemSourcePathIndex.java).

## Invariant / observed defect
The optional hash slot table promises a 16-probe bound. Existing construction checks 16 occupied slots, advances to the seventeenth slot, and can insert there; lookup never probes that seventeenth slot. Existing binary-search fallback keeps exact lookup semantics, but violates the construction/probe bound. A 17-key deliberate Java hash collision is a minimal witness.

## Intended correction
Reject the optional hash-table construction when the first 16 slots are occupied; use existing `NO_INDEX` and binary-search fallback. Preserve all output ordinals, null behavior, binary-search fallback, source-image format, JNI exports, snapshots, input keys, and public Java signatures. Do not add new storage owners, native entry points, or per-record object allocations. The same Java/JNI contract tests remain required.

## Independent proof
Use deterministic exact 16- and 17-collision corpora plus an independent `Arrays.binarySearch` oracle for hits/misses. Verify Java 21 `--release 21 -Xlint:all -Werror`, JIT and `-Xint` after the code correction. The original 17-collision case must fail the stated bound, the corrected one must pass. Measure nothing as a speedup claim.

## Donor decisions
LeetCode Two Sum: hash lookup concept only; its pair semantics are not imported. HackerRank Sparse Arrays: exact query/string equality, not frequency semantics. GeeksforGeeks open addressing: collision probes and empty-slot semantics, but reject unbounded probing. GitHub fastutil: primitive collections reference, not a new binary dependency. No donor implementation code is copied.

## Required gates for product admission
Source pre/post SHA-256, canonical Maven/OpenRewrite recipe and fixed-point/source drift tests, compiler, JUnit, JNI/JMOD/JLINK, provenance and CI. Unexecuted gates are HOLD. No auto-merge or M3JDK21 receiving before actual executable evidence.
