<!-- SPDX-License-Identifier: Apache-2.0 -->
# Contracts of the sixteen received CPU tool modules

These modules receive the existing first-party Synexia tool implementations from
`hsoliwal/com.synexia` at `50f48cbbe11f17fb8dc209bac3812f8940a1e267`.
`DONOR_MODULES.tsv` records each exact Git blob and SHA-256. `DONOR_CHANGES.tsv`
records every target source, its payload SHA-256, and all implementation changes.
The mapping follows the donor `synexia-m3-jmods/NAMING_MAP.tsv` and the receiving
M3JDK21 naming map. Every listed module depends only on `java.base`.

Synexia remains the canonical implementation and recipe owner. Files here are
receiving payloads of `synexia-openrewrite-recipes/recipes/m3-cpu-tools-jmod-20261018`;
target materialization uses the existing hash-pinned OpenRewrite snapshot recipes.
Changes to these payloads must be sealed and materialized again through that recipe.

## Public Java APIs

Each row's module and package is `com.m3.tools.` followed by the displayed suffix.
Each class also retains its original `main(String[])` entry point and `--self-test`.
Return types shown here are intentional; the graph APIs accept record lists.

| Module / package suffix | Public owner and principal API | Exact contract |
| --- | --- | --- |
| `text.normalize` | `UnicodeNormalizer.normalize(String, Normalizer.Form)`; `nfkc(String)` | Delegates to the linked JDK's Unicode normalizer. All four normal forms remain available through `normalize`. |
| `text.tokenize` | `WordTokenizer.words(String): List<String>` | `BreakIterator` with `Locale.ROOT`; retains each word-break span containing any Unicode letter or digit. No token dictionary is created. |
| `text.regex` | `RegexMatcher.find(String, CharSequence, int): List<Hit>`; `Hit(start,end,group)` | JDK `Pattern`/`Matcher.find` semantics, UTF-16 offsets, capped match count. The pattern is compiled even for a zero cap; the cap does not bound regex execution time. |
| `text.search` | `ExactSearch.positions(String, String, int): int[]` | Literal `String.indexOf`, overlapping UTF-16 hits, nonnegative result cap. An empty needle matches every UTF-16 boundary, including the final boundary. |
| `text.ngram` | `NGrams.count(String, int): Map<String,Integer>` | Overlapping UTF-16 code-unit grams, width 1 through 64. A gram may contain an isolated surrogate when the width cuts a surrogate pair. Keys are exact Java strings; iteration order is unspecified. |
| `text.frequency` | `TermFrequency.count(List<String>): Map<String,Integer>` | Exact, case-sensitive term counts; rejects null terms. Returned map is immutable; iteration order is unspecified. |
| `text.similarity` | `EditDistance.within(String, String, int): int` | UTF-16 Levenshtein distance if at most the bound, otherwise `bound + 1`. Bound is 0 through 1,000,000. Refuses work exceeding 10,000,000 matrix cells after the length-difference shortcut. Uses two primitive rows. |
| `text.diff` | `TextDelta.changedSpan(String,String): Replacement`; `Replacement(fromStart,fromEnd,toStart,toEnd)` | One half-open replacement after maximal common prefix and suffix. Replacing the indicated source span with the indicated target span reconstructs the target. This API does not compute a minimal sequence of edit operations. |
| `text.index` | `InvertedIndex.build(List<? extends List<String>>)`; `documents(String): int[]`; `terms(): Set<String>` | Immutable term postings; duplicate occurrences in one document yield one document ID, and IDs ascend. Missing terms return an empty array. Every returned posting array is detached. The term view is immutable and lexicographically ordered. |
| `graph.topology` | `TopologicalOrder.sort(int,List<Arc>): int[]`; `Arc(from,to)` | Vertices are integer IDs in `[0, vertices)`. Kahn ordering selects the smallest currently ready vertex; isolated vertices are included. Parallel arcs count separately. Invalid endpoints throw `IndexOutOfBoundsException`; negative vertex counts and cycles throw `IllegalArgumentException`. |
| `graph.paths` | `ShortestPaths.distances(int,List<Edge>,int): long[]`; `Edge(from,to,weight)` | Nonnegative long edge weights. Invalid vertex IDs, source, and negative weights refuse. Distances use `Long.MAX_VALUE` for unreachable vertices and any distance not representable below that sentinel. Overflowing relaxations are skipped. |
| `stats.streaming` | `RunningStatistics.accept(double)`; `count()`, `mean()`, `populationVariance()`, `min()`, `max()` | Original Welford arithmetic with constant-size state. Rejects a nonfinite sample before mutation and refuses count overflow. Empty numeric queries return `NaN`. Finite input admission does not guarantee finite intermediate arithmetic or finite mean/variance for extreme magnitudes. |
| `constraint.interval` | `Intervals.intersects(Span,Span)`; `conflicting(List<Span>,Span): List<Integer>`; `Span(start,end)` | Half-open long-coordinate intervals; reversed endpoints refuse and empty spans never intersect. Conflict indexes refer to original list positions. |
| `codec.varint` | `VarInts.encode(long): byte[]`; `decode(byte[]): long` | Canonical nonnegative signed-long LEB128, range `0..Long.MAX_VALUE`, length 1 through 9 bytes. Negative values, empty/null input, overlong/trailing/unterminated encodings refuse. This API does not represent the upper half of the unsigned 64-bit range. |
| `verify.sha256` | `Sha256.hex(byte[]): String`; `utf8(String): String` | JDK SHA-256 with lowercase hex. `utf8` uses the JDK UTF-8 encoding policy, including its replacement policy for isolated surrogate code units. Raw-file byte identity is a separate `M3File` contract. |
| `plan.priority` | `StablePriorityQueue<T>.offer(T,double)`, `poll(): T`, `size(): int` | Minimum finite score first; equal scores retain insertion order. Uses `Double.compare`, so negative zero precedes positive zero. Null values and nonfinite scores refuse; empty `poll` returns null. Sequence exhaustion refuses. Mutable queue instances are not thread-safe. |

