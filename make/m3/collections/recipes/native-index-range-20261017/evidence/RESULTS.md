# Executed local qualification — 17 October 2026

All product images were emitted by the unchanged source-seal kernel. Original source Git
blobs were checked before template refinement. No tool/runtime doubles replace the two
actual product Java classes or their native compaction implementation.

| Gate | Result | Boundary |
|---|---:|---|
| Actual source-seal kernel | 784 checks, 32 shuffled input orders, 9 exact outputs | Not the OpenRewrite SDK |
| Original Java + original C/JNI | 638,432 checks, 2,048 seeded cases | Unchanged baseline |
| C-only serial pass | 638,432 checks | Old Java / new C legacy symbol |
| Compactor-Java serial pass | 638,447 checks | New range private-entry tests; old projection |
| Final projection pass | 638,454 checks | Actual Java/JNI, direct range and sorted sharing |
| Final repeats | 638,454 each at 32/128 MB and interpreted -Xint | Same finite corpus, not independent proofs |
| No native library | 529,306 checks | Java works; native-required raises even for empty input |
| Original old native binary | 638,439 checks | New Java uses actual legacy native ABI, not Java fallback |
| Legacy native dispatch sentinel | 2 checks | Deliberate test-only throwing native function proves native dispatch |
| Compiled faulty C variants | Both detected | Dropped offset; corrupted high-bit mask |
| Public ABI | Four javap -public outputs identical | Two product classes and two enums |
| Pipeline example | Java and JNI both physical [2,3], values [10,30] | Sorted, sliced, high-bit flags |
| Exact replay | All 9 outputs byte-identical | Includes generated CI, docs, example, tests, catalogue |

The original public contracts already passed. Its separate sharing witness intentionally
fails because it allocates new directory arrays: this is an optimization witness, not a
claim that the old public sorting results were wrong.

## Allocation observation

Actual ThreadMXBean per-thread allocated bytes, 65,500-row slice, 100 operations per sample,
three samples after warmup; baseline/new outputs were identical within each group:

| Operation | Original bytes | Refined bytes |
|---|---:|---:|
| Native filter selecting no rows | 26,206,400 | 4,800 |
| Repeated already-ascending sort | 52,406,400 | 3,200 |

The new filter avoids an int[n] clone; same-direction sorting avoids its two index arrays.
New descriptors and empty result arrays are still allocated. JNI work is not advertised
as faster than Java. These are narrow allocation observations, not JMH throughput, native
resident-memory, UI performance or whole-application benchmarks. Values are in ALLOCATION.json.

Compilation used actual Java21 --release 21 -proc:none -Xlint:all -Werror. C used C17
-Wall -Wextra -Werror -Wpedantic, UBSan/no-recover, -fPIC/-shared. Runtime used -Xcheck:jni.
Raw execution logs are included in the companion delivery archive; this packet includes
reproduction sources, exact generated-source receipts and the compact execution summary.

## Independent gates not run locally

Maven is absent and Maven Central resolution failed DNS in the local container. Thus actual
Maven/OpenRewrite scheduler/discovery and JUnit execution are **NOT_RUN locally**. They have
an executable focused POM/workflow; only a successful current-head run may close those gates.
Full custom collection reactor, OpenJDK image/jtreg/GC/platform qualification, broader live
wrapper families, SWT/Nebula/RWT/GWT application integration and universal web conversion
are not claimed. The private capability ABI and input validation are checked on Linux here;
other operating systems still require their existing native build integration and tests.
