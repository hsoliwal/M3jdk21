<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 CPU source-byte Aho-Corasick prefilter (opt-in)

## Existing owners retained
- `SourceArtifact.Span` remains the canonical immutable source/byte range.
- `BytePatternSearch` / `ByteEvidenceQuery` remain the authoritative exact
  offset and source-evidence projection APIs.
- `LfCount.install` is the sole SHA-256-pinned JNI installation authority.
- No `java.base`, `String`, regex, GC, optimizer or OS interning change.

## What this adds
`BytePatternSet.compile(List<byte[]>)` accepts **0..64 nonempty patterns**
(each 1..4096 bytes, total 16384). The input bytes are snapshotted, and a
transient trie is lowered to immutable **primitive** sorted sparse edges,
failure links and 64-bit output masks. No pattern byte[] survives in the plan.
`scan(SourceArtifact.Span)` reports exactly which patterns occur at least once,
including duplicate patterns, overlaps and bit 63. It returns **no offsets**;
resolve selected candidates with the existing KMP/evidence query when needed.
The retained primitive metadata is capped at **1 MiB**. Source text is never
UTF-8 decoded, normalized, allocated again, or placed in an extra cache.

`scanNative` is explicit, checks the existing pinned JNI install via `LfCount`,
and copies only bounded primitive automaton metadata into C11 work arrays. It
reads a short-lived direct-buffer source-range projection synchronously.
The new native symbol resides in independent `native/byte_pattern_set_jni.c`;
existing `lf_count_jni.c` and its ABI v1 exports are unchanged. Native memory
is released on all exit paths. No JNI pointers or arrays survive the call.

## Source and review lineage
Canonical source recipe: `hsoliwal/com.synexia` named
`com.synexia.rewrite.M3CpuBytePatternSet20261016`; existing class-backed
`M3HashPinnedJavaSnapshotRecipe` and `M3HashPinnedTextSnapshotRecipe`.
Donor comparison: existing first-party Apache-2.0 Synexia
`MIndexSegmentSearchAtoms.LiteralSet` (UTF-16 Aho-Corasick); code is newly
written for **raw byte** semantics, not copied from external challenges.
The catalogue also compares sequential KMP, dense ASCII automata, sparse
Aho-Corasick, Lucene pre-indexing and optional JNI. LeetCode 1032,
HackerRank Determining DNA Health, and GeeksforGeeks Aho-Corasick are only
problem-category/test-shape references and not source-copy authority.

## Test exactly this source
```sh
python3 m3/ports/capability-runtime/verify_native.py \
  --root m3/ports/capability-runtime \
  --out /fresh/external/m3-pattern-set-proof --developer-image
```
Strict Java 21, independent 2000-case naive oracle, C11 UBSan, `-Xcheck:jni`,
prior LF/line/KMP suites, and a freshly linked stock JDK21 JMOD developer-image
API smoke are required. The separately pinned Maven/OpenRewrite SDK fixed-point
and drift tests are mandatory for recipe admission. A stock linked JDK is **not**
a proven modified M3JDK HotSpot/GC/C1/C2/jtreg image. JNI performance gains
are unknown until setup-inclusive measurements show a crossover.
