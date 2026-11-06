# M3TokenCursor seek-locality contract — candidate/HOLD

## Source owner and invariant
Existing original owner: `m3/ports/regex-cursor/src/main/java/com/m3/regexcursor/M3TokenCursor.java`, receiver hsoliwal/M3jdk21, parent PR #2332. No other runtime owner, public method, source storage lane, JNI entry point or TokenPattern implementation may change. The only candidate change is inside existing `seek(int)`.

Before and after must preserve: `Objects.checkIndex(position, total + 1)` range and diagnostic; empty-cursor behavior; cursor offset and unit semantics for every `0 <= position <= total`; exactly the same handling of `position == total` (last nonempty chunk); public Java bytecode descriptors; retained inner-lane identity; `M3CursorFacts` presence mask; `M3ChunkCursor` interface; original Java21/JNI output and error behavior. This leaf does NOT fix the pre-existing `total+1` integer overflow corner at total=Integer.MAX_VALUE; changing that exception semantic is outside scope and requires a separate contract review.

## Algorithm
All nonempty lane start offsets are already stored in sorted primitive `starts[]`. The cursor has a mutable current lane. Before the existing canonical binary-search fallback, check current lane, one immediately following lane, and one immediately preceding lane. No extra arrays, caches, allocations, or JNI calls. Keep fallback for all other positions. Because `seek(total)` targets the final lane, guard its boundary explicitly. A locality hint is never semantic evidence: if the hint cannot prove the target is in its interval, use the unchanged binary search.

## Verification order
1. Read exact full owner and pin Git blob plus SHA-256 before any edit.
2. Documentation, source diff, strict Java21 `-Xlint:all -Werror` compilation.
3. Independent Java binary-search oracle for seeded random and exhaustive small lane geometries, source ranges and boundary jumps; compare original and new checksums.
4. Public `javap -public -s` equality, existing Java grammar/projection/regex regression, test-only C17/JNI checked and interpreted oracle.
5. Measure same-lane, neighbor-lane and global-random seek timing without claiming universal speedup. If random fallback regresses materially on this host, do not publish.
6. Canonical Synexia Maven/OpenRewrite hash-pinned recipe, fixed point, source drift and occupied-target refusals. Real SDK/JUnit/full JDK and cross-platform native require external evidence and remain HOLD.

## Donor intake
LeetCode 34 (range boundaries), 704 (binary search), HackerRank sparse arrays and binary search categories, GeeksforGeeks exponential/galloping search categories: **problem/test-shape review only**, no source code copied. Algorithm derives from preexisting first-party `starts[]` and JDK21 `Arrays.binarySearch` semantics.
