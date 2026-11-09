# M3JDK21 problem-area catalogue: inventory before optimization

M3JDK21 is a forked JDK implementation, **not** an application plugged into a
standard JDK. Problems from LeetCode, HackerRank, GeeksforGeeks and GitHub are
useful **algorithmic evidence** for JDK collection, String, regex, stream,
compiler, HotSpot and native storage work. A problem is never a drop-in patch:
its constraints must be compared with the target's public and VM contracts.

## The invariant

**Every newly admitted problem must have at least one explicit problem area,
technique, M3JDK21 target, and precondition.** Problems may have multiple
orthogonal areas and targets. The index preserves the source identity even
when several sites describe equivalent algorithms, so donor comparison and
attribution remain explicit. The catalogue must record rejection decisions,
not just approved changes.

1. Inventory the target source **file by file** with `M3InventoryRecipe` and
   preserve SHA-256 source/behavioral-atom fingerprints. Signals remain
   `UNPROVEN`; parsing does not demonstrate semantic equivalence.
2. Classify each operation by **problem shape** (for example `prefix_sum`,
   `sliding_window`, `cyclic_offset`, `monotonic_stack`, `union_find`, `trie`,
   `dynamic_programming`, `bitmask`, `scheduling`).
3. Query related indexed problems; evaluate asymptotic and measured costs,
   stability, mutability, memory, exact arithmetic, concurrent visibility,
   null/identity/equality contracts, JNI and GC barriers.
4. Select an original implementation or appropriately licensed donor; record
   source, qualification and rejection grounds. Problem links do **not**
   license copied descriptions or solutions.
5. Author a reusable deterministic transformation recipe, using OpenRewrite
   where source-to-source rewriting is suitable. Emit counterexample fixtures,
   compare behavior before/after, and apply one pass per file. If a change is
   invalid, fix the recipe—not the transformed output. Verify fixed-point,
   compiler, jtreg, JNI/GC gates and benchmarks before promotion.

## Current coverage is a seed, not an exhaustive platform mirror

`problem-areas.tsv` contains **127 curated problem links** (94 LeetCode and 33
HackerRank) across **45 distinct problem areas**. The offline catalogue supports
arbitrarily added well-formed records from authorized metadata, rather than
claiming that all problems on either site were fetched or implemented.
**No automatic LeetCode crawling or scraping** is provided. HackerRank's own
Algorithms and Data Structures classifications provide additional discovery
starting points. Curated metadata is not proof that any solution is licensed.

Columns in the TSV are `source`, `id`, `title`, `url`, `areas` (semicolon list),
`techniques` (semicolon list), `m3_targets` (semicolon list), `preconditions`,
`time`, `space`, `license`, `decision`, and `status`. Status is one of:

- `DISCOVERY`: indexed idea; no target equivalence or implementation claim.
- `EVALUATED`: documented tradeoffs; not necessarily selected.
- `REJECTED_CONTRACT`: excluded after observing a contract mismatch.
- `LOCAL_TESTED`: related M3 implementation qualified locally; **not** a full
  fork CI pass, native integration, or JDK compatibility proof.

`license=metadata_link_only` denotes short bibliographic problem metadata and
original engineering notes, **not** source-code reuse authorization. Any code
port requires independent licensing and attribution review.

## Search through a primitive-array inverted index

`M3ProblemCatalogue.java` lives with the existing Maven-compatible recipe
(`make/m3/recipes`) and has no network dependency. Persistent row metadata
is kept in parallel arrays. Index postings are flat `String[]` keys + `int[]`
row indexes, sorted by an original stable iterative primitive-index merge
sort. There are no stored per-row `Entry`/node objects.

```shell
./make/m3/recipes/test-local.sh
CLASSES=out/recipes
CAT=docs/m3/problem-areas.tsv
java -cp "$CLASSES" org.synexia.m3.recipe.M3ProblemCatalogue "$CAT" areas
java -cp "$CLASSES" org.synexia.m3.recipe.M3ProblemCatalogue "$CAT" techniques
java -cp "$CLASSES" org.synexia.m3.recipe.M3ProblemCatalogue "$CAT" targets
java -cp "$CLASSES" org.synexia.m3.recipe.M3ProblemCatalogue "$CAT" find area bitmask
java -cp "$CLASSES" org.synexia.m3.recipe.M3ProblemCatalogue "$CAT" find technique xor_reduction
java -cp "$CLASSES" org.synexia.m3.recipe.M3ProblemCatalogue "$CAT" find target strings
java -cp "$CLASSES" org.synexia.m3.recipe.M3ProblemCatalogue "$CAT" find id HR:array-manipulation
```

