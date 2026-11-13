<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Commons Collections 4.5.0 array storage (E2), successor of commons-collections-4.5.0-20261018

The array-storage work of the commons-collections fork (`m3/array-storage-20261019`, `codex/array-operations`, merged
on fork master `0268162dfc5e`) lands on the received 4.5.0 copy `implementations/commons-collections-4.5.0`. No 4.5.0
fork branch exists, so each fork edit is 3-way merged onto the received body (ours = 4.5.0, base and theirs = the
fork's pre- and postimage). The 20261018 receipt stays unchanged; `FULL_SOURCE_BINDINGS.tsv` here binds the 20
changed or new paths only (target paths relative to the receive root).

| Owner | Array storage | Merge |
|---|---|---|
| TreeList | AVL nodes in parallel slot lanes | 12 hunks: fork side on the doc-only hunks; 4.5.0 kept for `"tree inconsistent!"` and the `checkInterval` before insertion |
| IteratorChain | `ArrayDeque` pending queue | 1 hunk: the 4.5.0 `addIterator` kept; master's post-4.5.0 flattening (and the fork helpers that served it) left out |
| SparseBloomFilter | `int[]` index lanes | clean; contributor line added |
| PermutationIterator | `Object[]` payloads by permutation key | clean |
| CircularFifoQueue | occupied-span clearing | clean (4.5.0 `readObject` kept) |

Tests: the oracle (`M3ArrayStorageOracle`, `M3SparseBloomFilterArrayStorageTest`) compiles the exact pre-edit bodies
at runtime. The fork pinned its master-era bodies; here the six originals are the received 4.5.0 bytes, so every
comparison is against 4.5.0. Three IteratorChain cases now assert 4.5.0 parity: self-admission adds the chain as one
child, and a removal after a look-ahead `hasNext()` targets the next child. The seven tests carry the Synexia header
(the bloom test had an ASF header although first-party).

Gates (local, JDK 21.0.9): main `javac --release 8 -g`, 359 sources, 0 errors. The 7 array-storage tests plus the donor
`SparseBloomFilterTest`, `IteratorChainTest`, `PermutationIteratorTest`, `TreeListTest` and `CircularFifoQueueTest`
pass 200/200. `CircularFifoClearContract` runs 6,849,617 checks. The recipe
`../../recipe/array-storage-commons-4.5.0-20261024` passes 128 transition states with fixed points and 29 refusals.
Not run: `CircularFifoClearBenchmark` (JMH) and the rest of the receive's donor suite.
