<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->

# Collection terminal and JNI resource qualification

The changed collection leaves preserve their current APIs, live-view behavior, callback and exception order, and all nine existing JNI exports. They reduce temporary filtered-cursor allocation sites, native selection scratch, and rejected-prefix row transfer. The optional collection pack now checks every symbol declared by its actual generated JNI header.

This qualification is scoped to the source revisions and measurements in the adjacent `qualification.json`. It records resource improvements with measured timing tradeoffs.

## Changed owners

| Public source path | Change |
| --- | --- |
| `m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java` | Direct source traversal in `CollectionView.isEmpty`, `SortedMapView.isEmpty`, `NavigableSetView.firstVisible` and `NavigableMapView.firstVisible`. Successful map navigation still returns a fresh detached immutable entry. |
| `m3/collections/src/main/native/collections/m3_index_mask_compactor.c` | The int/long selection-window emission passes reuse their input scratch for accepted values. All seven current compactor exports remain. |
| `m3/collections/src/main/native/collections/m3_index_mask_window.c` | The shared legacy/bounded scan transfers row payload only from the first admitted flag onward. Both window exports remain. |
| `m3/tooling/module-packs/collections/build.py` | Parse the complete actual `javac -h` declaration set, reject malformed/duplicate/missing declarations, and require exact binary export equality. |

Dynamic arrays, row/index directories, aligned state masks, existing sorting and mutable-filter owners retain their current implementations. Live predicates are evaluated in Java; no cached visibility result or per-entry native object is introduced. The primitive row store retains primitive lanes and metadata arrays. Generic APIs retain their existing GC-visible reference arrays.

## Executed correctness checks

The actual JDK 21.0.12.1 toolchain compiled all 134 current collection main Java sources with strict lint and warnings treated as errors. The affected Java owner's public API output was identical.

`m3/collections/src/test/java/com/m3/collections/M3FilteredTerminalTraversalContract.java` produced byte-identical callback/exception tapes: 48 cases, 595 injected exceptions and 283,903 bytes per variant. The fixture wraps real collections, entries and iterators and covers encounter order, predicate/getter demand, early exits, nulls, backing edits, nested descending views and fresh immutable entries.

The unchanged filtered, mutable-filtered and bounded-navigation contracts passed. Explicit native-required tests ran in fresh JVMs with `-Xcheck:jni`:

| Existing test in `m3/collections/src/test/java/com/m3/collections/` | Native result, each variant |
| --- | ---: |
| `M3IndexMaskCompactorSelfTest.java` | 26,152 checks |
| `M3IndexMaskWindowRecoverySelfTest.java` | 10,528 checks |
| `M3IndexMaskWindowSelfTest.java` | 30,596 checks |
| `M3BudgetedMaskWindowContract.java` | 28,914 assertions; 5,000 randomized cases |
| `M3MaskWindowSparseContract.java` | 357,095 checks |

The Java reference modes and both existing missing-library refusal contracts also passed. Actual exported symbols matched the union of the two real generated headers: seven compactor methods and two window methods.

The new native fixture at `m3/collections/src/test/native/m3_mask_window_bounded_regions.c` includes the production window C source. It covers both exports, all rejected prefixes for bounded lengths 1 through 128, nonzero origins, exclusive-end sentinels, output offsets, multiple chunks, aliases, invalid inputs and pending-exception stops. The eight header-parser cases at `m3/tooling/module-packs/collections/test_build_jni_symbols.py` passed.

## Resource measurements and timing limits

Compiler frame results used GCC 13.3.0 with the same C17, `-O2`, `-fPIC`, strict-warning and `-fstack-usage` options for both variants.

| Function | Before frame | After frame |
| --- | ---: | ---: |
| `M3IndexMaskCompactor.selectWindowNative` | 2,464 bytes | 1,424 bytes |
| `M3IndexMaskCompactor.selectLongWindowNative` | 4,512 bytes | 2,448 bytes |

Both frames are classified as dynamic, bounded. The source removes one 256-element scratch array: 1,024 bytes for int selection and 2,048 bytes for long selection. Source storage and compiler frame deltas are distinct measurements.

For a 128-lane chunk with 127 rejected prefix flags, each window export still reads 128 flag lanes, writes one row and performs three region calls. Row payload transfer falls from 128 lanes to one. Dense identity and contradictory-mask paths retain their existing transfer behavior.

The fixed-workload benchmark used three alternating baseline/candidate JVM forks and seven warm samples, with default JIT and disabled escape analysis reported separately. Each JVM used `-Xms256m -Xmx256m`. Terminal samples had 50,000 operations after 100,000 warmup operations; native samples had 1,000 operations after 2,000 warmup operations. All 528 paired cold/warm checksums and operation counts matched across the 18 processes.

The following ratios are medians of paired fork times, candidate divided by baseline:

| Observed workload | Time ratio | Observed fork range |
| --- | ---: | ---: |
| Default JIT, filtered collection isEmpty, member 127 admitted | 0.806 | 0.779–0.980 |
| Default JIT, sorted-map isEmpty, member 127 admitted | 0.368 | 0.363–0.424 |
| Default JIT, navigable-set ceiling, member 127 admitted | 0.441 | 0.371–0.460 |
| Default JIT, navigable-map firstEntry, member 127 admitted | 0.421 | 0.373–0.422 |
| JNI int sparse selection window | 0.964 | 0.929–1.014 |
| JNI long sparse selection window | 1.071 | 1.054–1.096 |
| JNI bounded scan with rejected prefix 127 | 1.012 | 1.003–1.021 |

The long sparse native case was about 7.1% slower in this observation, despite the smaller stack frame. Early-match default-JIT terminal results were mixed. With escape analysis disabled, early navigable-set ceiling had ratio 1.626 even though allocation fell from 152 to 120 bytes per operation. These are explicit tradeoffs.

The candidate's map entry did not escape in the benchmark, allowing default-JIT allocation elimination. The source contract still requires a fresh detached immutable entry; callers that retain entries may allocate. Native result-array allocation was unchanged. Thread allocation counters measure Java allocation, while the independent frame and region proofs establish native resource changes.

These are short custom-harness observations on one Linux x86_64 host and toolchain. Fork ranges are descriptive, not confidence intervals. Compilation, GC, scheduling and optimization effects remain possible. Cold rows mean a first invocation after fixture construction, excluding JVM startup and construction. No universal throughput, cold-start or process-memory claim follows.

## Actual optional module pack

The original builder failed specifically because it required one export while its current compactor source declares seven. The repaired builder passed exact generated-header parity, built `com.m3.collections` and `jdk.m3.collections`, packaged JNI and linked an image.

Both the host JDK and linked image passed the existing filtered contract (123,500 checks), mutable contract (288,223 assertions), primitive-index filtering contract (798,846 assertions across eight primitive kinds and 64 fixtures), and native compactor oracle (26,152 checks). Linked native bytes matched the builder's `objcopy --strip-debug` reference.

The current pack packages the compactor's seven exports. Window JNI qualification used the separate combined nine-export library.

Full target Maven qualification with the source-built JUnit version, a historical `--old-binary` branch, a custom VM build, and `java.util` receiving promotion were not executed by this qualification.

