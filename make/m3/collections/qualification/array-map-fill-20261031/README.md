<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Array map fill (recipe ARRAY_MAP_FILL)

Fills the node-based map scaffolds of `m3/collections/implementations` with M3 storage. The scaffolds come from
`M3FiveProviderScaffold`, the fill from this recipe; the user's directive is "bulk modify all the collection forks
using LeetCode, arrayifying everything". The members are owned M3 classes over arrays, offsets and bit masks: no
Entry or Node object is stored.

| Member | Module | Provider (oracle) |
|---|---|---|
| `M3HashedMap` | `apache-hashed-map` | `org.apache.commons.collections4.map.HashedMap` |
| `M3LinkedMap` | `apache-linked-map` | `org.apache.commons.collections4.map.LinkedMap` |
| `M3LRUMap` | `apache-lru-map` | `org.apache.commons.collections4.map.LRUMap` |

`apache-abstract-hashed-map` is not a member. Its scaffold mirrors an abstract class and has no constructor, so it
cannot be instantiated or proven.

## Storage (one template, `templates/ARRAY_MAP.java.tmpl`)

| Lane | Role | Catalogue category |
|---|---|---|
| `int[] index` | open-addressed table, linear probe, 0 empty, -1 tombstone, slot + 1 | open addressing (LeetCode 706 Design HashMap) |
| `Object[] keys`, `Object[] values`, `int[] hashes` | parallel lanes by slot | structure of arrays |
| `long[] liveMask` | one bit per live slot; iteration, first/last, next/previous by bit scans | bit manipulation |
| pattern SLOTS (LinkedMap) | slots are appended and never reused before a stable compaction, so slot order is insertion order | - |
| pattern LINKS (HashedMap, LRUMap) | `int[] before`, `int[] after` link the live slots; a free list reuses removed slots; growth keeps slot ids | doubly linked list in int lanes |
| LRU recency | a get, or a put of a present key, relinks the entry at the end in O(1); eviction takes the head | LRU cache (LeetCode 146) in int lanes |
| iteration atoms | `first`, `last`, `following`, `preceding`: bit scans (SLOTS) or link lanes (LINKS); every traversal, iterator and view uses them | - |
| `get(int)`, `indexOf` | select and rank by word popcount, O(n/64) where the provider walks its list in O(n) | rank/select over a bit set (LeetCode 307 family) |
| compaction | stable two-pointer compaction of the live slots, in place when half are dead, otherwise doubling | move zeroes / remove element (LeetCode 283, 27) |

The `Map` default methods (`getOrDefault`, `putIfAbsent`, `compute*`, `merge`, `replace*`, `forEach`, `replaceAll`)
use the JDK default algorithms over the member's own `get`/`put`/`remove`. The provider does not override them, so
every side effect (LRU recency) is the provider's.

## Atoms

The pure kernels live in one nested class, `Atoms`, identical in every member: `tableSize`, `hash`, `mix`, `sameKey`,
`find`, `positionOf`, `insert`, `nextLive`, `prevLive`, `select`, `rank` and `compact`. Arrays go in and values come
out; no atom reads instance state. Each atom has four parts, and `M3ArrayMapAtomContractTest` fails the build when one
is missing. It reads the atom set by reflection, so a new atom cannot skip them:

| Part | Where |
|---|---|
| contract | the atom's Javadoc states Requires, Ensures, Effects and Cost, word for word its row of `ATOMS.tsv` |
| JUnit | its own test in `M3ArrayMapAtomsTest` (named in `ATOMS.tsv`), against a naive reference inside the Requires clause |
| performance | its own row in `M3ArrayMapAtomsPerformanceTest`, against a naive or `java.util` reference |
| documentation | the same Javadoc, generated into every member from the template |

No magic number and no floating string: `M3ArrayMapLiteralsTest` applies the M3Culprits rule to the generated members.
Every numeric literal above 2 and every non-empty string is a named `static final` of the template (`WORD_SHIFT`,
`WORD_MASK`, `GOLDEN_RATIO_32`, `FOLD_SHIFT`, `THIS_MAP`, `KEY_VALUE_SEPARATOR` ...). As compile-time constants, the
strings are interned by the JVM string table (M3 String storage on M3JDK).

