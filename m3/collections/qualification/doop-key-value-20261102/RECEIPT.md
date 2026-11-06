# DOOP key/value traversal receiver — 2026-11-02

Canonical private source: [Synexia recipe commit](https://github.com/hsoliwal/com.synexia/commit/a772f4266e8010cc737f1241ef379c5add9a46ab), in existing PR #14952. Dataset: `doop-key-value-m3-20261102`, using the unchanged `M3HashPinnedJavaSnapshotRecipe`.

This additive receiver extends existing PR #2194 head `dabb70990aaa3adee9a8f4a96ae04048ee6af2a7`, preserving the intervening token/regex/cursor recovery. Both production preimages were reread at that head and remain exactly equal to the qualified `f44c953a3d91d0cd1d7462b771c3e86b9f33c983` inputs; both new test paths and this receipt remained absent. Both production preimages also match master `ce005d25b29fb6246e013bffb462afd5ed070362`. No preceding recovery source, naming record or receipt is removed.

## Change

The existing bimap, list multivalue map and first-value live view traverse existing keys and values in `forEach`, avoiding one temporary entry per mapping. Lookup/callback order, aliases, structural-mutation checks and exception boundaries are preserved. Public `entrySet` projections and inverse mutation paths remain unchanged. No runtime dependency, native symbol or canonical recipe implementation is added to this product repository.

All four Java files below were emitted by actual `Recipe.run` after exact-hash checks, zero-change fixed point, reversed source order and five refusal cases. Receiving bytes equal the emitted bytes and the versions compiled and tested under strict Java 21.

| Path | Emitted Git blob | SHA-256 |
|---|---|---|
| `m3/collections/src/main/java/com/m3/collections/M3IndexedBiMap.java` | `ed22e8bcff8a3a3de14acab469d1b4afa5f2a972` | `f5a6b8d6bb4ba635c7983e452c523142c51abbb8515c3683a01c37d917fce34c` |
| `m3/collections/src/main/java/com/m3/collections/M3ListMultiValueMap.java` | `1bf76cdc0556bfcfbd287f0c62acb244ecaad49d` | `71bdbb9f7ef60aa7dc00b10e116f6fef61395688bd1d2b9124312f0efce94c3c` |
| `m3/collections/src/test/java/com/m3/collections/M3RelationForEachSelfTest.java` | `a596d0efd918c419820994d0dea5d8dfa5eb89c0` | `f69c70cd86b1903c8ee6c13de0e116665d197c03a4355f48382e92e523eb50c6` |
| `m3/collections/src/test/java/com/m3/collections/M3RelationForEachTest.java` | `fda02aea7a2d2cbf9ee7ac838a0aff986df2ec4f` | `6b0a9a7ac35629e5d293ed4d3acc0821e5e5fc89350ccb16e575500352bda72a` |

## Executed evidence

- Java 21 `-proc:none -Xlint:all -Werror`, actual original class dependencies, no stubs.
- 19/19 focused Jupiter tests: eighteen original map-contract cases plus the new relation wrapper; 32 behavior checks in the standalone oracle.
- Original native five-library oracle: 1,653,019 checks in JIT and interpreter modes.
- Actual pinned arrays C17/JNI library built and loaded; `-Xcheck:jni` used.
- For 524,288 visits with escape analysis disabled, original allocation was 12,596,224 bytes (bimap), 16,786,432 (list) and 12,597,248 (first-value view). New traversal allocated 9,216 bytes in each case, also under `-Xint`. These are bounded traversal allocations, not whole-product throughput.

Full Maven reactor, installed M3JDK/JMOD/jtreg/GC/CDS, cross-platform and controlled throughput gates are not certified. Historical source-admission/naming gates are not bypassed or resealed here. Existing parent PR qualification holds remain. Keep this PR draft until its complete gates pass.
