<!-- SPDX-License-Identifier: Apache-2.0 -->
# Serial source step 2: direct primitive-span search in LongArrayDeque

This reviewed follow-on atom stacks after the initial deque shift/clear
source commit `10419749827a364a16a3e78699c96c5f15c3cd2d`. The original pre/post Git identities of
step 1 remain intact and unmodified.

The preimage `LongArrayDeque` already uses primitive span shifts and
two-span clearing. This step changes only the scan/search atom:

- `contains`, `removeFirstOccurrence` use `firstOccurrenceIndex`.
- `removeLastOccurrence` uses `lastOccurrenceIndex`.
- Each search visits the **high and low physical live spans** in exact
  logical order (or the reverse), without repeated modulo/mask index
  mapping on every element. No temporary arrays, allocation, node
  object, hashing, JNI or public descriptor change.
- Duplicate first/last semantics, absence/no structural epoch change,
  null-free primitive values, complete physical ring bytes and the prior
  copy/clear fast path remain unchanged.

## Executed local independent source-atom proof

`LongDequeRingScanOracle.java` was compiled using OpenJDK 21 with
`javac --release 21 -Xlint:all -Werror`, then passed:

- 1,390,536 JIT assertions: original masked scalar linear scan vs
  two-span first/last scan and contains parity over capacities
  8/16/32/64/128, every ring head/size, repeated values, zero,
  Long.MIN_VALUE, Long.MAX_VALUE, absent keys and seeded random keys.
- 37,800 `-Xint` assertions in smaller exhaustive geometries.

Target-owned `LongArrayDequeRingSpanTest` also now checks actual
`LongArrayDeque.contains`, both occurrence-removal directions and
duplicate-match physical state/epochs against the old scalar reference;
it adds 88,960 target-module cases on top of its existing first
source step.

## Custody and qualification

Exact SHA-256 preimage, postimage and Git blob identities are retained in
`SCAN_SOURCE_SEALS.tsv`, with byte-identical full postimage templates.
No source and test file can be modified silently without changing its
pin. The first receipt's preimage remains immutable.

This is a **source-sealed receiver packet**, not a claim that the
full Synexia Maven/OpenRewrite recipe or OpenJDK21 jtreg/JUnit module
was executed. Those product gates and measured benchmarks remain HOLD.
No universal throughput speedup is asserted from the reduced masked
operations alone.
