<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21 known-slice JNI array materialization — 2026-10-21

Status: **draft source candidate, installed-image/HotSpot/JNI gates pending**.

M3JDK21 is **one integrated Java 21 SDK**, not M3 plus a wrapper JDK. Canonical `java.lang.M3String` remains exactly two instance fields, `M3StringOwner owner` and `long value` (packed range). Existing parent-owned fixed long[] immediate pages, OS-mapped lexicon atoms, tuple compositions, native snapshots and the `M3StringPool.concatViewsNative0` zero-payload-copy concatenation remain authoritative.

Two distinct contracts are implemented:

1. **Persistent composition**: joining already interned M3String values retains canonical owner/index/offset/length descriptors, no new text payload, no artificial `byte[]` or `char[]` backing. This is the default M3String representation.
2. **Explicit compatibility materialization**: when an API truly requires a mutable Java primitive array, exactly **one final** `char[]` or `byte[]` is allocated. Four package-internal Java-to-JNI primitives take two specified slices, validated before writes: `concatKnownChars`, `concatKnownBytes` (actual Java primitive arrays), `concatMappedUtf16`, `concatMappedBytes` (already-known direct/native/OS-mapped windows).

The mapped UTF-16 operation interprets each input as exact **UTF-16 code units**, big-endian or little-endian separately, preserving unpaired surrogate units and split surrogate-pair seams with no transcoding. Mapped byte copying preserves raw bytes. Source offsets/lengths are in code units for UTF-16, bytes for raw bytes. Heap JNI uses `GetCharArrayRegion`/`GetByteArrayRegion`; direct JNI reads `GetDirectBufferAddress` and sends bounded chunks to the newly allocated output Java array. No per-slice character arrays, no `GetPrimitiveArrayCritical`, no Java heap pointer persistence, no unqualified OS virtual remapping.

**Lifetime and identity:** direct buffers must refer to live, immutable canonical source storage for the whole synchronous call; strong JNI arguments keep the Java owner reachable. Caller/owner must forbid explicit concurrent unmapping, external file truncation or concurrent source mutation. The final array is independently mutable and consumes memory intentionally; ordinary JDK21 `String` has a `byte[] value` field, not a `char[]` instance field, and never permanently owns both full flattened payloads.

**Design rationale and donor reuse:** prefer existing `synexia-indexstring/native/src/mindex_array_flatten_jni.c` + `MIndexNativeArrayFlatten.java` for the general Java-array slice problem and `SharedArrayNative.copyTo`/`synexia_shared_view_join` for native/OS-backed views. That source is Apache-2.0 and is an **algorithmic reference**; JDK product `String.c` code is independently authored under its existing GPLv2-with-Classpath source header rather than copying incompatible licensed implementations. LeetCode concatenation-of-array and HackerRank arrays category are taxonomy, not code grants. Android JNI/ART design informs *batching and limited crossings*, not ART-only annotations or a second runtime.

**Review and proof:** canonical Maven/OpenRewrite source-sealed recipe in Synexia, original JDK21 Java/C sources, `M3StringKnownSliceConcatJNI` jtreg with randomized heap slices, arbitrary UTF-16/NUL/surrogate seams and a read-only file mapping. The new static JNI methods are opt-in internal export boundaries, not forced onto every `String.concat` hot path; no claimed speed gain until setup-inclusive Java/JNI CPU, heap, native, process, GC and Android measurements pass. Source-built JDK21 native library, full jtreg/HotSpot, Windows/Linux/Android and ABI proof are **NOT_RUN** until executed.

Related stacked receiver: M3JDK21 `m3/fixed-shortstring-pages-20261020`, draft PR #1536.
