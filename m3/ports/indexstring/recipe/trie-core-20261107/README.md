<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# trie-core-20261107: the indexstring trie core in M3JDK (recipe INDEXSTRING_RELOCATE_IX1 v1)

This is the first leaf of the indexstring word-break family (census2 rows P2-108 and P2-056, owner 4d). It brings two
owners from `synexia-indexstring` into the existing module `m3/ports/indexstring`, package `jdk.internal.m3.indexstring`:

| Owner | What it does |
| --- | --- |
| `MIndexLiteralSearchTree` | Frozen UTF-16 literal trie with Aho-Corasick search. `exact`, `findFirst` and `visitMatches` (stops early when the consumer returns false). `image()` / `fromImage` round trip. Lazily prepared navigation. |
| `MIndexTrieNavigation` | Precomputed coordinates over that trie: parent, depth, lexical rank intervals, minimum/maximum descendant length, terminal rank to state. Supports `step`, `stateFor` and `longestPrefixPayload` in both UTF-16 directions, payload pages, a bounded Levenshtein `Utf16Machine`, and `select` / `precompute` walks with work limits. |

The donor word-break plan `MIndexWordBreakPlan` walks this navigation one UTF-16 unit at a time. That plan, and the
vocabulary rows it reads, are the next leaf.

## Why only these two

`closure.py` sizes the same-package closure of each donor type:

- `MIndexWordBreakPlan` reaches 356 classes (3.4 MB) through `MIndexString`. That is the donor string model; M3JDK's
  `java.lang.M3String` is a different design.
- `MIndexTrieNavigation` reaches one other class, `MIndexStringEditDistancePlan`. M3JDK already has an owner for it:
  `jdk.m3.precompute.M3EditDistancePlan` in `m3/ports/precompute`.

The trie therefore lands first, against M3JDK owners only.

## Relocation (relocate.py, RELOCATION_MAP.tsv)

1. Package `com.synexia.indexstring` becomes `jdk.internal.m3.indexstring` (JDK naming rule PKG).
2. The progress monitor becomes `jdk.m3.collections.M3ProgressMonitor`, and `noop()` becomes `new M3DefaultProgressMonitor()`.
   This is the same rule the m3/search leaves use.
3. The query plan becomes `jdk.m3.precompute.M3EditDistancePlan`. The trie uses only `compile(CharSequence)` and the
   `CharSequence` view of the plan.
4. Four `MIndexString` overloads of `exact`, `findFirst` and `visitMatches` are removed. They were token-arena fast
   paths; a String input takes the `CharSequence` overloads.

Type names stay as in the donor, like the module's existing `MIndexJoined*` owners, so bodies remain diffable against
the donor blobs in `PROVENANCE.tsv`.

`pom.xml` gains two dependencies, `com.m3:m3-collections` and `com.m3:m3-precompute`, plus one exec main,
`MIndexTrieCoreSmokeMain`. A Maven build of the module now needs those two modules installed. The
`character-flags-20261106` verify step builds the module the same way.

## Proof (GATES.tsv)

- Maven package with the module's flags, all four exec mains PASS.
- javac live vs leaf: 0 warnings under `-Xlint:all -Werror`; the existing mains give the same digests on both sides.
- `Ix1Diff`: donor vs relocated. The donor is compiled with `stub/`, which declares only the shapes of the donor
  monitor, native loader and `MIndexString`; it is never instantiated and adds no behaviour. Two seeds x 20,000 random
  literal sets (small alphabet, isolated surrogates, empty and duplicate literals, tripping state budgets) give 7.1M
  checks, 0 mismatches and equal digests.

## Not in this leaf

- `MIndexVocabularySearchTree` and `MIndexWordBreakPlan`: the next leaf, over String vocabulary rows.
- The donor's native distance frontier (`MIndexStringEditDistancePlan.appendDirect`): not used by the trie.
