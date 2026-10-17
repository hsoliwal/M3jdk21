# M3JDK21 indexed window-mask compaction — 2026-10-17

Status: exact candidate implementation, Java21/JNI and OpenRewrite runtime proof pending.

## Source and product boundaries

Canonical convergence workspace: `hsoliwal/com.synexia`, a non-public source/recipe development plane. The downstream M3JDK21 product owns the Java21 runtime, C17 JNI symbols and acceptance. The upstream donor is the existing M3JDK21 `M3IndexMaskCompactor`, together with exact reviewed primitive-long JNI adaptation from earlier target PR #1034. All source is first-party Apache-2.0; no third-party algorithm or OpenJDK source is relicensed.

This recipe is `com.synexia.m3.IndexedWindowMask`, composed only of existing canonical OpenRewrite Java AST and text-source snapshot owners. Its separately pinned Java, native and metadata crate manifests each carry exact historical before and reviewed after SHA-256 values. Target modifications are limited to `M3IndexMaskCompactor.java`, `m3_index_mask_compactor.c`, the existing `M3IndexMaskCompactorSelfTest.java`, the two existing source verification scripts, and the existing focused workflow. Four new artifacts are a dated source successor receipt, its read-only validator, an adversarial receiver test and this documentation. No `java.base` / HotSpot / public `java.util` code is replaced.

## Dynamic indexed-array contract

Two **additive** APIs live on the existing target owner:

```java
int[] selectWindow(int[] rows, int rowOffset, byte[] states,
                   int stateOffset, int count, int requiredMask, Backend backend)
long[] selectLongWindow(long[] rows, int rowOffset, byte[] states,
                        int stateOffset, int count, int requiredMask, Backend backend)
```

The two source arrays may have independent offsets and capacities. The logical `count` and each `Objects.checkFromIndexSize` range are checked before selecting. Zero-count at the physical end is accepted, invalid offsets, negative counts and overflowing windows refuse before output allocation. Masks use all eight unsigned-byte bits, including required mask zero; order and duplicate IDs stay stable and every signed int/long value is permitted.

Each Java selection runs two linear passes, producing one exactly-sized detached array of k row IDs: O(count) time, O(k) result and O(1) local state. There is **no subarray copy, boxing, index-entry object or retention of source arrays**. The reference objects and mutation epoch remain owned by the originating dynamic collection. Both input arrays must remain stable during selection; these are *not* live views and no atomic snapshot/concurrency claim is made.

JNI uses the **existing C17 file and m3_collections_mask library**, with two new explicit native window symbols. The same 256-position bounded GetByteArrayRegion / GetIntArrayRegion / GetLongArrayRegion / Set...ArrayRegion protocol is used. There is no GetPrimitiveArrayCritical, no stored Java pointer, reference count assumption, predicate/comparator callback or implicit switch to JNI. `Backend.NATIVE_REQUIRED` fails explicitly if the native library is missing.

The current whole-array `select`, `selectLong` and their JNI behavior remain callable and semantically unchanged. This candidate also converges the historical optional native64 whole-array donor without inventing a second compactor.

## Tests and admission

The *existing* M3IndexMaskCompactorSelfTest is extended rather than replaced:
- 256 mask values across 0, 1, 2, 255, 256, 257, 511, 512, 513, 1025 logical lengths;
- independent row offsets (0,1,7) and mask offsets (0,2,9), signed minima/maxima, duplicates;
- 512 seeded randomized independent offsets/counts;
- JDK Arrays.copyOfRange oracle **only in tests**, Java result parity and real optional JNI parity;
- zero-window at end, null operands/backend, mask range, negative and overflowed offsets, mutation/nonalias checks;
- inherited int/native, long/native, ArrayList/Deque fail-fast and masked retention tests unchanged.

The existing core source-inventory manifest and `synexia-donation.json` remain historical and unchanged. A new 3-row old->new source successor is hash sealed, tested for symlink/duplicate/tampering/alias and applies only if the original source bytes match. All unrelated recorded files retain their previous expected hashes; no broad source-verifier bypass. The original mask CI adds only its negative/positive successor test and retains strict Java21 javac, C17 warnings-as-errors and `-Xcheck:jni`.

Required gates: actual canonical OpenRewrite SDK one-pass/second-pass fixed-point and drift refusal; exact source readback; Java21 `-Xlint:all -Werror`, tests both default/JIT and `-Xint`, real C17 JNI parity with checked JNI; entire collection module tests and cross-platform/native support where applicable. Performance comparisons are *unproven* until controlled Java-only/JNI time and allocation measurements. Nothing here advances String-first global product phase or completes all JEPs.

Use the category catalogue `catalogs/m3jdk21-indexed-window-mask-20261017.tsv` for repeated per-file donor decisions. LeetCode/HackerRank/GFG classify two-pass compaction, prefix masks and range offsets; they are not source-copy authorities.
