<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# LeetCode and HackerRank as a category catalogue

The optimisation decision starts with the problem's invariants and the actual
receiving workload. `CATALOG.tsv` makes the category, preconditions, time/space
tradeoff, M3AST owner and admission decision searchable with the repository's
existing text tools. The challenge pages are problem references; no third-party
solution implementation was copied.

## What the supplied examples establish

[LeetCode 136](https://leetcode.com/problems/single-number/) and
[HackerRank Lonely Integer](https://www.hackerrank.com/challenges/lonely-integer/problem)
use the paired-duplicate uniqueness invariant. An XOR reduction is a suitable
primitive operation for that contract: linear work, constant auxiliary storage.
It does not validate that its precondition held. This is useful catalogue
knowledge for primitive integer data, but it cannot seal ordered compiler input:
permutations have the same XOR, and different equal-length sequences can collide.

[LeetCode 2150](https://leetcode.com/problems/find-all-lonely-numbers-in-the-array/)
adds singleton-frequency and absent-neighbour requirements. The retrieved problem
has values from 0 through 1,000,000, so dense storage is a candidate under that
specific bound. The general workload decision is broader than choosing a boxed
map: consider existing primitive counts for sparse domains, saturated counts when
only zero/one/multiple matters, and packed two-bit states for a bounded dense
domain. Include allocation/clearing cost, domain width, result storage, ordering
and integer-boundary behaviour in the comparison. These are original candidate
designs, not claimed challenge submissions or received M3AST implementations.

[LeetCode 146](https://leetcode.com/problems/lru-cache/) is a useful eviction
category reference. M3AST's actual projection contract is narrower: identity of
the key object and lifetime of one compiler event. A new global LRU would change
that contract. Preserve the existing event owner and the separately established
AST swap/cache owners instead of replacing them with a challenge implementation.

## Concrete optimisation already received

The compiler-owner source-seal leaf hashes the same ordered UTF-16 bytes using a
bounded primitive byte buffer. For N code units, the old source made 2N scalar
digest-update calls; the received source makes ceil(N / 4096) bulk calls, using
at most 8192 temporary buffer bytes. Both algorithms remain O(N). This is a
source-level operation-count comparison, not an elapsed-time or allocation-rate
benchmark. Exact output is verified by 23 digest cases, including all 65,536
UTF-16 code-unit values. Source length is checked before digest work on reread.

Frontend work is reused directly: the qualified Gradle
provider uses one original task per selected compilation, and two consumers share
one projection computation per parsed unit. Its emitted-code probe records two
units, two computations, two hits and zero extra M3AST source reads. No new AST is
built to obtain those results. Existing javac nodes remain the compiler's nodes;
primitive state belongs in metadata that does not replace their semantic identity.

## Admission discipline

For a matching category, compare the existing owner, a suitable licensed donor,
and an original primitive-storage candidate. Retain the owner unless a compatible
change has evidence. Apply any accepted production change through the existing
Maven/OpenRewrite recipe, then lint, compile, test and run it on the intended
workload. Record wall time, bytes allocated and peak live memory before claiming
a space/time win. JNI is a candidate only when the existing native owner and
measured workload justify the boundary; this review adds no JNI entrypoint.

This late catalogue leaf changes documentation and custody only. The previously
verified compiler, Maven and Gradle sources, runtime receipts and frozen manifests
remain unchanged. The paired-XOR, neighbour-counting and global-LRU candidates are
explicitly not new compiler implementations.
