<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 String comparison refinement

M3JDK21 owns these implementations in `java.base`. The canonical Synexia
Maven/OpenRewrite recipe binds the exact current preimages and candidate outputs;
its receiving receipt is the authority for the published source revision. No
Synexia runtime dependency or additional text store is introduced.

## Contract-bearing changes

`M3String.contentEquals`, `M3String.startsWith`, the existing precompute bridge's
comparison/prefix methods, and `String`'s builder/generic-CharSequence comparisons
reuse the existing `M3String.CodeUnitCursor` for composite spans of at least
**256 UTF-16 code units**. A first-unit mismatch returns before cursor allocation.
Only a composite side receives a cursor. Direct scalar and shorter paths retain
their existing loops. Prepared negative facts, exact numeric `compareTo` results,
ordered callback reads, original exceptions and the `StringBuffer` monitor remain.

The cursor body and the two `M3String` instance fields are unchanged. Native and
mapped owners, canonical coordinates, tuple balancing, explicit JNI shadows and
search failure handling retain their existing implementations. The latest
`trigramFactsIfPrepared` method and both search-precompute gates are preserved;
receiving an older whole-file bridge would have deleted newer work.

Both `String.regionMatches` overloads return true for a nonpositive length only
after their existing ordered, long-arithmetic bounds and null checks. This repairs
the incumbent's attempt to construct an invalid canonical slice for a negative
length. It is an explicitly recorded defect repair, not an assertion that the
old failing observation was equivalent to the intended public contract.

Eight comment em dashes in `M3StringImmediate.java` become ASCII hyphens. The
baseline ASCII compile reports 24 unmappable-byte diagnostics at those comments;
the candidate's class file is byte-identical to the UTF-8-compiled baseline.

## Reuse and catalogue decisions

The existing first-party cursor is the selected implementation. Prefix call-site
structure and its fixture originate in M3JDK21 PR #1552 at
`c8a161c3348330ce1a8beacca9f430e908e44e7a`; the 256-unit boundary is a new refinement.
The path-cursor fixture is copied exactly from PR #1531 at
`51505752acf57490e77f7b22883725f73da3673d`, retaining its original license.

Abseil Cord at `48c8fd3b582e33a302fb2dd59e9ca2676129c883`,
`absl/strings/cord.h` blob `8fe8e9f8a1c38a1b7c935d2cebcd90705dcb3d32`, supplies a
comparison reference for chunk iteration. Its unsigned-byte ordering and
three-valued comparison result do not implement Java's UTF-16 and exact-difference
contract. No Abseil source body is copied. LeetCode's Longest Common Prefix,
HackerRank's Java Substring Comparisons and GeeksforGeeks' rope article nominate
problem categories; their restricted alphabets or illustrative storage models
do not qualify Java String behavior or authorize copying submissions. The task
catalogue retains their URLs, dispositions, candidate comparisons and evidence.

For a comparison of m units over a tree of height h, the incumbent performs up to
O(m*h) traversal work. The reused cursor performs O(m + k*h), where k is the
number of visited scalar runs. Dense one-unit leaves need not benefit. No new
permanent leaf directory, payload array, index or native symbol is added.

## Fresh authoring qualification

The candidate is based on M3JDK21
`2779aba3f98f31c324be43f731be7914e1a05935`, with a hash-verified 63-source dependency
closure. Source qualification proceeds serially through `M3StringImmediate`,
`M3String`, `M3StringPrecomputeBridge`, then `String`. Each pass compiles with
Java 21, ASCII source encoding, lint and warnings as errors, and compares all
154 emitted class member descriptors and nine generated JNI headers with the
baseline. These checks establish their stated source/ABI scope.

The actual-owner path-cursor fixture performs 1,746,792 checks across 35,914
scenarios per run in JIT and interpreter modes. It uses real M3 owners and
test-owned native buffers, and explicitly does not activate public String
admission. The two public String fixtures compile with strict Java 21 and are
supplied for source-built M3JDK21 execution with `UseM3StringStorage`, compact
strings enabled/disabled, and interpreter coverage. The new fixture verifies
63/64/65 and 255/256/257 boundaries, exact UTF-16 order, a controlled flat side,
hash collisions, callback order and exception identity, canonical owner identity,
builders/buffers, and negative-region ordered null behavior.

Actual source-built image results belong in the task's receiving evidence. A
boot-JDK owner test or compile cannot stand in for those product results.

## Measured selection and limitations

The 64-unit candidate is retained in canonical correction memory as rejected:
at 64 units split over 64 pieces it allocated approximately 38.6 bytes per equal
comparison and 64 bytes per last-mismatch comparison, while paired elapsed-time
medians regressed to 1.225 and 1.527 times the incumbent.

The selected 256-unit candidate was measured in three alternating before/after
fresh-process pairs over 42 cases. All expected result checksums passed. The
63/64/65/255-unit cases matched baseline allocation. For 256 units over 64 pieces,
equal and last-mismatch elapsed-time medians were 0.229 and 0.277 times baseline;
the latter still allocated about 20.75 bytes per call versus 0.0625 bytes of
amortized benchmark overhead in the baseline. This is a documented temporary
allocation tradeoff. At 4096 units over 64 pieces, corresponding medians were
0.065 and 0.088 with allocation equal to the baseline measurement.

Flat 257-unit cases had elapsed-time ratios up to 1.413 despite unchanged direct
loops and allocation. Compilation profiles and this shared host affect the
measurements. These data justify the bounded candidate choice and do not prove
a universal CPU improvement, zero allocation, retained-heap parity or whole-process
native-memory parity. Full cold/warm resource, platform, GC/CDS/JVMTI and complete
jtreg qualification remain separate gates. The global receiving phase remains
String qualification; no later family is promoted by this packet.
