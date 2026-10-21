# First-class bulk JNI execution in M3JDK21 — 2026-10-21

M3JDK21 **is the Java/HotSpot/JNI SDK product**, not an M3 library on top of an unchanged JDK. The native selector is owned at `m3/collections/src/main/native/collections/m3_index_path_masks.c` and uses the established `M3IndexPathMasks` class and JNI signature. This is a deliberately narrow **stacked successor** of [PR #1545](https://github.com/hsoliwal/M3jdk21/pull/1545), which already introduced bounded per-call stage metadata caching. Do not replace its C implementation or introduce another independent native owner.

## Android-inspired native work

Android/ART and the AOSP libcore memory library demonstrate moving selected heavy bulk operations into native code. The Android NDK JNI guide specifically warns that marshalling/crossing overhead can outweigh advantages for fine-grained methods. M3JDK21 should do **one operation per native call with an array owner, indexes, offsets, masks and a bounded result**, not JNI per Java element.

In the existing C17 selector, the original cached path still executed `GetIntArrayRegion(offsets, ..., 1)` for every row and `GetIntArrayRegion(rows, ..., 1)` plus `SetIntArrayRegion(output, ..., 1)` for every accepted row. The new file-local `m3_select_cached_contiguous` atom batches offsets/row IDs and emitted result indices through 256-element C stack arrays, but continues checking every required/excluded full-width state word in the existing `m3_matches_cached` atom. It never acquires or retains a Java heap array pointer.

- Only the **existing** native cached path with 1–8 state stages, at least eight logical rows **and stride +1** chooses the bulk kernel.
- Strided/reversed paths, wider stage sets, and short paths stay on their exact previous source algorithms.
- Logical signed row IDs, duplicates, physical offset checks, state-plane ordering, empty matches, Java native-unavailable refusal, two-pass count/emit, exact detached result size and concurrent-state-drift refusal remain unchanged.
- The native scratch is four fixed `jint[256]` arrays (4096 bytes). No malloc or per-row heap allocation, `GetPrimitiveArrayCritical`, stored Java heap pointer or JNI ABI change.
- Using C17 to do a calculation is not automatically faster: no universal CPU/energy/Android speedup is claimed. Per-slot state reads continue through JNI because physical offsets are an arbitrary gather. Further state batching needs independent proof.

## Source-pinned serial recipes

The parent PR #1545's qualified `M3HashPinnedJavaSnapshotRecipe` has explicit PlainText custody for native `.c` source, while Java remains Java-LST based. This successor uses **two** exact source-sealed recipes:
`com.synexia.rewrite.M3NativeBulkPathMask20261021` (`lst:false`, native C file) and `com.synexia.rewrite.M3NativeBulkPathMaskTest20261021` (`lst:true`, Java test).

Native preimage SHA-256: `559f4cfe5a1fa1eb6f77d8c7cd805915b7bc09ae91abbc87914a9e1543b63058`.
Native postimage SHA-256: `854924fb4ac65949bd4d77230040db1ec21dce0a65ed4722121fd9f0f867274d`.
New Java regression file SHA-256: `62a32fe126710ba3c9187066c01622feec78991f8d48bc4a7334883cc29f5721`.

The native preimage exists unchanged as PR #1545's qualified *postimage* in the prior recipe crate, so the receipt forms an explicit chain. Unknown source drift must refuse; a repeated application should be a fixed point. A source seal and a C compiler pass do not constitute an actual Maven/OpenRewrite recipe execution.

## Actual product-owner differential gate

```bash
bash m3/collections/qualification/native-bulk-pathmask-20261021/verify.sh
```

The verifier compiles `M3IndexPathMasks.java` itself, checks both recipe/template source digests, then builds **both original native C source and new C source** with strict C17 `-Werror`/UBSan. It executes the existing stage-cache parity suite and a new deterministic index/offset/mask Java/JNI differential on the actual Java owner against each native binary, in separate checked-JNI processes. It compares their complete outputs byte-for-byte, including a stable digest, to prevent the new backend from silently changing selection semantics. The new test targets 127/128/129 and 255/256/257 chunk seams, null/empty/rejected selections, arbitrary physical offsets, signed rows, duplicates, stride directions, and 9+ stage fallback.

## Donor catalogue, licensing and release boundary

`m3/collections/algorithm-catalog/native-bulk-pathmask-20261021.tsv` includes accepted, reused, rejected and deferred patterns from AOSP libcore, official JNI tips, existing Synexia/M3 code, LeetCode, HackerRank and GeeksforGeeks. No third-party source was copied. AOSP/native-bulk examples are design references, not performance evidence or license-free donors.

Pending: actual GitHub Java/C17 parity CI, full Maven/OpenRewrite replay/fixed-point/refusal, all collection Java/JNI modules, OpenJDK 21/HotSpot/image jtreg/JCK gates, JNI/GC stress and Android ART/ARM64/NDK PSS/RSS/energy/thermal benchmarks. The branch and PR must remain a draft until its focused acceptance gates pass and PR #1545 is reconciled.