Index construction is O(P log P) for P area/technique/target/source postings;
query is O(log P + matches) and memory is O(P). This is a **search index for
problem metadata**, not a replacement for runtime M3 collection storage.
`normalize` emits canonical ID-ordered TSV; the test suite verifies its fixed
point, duplicate-ID rejection, host validation, bounded 12,000-record fixture,
and absence of unapproved donor content.

## Examples: one problem, multiple reusable areas

| Problem | Reusable area | Candidate M3JDK21 use | Proof boundary |
| --- | --- | --- | --- |
| LeetCode 136 / HackerRank Lonely Integer | bitmask, XOR reduction | primitive-array scans | Only valid when all other values occur exactly twice |
| LeetCode 2150 | frequency, bitmask, hashing | exact frequency and neighbor indexes | XOR parity is insufficient for uniqueness |
| HackerRank Array Manipulation / LeetCode 1109 | difference array, prefix sum | batched range updates | Inclusivity, bounds, overflow and mutation contracts |
| LeetCode 146 / 460 | LRU / LFU | index-managed recency/frequency in primitive arrays | Exact eviction and tie-breaking, facade identity |
| LeetCode 239 / 739 | monotonic queue / stack | sliding extrema and next-greater offsets | Strictness, encounter order, window expiry |
| LeetCode 307 | Fenwick tree, range aggregation | primitive-range index | Update/query indexing semantics |
| LeetCode 208 / HackerRank Contacts | trie, prefix index | interned composite M3String search | Unicode normalization and prefix equality |
| LeetCode 10 | dynamic programming, regex | constrained pattern-plan precompute | LeetCode dot-star grammar is NOT `java.util.regex` |
| LeetCode 1143 / HackerRank Common Child | dynamic programming, strings | lazy LCS on composite byte/char cursors | Unicode and subsequence semantics |
| LeetCode 743 / HackerRank shortest reach | graph, shortest path | M3FS, dependency-DAG index | Directedness, weights, disconnected components |
| HackerRank Left Rotation | cyclic offsets | logical array rotation | View versus materialized array observable behavior |

## Cross-cutting JDK correctness constraints

* Object identity must be maintained when a standard Java API exposes an
  object. Internal offset/bitmask tables may avoid per-entry allocation, but
  API-requested facade objects must honor their lifetime and mutation rules.
* A Java `Comparator` and a primitive fixed-key ordering are not equivalent
  without proving comparator consistency and null semantics.
* C1/C2/compiler optimizations need escape/effect proofs and conservative
  deoptimization fallback. A challenge's time complexity is **not** a proof of
  a HotSpot optimization or permission to elide ownership release.
* Sparse versus dense indexes must be chosen using measured domain span,
  element count, capacity and memory pressure. Retained memory, GC effects
  and array-copy costs matter as much as asymptotic complexity.
* Frozen dictionary M3String atoms and VM-local overlay atoms require exact
  string/JNI/CDS behavior qualification before replacing ordinary String.
* The recipe output remains `UNPROVEN`. The catalogue makes candidates
  discoverable; JDK behavioral equivalence comes from tests and compiler/VM
  evidence, never from the problem's title.

## Sources, scope and licensing

Curated links are maintained in `problem-areas.tsv`. Discovery resources:

- https://www.hackerrank.com/domains/algorithms
- https://www.hackerrank.com/domains/data-structures
- https://leetcode.com/problemset/
- https://leetcode.com/terms/
- https://github.com/apple/swift-collections
- https://github.com/eclipse-collections/eclipse-collections
- https://github.com/google/guava

Do not bypass access restrictions or fetch/copy hosted solution text. Public
problem classifications are used for discovery, with original concise
engineering notes and independent implementations. All changes stay in
reviewable M3JDK21 PRs, with the existing Synexia convergence workspace
providing donor qualification and reusable recipes.
