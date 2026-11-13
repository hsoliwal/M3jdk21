<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3 long bitmap filter plan — canonical Synexia recipe (2026-10-17)

**Owner:** Synexia remains the canonical recipe/convergence workspace. The M3JDK21 `m3/collections` module is the separately built Apache-2.0 public runtime owner. Preserve the product's String → arrays → collections receiving order; this is an opt-in proving-ground addition, not JDK `java.util` replacement.

## Semantically bounded operation

The new `M3LongBitmapFilterPlan` is an immutable algebra over **logical occurrence positions** in an existing `M3LongIndexProjection`. It evaluates each predicate once and stores the results in a dynamically grown `BitSet`, never allocating `BitSet(Integer.MAX_VALUE)`. Duplicate values and repeated physical addresses remain distinct occurrences. AND/OR/XOR/AND-NOT and bounded complement compose cached masks without rerunning predicate callbacks.

Its Java materialization and ascending/descending stable index permutations are memoized. Logical slicing shares the unchanged snapshot payload and physical index vectors. It copies imported `BitSet` values and rejects composition between distinct source projection identities. Java `LongPredicate` calls are not sent to native code: explicit `NATIVE_REQUIRED` reuses only existing `M3IndexMaskCompactor` Java/C17 selection ABI and never silently falls back. The existing live filtered wrappers and mutation/revision contracts are left untouched.

## Recipe-first production admission

The **only** two generated source postimages are sealed templates under `src/main/resources/templates/`; the Maven-bound `M3LongBitmapFilterPlanRecipe` preflights all paths, SHA-256, occupation, drift and symlink refusal before writing. Generation is idempotent with a verified fixed point. This task adds files rather than rewriting a Java AST; any later existing-owner transformation should use an OpenRewrite AST rule with a new exact preimage seal.

Local Java 21 `javac --release 21 -proc:none -Xlint:all -Werror`: PASS **637,172** deterministic differential assertions (450 randomized trials), including duplicate occurrence identity, bitwise algebra, sorted wrappers, predicate-call counts, bounds, immutable arrays, and parallel stream splitting. Local checked JNI / strict C17 UBSan backend: PASS the same **637,172** assertions using a retained real backend. Generator direct-Java selftest: PASS missing/drift refusal, repeat apply, fixed-point, exact emitted-byte comparison.

Local Maven was not installed. GitHub head Maven/SDK, full receiving module, whole JDK/jtreg, cross-platform ABI, heap and performance measurements are **PENDING**; no universal speedup or phase-promotion claim.

All new source, recipe, and tests Copyright 2026 Hitesh Soliwal and contributors, Apache-2.0. LeetCode, HackerRank, GeeksforGeeks and other donors in `CATALOG.tsv` are category/reference evidence only; no third-party solution code copied.
