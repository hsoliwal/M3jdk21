# Mandatory literal affixes: recipe handoff

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0. Original target Java payloads keep their own GPLv2/ClassPath headers.

## Candidate and exact preimage

The candidate in `after/` is a recipe input only. It stacks on M3JDK21 #1029, commit `3bbb4712fb20adba1405741db8457164b9f436f5`, retaining #1003's rolling cursor and #1004/#1029's minimum, edge, unit, pair and triple facts. No production repository path or GitHub object was edited by this task.

The complete target DP preimage SHA-256 is `9536a6df2042fe047f02ef07834edca91ce7a863e5b3c1daafffe55fdad48e6e`; candidate SHA-256 is `3029950078524419af52edbcbaaccab545bd5b1bed824ee80218250b8c748ef4`. The jtreg candidate SHA-256 is `42b8a284550c9f8eddb5610e6907a071b22d70650feb1ff2705abcbc17654bfe`. See `payload-manifest.json` for all exact pre/postimages.

## Selected behavior

The existing bounded Plan retains two additional int facts: the length of its uninterrupted leading mandatory literal run and the length of its uninterrupted trailing mandatory literal run. A dot or starred atom ends such a run. The suffix length comes directly from the existing gram-accumulation pass; the prefix uses a bounded scan of the existing atom lane. No literal copy, new string, new array, second text payload or second cache is retained.

After existing cheap length/edge/available-gram guards, a code-unit cursor checks remaining prefix units and then the suffix range in increasing source order. First and last source units are not reread. The existing forward cursor therefore reuses scalar intervals on both sides; no reverse cursor is required. With prefix and suffix lengths p and s, extra reads are max(0,p-1)+max(0,s-1), always bounded by the existing 128-pattern-unit admission cap.

Successful boundary checks are only necessary conditions. Matching continues through the existing exact bitset/effect or rolling DP. A definite ASCII literal boundary mismatch is a sound full-match rejection even if unrelated middle units contain a surrogate. A still-possible surrogate-containing match retains the existing UNKNOWN/Pattern path.

Before these scans, an exact existing whole-range effect may be reused. The early lookup retains weak owner identity, exact coordinate, exact Plan object and incoming frontier checks. UNKNOWN is deliberately excluded from this early return so newly prepared facts can still prove rejection. It is not converted to false. The later evaluator's unchanged UNKNOWN behavior remains authoritative.

A failed full-match guard is not stored as an arbitrary fragment transition: a suffix-based failure can still have nonaccepting live states that become accepting when another fragment follows.

## Verification and measured tradeoff

The stock-Java21 proof extracts complete before/after target classes, actual CodeUnitCursor and actual fact mixers. Only package/class naming is adapted; mocked immutable owners are instrumented for reads. It compiles the expanded product jtreg source as well. This fixture does not prove matched M3JDK/HotSpot/JNI delivery.

The proof passed 47,904 checks: independent Pattern parity; constructed matching witnesses and grammar-aware near misses; bitset and >32-atom rolling paths; line terminators; pairs and lone surrogates; shared DAG slices; prepared and absent facts; cache collisions in five identity lanes; unresolved and newly resolvable UNKNOWN; exact warm hits; work caps; and the retained primitive bound.

For a 2,057-unit source containing every mandatory gram, the cold prefix near miss reads 2,059 -> 3 units and the suffix near miss reads 2,059 -> 6. A 24-unit compatible affix scan reads exactly 24 scalar units and does not add root-charAt traversal calls. A warm exact root hit reads no source units. The derived retained-primitive upper bound grows from 600,064 to 602,624 bytes, a maximum increase of 2,560 bytes. This is not an object-layout or retained-heap measurement.

A preliminary local warm-hit probe exposed overhead in a guard-only version; the exact root-cache lookup then removed source rereads on exact warm hits. Small timing medians nevertheless vary across JVM runs. The captured final run in `warm-probe.log` measured short positive affixes 25.63 -> 16.46 ns, long positive affixes 45.33 -> 89.17 ns, repeated uncached prefix rejections 27.87 -> 37.04 ns, and cache-churn prefix misses 2,808.81 -> 59.46 ns. An earlier run of the same final source measured long positives 47.20 -> 33.00 ns, so no uniform CPU improvement is established by this fixture. These medians are diagnostic observations, not product throughput claims; the observed warm-negative overhead and unstable positive timing must remain visible. Compatible cold inputs also pay additional bounded comparisons. Whole-product workload and JVM compilation measurements remain required.

