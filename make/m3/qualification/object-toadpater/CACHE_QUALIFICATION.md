# M3Object cache and installed JNI qualification
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: GPL-2.0-only -->

This additive follow-up qualifies the weak owner index and bounded authorized access-plan cache on a source-built Linux x86-64 M3JDK image. The exact requested API remains:

```java
public <T> T toAdpater(Class<T> clazz) {
    return toAdapter(clazz);
}
```

Both method names remain nonfinal. The alias delegates virtually, so existing subclass overrides, including the frozen pre-alias binary, retain dispatch. Object gains no fields, and HotSpot retains seven Object vtable entries. The previous alias qualification records remain historical evidence. [CACHE_QUALIFICATION.json](CACHE_QUALIFICATION.json) contains exact source, image and evidence identities for this follow-up.

## Implemented behavior

The existing weak owner registry now uses primitive identity-hash buckets and slot chains. Lookup checks exact source reference identity. Reachable facades are never cache-evicted; weak references allow unused owners and their payloads to disappear. Expected lookup is constant time, with linear worst-case collision chains. The additional `ownerNext[N]` and `ownerBuckets[2N]` arrays increase resident memory.

Authorized access plans remain weak values keyed by exact `MethodHandles.Lookup` identity, declaring class identity, field name and field type. The bounded 16/64/256-slot policy uses frequency with recency as a tie-breaker and ages on hits and misses. The capacity signal is JVM heap occupancy. Field capabilities retain their authority after a cache entry is evicted; handles remain lazy and access checks are preserved.

A real-image counterexample exposed repeated owner-lane rebuilding when 1,023 owners remained in a 1,024-slot table. The correction grows when live occupancy is **greater than 75%** at compaction. The four companion arrays are prepared before publication. Across 32 temporary admissions, both default and interpreter runs changed from 32 rebuilds / 128 lane replacements to one rebuild / four replacements. Final capacity is 2,048. Sparse shrinkage and the exact 768/769-live boundary also pass. These are structural counts, not timing results. Growth increases resident capacity and transient allocation, and may fail where same-size compaction would fit.

Both native fixtures now link against installed `libjava` instead of including the bridge C source. Actual native make commands contain the library link; strict C17 builds import the expected notification symbols and define no bridge copies. Focused loader traces resolve the symbols to the exact qualified image's `lib/libjava.so`. The production C bridge, exported symbols and Java descriptors are unchanged. Ordinary JNI writes still require explicit notification to produce M3Object events.

## Executed gates

| Gate | Observed result |
|---|---|
| Original candidate SDK | Retained failure: unsorted Java manifest, 2 failures and 5 errors in 13 tests |
| Sorted original SDK | 13 tests passed; six exact outputs |
| Final Java/text SDK | 16 tests passed; fixed point, refusal, input-order and six installed-output checks; 33 existing fault-model assertions |
| Source build | Existing release/server build, four jobs, warnings treated as errors; real image, normal and CDS boot passed |
| Source archive audit | 23,505 production files; only Object, classLoader bootstrap and M3ObjectRuntime differ from the pinned base |
| Native make and C17 | Both libraries built and executed; generated JNI headers and installed export bindings verified |
| Actual jtreg | Nine descriptions, 21 main actions passed |
| Loader lifetime | Serial and G1, 32 assertions each; includes retained Field use after actual plan eviction and subsequent weak release |
| Weak bounded-heap lifetime | 978 and 990 of 1,024 abandoned sources collected in default and interpreter runs |
| Alias Java/JNI matrix | Six direct image modes passed: 10,770 assertions and 1,032,192 repeated JNI calls |
| Historical binary | Frozen stock-Java-21 class bytes passed 12 checks in each of default, interpreter and forced-C2 modes |
| CDS compatibility | Explicit archive mapping and a separate full enabled alias run passed |
| Scoped public/protected API | Object, M3Object, Field and event API signatures unchanged from the prior qualified image |

The 21 jtreg actions include pressure policy, identity/concurrency, weak lifetime, native stress, both collector-specific loader IDs, eight growth/shrink boundary actions, and the retained Java/native M3Object contracts. Explicit GC handling was enabled. The two cache-stress actions complete 40,580 native notification calls, counted from successful source control flow. The separate alias runs are direct JVM executions and are not relabeled jtreg.

## Measured behavior and tradeoffs

The bounded first-party probe compares the immutable linear-registry image with the intermediate indexed `f22` image. Five repeats run within one fresh JVM per image/live-set configuration, after warmup, using thread CPU time. Every lookup checks facade identity. No GC occurred in a measured interval. The final growth-only correction is separately tested; these numbers do not claim a final-image throughput benchmark.

| Retained facades | Hot lookup, linear ns/op | Hot lookup, indexed ns/op | Field creation including adaptation, linear ns/op | Field creation including adaptation, indexed ns/op |
|---:|---:|---:|---:|---:|
| 16 | 9.98 | 9.59 | 31.60 | 37.07 |
| 256 | 62.57 | 9.74 | 95.64 | 40.68 |
| 8,192 | 2,041.85 | 22.08 | 1,894.23 | 54.36 |

All repeats report zero allocated bytes per hot lookup and 24 bytes per created Field in both images. The 16-owner Field result is slower in the indexed candidate. The results are limited to these workloads and environment; there is no universal speedup claim. CPU affinity was not pinned, and one process per configuration does not establish between-fork or platform variability. Unforced whole-heap occupancy snapshots are not retained-object-size measurements. The machine-readable receipt retains ranges, warmup, allocation and collector counts.

## Category and donor evaluation

The existing Synexia challenge catalogue and category reader were reused. LeetCode 460/706, HackerRank Frequency Queries/Java Hashset, and GeeksforGeeks separate chaining supply problem categories and comparison questions. Their submissions and editorial implementations were not copied. Existing first-party M3Object semantics determine the implementation contract.

Pinned Caffeine and Guava sources were reviewed with their Apache-2.0 notices and licenses. Caffeine's frequency sketch informs aging; frequency approximation is distinct from key identity. The small bounded exact plan cache remains the selected implementation. Guava's entry/segment structure is a reference, with no measured retained-memory ranking asserted. A weak-key/strong-facade map is unsuitable here because the facade strongly owns its source and ordinary value equality differs from reference identity. Immutable source pins and the nine-column donor decisions remain in the private canonical recipe owner.

## Reproduction and remaining scope

The private canonical POM is `synexia-openrewrite-recipes/pom-m3-object-cache-converged-20261102.xml`. Its existing focused job now verifies the `installed` six-output lane as well as the original Java, native and converged lanes. The ordered recipes emit six outputs each, with two overlaps, yielding **10 unique product paths**. The source chain preserves the intermediate runtime and native preimages. Three passive public record outputs are emitted separately through the existing shared text recipe harness. Product repositories receive executed result bytes, not the private authoring framework.

This is targeted Linux x86-64 acceptance. Windows, macOS, other architectures and the whole JDK suite were not run. Existing historical source-seal and TQ failures remain visible; their guards were not weakened. New-head hosted CI is independent of these recorded executions. The private metadata/provenance and commit-bound result-only handoff receipts record publication custody separately. No repository-wide completion, automatic JNI write observation or blanket performance improvement is claimed.
