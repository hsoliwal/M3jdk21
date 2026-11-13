# Indexed-tree query validation

Nonempty `M3TreeMap` lookups previously compared the query with itself before
checking bounds and searching. The existing bound comparison or indexed BST
descent now validates that query. An unbounded empty natural-order lookup keeps
explicit key validation; an empty comparator-backed lookup keeps its zero-call
behavior. Payload arrays, integer links, public signatures and storage layout
remain unchanged.

The donor is the existing Apache-2.0 M3 implementation. [LeetCode 700](https://leetcode.com/problems/search-in-a-binary-search-tree/) and
[HackerRank BST Insertion](https://www.hackerrank.com/challenges/binary-search-tree-insertion/problem) classify the algorithm problem; no challenge solution
code or OpenJDK implementation was copied. Existing SDK donor notices apply.
`FOSS_REUSE_DECISION.tsv` and the admission record precede implementation.
The hash-pinned file recipe passed exact application, fixed-point, inverse and
unknown-preimage refusal checks.

Executed checks: strict Java21 selected-class compilation with warnings as
errors; exact 88 public API rows; frozen 132 entry/lookup cases; 113 sorted-bulk
cases with 257312 checks / 1873 invariants; 5040 permutations and 98304 random
operations with 1438195 checks / 238593 invariants. Existing fixtures and reference
oracles were reused unchanged. The source-blind qualifier read only opaque
production bytes. Source-aware Codex autoreview exited 0 with `findings: []`.
The review command, structured report and frozen review target are retained.

The unchanged public churn benchmark ran 12 cells / 24 forks / 120 measured batches.
All six baseline/candidate 99.9% confidence-interval pairs overlap. Five point
estimates are lower; shuffled 65536 is 17.65% higher. Throughput is inconclusive.
`benchmark/*/JMH.json` retains every fork and measurement, and
`MEASURED_RESULTS.tsv` reports all six normalized comparisons. No peak,
allocation-reduction or broad throughput claim is made.

This is stacked on [PR 1342](https://github.com/hsoliwal/M3jdk21/pull/1342) and changes one SDK production file (+7/-2 lines).
The canonical Eclipse owner/helper remain separate local candidates: four
source-callback destination-reentrancy cases differ. Full five-library
ownership, JNI CRUD and full Eclipse Maven/JPMS/OSGi integration remain open.
The selected-class proof is not a new full clean SDK or Eclipse build claim.