## Preserving inherited gates

Five inherited source-seal scripts must point to the exact successor DP/test images. Their historical baseline images stay unchanged. Two inherited #1029 checks were stale before this task: the amalgamation receiver required the old variable name `required` instead of `requiredSignals`, and its before/after memory assertion omitted the two gram longs added by #1029. The candidate retains these checks, correcting their exact expected source/total bound; it does not relax them. The full delta beyond #1003 is five ints and three longs per potentially retained Plan.

## Existing composite algebra and next meaningful query work

The first-party source inventory already supplies the relevant all-entry relation algebra. Synexia MIndexRegexAtomAlgebra stores q exit rows plus prefix-hit input bits, a restart/suffix frontier, an internal-find flag and UTF16 length. Its existing forward/prefix/find composition should not be recreated in a new engine. The suffix-acceptance predicate can be obtained from its restart frontier and accepting states, subject to the same machine semantics and boundary contract.

For a regular machine with context-independent transition relation R, forward frontier I and accepting mask A, define the backward set B as the states q for which R_suffix(q) intersects A. A row then matches in fixed context precisely when R_row(I) intersects B. Ordered composition supports preimage propagation from the right and frontier propagation from the left. This is a useful prepared-query projection of existing relations, not a reason to mutate cached all-entry rows: those rows must remain valid for other callers and suffixes.

Java Pattern's codepoint behavior makes a plain UTF16 state mask insufficient at arbitrary fragment seams. Target M3RegexDpPlan.Effect already carries its normal rows, a conditional withoutLeadingLow lane, UTF16 length and a trailingHigh flag. Fixed prefix/suffix composition and any bidirectional pruning must preserve these lanes until seams are resolved; treating each fragment as independently closed would mis-handle split surrogate pairs. In particular, the existing numeric-only M3RelationImage cannot silently serve as a richer UTF16 effect codec without including the boundary flags/lanes in a versioned, hash-bound contract.

The sibling batch task is extending the existing target M3RegexDpPlan/JNI owner with prepared CSR sequences of effect IDs and optional fixed prefix/suffix Effects. That is the immediate full-composite reuse opportunity. Any future backward projection must be derived after boundary-aware composition, must carry exact program/state/Unicode and resource-budget identities, and should reuse Synexia's existing algebra/codec owners. A new generic prefix/suffix matcher or tokenizer is unnecessary.

## Publication and phase limits

Root should qualify and emit all templates through the existing canonical Synexia recipe machinery, record refusal/fixed-point receipts and source identities, then run inherited/new receiver checks. Product jtreg with M3 storage and CompactStrings variants, matched native/HotSpot execution, full regex compatibility, product CPU/heap/process-memory measurements and the whole String receiving stage remain explicit pending gates. No later family phase or full-borrow row is advanced by this packet.

The platform catalogue records first-party inventory first, then LeetCode, HackerRank and GeeksforGeeks problem categories. It contains original descriptions and independently authored tests; no platform solution code was copied.

## Qualified Java proof layout

Both active Java proof inputs live under `src/test/java` so the existing canonical Java snapshot recipe admits their paths without any relaxation of its path rules:

- `m3/runtime-integration/regex-mandatory-affixes/src/test/java/RegexAffixBeforeAfter.java`
- `m3/runtime-integration/regex-plan-amalgamation/src/test/java/RegexAmalgamationBeforeAfter.java`

The historical flat `m3/runtime-integration/regex-plan-amalgamation/RegexAmalgamationBeforeAfter.java` remains untouched in the target. The corrected bound belongs to a new successor under `src/test/java`, and the active amalgamation Python runner selects that successor. Its original bytes are retained under this packet's `history/`, outside the before/after emission trees, so no delete or rewrite of the historical target is requested. The packet still contains 11 target payloads: seven modified paths and four new paths.

The first-party catalogue URL is pinned to reviewed Synexia commit `11c94feb18155a53cc213708c353791132edc4a7`, with canonical branch `develop`; both reviewed owner blobs were rechecked at that exact commit.
