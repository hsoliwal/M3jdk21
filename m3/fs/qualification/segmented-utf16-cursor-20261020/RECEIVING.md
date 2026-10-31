# M3JDK21 M3FS composite UTF-16 cumulative-length index and cursor

**M3JDK21 is the product.** M3FS was merged into M3JDK21 as
`jdk.m3fs` by [product PR #1425](https://github.com/hsoliwal/M3jdk21/pull/1425).
No M3-on-JDK adapter, new String pool or filesystem module is introduced.
This is a one-owner, per-file refinement of the existing
`m3/fs/src/main/java/com/m3/fs/M3FsPrecomputeBundle.java`.

## Existing canonical geometry, re-used

`SegmentedUtf16String` already owns mapped immutable parent byte buffers
plus primitive parallel `parentIndexes`, `parentStarts`, `lengths` and
**cumulative `logicalEnds`**. `charAt(index)` correctly uses binary
search on these ends in O(log segments). `copyChars` already finds its
first segment once and copies forward. Do not introduce another rope,
prefix array, object per character, Java heap text payload, lexicon or
native pointer hierarchy.

The one behavioral atom is `contentEquals(CharSequence)`. Previously
it called `charAt(index)` for every UTF-16 code unit and repeated the
segment binary search. The candidate walks segment end coordinates
sequentially, and for a second mapped composite or `Range` it advances
**two primitive segment cursors** with exact byte-buffer UTF-16 reads.
The generic CharSequence path still calls its `charAt(i)` once, strictly
in ascending order, with the same mismatch short circuit. No flattening
or allocation, no changing the on-disk M3FS precompute format or JNI ABI.

For L compared UTF-16 units and S physical segments:
- random `charAt` stays O(log S), using the existing prefix end index;
- comparison with ordinary String/CharSequence becomes O(L+S);
- comparison with another segmented/ranged owner becomes O(L+S1+S2);
- auxiliary allocation is O(1), without new per-value fields.

## Recipe first, then product receiving

Reuse the existing first-party `M3HashPinnedJavaSnapshotRecipe` and
`M3HashPinnedYamlSnapshotRecipe` Maven/OpenRewrite engines, plus the
existing `com.synexia.m3.SegmentedImmutableStorage` source authority.
The new named recipe is
`com.synexia.m3.M3FsSegmentedUtf16IndexedEquals20261020`.
All complete Java compilation-unit postimages are source-sealed by SHA-256;
the exact existing M3FS owner Git preimage is
`71e2a7d3a7b9da5670ca1ffeeeee208941eba6d6`.
Do not hand-patch generated receiver files.

## Qualification

A new actual-M3FS mapped-page plain Java21 oracle admits 307 segments
over content-addressed immutable parents, including empty pieces, NUL,
surrogate halves, repeated text and unaligned subranges. It compares
generic callbacks, composite-composite/range, first/last mismatch and
copyChars/charAt against a separate Java oracle, then repeats the operation
512 times. The JUnit wrapper joins the existing M3FS regression module.
The source-sealed workflow runs `javac --release 21 -Xlint:all -Werror`
on the existing full Java owner set, then the oracle, and separately
runs `M3SegmentedUtf16FsTest` plus the new JUnit under the M3FS Maven
native build. No M3FS C source changes. Hosted execution and all
whole-JDK, GC, HotSpot, Windows and memory benchmarks are distinct gates.

GitHub/FOSS donor judgement: the existing balanced M3StringTuple, M3FS
mapped-prefix lanes and Synexia segmented-storage recipes are the
source owners. Finger trees/ropes are considered design references;
LeetCode/HackerRank/GFG are catalogue taxonomy only. No external
implementation source is copied. Existing Apache-2.0 copyright retained.
