<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3JDK21 finite in-memory sorting catalogue

Snapshot: **2026-10-18**. Schema: `m3-sort-catalogue-v1`.
Canonical task recipe: `m3-java-util-sorts-20261018`.
Target source baseline: `hsoliwal/M3jdk21@2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.

This catalogue defines a finite registry of **34 named candidate algorithms: 28 comparison-capable variants and six numeric-key variants**. It covers registered standard in-memory families, including TimSort and quicksort, with distinct implementation variants recorded explicitly. It does not claim to enumerate all conceivable sorts, every historical nickname, or all hybrid compositions.

The matching `sort-catalogue.tsv` contains exactly one row per algorithm ID. Its first non-comment line is the header; lines beginning with `#` are metadata. Names, stability and complexity below are the candidate contracts to qualify against the exact generated source. **This catalogue is not a compiler, test, benchmark, mastery, JNI or product-acceptance receipt.** Executed evidence and missing gates belong in the task's separate qualification records.

## Ownership, naming and rights

The canonical reusable recipe, payload templates, catalogue, fixtures and convergence evidence belong to `hsoliwal/com.synexia`, under `synexia-openrewrite-recipes/recipes/m3-java-util-sorts-20261018`. M3JDK21 owns the target `java.util` implementation, requested SDK extension, bootstrap/module compatibility and final product gates. A target-local copy is a pinned receiver or proof copy, never a second portable recipe owner.

The candidate surfaces are `java.util.Sorts` and `java.util.SortAlgorithm`, with internal index-lane kernels. Existing Java SE names and their established sort contracts remain authoritative. This requested additive SDK work does not advance the separate ordered String -> arrays -> collections -> AST/compiler receiving plan, establish backend-migration parity, or introduce a Synexia runtime dependency.

This original catalogue and recipe documentation are Apache-2.0, with **Copyright 2026 Hitesh Soliwal and contributors**. Target source payloads carry their own GPL/OpenJDK-compatible headers. The OpenJDK TimSort donor retains its Oracle and Google copyright and GPL-2.0-only WITH Classpath-exception-2.0 notice. A recipe or catalogue license does not relicense its inputs, donor bodies or generated output. Algorithm concepts are recorded as design lineage; this notice concerns authored expression.

The source-role column distinguishes:

- **ADAPT:** exact OpenJDK source is the implementation donor, with its header and lineage retained.
- **EVIDENCE_ONLY:** a pinned implementation, publication or category informs review. The row does not authorize or claim copying its source.
- A reference marked **FAMILY_REFERENCE** may describe a related variant; it is not a claim that the referenced file implements this row's exact variant.
- Rejected donor bodies remain useful counterexample evidence and do not become admitted implementation text.

## Shared index and payload model

Every candidate kernel orders an `int[]` permutation of the input range. The payload remains the original parent-owned array or object references; the permutation is applied by cycles only after key extraction, comparison and pre-commit progress checks finish. This keeps the algorithm's work on indices and offsets and provides one place to preserve payload identity and range boundaries.

For complexity below:

- `n` is the range length; logarithms mean `log(max(2,n))`.
- `d` is the number of distinct ordering keys.
- `k` is the inclusive histogram key span.
- `b <= 256` is the bucket count, and `n_i` is the population of bucket `i`.
- `R = 256` is the byte radix; `w <= 8` is the number of byte levels.
- `v` is the number of visited MSD/American-flag distribution nodes.

**All complete calls use O(n) common permutation storage.** Numeric-key calls additionally retain an O(n) canonical `long[]` key snapshot. The tables show extra kernel workspace beyond those common lanes. Therefore, an O(1)-workspace heap or insertion kernel does not make the complete indirect sorting call O(1)-space. `IN_PLACE_MERGE` and `AMERICAN_FLAG` describe their treatment of the index lane, not zero-copy or constant-space end-to-end payload sorting.

Stability is an ordering guarantee for equal keys and original occurrence identities. Merely returning numerically identical primitive values cannot demonstrate stability. A stable row requires original-index and distinct-object tie tests. An unstable row promises ordered permutation preservation but does not promise an ordering among ties.