## Atom passes: atomize, then JIT, then patternize

Each atom passes three gates, in this order, in the default (blocking) test run.

1. **Atomize.** The kernel is an atom of the shared `Atoms` class. It has a contract, a JUnit test and a
   performance row (`M3ArrayMapAtomContractTest`, above).
2. **JIT.** `M3ArrayMapJitTest` runs `M3ArrayMapJitDriver` in a forked HotSpot JVM. The flags are C2 only,
   `-Xbatch` and `-XX:+PrintInlining`. The driver calls every atom in a hot loop. The rules:
   - an atom of at most 35 bytecode bytes (HotSpot's `MaxInlineSize`) must be inlined;
   - a larger atom must be inlined or called as compiled code;
   - no atom may be refused for its size.
   The report is `target/jit/ARRAY_MAP_JIT.tsv`. One local run:
   - 12 atoms are inlined hot;
   - `find` (75 bytes), `compact` (115) and `compactIds` (105) are compiled calls. Their own C2 code is larger
     than `InlineSmallCode`, because `String.equals` or reference-store barriers are inlined into them. They are
     not refusals.
3. **Patternize.** `M3ArrayMapPatternPassTest` checks that each atom maps to exactly one pattern of the closed
   catalogue. `PATTERNS.tsv` holds 9 patterns: open addressing, Fibonacci hashing, bit scan, rank/select, stable
   two-pointer compaction and others, each with its catalogue reference. `ATOM_PATTERNS.tsv` holds the mapping.
   An unknown pattern, an unclassified atom or an unused catalogue row fails the pass.

## Id lane (family ID_MAP): M3 keys

An M3 key is an id: the canonical id of an interned M3String, the node id of an M3AST, or any M3 key id. Keyed by
such ids, a map needs no key object at all. `templates/ID_MAP.java.tmpl` (members in `ID_MEMBERS.tsv`) generates
`jdk.m3.collections.id.M3LongObjectLinkedHashMap<V>`, an insertion-ordered map from `long` ids to values:

- the id is its own hash (`Atoms.foldId`, which is `Long.hashCode`);
- equality is `==` on the `long[] ids` lane (`Atoms.findId`: one lane read per probe, no hash lane, no `equals`);
- nothing is boxed: a lookup allocates 0 bytes;
- slots follow the SLOTS pattern (stable `Atoms.compactIds`), so `idAt`, `valueAt` and `indexOf` are select and rank
  over the live mask.

Both templates include `templates/ATOMS.java.inc`, so every member carries the same 15 atoms. Each atom has its
contract, its JUnit test and its performance row, as the gate above enforces. `M3IdMapDifferentialTest` runs
seeded scripts against `LinkedHashMap<Long, String>`, comparing every return value, the order, positional access,
the hash code and the text. Its cases include ids built to share one fold, wide runs with growth and compaction,
and fail-fast `forEach`. One local run of `M3IdMapPerformanceTest` (200,000 random ids; JDK 21, Windows):

| Workload | M3 ns/op | LinkedHashMap ns/op | HashMap ns/op | M3 / LinkedHashMap bytes/op |
|---|---:|---:|---:|---|
| `put` | 44.7 | 114.5 | 96.9 | 52.8 / 85.0 |
| `get` (hit) | 11.0 | 18.3 | 18.8 | 0 / 24 |
| `get` (miss) | 13.9 | 28.1 | 17.0 | 0 / 24 |
| remove-put churn | 53.6 | 133.5 | 128.7 | 52.8 / 88.0 |
| iteration | 5.9 | 8.1 | 27.6 | 0 / 0 |
| `idAt` (select vs an iterator walk) | 808 | 497,405 | - | 0 / 32 |

The id lane takes ids. Giving `java.lang.String` keys an id needs an accessor for `M3StringAtom.canonicalId` in
java.base; that accessor is not part of this crate.

## Proof

`M3ArrayMapDifferentialTest` runs the same seeded scripts on the provider class and on the member.

- **Scale:** 40 seeds of 2,500 steps per member, plus wide runs over 600 keys so that rank and select cross mask words and the lanes grow and compact.
- **Compared on every call:** every return value, and every exception's class and message.
- **Compared after every step:** size, `hashCode`, equality both ways, and the exact iteration order. HashedMap, whose order is unspecified, is compared by entry set.
- **Also covered:** constructor validation, empty iterators, view identity, clone and serialization round trips, and the fixed point (the committed member sources are the template's output).

`DIVERGENCE.tsv` lists where the M3 member differs from the provider on purpose. Two of its rows are provider
defects, each with a guard: commons 4.5.0 `LRUMap` loses a null key on eviction, and the linked iterator fails after
`previous()` then `remove()`.

## Performance

`M3ArrayMapPerformanceTest` (tag `performance`) runs the same workloads on both sides (50,000 keys; best of 5 rounds
after 3 warm-up rounds). It writes `target/perf/ARRAY_MAP_PERF.tsv` with ns/op and allocated bytes/op. It asserts
only the asymptotic index-access win and that no workload is more than 5x slower; it makes no general speed claim.
One local run (JDK 21, Windows), after the LINKS pattern:

| Workload | M3 ns/op | provider ns/op | Ratio |
|---|---:|---:|---|
| LinkedMap `get(int)` | 298 | 16,043 | 54x faster (rank/select) |
| LinkedMap `indexOf` | 177 | 52,183 | 295x faster |
| HashedMap entry iteration | 3.7 | 19.1 | 5x faster |
| HashedMap `put` | 32.6 | 41.1 | 1.3x faster |
| LRUMap `put` | 25.1 | 29.0 | 1.2x faster |
| LRUMap remove-put churn / evicting access | 18.9 / 29.2 | 18.4 / 27.8 | parity (1.03x / 1.05x) |
| HashedMap remove-put churn | 20.5 (0 bytes/op) | 15.1 (16 bytes/op) | 1.36x slower, no allocation |
| HashedMap remove-half | 34.0 | 19.4 | 1.75x slower |

The first run, with every member on append-only slots, measured LRU churn 3.3x and evicting access 2.3x slower than
the provider. That run is why HashedMap and LRUMap use the LINKS pattern (RECIPE_MEMORY `LRU_REAPPEND_COST`).

`M3ArrayMapAtomsPerformanceTest` writes `target/perf/ARRAY_MAP_ATOMS_PERF.tsv` (best of 7 rounds of 20,000 operations
after 3 warm-up rounds). It asserts only the word-scan atoms against bit-by-bit loops on a 1% live mask. One local
run:

| Atom | M3 ns/op | Reference | Reference ns/op | Ratio |
|---|---:|---|---:|---|
| `select` / `rank` | 198 / 159 | bit-by-bit count | 7,267 / 7,735 | 37x / 49x faster |
| `nextLive` / `prevLive` | 11.9 / 10.4 | bit-by-bit scan | 28.2 / 26.4 | 2.4x / 2.5x faster |
| `find` (hit) | 10.6 | `HashMap.get` | 53.1 | 5x faster |
| `find` (miss) | 12.8 | `HashMap.containsKey` | 9.6 | 1.3x slower: each probe reads the hash lane |
| `positionOf` / `insert` | 6.8 / 12.4 | `HashMap.get` / `put` | 12.3 / 14.6 | 1.8x / 1.2x faster |
| `tableSize` / `hash` / `sameKey` / `mix` | 2.5 / 2.0 / 2.7 / 4.2 | loop / `Objects.hashCode` / `Objects.equals` / xor fold | 5.0 / 2.8 / 3.3 / 4.2 | parity to 2x |
| `compact` (4,096 slots) | 7,028 | `ArrayList` filter | 6,126 | 1.15x slower, writes four lanes |

## Run

```text
./run.sh        # regenerate the members from the template, then mvn test (differential, fixed point, performance)
```

Change the template or `MEMBERS.tsv`, never a member source. Record each correction in `RECIPE_MEMORY.tsv`.
