<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3 indexed primitive projection — 2026-10-17

## One bounded, additive semantic atom

**Canonical recipe owner:** `hsoliwal/com.synexia`, `synexia-m3-recipe/recipes/m3-long-index-projection-20261017/`.
**Product receiver:** `hsoliwal/M3jdk21`, `m3/collections` as a separately built Apache-2.0 module. This task must not modify `java.util`, M3 String/array receiving order, or active JDK/HOTSPOT/JNI ABI.

The implementation snapshots `long[]` once and represents logical sequences with an `int[]` physical-slot permutation plus `from/to` offsets. It supports stable primitive indexed ascending/descending ordering (including duplicate values), half-open metadata slices, and unsigned-byte logical masks through the *existing* `M3IndexMaskCompactor` Java/JNI selector; it creates no `Map.Entry` or per-row object. Input and materialized output arrays never alias immutable backing storage. Mask and order selection are **detached snapshot** transformations, not live JDK-compatible collection wrappers. Physical index tie order is maintained by stable bottom-up mergesort, without overflow-prone comparator subtraction. Concurrent mutation of input during source snapshot is outside the contract.

## Per-file recipe sequence and proof

1. `M3LongIndexProjection.java`: original target ABSENT; generate immutable Java owner; all externally observable methods are additive.
2. `M3LongIndexProjectionSelfTest.java`: original target ABSENT; generate Java 21 regression/differential test. Check 350 randomized trials, signed extremes, duplicate tie ordering, mask alignment, source/metadata isolation, and explicit native-optional refusal/parity.
3. `M3IndexMaskCompactor`: existing target **UNCHANGED**. Reuse its bounded JNI Region implementation and its Java fallback. The new owner does not implement an alternate native filter and does not retain Java heap pointers.

Source recipe: hash-sealed Maven module; `javac --release 21 -Xlint:all -Werror` direct verification is possible without Maven. `--apply` preflights all files, refuses occupied/drifted/symlinked paths, writes only absent targets and is fixed-point/idempotent. `--check` demands exact template-byte identity. An existing-file transformation in any future revision MUST use a qualified OpenRewrite recipe rather than manually patch the generated file.

```sh
mvn -f m3/collections/qualification/m3-long-index-projection-20261017/recipe/pom.xml verify
java -cp m3/collections/qualification/m3-long-index-projection-20261017/recipe/target/classes com.synexia.m3.M3LongIndexProjectionRecipe --check .
# source materialization is `--apply .`, only on an admitted absent receiver
```

The PR workflow builds the existing Java JNI mask kernel with platform JDK headers, checks the recipe fixed point, and runs the primitive oracle twice: without the native library and with it. This is selected-source verification only, not a complete Maven reactor, JDK image, jtreg, JMM, GC, or CPU/retained-heap/large-corpus benchmark. JNI success must come from the CI execution receipt, not assumptions.

## Source / license provenance

- New implementation and recipe: Copyright 2026 Hitesh Soliwal and contributors; Apache-2.0, authored for M3.
- Existing `M3IndexMaskCompactor` and JNI kernel: exact current M3JDK21 owners; unchanged.
- LeetCode / HackerRank / GeeksforGeeks: categories and reference problems only; no submission code copied.
- Apache Commons Collections: Apache-2.0 donor evaluation only; no code copied.
- Algorithm selection, alternatives and classification: `CATALOG.tsv`.

**Phase disposition:** COLLECTIONS_PROVING_GROUND_ONLY; String → arrays → collections production receiving order remains unchanged.