Comparator-capable rows use only the sign of comparisons. A comparator result must never be interpreted as a metric, subtracted as a numeric key, or required to be exactly -1, 0 or 1. Supplied key/comparator behavior, exceptions, null handling and range contracts require their own target tests.

## Registered comparison-capable variants

| ID | Family | Exact candidate variant | Stable | Worst-case kernel work | Extra kernel space |
| --- | --- | --- | --- | --- | --- |
| `TIM` | MERGE | OpenJDK adaptive TimSort specialized to primitive indices | Yes | O(n log n) | O(n) |
| `DUAL_PIVOT_QUICK` | PARTITION | Deterministic dual-pivot partitioning with depth-bounded heap fallback | No | O(n log n) | O(log n) |
| `QUICK` | PARTITION | Median-of-three three-way quicksort; smaller-side recursion | No | O(n^2) | O(log n) |
| `INTRO` | PARTITION | Three-way introsort with heap fallback | No | O(n log n) | O(log n) |
| `MERGE` | MERGE | Top-down buffered merge | Yes | O(n log n) | O(n) |
| `BOTTOM_UP_MERGE` | MERGE | Iterative bottom-up buffered merge | Yes | O(n log n) | O(n) |
| `NATURAL_MERGE` | MERGE | Natural-run detection and repeated stable merges | Yes | O(n log n) | O(n) |
| `IN_PLACE_MERGE` | MERGE | Stable binary-cut divide-and-rotate merge | Yes | O(n log^2 n) | O(log n) |
| `HEAP` | HEAP | Binary heap construction and extraction | No | O(n log n) | O(1) |
| `SHELL` | INSERTION | Knuth gaps 1,4,13,... with bounded growth | No | O(n^2) conservative upper bound | O(1) |
| `INSERTION` | INSERTION | Straight insertion | Yes | O(n^2) | O(1) |
| `BINARY_INSERTION` | INSERTION | Upper-bound binary insertion | Yes | O(n log n) comparisons; O(n^2) moves | O(1) |
| `SELECTION` | SELECTION | Minimum selection and swap | No | O(n^2) | O(1) |
| `DOUBLE_SELECTION` | SELECTION | Bidirectional minimum/maximum selection | No | O(n^2) | O(1) |
| `BUBBLE` | EXCHANGE | Adjacent-exchange bubble | Yes | O(n^2) | O(1) |
| `COCKTAIL` | EXCHANGE | Bidirectional adjacent-exchange cocktail shaker | Yes | O(n^2) | O(1) |
| `COMB` | EXCHANGE | Shrinking-gap comb | No | O(n^2) | O(1) |
| `GNOME` | EXCHANGE | Adjacent-exchange gnome | Yes | O(n^2) | O(1) |
| `ODD_EVEN` | NETWORK | Odd-even transposition passes | Yes | O(n^2) | O(1) |
| `CYCLE` | SELECTION | Duplicate-aware cycle placement | No | O(n^2) | O(1) |
| `PANCAKE` | SELECTION | Prefix-reversal pancake | No | O(n^2) | O(1) |
| `BINGO` | SELECTION | Move each group of equal minimum keys together | No | O(n d); O(n^2) when all keys differ | O(1) |
| `TREE` | TREE | Flat-array AVL tree with every occurrence retained | Yes | O(n log n) | O(n) |
| `TOURNAMENT` | TREE | Non-padded flat winner tree with original-position ties | Yes | O(n log n) | O(n) |
| `PATIENCE` | MERGE | Strictly-greater pile placement and heap extraction | Yes | O(n log n) | O(n) |
| `STRAND` | MERGE | Stable strand extraction and merging | Yes | O(n^2) | O(n) |
| `BITONIC` | NETWORK | Arbitrary-length bitonic compare-exchange recursion | No | O(n log^2 n) | O(log n) |
| `ODD_EVEN_MERGE` | NETWORK | Batcher network with pruned virtual suffix wires | No | O(n log^2 n) | O(log n) |

