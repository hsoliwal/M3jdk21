<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 CPU-tools: result-only Wave19 → Wave20 run-length postings receiver

**Source-of-truth donor**: [Synexia #15240](https://github.com/hsoliwal/com.synexia/pull/15240)
`3659e2bec459fb8f1916e2530c0b47023ff440e1` (Wave19 #15220, Wave20 #15240).
Original versioned `M3HashPinnedJavaSnapshotRecipe` and
`M3HashPinnedTextSnapshotRecipe` Maven/OpenRewrite source crates live in Synexia.
This downstream receiver does not replicate, relax or replace those recipes.
Canonical source paths and SHA-256 pre/post outputs are sealed in
`SOURCE_RECEIPT.tsv`. All 11 receiver paths were absent at
`hsoliwal/M3jdk21 develop@984f1b9bc5f83de4606a27ab7b58a4d3978fbc4c`.

## Actual Java/C product files

- `M3RunLengthPostingIndex` is received into the **existing**
  `com.m3.tools.cpu` module, not a wrapper. Its internal storage is
  two primitive int[] lanes: signed distinct values and cumulative run-end
  positions; no per-entry objects. Contract: immutable detached arrays,
  original ascending multiset rank, count, select and positional windows.
- `M3RunLengthPostingIndexNative` is the original explicit opt-in Java/JNI
  entrypoint. Native library loading requires an **absolute property path**
  (`m3.cpu.runlength.native.path`); absent/invalid inputs are refused.
- `native/m3_runlength_postings.c` is the exact Wave20 original C17
  postimage: sorted large batch queries use a monotone scan; unsorted/small
  queries retain binary lookup. JNI release paths and original behavior
  come from the canonical Synexia donor, not newly synthesized code.
- Four unchanged Java oracle files, two independent C17 kernel oracles,
  and Wave19/20 category ledgers are received verbatim and hash checked.

**Build boundary:** Existing `cpu-tools/build.py` discovers Java classes
inside `src/com.m3.tools.cpu`; Java source inclusion is automatic.
The existing packager packages only its established `m3textio` native
library from the text-tools owner. This new optional run-length JNI
library is **NOT yet installed in an M3JDK21 JMOD or linked image**.
It can be compiled as an explicit sidecar shared library for test parity;
do not claim product-native delivery or alter `build.py`/module-info.
Existing public Java and JNI exports remain untouched.

## Contract and category admission

Inventory before patch, validate Git blob and UTF-8 SHA-256 for each
file, then source receipt, strict Java21 (`-Xlint:all -Werror`),
C17 (`-std=c17 -pedantic -Wall -Wextra -Werror`), JNI header and
original Java/C oracle parity in JIT, `-Xint`, `-Xcheck:jni`.
Then verify original CPU-tools JMOD/jtreg, full Maven/OpenRewrite SDK,
JDK ABI, provenance and runtime only when these gates actually execute.

LeetCode #2080 Range Frequency Queries, HackerRank Climbing the
Leaderboard, GeeksforGeeks run-length/sorted frequency patterns,
RoaringBitmap and JavaFastPFOR are **REFERENCE_ONLY** problem categories.
Sorted signed duplicate-preserving run counts are not source-equivalent
to arbitrary unsorted range-frequency, distinct bitmaps or dense rank.
No challenge solution copied; preserve Apache-2.0 donor provenance.

**DRAFT / HOLD:** No protected/default branch mutation, no rebase, no
silent move/delete, no claim of full `hsoliwal/*` convergence.
Cross-platform JNI/GC, full M3JDK21 JMOD/jtreg and Maven producer
SDK must be independently verified before release or promotion.

