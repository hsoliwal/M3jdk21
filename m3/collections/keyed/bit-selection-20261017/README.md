# M3 keyed selection — packed atomic bit-mask refinement (2026-10-17)

Receiver `hsoliwal/M3jdk21`; base is the exact #1072 keyed receiver head `b8c2ded94a1c99e8840923d3b618083726d3a620`. The earlier Synexia convergence donor is pinned to `2f9a579c60a41422f5b164049cf65746a0830713`; all first-party adaptations are Apache-2.0 with existing contributor attribution.

## Contract, composability and storage

The existing `M3Parallel.Plan.filter` uses `boolean[]` with one byte-sized flag per captured source row. Across parallel keys, disjoint rows may share a 64-bit word; ordinary `long[word] |= mask` loses concurrent writes. This bounded, per-file refinement replaces only the private selection atom with `M3SelectionBits`, one packed `long[]` lane, and Java21 `VarHandle.getAndBitwiseOr`. The bitset contains **selection state per captured row**, not object lifecycle state. `M3ObjectStateSlots28` remains the separate admitted owner of generation-safe object-state flags.

After the existing keyed worker drain, the mask counts bits with `Long.bitCount` and enumerates set-bit source indices using `Long.numberOfTrailingZeros`, preserving encounter order, duplicate references, nulls and exactly one stable output array. It adds no public API, object payload, Entry/Node storage or JNI ABI. Predicate exception, cancellation, source-snapshot, strict same-key sequencing and filtered/sorted-wrapper contracts remain unchanged.

Approximate temporary mask payload: `boolean[N]` is about N bytes; the new `long[ceil(N/64)]` is `8*ceil(N/64)` bytes. This excludes headers/output memory and makes **no throughput claim**; atomic updates may contend across keys.

## Mandatory recipe / proof gating

Reuse **the existing M3 hash-pinned Java OpenRewrite provider**, through the named recipe `com.synexia.m3jdk.PackedKeyedSelection20261017`. Resources seal one exact existing `M3Parallel.java` pre/post image and two formerly absent postimages: package-local `M3SelectionBits.java` and `M3SelectionBitsSelfTest.java`. The named SDK/JUnit proof checks LST emissions, source hashes, refusal cases, idempotence, partial replay and absence of duplicate targets. Fix recipe resources and rerun, never hand-edit installed generated output.

```bash
mvn -B -ntp -f m3/collections/keyed/bit-selection-20261017/recipe/pom.xml \
  -Dm3.bits.sdk.output=/absolute/fresh/sdk-output clean test
```

The qualified first-party *offline replay adapter* emitted exactly three files and passed check/second-apply fixed-point verification. That is **not** Maven/OpenRewrite SDK proof. The exact-head workflow independently compiles the five unchanged *real* M3 slot owners, verifies source hashes and public `javap` declarations, runs all existing/new Java and JNI/UBSan/missing-native tests.

## Actually executed local checks

- Java21 `javac --release 21 -proc:none -Xlint:all -Werror`: passed; **used a temporary LinkedHashMap-compatible test stand-in for the unavailable real M3SlotMap dependency** and existing wrapper snapshots, not a receiver-module build.
- New packed-mask suite: **54,429** assertions, 15 boundary sizes including 63/64, 400 rounds with 32 competing bit writers, 260 seeded keyed filtering comparisons, failure/reuse/immutability.
- Existing 5 Java contract mains passed; four JNI `-Xcheck:jni` suites with fatal C++17 UBSan passed; three missing-native regression paths passed.
- `javap -public -s`: exact before/after public descriptor parity for `M3Parallel` and its `Plan`.
- Offline replay: 3 SHA-256-sealed outputs, check-only fixed point and second apply fixed point.
- **Unverified until hosted CI:** actual Maven/OpenRewrite SDK execution, full real M3 owner compiler and receiver-reactor/JDK/JCK tests, performance and non-Linux native coverage.

Selection method comparisons and problem-category references are separated in `CATALOGUE.tsv`; donor and file hashes are in `DONORS.tsv`, `FILE_PASSES.tsv` and `evidence/LOCAL_RECEIPT.json`. No competitive-challenge solution was copied. The historic #1072 workflow is successor-aware without changing its sealed recipe output.