The dual-pivot row is a deterministic partition/heap hybrid. It is not an alias for the complete OpenJDK `DualPivotQuicksort` implementation, which also contains type-specific and adaptive mechanisms. `QUICK` remains plain three-way quicksort with an O(n²) time bound; smaller-side recursion bounds its stack without improving that time bound. `INTRO` consumes the partition-depth budget in every recursive and tail-iterated partition.

`IN_PLACE_MERGE` is the binary-cut, divide-and-rotate stable merge variant; the documented O(n log² n) bound deliberately does not claim Grailsort/WikiSort's distinct block-merging guarantee. The Shell row names its Knuth gap sequence and records a conservative O(n²) upper bound. Binary insertion separates comparison work from movement work.

Tree and tournament structure belongs in flat primitive lanes. An equal-key occurrence is retained as a distinct original index. Patience sorting uses strictly-greater pile placement and original-position tie-breaking on extraction. The bitonic implementation handles arbitrary lengths with a greatest-power-below-length merge; the odd-even merge network prunes virtual suffix wires using wide index arithmetic. Neither requires a real key value as a padding sentinel.

## Registered numeric-key variants

All six rows consume canonical signed `long` ordering keys. Numeric entry points may also select a comparison-capable row. A comparator-only entry point cannot supply the required numeric domain by inference.

| ID | Exact candidate variant | Stable | Worst-case kernel work | Extra kernel space |
| --- | --- | --- | --- | --- |
| `COUNTING` | Stable histogram and cumulative placement | Yes | O(n + k) | O(n + k) |
| `PIGEONHOLE` | Stable linked index holes | Yes | O(n + k) | O(n + k) |
| `BUCKET` | At most 256 ordered bit-range buckets; stable scatter and insertion | Yes | O(n + b + sum(n_i^2)); O(n^2 + b) worst case | O(n + b) |
| `LSD_RADIX` | Eight stable least-significant-byte passes | Yes | O(w (n + R)) | O(n + R) |
| `MSD_RADIX` | Stable most-significant-byte distribution | Yes | O(w n + R v) | O(n + w R) |
| `AMERICAN_FLAG` | In-place most-significant-byte index distribution | No | O(w n + R v) | O(w R) |

For nonempty counting/pigeonhole inputs, the admitted span is `1 <= k <= 2^20` (`MAX_HISTOGRAM_SPAN = 1 << 20`). Wider or signed-overflowing spans are explicitly rejected with `IllegalArgumentException` before payload commit. There is no silent switch to quicksort, TimSort or radix sort under the selected histogram algorithm ID. Empty inputs require no histogram.

Bucket distribution treats `max - min` as an unsigned span, computes its bit width, and uses `shift = max(0, bitWidth - 8)`. The bucket ID is `(key - min) >>> shift`; this yields at most 256 ordered buckets across the signed key domain. Stable scattering followed by stable insertion within each bucket gives the stated skew-sensitive bound. It is not a promise of linear time on arbitrary distributions.

Byte-radix paths normalize the sign bit for unsigned byte distribution. Floating-point entry points derive total-order keys with canonicalized NaNs: negative zero precedes positive zero, and all NaNs follow positive infinity, as in Java 21 `Float.compare` / `Double.compare`. The permutation is applied to the original payload. Canonical ordering keys are never used to reconstruct payload values. Histogram suitability is based on the transformed key span, not the apparent closeness of displayed floating-point values.

Payload movement uses ordinary Java floating-point copying and follows the platform's signaling-NaN semantics. Java 21 documents that copying a signaling NaN may quiet it and change its representation on some processors; see [`Float.intBitsToFloat`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Float.html#intBitsToFloat(int)) and [`Double.longBitsToDouble`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Double.html#longBitsToDouble(long)). Any exact raw-bit preservation result qualifies only the JVM and processor configuration recorded in that receipt. It does not establish an unconditional bit-preservation guarantee across Java platforms.

## Pinned implementation and publication references

Git blob IDs below are Git object identities, not content SHA-256 values. The recipe preimage/postimage manifest separately binds exact file bytes by SHA-256. A mutable branch URL alone is not an implementation pin. References without source-code adoption are explicitly evidence-only.

