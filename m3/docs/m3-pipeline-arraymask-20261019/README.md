# M3JDK21 java.base: indexed pipeline stage atoms (2026-10-19)

**Product owner:** `src/java.base/share/classes/jdk/internal/util/M3PipelinePlan.java`.
This is **M3JDK21 itself**, not M3 as a dependency or a service attached to another JDK. The public `M3PipelinePlan` declarations are unchanged. The transformation is generated from the canonical source-sealed, Maven-backed Synexia recipe in [companion PR #12494](https://github.com/hsoliwal/com.synexia/pull/12494), pinned to exact before and after Java blobs. Maven/OpenRewrite and Synexia are tool-time only.

## Behavioral atoms

1. Replace private `List<Stage>` and recursive `Sink` dispatch with immutable dynamic `Object[]` callbacks and a `long[]` filter bit lane, using `offset >>> 6` and `1L << (offset & 63)`. These are *stage metadata*, not per-element objects.
2. `append` copies only the plan metadata arrays before publication. Earlier plans retain their original callback order and live `Iterable` source; filters short-circuit, maps preserve nulls, and invocation exceptions escape unchanged.
3. Traverse every source element by ascending stage index in a constant-stack loop. Preserve encounter order, laziness and terminal boundaries. `distinctToList` and `sortedToList` remain existing JDK `HashSet` and stable `ArrayList.sort` terminal operations.

**No** new JNI entry points, public API, runtime module, global mutable state, framework callback, or foreign collection hierarchy. No external library is required in the generated runtime.

## Comparison and qualification

| Evidence | Result |
|---|---|
| Original deep chain, 4096 map operations, 256 KB thread stack | StackOverflowError reproduced |
| Generated deep chain, identical input/stack | Pass |
| Focused checks (callback order, exceptions, null, distinct/sorted, plan immutability, mask boundaries) | 14 checks passed |
| Deterministic 500-seed differential x 3 terminals against sequential JDK streams | 1500 comparisons passed |
| `javac -Xlint:all --patch-module java.base` | Pass |
| `javap -public` exact declared-public-surface diff | No difference |
| Exact-source generator check/apply/apply | CHANGED / CHANGED / UNCHANGED |
| Canonical recipe unittest, drift and scope refusals | 5/5 passed |
| Maven executable recipe test, JDK image build/jtreg, JNI, Android and iOS | NOT_RUN |

This is not a claim of universal speedup. Both designs have O(N × K) callback time; the selected design reduces execution stack from O(K) to O(1), and removes per-traversal sink-chain closures. It still incurs O(K) copies when constructing persistent plans. Mask representation adds approximately one state bit per stage plus callback references.

## Category evaluation

The searchable companion selection ledger is [`M3_PIPELINE_SELECTION.tsv`](../collections/algorithm-catalogue/public-forks-20261019/M3_PIPELINE_SELECTION.tsv). The current owner `M3IndexedStorage` and other sorted/filtered collection wrappers remain independent and unchanged. Existing `java.util.Collections` in-place sorted/filtered APIs being reviewed in [PR #1435](https://github.com/hsoliwal/M3jdk21/pull/1435) must be reconciled before additional changes to those factories. Existing native bitmap/JNI work is preserved.

**Promotion gates:** run the canonical Maven test under the reviewed Synexia toolchain; apply from a pinned JDK revision; run standard JDK jtreg tests, full M3JDK21 bootstrap/image build and installed-image smoke tests; re-evaluate after any upstream change. No automatic merge.