## Canonical storage and ownership

Public text boundaries remain Java `String` and `CharSequence`. On M3JDK21 those
boundaries use the existing M3String-backed implementation. These ports add no
alternate string interner, token resolver, copied spelling pool, or file snapshot
store. A returned map, list, record, or primitive array is the tool's result; it is
not a new shared source registry. `M3File`, M3AST, framing, DAG validation,
capability policy, and receipts are owned by their receiving modules and the
aggregate facade, not by these sixteen algorithm modules.

Five payloads have implementation changes in addition to receiving names and
license notices:

- `TopologicalOrder` repairs a genuine donor compilation error: the declaration
  `int[] result=new int[vertices],written=0` accidentally declared the cursor as
  an array. The receiving implementation uses a scalar cursor, primitive CSR
  adjacency, an indegree lane, and a primitive minimum heap.
- `ShortestPaths` keeps the original Dijkstra and sentinel contract while using
  primitive weighted CSR and an indexed decrease-key heap. It retains no
  per-edge adjacency wrapper or per-relaxation `Step` object.
- `StablePriorityQueue` stores caller values, scores, and sequence numbers in
  parallel parent-owned arrays. It retains no per-offer `Entry` wrapper and
  clears removed value references.
- `InvertedIndex` retains sorted existing String references, a CSR start lane,
  and one primitive posting lane. It retains no source list or per-term map
  entry. Construction uses sorting and binary search: approximately
  `O(T log T + T log U)` for `T` token occurrences and `U` unique terms. This
  trades the donor map's expected linear construction for deterministic packed
  storage; no construction-speed improvement is claimed.
- `ExactSearch` uses long arithmetic for buffer growth and explicitly stops at
  the final empty-needle boundary before advancing the UTF-16 cursor.

The public graph records remain caller-facing inputs. Packed storage admission
copies their primitive values, without retaining caller lists. Scratch arrays
are local to each invocation. Record-list parsing in the aggregate facade is a
boundary representation and is not a persistent graph owner.

## Bounds and delivery scope

The direct APIs retain their individual donor contracts. They do not share a
universal input-size, process-memory, or Java-instruction budget. Callers must
bound source sizes and graph geometry before invoking allocation-heavy tools.
The aggregate `CpuTools` session applies the existing `Limits` at frame, output,
and plan boundaries; its cooperative cancellation policy is documented there.

The immutable index and stateless methods are safe for concurrent reads after
normal safe publication. `RunningStatistics` and `StablePriorityQueue` are
mutable and require caller synchronization for shared mutation. Input lists
must remain stable for the duration of construction or execution.

Lucene, OpenNLP, model distillation, WebGPU/native execution, and quantization
are outside these sixteen ports. Their existing owners are unchanged by this
receipt. Strongly connected components, flow, matching, and rolling-window
algorithms found in other donor branches are not claimed by this pack.
