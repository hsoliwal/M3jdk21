# Common JNI support and platform mapping qualification

## Scope and source ownership

This change gives thirteen existing M3JDK21 native C owners one internal, pure-JNI exception helper in `src/java.base/share/native/libjava/m3_jni_util.h`. It consolidates eighteen matching `FindClass`, conditional `ThrowNew`, and `DeleteLocalRef` sites while preserving all twenty-seven existing JNI declarations. The helper compiles as C17 and C++20 and introduces no exported function, native library, class cache, global reference, public Java API, or separate runtime family.

M3JDK21 owns this support within its integrated product. The existing Java and native owners retain their behavior and source locations. Synexia owns reusable recipe execution and convergence; the product gains no Synexia runtime dependency. This is a bounded shared support change, not a claim that every JNI duplicate or platform operation has been consolidated.

The pinned receiving baseline is `d6d76b563050fc13c4fa79bc494edf29f1614b37`. [NATIVE_PROVENANCE.json](NATIVE_PROVENANCE.json) records every before/after blob, SHA256, source license, donor role, and explicit exclusion. The receiving phase remains **STRING / QUALIFICATION_REQUIRED**; this change does not advance the ordered collection, array, or AST phases.

| Existing owner | Preserved JNI declarations | Shared throw sites |
| --- | ---: | ---: |
| `m3/collections/src/main/native/collections/m3_index_mask_compactor.c` | 9 | 6 |
| `m3/collections/src/main/native/collections/m3_index_mask_window.c` | 2 | 1 |
| `m3/collections/src/main/native/collections/m3_index_path_masks.c` | 1 | 1 |
| `m3/collections/src/main/native/collections/m3_long_index_path_masks_jni.c` | 2 | 1 |
| `m3/collections/src/main/native/collections/m3_residency_state_mask.c` | 1 | 1 |
| `m3/collections/src/main/native/collections/m3_state_mask_histogram.c` | 1 | 1 |
| `m3/collections/src/main/native/collections/segmented_bit_lane.c` | 1 | 1 |
| `src/java.base/share/native/libjava/M3BitLane28.c` | 1 | 1 |
| `src/java.base/share/native/libjava/M3RX7.c` | 1 | 1 |
| `src/java.base/share/native/libjava/M3RegexDpPlan.c` | 3 | 1 |
| `src/java.base/share/native/libjava/M3StateRelation.c` | 2 | 1 |
| `src/java.base/share/native/libjava/M3StringPool.c` | 2 | 1 |
| `src/java.base/share/native/libjava/M3StringRegexDp.c` | 1 | 1 |
| **Total** | **27** | **18** |

## JNI contract and exclusions

`m3_jni_throw_by_name(JNIEnv *env, const char *type, const char *message)` performs the existing three-operation sequence. It does not add an `ExceptionCheck`, initialization protocol, cache, attach/detach operation, or class-loader policy. Existing wrappers retain their exact null, `-1`, and void return behavior. Exception classes and messages, validation order, primitive-region access, output ownership, and native-required refusal remain with the caller. The residency-state wrapper retains its existing leading `ExceptionCheck` return.

The existing `JNU_ThrowByName` is not an exact replacement: its pinned implementation lacks the explicit local-reference deletion used by these sites, and independent JNI libraries cannot assume the JDK-internal export. Four other current owners also have a different lifetime sequence and remain unchanged: `M3ObjectMetrics.c`, `m3_long_address_kernel.c`, `m3_long_rank_batch_jni.c`, and `m3_signal_order_jni.c`. Their hashes and reasons are recorded in provenance so later work can qualify those operations deliberately.

## Exact-address mapping extension

`M3StringPool.c` remains the mapping owner. Its Linux branch retains `MAP_SHARED | MAP_FIXED_NOREPLACE` when the Linux headers define that flag. Its existing Windows mapping and cleanup body is retained. Apple and Linux headers without that macro now attempt a non-fixed `MAP_SHARED` mapping using the requested address as a hint. The owner accepts only the exact address; an alternate mapping is released and the existing exception is reported. Apple also uses the existing POSIX unmapping path.

This intentionally extends two previously unsupported source branches. It does not assert that unsupported behavior and a safe mapping attempt are identical. The exact-address requirement, occupied-address preservation, 64 KiB alignment, positive range, `INT_MAX` length bound, conversion and overflow checks, regular-file and exact-length checks, and direct-buffer allocation cleanup remain in force. No ordinary `MAP_FIXED`, reservation/unmap/fixed sequence, or relocated-address acceptance is introduced.