| Key | Source and exact pin | Rights and role |
| --- | --- | --- |
| JDK-TIM | [Fork TimSort source at target baseline](https://github.com/hsoliwal/M3jdk21/blob/2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49/src/java.base/share/classes/java/util/TimSort.java); Git blob `7cce1cc450cb2894506f6f56140f4bdc938b703b` | Actual ADAPT donor. GPL-2.0-only WITH Classpath-exception-2.0; Oracle ©2009, 2013 and Google ©2009 notices retained. |
| TA-JAVA | [Sorting directory](https://github.com/TheAlgorithms/Java/tree/8dbdc51edea193d103fe212c897f1a84ae479ce6/src/main/java/com/thealgorithms/sorts) at commit `8dbdc51edea193d103fe212c897f1a84ae479ce6`; individual source blobs are in the TSV. [LICENSE](https://github.com/TheAlgorithms/Java/blob/8dbdc51edea193d103fe212c897f1a84ae479ce6/LICENSE) blob `f6bcf04e7773b3bfca7a30836672dc7322e38944` | MIT, ©2021 The Algorithms; preserve individual authors on any separately admitted reuse. EVIDENCE_ONLY in this packet. The 54 directory files include helpers and operations that are not full permutation sorts. |
| FASTUTIL | [`drv/Arrays.drv`](https://github.com/vigna/fastutil/blob/e70f36a137c349efa50f346818d16917a9f91986/drv/Arrays.drv) at commit `e70f36a137c349efa50f346818d16917a9f91986`; blob `5d25ae088264d459afa3c93f235fbf9df19cab27` | Apache-2.0, ©2002–2026 Sebastiano Vigna, plus the file's additional CERN ©1999 sorting/search permission notice. Direct, indirect and paired-array radix design evidence; no source body imported by this catalogue. |
| WIKISORT | [Exact Java blob](https://api.github.com/repos/BonzaiThePenguin/WikiSort/git/blobs/9f69b8f96258eeeb0ba4d019f6060488b83e6ffc); [license blob](https://api.github.com/repos/BonzaiThePenguin/WikiSort/git/blobs/cf1ab25da0349f84a3fdd40032f0ce99db813b8b) | Unlicense/public-domain dedication. Rotation/block-merge reference only; this packet does not register a WikiSort implementation. |
| NIST-BINGO | [NIST bingo sort definition](https://xlinux.nist.gov/dads/HTML/bingosort.html), reviewed 2026-10-18 | Institutional algorithm definition and selection-family relationship. No source-copy grant or immutable code revision is asserted. |
| BATCHER-1968 | K. E. Batcher, [Sorting networks and their applications](https://doi.org/10.1145/1468075.1468121), AFIPS 1968, pp. 307–314; DOI `10.1145/1468075.1468121` | Original bitonic/odd-even merging-network lineage; publication evidence only, not imported source. |
| RADIX-1993 | P. M. McIlroy, K. Bostic and M. D. McIlroy, [Engineering Radix Sort](https://www.usenix.org/publications/compsystems/1993/win_mcilroy.pdf), Computing Systems 6(1), 1993, pp. 5–27 | Original byte-distribution/American-flag lineage. Publication evidence only; donor benchmark figures are not M3 measurements. |
| JDK21-CONTRACT | [Java 21 Arrays specification](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html) | Stable object sorting, half-open ranges and primitive total-order reference. Documentation reference; not a new algorithm or source grant. |

### Observed donor incompatibilities retained as review evidence

The following findings concern the exact TA-JAVA snapshot above. They explain why catalogue discovery is not bulk source admission.

| Source | Observed mismatch | Required target distinction |
| --- | --- | --- |
| `TimSort.java` | The file explicitly describes itself as simplified and uses fixed 32-element insertion chunks followed by bottom-up merging. | Use genuine OpenJDK adaptive TimSort for `TIM`; do not relabel this simplified algorithm. |
| `IntrospectiveSort.java` | Its final depth parameter is reduced in recursion but not on the left tail-loop path. | Every partition consumes the target introspective budget; retain an equal-key/adversarial regression. |
| `QuickSort.java` | Calls `Math.random()`. | The registered quicksort path has deterministic median-of-three pivoting. |
| `TreeSort.java` plus `BSTRecursiveGeneric` | The BST ignores equal keys, so multiplicity and object occurrences can be lost. | Keep every original index; duplicate values never become set membership. |
| `SpreadSort.java` | Uses comparison-result magnitudes as distances, with unchecked integer multiplication and no universal shrinking guarantee. | The generic source body is rejected; any future genuine Spreadsort requires an explicit numeric/key model. |
| `CountingSort.java` | Computes `max - min + 1` in an `int` and returns a separate result array. | Prove bounded signed-key span and the target mutation/return contract independently. |
| `RadixSort.java` | Handles nonnegative decimal `int` keys only. | The registered byte-radix paths support the declared canonical signed-long domain. |
| `TournamentSort.java` | Computes padded powers of two and doubled allocation sizes using unchecked `int` arithmetic. | The target uses a non-padded winner tree and checked size/index arithmetic. |

### Additional named candidates: not registered or qualified here

These are explicit future dispositions, not unimplemented enum constants or aliases for an existing row.

| Candidate family | Reference/pin and rights | Disposition |
| --- | --- | --- |
| PowerSort and PeekSort | [Java research repository](https://github.com/sebawild/nearly-optimal-mergesort-code/tree/7fcabfa141080ea8379ba6acdaab3ef44c94567d/src/wildinter/net/mergesort), commit `7fcabfa141080ea8379ba6acdaab3ef44c94567d`; PowerSort blob `a4f7ea7012569a191954d186ab452faf01ea4871`; PeekSort blob `1abc9d030c3b84a2236eb8efa41a2c1d63b8be1b`; MIT ©2018 Sebastian Wild. [Original paper](https://arxiv.org/abs/1805.04154). | NOT_REGISTERED. Qualify inclusive-to-exclusive ranges, shared static thresholds and large-index arithmetic. The same repository's self-described JDK-copy Timsort file is not automatically relicensed by its root MIT file. |
| PDQsort | [Exact header blob](https://api.github.com/repos/orlp/pdqsort/git/blobs/36360cd1d69d0bd6977d9ba2313b10612b889142), Zlib, ©2021 Orson Peters | NOT_REGISTERED. Needs a genuine Java adaptation or separately qualified native path; preserve alteration and origin notices. |
| Grailsort | [Exact original header blob](https://api.github.com/repos/Mrrl/GrailSort/git/blobs/368f439ca87b7fccdb727404671c5fbbc4c0af5f); MIT ©2013 Andrey Astrelin | NOT_REGISTERED. Original donor is C-compatible `GrailSort.h`, not Java. Qualify low-distinct-key behavior, constant/fixed/dynamic-buffer variants separately. |
| WikiSort | WIKISORT pin above; Unlicense | NOT_REGISTERED. A rotation helper or `IN_PLACE_MERGE` does not constitute the complete block-merge algorithm. |
| Genuine Spreadsort | [Boost integer-sort header blob](https://api.github.com/repos/boostorg/sort/git/blobs/f49f37d76691c6cbbf562ef508ad8519b7b6bd4b); BSL-1.0, ©2001–2014 Steven J. Ross | NOT_REGISTERED. Native numeric distribution hybrid; the TA generic comparison-distance version is not a replacement. |
| Quadsort | [README blob](https://api.github.com/repos/scandum/quadsort/git/blobs/c3c34144880d9c216512161e920d55966fbc1624), [license blob](https://api.github.com/repos/scandum/quadsort/git/blobs/cf1ab25da0349f84a3fdd40032f0ce99db813b8b); Unlicense | NOT_REGISTERED. README/license discovery only; implementation pin and target qualification still required. |
| IPS⁴o / samplesort | [README blob](https://api.github.com/repos/ips4o/ips4o/git/blobs/ac3d17b3d714704e505dc87759480ade3754c06d); [license blob](https://api.github.com/repos/ips4o/ips4o/git/blobs/5480b69c2201b0d8ebf2edf2187116b1aa40f6ad), BSD-2-Clause, ©2017 Michael Axtmann, Daniel Ferizovic and Sascha Witt | NOT_REGISTERED. Native/parallel implementation, platform, allocation, scheduling and determinism gates remain separate. |
| Smoothsort, library sort, circle sort, Flashsort, bead sort, stooge sort and slow sort | Named in TA-JAVA's pinned directory; MIT source candidates, with exact file/contract review required before adoption | NOT_REGISTERED. They are not aliases for the 34 entries. Domain/resource limits, genuine algorithm behavior and independent tests must be established before a successor expands the registry. |
| Binary radix exchange, multiway merge, merge-insertion/Ford–Johnson, spinsort, flat stable sort and other named hybrids | Additional catalogue research required | NOT_REGISTERED. No reviewed source pin or implementation claim is made here. |

## Scope exclusions

- **External, replacement-selection, disk/tape and distributed sorting:** require I/O, run ownership, failure recovery and resource protocols beyond this array-range registry. In-memory merge helpers do not implement those protocols.
- **Parallel, SIMD, GPU and JNI versions:** are execution variants with separate thresholds, scheduling, memory-transfer, ABI and platform evidence. This Java catalogue does not claim an accelerated backend.
- **Topological sorting:** produces an order constrained by a graph. It needs a graph contract, cycle behavior and its own API.
- **Quickselect/top-k:** provides partial ordering or selection; it does not satisfy a complete sorting contract.
- **Wave/wiggle arrangements:** satisfy an alternating relation, not a total ordering.
- **Dutch national flag / three-color partitioning:** is a useful three-domain partition primitive, not an additional arbitrary-domain full-sort algorithm in this registry.
- **Stalin sort:** discards elements and therefore fails the permutation/multiplicity contract.
- **Bogosort, bogobogosort, sleep sort and other timing/randomness or joke methods:** do not meet this deterministic finite-work sorting scope. A bounded teaching demonstration would require a separately named limited contract.
- Recursive/iterative spelling aliases, natural-order versus comparator overloads, primitive specializations and alternate entry-point names are not counted as new algorithms unless their implementation variant is separately registered and qualified.

## Ordered category evidence

Existing Synexia and OpenJDK owners precede external discovery. The task follows the LeetCode -> HackerRank -> GeeksforGeeks category-review order, then evaluates pinned GitHub references. The following pages were inspected as problem/category evidence on 2026-10-18:

| Stage | Page | Use and boundary |
| --- | --- | --- |
| LeetCode | [912: Sort an Array](https://leetcode.com/problems/sort-an-array/) | General integer ordering and duplicate-preservation challenge category. Its bounded domain and time requirement do not define the broader SDK contract. |
| HackerRank | [Quicksort 1: Partition](https://www.hackerrank.com/challenges/quicksort1/problem) | Partition invariants. This challenge alone is not a full sorting algorithm and its sample constraints do not replace duplicate/identity tests. |
| HackerRank | [Counting Sort 1](https://www.hackerrank.com/challenges/countingsort1/problem) | Histogram category. Its output is a frequency array, not the target payload-permutation contract. |
| GeeksforGeeks | [Sorting algorithms taxonomy](https://www.geeksforgeeks.org/dsa/sorting-algorithms/) | Comparison, distribution and hybrid category checklist; specialized relations remain separate. |

No challenge statement, editorial, submitted solution, downloaded challenge corpus or test-data body is copied into the implementation or fixtures. Independently authored adversarial inputs exercise the algorithm contracts. Category discovery is not source-copy permission or evidence that every page, family or target has passed.

## Required qualification and evidence boundaries

Qualification must bind the exact target baseline, per-file recipe postimages, immutable independent oracle fixtures, compiler/runtime configuration and declared budgets. Improve the canonical recipe and replay fresh inputs after a defect; do not patch materialized target outputs by hand.

At minimum, exact-source receipts must cover:

1. **Sortedness and bijection:** every output index occurs once, range prefixes/suffixes remain untouched, duplicate occurrences and aliased object references survive cycle application.
2. **Stability:** distinct original identities with equal comparator/key values; equality intentionally different from comparator equality.
3. **Range and arithmetic boundaries:** empty/singleton/interior ranges; powers of two and their neighbors; key extremes; histogram bound and overflow refusals; network virtual-wire arithmetic.
4. **Independent adversarial shapes:** sorted/reversed, all equal, few distinct, organ-pipe, sawtooth, alternating extremes, skewed buckets and adversarial pivot/natural-run sequences. For networks, exhaustively enumerate all small binary inputs under a declared finite budget.
5. **Comparator and key semantics:** results of arbitrary positive/negative magnitude; custom orderings; lawful null-aware comparators where supported; comparator/key exceptions before payload commit.
6. **Floating-point payloads:** infinities, both zeros, positive and negative NaNs with multiple payloads; order via Java's total-order oracle and before/after raw-bit multiplicity observations. Record the JVM/processor configuration and its signaling-NaN copying behavior. Exact-bit observations apply only to that tested configuration.
7. **Progress and mutation boundary:** cancellation/monitor failure before commit, late final progress flush, and no callbacks during cycle application if unchanged payload on callback failure is promised.
8. **Recipe mastery:** real Maven/OpenRewrite parsing/execution, exact postimages, absent-before admission, source-drift refusal, deterministic serial replay, bounded source-order/worker schedules, same-failure counterexamples and a full no-change fixed-point pass.
9. **Target gates:** Java 21 source/API/behavior proof and the applicable actual M3JDK build, jtreg and runtime gates. Patch-module/source fixtures alone do not establish complete custom-JVM or full-reactor acceptance.
10. **Performance when claimed:** measured cold key/permutation preparation, kernel work, payload application, allocation/retention and any native transfer cost. Comparator counts are separate from wall-clock time; correctness is separate from speed and memory gains.

This packet does not declare any unexecuted item passed. JNI parity becomes mandatory for a later admitted native follow-up; an all-Java implementation does not acquire a fictitious JNI pass or a native requirement solely from a catalogue label.

## Canonical mastery and policy references read for this packet

| Authority | Observed identity | Applied distinction |
| --- | --- | --- |
| Synexia `.claude/skills/m3-recipe-mastery/SKILL.md` | Git blob `7a5d73ad8ddb6fb2424b3953622efc8829434b8a` | Master a class-backed recipe; immutable fixtures and exact failures; independent files, serial per-file mutation. |
| Synexia `synexia-m3-recipe/docs/M3_RECIPE_FIRST_MASTERY_INVARIANT.md` | Git blob `09a9d0f9fd5da017f68e6050859b065ba3afc9fb` | Separate coverage, correctness and custody; finite evidence does not grant unrestricted semantic equivalence. |
| Synexia `docs/M3-SCALE/invariants/M3-ALGORITHM-TASK-MASTERY-ADMISSION.md` | Git blob `1e23886bc3496d20d25c13f6029cbe6426b2b12f` | Category and donor evidence nominate work; source mutation and promotion remain proof-gated. |
| Synexia `synexia-openrewrite-recipes/CANONICAL_RECIPE_HOME.tsv` | Git blob `098ac62651dcd7636f6f6eaad964533c76dfa97e` | Reuse existing algorithm/full-review/mastery owners; this task catalogue is data in that control plane. |
| M3JDK21 `AGENTS.md`, `m3/docs/m3jdk21-porting-invariant.md`, `m3/docs/name-mapping.json`, `m3/docs/a3-mastery.md` and `m3/docs/A3_SYNX_MASTERY_CUSTODY.md` | Target baseline `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49` | Preserve public JDK names, target license/API authority, one payload owner, ordered receiving phases and actual product-gate distinctions. |

The authority paths are provenance for this snapshot. A later continuation must re-read changed policy/source identities and requalify affected rows; a stale catalogue is not current agent knowledge.
