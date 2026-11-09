<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3 index-path rank/select — recipe-first candidate

## Existing owner and identified gap

`M3IndexPathMasks` already selects caller-supplied signed/duplicate row IDs through
ordered paths, aligned physical offsets, multi-stage full 32-bit required/excluded masks
and an optional JNI gather kernel. `M3BitLane28` already supplies bit-rank/select over
sparse 28-bit owner slots. Neither is an immutable *occurrence-rank* view of the
results of a strided multistage path. Do not create another selector or collection
backend; this candidate composes their established semantic atoms.

## New implementation

`M3IndexPathRank` captures selected **path occurrences**, not row-ID equality, in a
compact `int[]` plus a `long[]` bitmap and `int[]` word-prefix directory. It exposes
exclusive rank, selected-source ordinal, selected row ID, exact detached selected-array
materialization, and immutable half-open source-ordinal windows that share backing
arrays. No retained per-entry objects, no source array exposure, no reference/payload
ownership, no collection ABI modification. The entire capture is single-threaded;
concurrent modification is outside contract. All path geometry and physical bounds
are preflight-checked. Signed row IDs and duplicates are legal.

Build: `O(n * stages)`, retained primitive arrays ~`4n + n/8 + n/16` bytes before
headers (plus a fixed prefix slot). Rank: `O(1)`. Select: `O(log(n/64)+64)` worst
case using binary search over word prefix and up to 63 bit clears. A window is O(1).
No benchmark speedup is claimed.

## Recipe and proof

Existing target mirror of the **Synexia-authored** OpenRewrite class-hook engine
`com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe` is reused. Crate:
`m3-index-path-rank-20261017`, which declares four sorted `ABSENT → SHA-256` Java
postimages in a strict manifest. Do not patch generated outputs. Change template,
recompute manifest, rerun recipe and requalify on exact next head. The Maven test
`M3IndexPathRankRecipeTest` verifies LST generation, hashes, replay fixed-point and
source-drift refusal. Local manifest verification is merely a preflight, **not**
execution of the OpenRewrite engine.

Pure Java 21 local compiler/test: `javac --release 21 -Xlint:all -Werror`, standalone
regression PASS on 900 random paths, 36,476 rank points, 10 random windows per path,
additional 64/128/256 word boundaries, duplicate/signed keys, mutable caller arrays
and bad geometry. Test harness has an independent loop oracle. Native integration
reuses *unchanged* `m3_index_path_masks.c` and the `M3IndexPathMasks.select(...,
NATIVE_REQUIRED)` ABI, rather than adding a second JNI selector. The separate
native self-test requires `-Xcheck:jni` and fails when the native library is missing.

NOT_RUN locally: Maven/OpenRewrite SDK/JUnit and JNI native runtime (Maven unavailable
in the current isolated environment), full M3 reactor, OpenJDK jtreg/HotSpot,
ABI/public-collection parity, benchmark/retained-heap proof. CI is configured to
execute the real Maven and JNI gates. This is a draft **internal adapter**, not
replacement of JDK `java.util`, nor runtime String→arrays→collections promotion.

## Algorithm and reuse accounting

See `m3/collections/algorithm-catalog/index-path-rank-20261017.tsv` for individual
categories, candidates, complexity, compatibility and licensing. LeetCode 303/338,
HackerRank Java BitSet and GeeksforGeeks prefix-sum are problem-category references
only: no challenge code or explanation is copied. Existing M3 Java/JNI owns behavior.

Copyright 2026 Hitesh Soliwal and contributors, Apache-2.0 in this independent
M3 library. OpenJDK and third-party upstream rights remain unaffected.
