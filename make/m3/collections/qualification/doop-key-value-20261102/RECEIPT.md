# DOOP key/value traversal receiver — 2026-11-02

Canonical private source: [Synexia recipe commit](https://github.com/hsoliwal/com.synexia/commit/a772f4266e8010cc737f1241ef379c5add9a46ab), in existing PR #14952. Dataset: `doop-key-value-m3-20261102`, using the unchanged `M3HashPinnedJavaSnapshotRecipe`.

This additive receiver extends existing PR #2194 head `dabb70990aaa3adee9a8f4a96ae04048ee6af2a7`, preserving the intervening token/regex/cursor recovery. Both production preimages were reread at that head and remain exactly equal to the qualified `f44c953a3d91d0cd1d7462b771c3e86b9f33c983` inputs; both new test paths and this receipt remained absent. Both production preimages also match master `ce005d25b29fb6246e013bffb462afd5ed070362`. No preceding recovery source, naming record or receipt is removed.

## Change

The existing bimap, list multivalue map and first-value live view traverse existing keys and values in `forEach`, avoiding one temporary entry per mapping. Lookup/callback order, aliases, structural-mutation checks and exception boundaries are preserved. Public `entrySet` projections and inverse mutation paths remain unchanged. No runtime dependency, native symbol or canonical recipe implementation is added to this product repository.

All four Java files below were emitted by actual `Recipe.run` after exact-hash checks, zero-change fixed point, reversed source order and five refusal cases. Receiving bytes equal the emitted bytes and the versions compiled and tested under strict Java 21.

| Path | Emitted Git blob | SHA-256 |
|---|---|---|
| `m3/collections/src/main/java/jdk/m3/collections/M3IndexedBiMap.java` | `3e173663a7196e700bfcf1da8815c62f3f60807d` | `8a430604b16f18be57adf3725829d85256a19e4efbfb79c4b48113dd4e5e9188` |
| `m3/collections/src/main/java/jdk/m3/collections/M3ListMultiValueMap.java` | `a7459cb080eab9b37a3936b0548d4816f5999217` | `3e0ed3a9bf13f6a9de62c493eff3c7400bae5804e26476397e6ae982768f3f03` |
| `m3/collections/src/test/java/jdk/m3/collections/M3RelationForEachSelfTest.java` | `feaef0f8881d647d3cf74e02f6beee95968f55c6` | `1430aadfd3b2e6a943cd95eac92086cd5c788b188130d48737a6c4d9a347dd7e` |
| `m3/collections/src/test/java/jdk/m3/collections/M3RelationForEachTest.java` | `6a678dc7d1fd0255d66ffe30d6ffe044e393369f` | `e77f7b82d2bc8570a872429e4db126349b081cfb73525be178f0b84fd0de7c76` |

## Executed evidence

- Java 21 `-proc:none -Xlint:all -Werror`, actual original class dependencies, no stubs.
- 19/19 focused Jupiter tests: eighteen original map-contract cases plus the new relation wrapper; 32 behavior checks in the standalone oracle.
- Original native five-library oracle: 1,653,019 checks in JIT and interpreter modes.
- Actual pinned arrays C17/JNI library built and loaded; `-Xcheck:jni` used.
- For 524,288 visits with escape analysis disabled, original allocation was 12,596,224 bytes (bimap), 16,786,432 (list) and 12,597,248 (first-value view). New traversal allocated 9,216 bytes in each case, also under `-Xint`. These are bounded traversal allocations, not whole-product throughput.

Full Maven reactor, installed M3JDK/JMOD/jtreg/GC/CDS, cross-platform and controlled throughput gates are not certified. Historical source-admission/naming gates are not bypassed or resealed here. Existing parent PR qualification holds remain. Keep this PR draft until its complete gates pass.
