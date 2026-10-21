<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Apache NetBeans donor decision

**REUSE the existing `M3ArrayList.removeIf`; REFUSE all five experimental Java
compactors on measured cost.** This packet admits independent contract tests and
review evidence only. It adopts no Apache NetBeans runtime implementation, new
collection owner, text payload store, JNI symbol, lexer, parser, or compiler module.

The user-supplied [Antora documentation tree](https://github.com/apache/netbeans-antora-site/tree/main/modules/ROOT)
provides documentation context. Implementation review used
[apache/netbeans at `f26ca8d8025ff820b8ffe751b6db6c1cc67baa97`](https://github.com/apache/netbeans/tree/f26ca8d8025ff820b8ffe751b6db6c1cc67baa97).
`SOURCE_RECEIPTS.json` binds 13 inspected source, test, LICENSE and NOTICE files to
their exact Git blobs and SHA-256 bytes. Its M3 preimage is commit
`677437be295b23f7ccf7409be287008944d0e8b0`, with `M3ArrayList.java` SHA-256
`ae1092d66bb27e18c0f435d9dc148bd0f46fbc33f9731d9e2d2194142ccc3d32`.

## Existing ownership and evaluated atom

The existing generic-object-list, state-mask-array and full-family catalogues were
read before nomination. `M3ArrayList` already owns the sole `Object[]` payload,
two-phase predicate planning, stable compaction and tail clearing. Its `removeRange`
already uses an overlapping `System.arraycopy` to move a surviving suffix.

NetBeans `GapList.copyElements`, `removeRange` and `moveGap` supplied a concrete
range-transfer comparison. The first experiment copied every survivor run. Later
experiments reduced dispatch or specialized a single rejected interval. The final
one merely detected that interval and called M3's existing `removeRange`, preserving
the exact original scalar block for other masks. All five remained fixture-only
experiments and failed resource admission. Their complete source postimages remain
under `fixtures/`; the incumbent is retained there as the reference preimage.

For a single rejected interval `[first,end)`, a range move transfers `n-end` surviving
references, while the original scalar commit assigns `n-rejected` surviving
references. Both clear `rejected` trailing slots. These are source-level counts,
not measured memory traffic. The historical raw `run_copy_refs_model` column refers
only to the original general-run idea; it is not instrumentation for every variant.

## Behavioral qualification

The independent `M3ArrayListRemoveIfProof` passed against the incumbent and all five
refused source templates: **12,013 cases and 1,883,123 assertions** per run. It tests
all rejection masks through length 12, all nested sublist ranges/masks through length
8, bit boundaries, 1,500 seeded arbitrary masks, nulls, duplicate reference identity,
callback traces, failures, nonstructural updates, reentrant structural callbacks,
iterator/view epochs, unchanged array ownership/capacity, and directly inspected
null storage tails. Normal operations use real JDK 21 `ArrayList` as the oracle.

One established distinction is preserved explicitly: the inherited M3 sublist view
can remove a visited prefix before a predicate throws, whereas concrete
`ArrayList.SubList` has an atomic bulk override. The inherited-view failure oracle
uses JDK `LinkedList` and independently asserts the concrete `ArrayList` difference.
The root operation retains its own deferred commit and immediate reentrant refusal.
Null-tail checks prove absence of those retained references; they do not predict GC
scheduling. These finite checks are not whole-JDK, concurrency or universal proof.

The exact unmodified donor `GapList` and real `ArrayUtilities` dependency compiled
and passed an independent 15,000-operation JDK range/mutation oracle with 388,644
checks. Original upstream `GapListRandomTest` and `CharSequenceUtilitiesTest` bytes
were retained but their JUnit3/4 suite was not executed; the required artifact was
unavailable. `PROOF_RESULTS.json` preserves actual stdout and source/log digests.

## Measured refusal evidence

`MEASURED_COSTS.tsv` retains every comparison, and `RAW_BENCHMARK_ROWS.tsv` retains
all 378 input rows from the executed fork files. The following counterexamples are
all at 65,536 references. Ratios are candidate CPU time divided by incumbent CPU
time; values above 1 are slower in this experiment.

| Refused fixture | Counterexample mask | CPU ratio | JVM forks per variant |
| --- | --- | ---: | ---: |
| Unconditional survivor runs | Alternating | 1.555 | 1 |
| Inline short-run fallback | Seeded random half | 1.206 | 1 |
| Private-helper fallback | Seeded random half | 1.220 | 1 |
| Inline single interval | One rejection per 1,024 positions | 1.638 | 3 |
| Existing `removeRange` delegation | One rejection per 1,024 positions | 2.124 | 3 |

The final delegation also measured ratios 1.799 for alternating and 1.225 for random
half at that size, despite improvements for all, last and middle-block deletions.
These observations support refusal of a general optimization, not a universal
performance ranking. Substantial fork/profile variation is visible even on an
unchanged no-removal path. No speed claim is inferred solely from fewer copies.

The probe measured current-thread CPU, wall time, allocated bytes and GC counts,
with construction outside the measured operation. It used 64, 8,192 and 65,536
references, seven shapes, 12 warmup batches, seven measured batches, Java 21,
`-Xms512m -Xmx512m -Xbatch -XX:+UseSerialGC`, and reversed variant order in the
middle of each three-fork comparison. All corresponding rows measured equal Java
allocation and zero GC events. `ENVIRONMENT.json` records the shared, unpinned host;
it does not establish production hardware representativeness or JMH-level isolation.

## Other NetBeans opportunities and prior challenge donors

The combined
[`netbeans-collection-atoms-20261019.tsv`](../../algorithm-catalog/netbeans-collection-atoms-20261019.tsv)
uses the established `REVIEWED_CANDIDATES.tsv` schema. Its additional source-reviewed
areas are deferred: `CharSequenceUtilities` content operations, offset-gap metadata,
flyweight token positions, incremental lexer restart/convergence, snapshot coordinate
maps, and compiler element handles. Each row states a concrete contract gate and
exact source pin. Compact `CharSequences` storage is refused as a replacement for
canonical interned M3String ownership. A useful source pattern does not establish
its target owner, runtime integration, rights over unrelated files, or resource fit.

The five licensed LeetCode/HackerRank rows cross-reference the existing
[`challenge-forks-20261018`](../../algorithm-catalog/challenge-forks-20261018/README.md)
receipt. They preserve that packet's reference-only/deferred decisions and pins:
`javadev/LeetCode-in-Java` at `bed5d330e2b145e24e6fc1048ca28bf156b3d639` and
`RodneyShag/HackerRank_solutions` at `a98773d3a0c4f11b9cd90d6a1f37b994c170996a`.
Existing rotation, Fenwick, interval and stable index owners remain authoritative.
Those source reviews and licensed corpus coverage were not rerun here.

GFG remains separate: the inherited licensed repository discovery points to
`kishanrajput23/GFG-Problem-Solutions` at
`39d6a47408cd7a25796e314945258da9aa546227`; website move-zeroes terminology supplies
taxonomy only. This packet neither requalifies that corpus nor copies a website
editorial. Its GFG rows do not expand the two-corpus challenge inventory.

`NETBEANS-NOTICE.txt` preserves upstream attribution with evaluation/test-only
wording, and `licenses/NETBEANS-LICENSE.txt` retains exact root license bytes. The
new M3 proof, adapter and experimental fixtures are first-party Apache-2.0 work.
Canonical Synexia recipe execution and receiving-module gates remain the authority
for adding the independent tests; these source payloads do not claim that those
later gates have already run.