The safety pattern already exists in the pinned [HotSpot BSD mapping owner](https://github.com/hsoliwal/M3jdk21/blob/d6d76b563050fc13c4fa79bc494edf29f1614b37/src/hotspot/os/bsd/os_bsd.cpp), blob `a2a9a26e8e5cac1a4f676294e3b1a080a6e57af3`: `anon_mmap` omits `MAP_FIXED`, and `pd_attempt_reserve_memory_at` accepts the exact address or releases the alternative. This is behavioral evidence; no C++ donor function body is copied into the C target.

## Build and source packaging

Libjava consumers include `m3_jni_util.h` locally. Collection consumers include the same file through `../../../../../../src/java.base/share/native/libjava/m3_jni_util.h`. All thirteen includes resolve to one target header. Existing native compile commands need no extra include flags, library, or export.

The shared declaration selects `static __inline` for Microsoft C (`_MSC_VER` without `__cplusplus`) and `static inline` otherwise, with no global keyword macro. Microsoft's [C keyword documentation](https://learn.microsoft.com/en-us/cpp/c-language/c-keywords?view=msvc-170) places standard `inline` behind `/std:c11` or `/std:c17`; the extension avoids adding that requirement to an existing default-mode Microsoft C build. This source provision does not establish a Windows build or runtime result.

An integrated source package must retain the shared header and repository-relative layout. Copying a collection C file alone is insufficient. Do not create a second header mirror, separate common-JNI runtime service, overlay, or platform-specific copy. The existing `java.base` native compilation owner remains responsible for its headers and target libraries.

## Rights and donor distinctions

The new first-party helper retains the extracted target atom's `GPL-2.0-only WITH Classpath-exception-2.0` identification and Hitesh Soliwal/contributors attribution. It does not assign an Oracle grant or introduce a dual license. Existing source notices are byte-identical, including the Apache-2.0 notices on `M3BitLane28.c`, `M3StateRelation.c`, and the seven collection consumers. Retaining those notices does not resolve historical direct-copy intake status.

The GPL-only BSD source remains an evidence-only donor; it is not relicensed. Source packaging retains the separately identified helper, incumbent notices, and applicable repository and collection license/notice files. This record does not declare a new blanket license for a combined distribution. The pinned rights-policy sources and exact per-file distinctions are in provenance.

## Qualification evidence and remaining gates

The historical native receipt at `m3/qualification/common-jni-platform-20261020/native-mapping-candidate-receipt.json`, SHA256 `2b217adb5ee5ccb33dcedba114c9af21c88b0cb037bcd2312ed7fd77679eeb4e`, binds the actual mapping sources and the earlier helper hash recorded in that receipt. It predates the Microsoft C qualifier adjustment and does not qualify the updated header. It reports:

- Four deterministic syscall/JNI model lanes, twenty-four checks each: baseline Linux, candidate Linux, Linux without the no-replace macro, and the Apple preprocessor branch. The baseline/candidate Linux transcripts are identical.
- Three helper contract checks each in C17 and C++20 using modeled JNI tables.
- Eight checks each for the baseline and candidate actual C mapping owner, registered to a fixture class in a checked stock JDK21 JVM, using real Linux `mmap` on x86_64.

These results qualify the exercised source contracts at their recorded level. The Apple lane uses Linux host headers and modeled syscalls; it is not an Apple SDK or kernel result. The missing-macro lane is not an old-kernel execution. The real JVM run is not a source-built, installed M3JDK21 image. The receipt does not cover every consumer's Java/JNI runtime or establish Android, iOS, Windows, HarmonyOS, or universal device support.

Current qualification uses the receiving runner at `m3/collections/qualification/common-jni-20261020/run_native_tests.py`. It compiles all thirteen actual C owners against JNI headers generated from their actual Java declarations, binds the updated helper hash, and retains the mapping and helper checks. Its additional `MSVC_KEYWORD_MODEL` forces the Microsoft macro branch on the Linux host compiler, verifies `static __inline` is selected, and exercises the same three helper cases. It is explicitly a host keyword model, not Windows compiler, SDK, or runtime proof. The emitted product run must retain its own source-bound receipt.

[PLATFORM_MATRIX.tsv](PLATFORM_MATRIX.tsv) records the old/new source behavior, compilation evidence, model evidence, actual runtime evidence, and remaining gate for each platform. Target SDK/ABI builds, device and kernel execution, runtime binding, installed-image acceptance, and applicable jtreg remain separate requirements. No CPU, heap, native-memory, or throughput improvement is claimed without measurements.

## Canonical recipe and receiving invariant

The canonical recipe is `com.synexia.m3.CommonJniPlatform20261020` from `hsoliwal/com.synexia` at `the exact Synexia-Recipe-Commit trailer in the receiving commit (also linked by the receiving PR)`, composed through the existing `M3GpuToolJoinRecipe` engine. The receiving commit trailer and PR bind the exact canonical recipe commit without a self-referential template hash. The canonical crate retains the SDK emission, fixed-point, refusal and emitted-source verification receipts. Candidate templates alone are not execution evidence.

Apply the reusable recipe serially and per file against exact pinned preimages. Refuse unexpected source drift or occupied outputs, emit only the documented postimages, retain each source's rights and mapping, verify the touched contracts, and demonstrate a stable second application. Preserve each file's public and native contracts except for an explicitly documented regression restoration or the mapping capability extension described above. Record the concrete proof with the output; do not infer it from a recipe name or a host model.

The searchable candidate evaluation uses the existing ten-column schema in `m3/collections/algorithm-catalog/common-jni-platform-20261020.tsv`; `m3/algorithms/catalogue` is absent at the pinned baseline. The additive fragment at `m3/docs/name-mapping.d/m3-common-jni-platform-20261020.json` follows `M3_NAME_MAPPING_FRAGMENT_V1`. No root name mapping or receiving-sequence mutation is proposed.

The expanded [all-owner candidate receipt](native-all-owner-candidate-receipt.json) additionally binds independent compilation of all thirteen C owners against headers generated from their actual Java declarations. Recipe-emitted output is rechecked by the same receiving runner; its exact evidence is retained in the canonical crate and CI artifacts.
