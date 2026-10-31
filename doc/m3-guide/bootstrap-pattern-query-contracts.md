<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Bootstrap Pattern query and Boolean-plan contracts

This is an implementation reference for the complete `java.util.regex.M3PatternQuery` source body in `java.base`, including its two nested classes. The class is **package-private**, not a public JDK API. The reviewed public-repository revision is `1dce82bfb7f1058d74f7503c4cd54fa7d431c032`; its source blob is `741864212f351408f5ff18b5a37671cebc0c1632`, unchanged from the P02 inventory at `278b221e8365ebeb6095a3a5643b38a0564258b5`.

The direct `Pattern`, `Matcher`, `String` and `M3TQ` boundaries below were read selectively. This is **not** a complete member or node audit of those classes, the inherited `java.util.regex` package, or `java.base`. Six relevant regex test bodies were read, but no build, test, policy/recipe verifier, benchmark or runtime was executed for this documentation pass. Examples and test assertions are source evidence, not passing-run receipts.

Read [bootstrap String ownership](bootstrap-string-ownership.md) and [String operations/facts](bootstrap-string-contracts.md) for canonical storage; [bootstrap String precompute](bootstrap-string-precompute.md) owns the distinct `M3StringRegexDp` and five other P01 helper contracts. Higher-layer [regex precompute/oracles](regex-precompute-reference.md) and [CPU-tool plans](cpu-tool-precompute-reference.md) have different owners and admission rules.

## Three paths with different authority

| Entry path | Optional work | Authoritative result and scope |
| --- | --- | --- |
| `Matcher.find()` / `find(start)` → `search` | A compiled `M3TQ` requirement tested against cached exact UTF-16 trigram membership | False can prove absence in the current region. True only permits the existing regex engine to search; it supplies no match, span or capture. |
| `Pattern.matches(regex,input)` / `asMatchPredicate()` → `m3Matches` | A compiled-node `BooleanPlan`, for an already compiled pattern and a `String` input | Returns an exact full-input Boolean only for the admitted node subset and non-surrogate input. Refusal delegates to `matcher(input).matches()`. No observable `Matcher` is fabricated. |
| `String.matches(regex)` | For M3-backed storage: conservative literal equality, then the separate bounded `M3StringRegexDp`; otherwise/following `UNKNOWN`, `Pattern.matches` | The String-local helper and compiled-node plan are different stages. Neither is a general regex implementation. Stateful `Matcher.matches()` and `lookingAt()` always call `match`, which invokes `matchRoot` directly. |

Sources: [String dispatch][string-dispatch], [Pattern entry/Boolean dispatch][pattern-entry], [Pattern predicate dispatch][pattern-predicate], [Matcher search and match][matcher-search]. The adapter takes compiled JDK nodes, not regex source text. `Pattern` remains the syntax parser and flag interpreter.

## Complete declared-member map

The file declares three classes, five fields, two explicit constructors and ten methods. There are no public/protected members, records, enums, native methods, persistence APIs or callbacks supplied by application callers. `Walker` has no explicit constructor; ordinary inherited `Object` members are outside this count.

| Owner and declaration | Visibility and contract |
| --- | --- |
| `M3PatternQuery` | Package-private final utility class. |
| `MAX_NODES` | Private static final `int`, 4,096. Bounds the graph admission pass and initializes a separate walker-visit budget. |
| `M3PatternQuery()` | Private empty constructor; no instances are needed by callers. |
| `derive(Pattern.Node matchRoot, Pattern.Node root)` | Package-private static. Returns a constrained necessary-condition `M3TQ`, or `null` for refusal/no constraint; does not receive flags, input or a region. |
| `deriveBoolean(Pattern.Node start)` | Package-private static. Returns the bounded compiled-node full-match plan, or `null`; its independent local atom limit is 256. |
| `admits(Pattern.Node start)` | Private static. Identity-visited, bounded iterative graph inspection before trigram derivation. |
| `push(ArrayDeque<Pattern.Node>, Pattern.Node)` | Private static. Pushes non-null nodes only. |
| `writesRequireEnd(Pattern.Node)` | Private static. Detects five disallowed node families listed below. |
| `known(Pattern.Node)` | Private static. Recognizes the listed JDK node classes/families; recognition does not mean the node contributes a constraint. |
| `BooleanPlan` | Package-private static final nested class. Immutable retained pattern facts; evaluation scratch is invocation-local. |
| `BooleanPlan.atoms` | Private final `Pattern.CharPredicate[]`; predicates taken from JDK nodes or synthesized with `Pattern.Single` for literal units. |
| `BooleanPlan.repeated` | Private final `boolean[]`; marks greedy zero-or-more atoms. |
| `BooleanPlan.empty` | Private final `boolean[]`; nullable-prefix row, of length `atoms.length + 1`. |
| `BooleanPlan(Pattern.CharPredicate[], boolean[], boolean[])` | Private constructor, assigns the freshly prepared arrays without cloning. No external array accessor exists. |
| `BooleanPlan.matches(String)` | Package-private. Clones the nullable row; returns `1`/`0` for match/no match or `-1` on a surrogate code unit. |
| `Walker` | Private static final nested class, used for one trigram derivation. |
| `Walker.budget` | Private mutable `int`, initially `MAX_NODES`; shared by that walker's recursive subchain calls. |
| `Walker.sequence(Pattern.Node start, Pattern.Node stop)` | Package-private within the private class. Collects conjunctive requirements from one bounded chain. |
| `Walker.alternation(Pattern.Branch)` | Private. ORs each alternative's requirement; a null alternative produces unconstrained `M3TQ.all()`. |
| `Walker.flush(List<M3TQ>, StringBuilder)` | Private static. Emits exact-run trigrams only for a run of at least three UTF-16 units, then clears the builder. |

Sources: [derivation and plan][query-plans], [admission helpers][query-admission], [walker][query-walker]. The two constructors plus ten methods are the file's complete explicitly declared callable surface; the detailed rules below apply to these trusted internal entry points rather than a newly supported application API.

## Trigram derivation: recognition, refusal and information loss

### Root and graph admission

`derive(matchRoot,root)` first requires `root instanceof Pattern.Start` or `root instanceof Pattern.BnM`. Their subclasses `StartS` and `BnMS` also satisfy the test. A `Begin`- or `First`-rooted search is refused. This is about failure-state behavior: the reviewed [Start/StartS][pattern-start] and [BnM/BnMS][pattern-bnm] scans set `hitEnd` when their search fails; other roots need not do so.

`admits` uses an `IdentityHashMap`, an `ArrayDeque`, and a distinct-node limit of 4,096. It visits `next` and the following explicit child edges:

- `Branch.atoms[0..size)` and `Branch.conn`
- `Ques.atom`, `Curly.atom`, `GroupCurly.atom`
- `Prolog.loop`, `Loop.body`
- `Pos.cond`, `Behind.cond`, `NotBehind.cond`

Revisited identities are skipped; null child references are not enqueued. A 4,097th distinct visited node, an unrecognized node, or a `writesRequireEnd` node rejects the entire query. The five explicit `requireEnd` families are `Dollar`, `UnixDollar`, `Bound`, `GraphemeBound` and `Neg`, including occurrences in inspected subexpressions, not only the top-level chain.

`known` accepts the exact base `Pattern.Node` class, plus instances of `LastNode`, `Start`, `Begin`, `End`, `Caret`, `UnixCaret`, `LastMatch`, `LineEnding`, `CharProperty`, `XGrapheme`, `SliceNode`, `Ques`, `CharPropertyGreedy`, `Curly`, `GroupCurly`, `BranchConn`, `Branch`, `GroupHead`, `GroupTail`, `Prolog`, `Loop`, `BackRef`, `CIBackRef`, `First`, `Pos`, `Behind`, `NotBehind`, `LookBehindEndNode` and `BnM`. These `instanceof` tests include subclasses; the list is not a promise that every future node implementation is supported.

### What the walker actually requires

`sequence` stops at null, its supplied stop node, `Pattern.accept`, a `LastNode`, or a `LookBehindEndNode`. Each processed node decrements the shared walker budget; exhaustion returns unconstrained `M3TQ.all()` for that sequence. It does not reuse the admission pass's visited set or offer an independent stack-depth bound.

| Encountered node | Requirement and continuation |
| --- | --- |
| Case-sensitive `Slice`, including `SliceS` | Appends each buffer value with `appendCodePoint`. The accumulated run therefore contains UTF-16 units even when the source node stores code points. |
| `GroupHead`, `GroupTail`, `BranchConn` | Continues through `next` without breaking the accumulated literal run. These are the specific sequencing nodes that join adjacent runs. |
| Any other admitted node | Flushes the current run first; it cannot silently concatenate literals across a character-consuming or contextual node. |
| `Branch` | Adds OR of alternative sequences, each stopped at `branch.conn`; continues at `branch.conn.next`. A null/empty alternative needs no trigram. |
| `Prolog` | If `loop.cmin >= 1`, adds a body sequence stopped at that loop; then continues at `loop.next`. |
| `GroupCurly` or `Curly` | Adds its atom sequence only when `cmin >= 1`; then continues at `next`. No repetition count or repeated-position proof is retained. |
| A directly encountered `Loop` | Returns unconstrained `all()` for that sequence. |
| Other known nodes, including `Ques`, character classes/properties, case-insensitive slices, backreferences, anchors and admitted lookarounds | Contributes no requirement from that node; continues at `next`. In particular, lookaround condition text is inspected for admission but is not mined for in-region constraints. |

`flush` adds `M3TQ.fromExact(List.of(run))` only at length ≥3. `sequence` ANDs emitted requirements; `alternation` ORs alternatives. The `M3TQ` composition rules eliminate unconstrained terms appropriately. `derive` returns `null` if the resulting query has no constraints. Individual single-character nodes and short runs need not yield useful trigrams, even when the full expression contains literals.

Examples pinned by `M3PatternQueryTest` include constraints for `abcd`, `ab(cd)ef`, `abc(?:x)?def`, `(?:abc|abd)efg` and `(abc)+d`; no constraint for `(abc)*d`, `ab`, `(?i)abcd`, `abc$`, `abc\b`, `abc(?!x)def` or `\Aabc`. `\Gabc` is constrained in the tested compiled shape because `LastMatch` is under a `Start` root. These are implementation design pins, not public guarantees of acceleration. See the [actual test][test-query].

### Flags belong to Pattern's installation boundary

The adapter itself takes no flags. [Pattern compilation][pattern-compile] applies two distinct rules:

1. `LITERAL` without `CASE_INSENSITIVE` first installs `M3TQ.fromExact(List.of(pattern))` directly, without calling `derive`. A literal shorter than three units yields an unconstrained query and is bypassed by Matcher. The direct literal route is not excluded merely by `CANON_EQ`; canonical normalization is itself disabled for `LITERAL`.
2. General-node derivation is attempted only when `m3Tq == null`, `!LITERAL` and `!CANON_EQ`. Other flag behavior is represented by the compiled nodes. A case-insensitive slice supplies no exact run, but a mixed/scoped-flags expression can still retain requirements from its case-sensitive portions.

This is neither a flags-blind source-text recognizer nor a guarantee that all case-insensitive expressions are rejected as a whole.

## Matcher facts, identity, region and observable state

`m3TqAllowsSearch()` returns true immediately for no query, an unconstrained query, or any input that is not a `String`. It also bypasses preparation for a region length below zero or above **32,768 UTF-16 units**. The guard concerns the selected region, not the complete backing String, resident bytes, number of matches or a process memory budget.

For an eligible String it prepares `M3TQ.precompute(text,this.from,to,32768)`. These are exact sorted distinct packed 48-bit UTF-16 trigram keys, not probabilistic hashes. Membership loses order and multiplicity, so presence cannot prove a regex match. Preparation scans the selected half-open region without retaining another input reference or producing a substring. The [relevant M3TQ methods][tq-facts] validate ranges/budgets, scan and sort; `testPrecomputed` evaluates the existing query against those facts.

The cached tuple is `m3TqFacts`, `m3TqFactsFrom`, `m3TqFactsTo`. Identity is maintained by the Matcher's existing `text` reference and its reset boundary, not by a text hash, value equality, backing-owner coordinate or global cache:

- `reset()` retains derived facts, resets ordinary match/append state, and restores the full-input region. The next gate compares both region endpoints before reuse.
- `reset(input)` clears the facts and sets both endpoints to `-1` **before** assigning a different input reference (`input != text`). Equal text in a distinct `String` invalidates; the identical String can reuse. A failed `reset(null)` has already released the old facts.
- `region(start,end)` validates before reset and endpoint assignment. Changed eligible endpoints cause later recomputation; an invalid region throws before changing the cache. Returning to a prior region does not imply a multi-region cache.
- `usePattern` leaves the facts in place: they describe text/range and can serve another query. Plain reset, `find(start)` and repeated searches can reuse them when input identity and endpoints still agree.
- Mutable/custom `CharSequence` inputs never use the gate/cache. An over-budget region bypass does not itself clear an old same-input cache; it simply cannot use it for that search.

Sources: [cache fields][matcher-fields], [usePattern/reset][matcher-reset], [region][matcher-region], [gate/search][matcher-search]. `useTransparentBounds` and `useAnchoringBounds` change regex semantics but do not invalidate these literal membership facts. Facts remain restricted to the isolated region; the adapter does not derive requirements from lookaround context outside it. The engine remains responsible for contextual and boundary decisions on allowed searches.

The region is `this.from..to`, not `search(int from)`'s later soft search position. A query allowed by the whole region can still fail from the current position; that is a safe loss of pruning. A rejected whole region has no admitted match to find at any later soft position.

### The early-failure state transition

Within `search`, before the gate, Matcher clears `hitEnd` and `requireEnd`, normalizes the soft start, initializes `first`/`oldLast`, clears all group slots and populated `localsPos` sets, and selects `NOANCHOR`. If the gate returns false it then:

- sets `hitEnd = true` and `first = -1`
- leaves `requireEnd = false`, the already cleared groups and the prior `last` intact
- assigns `oldLast = last`, increments `modCount`, and returns false

It creates no capture values or spans and does not change `lastAppendPosition`. Allowed searches execute `parentPattern.root.match`, which retains success/capture authority. `matches()` and `lookingAt()` use `match` and never enter this gate. `find()` also has a pre-existing beyond-region early return before `search`; therefore the description above is specifically the **gate rejection branch**, not a claim about every failed `find()` call. See [find and anchored entry points][matcher-find] and [search/match][matcher-search].

## BooleanPlan: exact only inside its admitted subset

`deriveBoolean` walks a single `next` chain until the exact `Pattern.lastAccept` sentinel. Each node must be:

- `CharPropertyGreedy` with `cmin == 0`: one repeated predicate atom
- `CharProperty`: one nonrepeated predicate atom
- `Slice`: one nonrepeated `Pattern.Single` atom per buffer entry, provided the slice is nonempty, fits the remaining atom budget, and every value is ≤`Character.MAX_VALUE` and not a surrogate

Any other node, more than 256 total atoms, or a chain with **no greedy repeat** returns null. Positive-minimum greedy properties do not qualify; groups, branching, anchors, lookarounds, backreferences, finite-curly nodes and reluctant/possessive forms qualify only if compilation actually eliminates them into the admitted straight-line shape. The source does not recognize those constructs separately. `SliceS` is a `Slice` subclass, but its supplementary values are refused by the explicit unit checks. Case-insensitive slice families are not `Slice` subclasses and are refused.

The preparation's second walk creates the predicate, repeat and nullable-prefix arrays. `Pattern` attempts this plan only without `CANON_EQ`. Other flags can be represented by the reused JDK predicates: for example, character-class and dot predicates already encode their compiled flag semantics. No adapter-specific parser or independent Unicode-property table is installed. See [plan construction][query-plans] and [the actual predicate/slice node types][pattern-atoms].

`BooleanPlan.matches` clones `empty`, then uses one rolling row. Nonrepeated atoms require the old diagonal cell and their predicate. Repeated atoms accept either the updated left cell (zero occurrences) or the old above cell plus their predicate (another occurrence). For any surrogate input unit, it returns `-1` and discards partial work so `m3Matches` can use Matcher for code-point semantics. This applies to paired and unpaired surrogates. Empty input returns the prepared nullable-prefix result.

For `m` prepared atoms and `n` non-surrogate UTF-16 input units, the loop has `O(n*m)` cell work and one `O(m)` row per call; `m <= 256`. There is **no separate input-length or total-cell ceiling** in this plan. The 8,192-unit and cell limits of `M3StringRegexDp` must not be imported here. Predicates/plan arrays are retained by Pattern, while the row is invocation-local; concurrent predicate calls do not share a mutable row. No input, match history, capture workspace or output String is retained by the plan.

`Pattern.m3BooleanPlan` is transient and published with the existing volatile `compiled` write. `m3Matches` consults it only when `compiled` is true and input is a `String`. Custom/mutable input, no plan, or `-1` uses `matcher(input).matches()`. Nonempty deserialized patterns first follow `matcher`'s synchronized lazy compilation; empty deserialized patterns have their direct empty roots and no plan. The plan is not a serialized execution image. See [Pattern fields][pattern-fields], [Matcher construction][pattern-matcher], [lazy reconstruction][pattern-lazy] and [Boolean dispatch][pattern-predicate].

## Direct String and higher-level caller boundaries

- `String.matches` explicitly rejects null regex first. With M3 storage it may return literal equality or the separate `M3StringRegexDp` decision before compiling a Pattern; `UNKNOWN` reaches `Pattern.matches`, which can then select `BooleanPlan`. These are independent fallback stages, not one shared plan/cache.
- `String.replaceFirst` and `replaceAll` may use the conservative M3 literal-replacement lane; otherwise they compile Pattern and invoke Matcher replacement. The reviewed String-replacement Matcher methods reset and call `find`, so the absence gate can participate there. Replacement parsing, `$`/backslash interpretation, capture substitution and returned String construction remain outside `M3PatternQuery`.
- `String.split`/`splitWithDelimiters` have character and literal paths before their Pattern fallback. Pattern's splitting loop uses Matcher searches; `splitAsStream` creates a Matcher lazily and calls `find`. `Pattern.asPredicate()` likewise invokes `matcher(s).find()`, unlike `asMatchPredicate()`'s Boolean full-match route.

Sources: [String matches][string-dispatch], [String replacement][string-replacement], [String split dispatch][string-split], [Matcher replacement calls][matcher-replacement], [Pattern split loop][pattern-split], [split stream][pattern-stream] and [predicates][pattern-predicate]. This is a direct dispatch map, not a complete replacement, splitting, stream or callback-contract audit; the [String-facing reference](bootstrap-string-contracts.md) covers its separately reviewed owner methods.

## Refusal and error behavior: exact catch boundaries

| Boundary | What actually happens |
| --- | --- |
| `derive` / `deriveBoolean` | Unsupported shape/budget/no useful plan returns null as described. The helper methods have no catch-all handler or standalone public argument-validation contract. |
| Literal `M3TQ` preparation in `Pattern.compile` | Catches `OutOfMemoryError` and sets the query to null. |
| General `derive` and `deriveBoolean` preparation in `Pattern.compile` | Each catches `OutOfMemoryError` or `RuntimeException` and clears that optional result. These catches do not include every `Error`. The Pattern constructor separately converts a `StackOverflowError` escaping compilation into its syntax-error path; the lazy `matcher` compilation boundary is distinct. |
| Region facts preparation in `Matcher.m3TqAllowsSearch` | Only `OutOfMemoryError` from the preparation call is caught, returning true to use the engine. The endpoint/cache assignments occur after successful preparation. There is no blanket catch around `query.testPrecomputed(facts)`. |
| `BooleanPlan.matches` invocation | Surrogate input gives the explicit `-1` fallback. Row cloning/allocation and predicate execution are not wrapped in an OOM/runtime-exception fallback by `m3Matches`; exceptions propagate. |
| Public syntax/null contracts | Pattern compilation still owns invalid syntax/flag rejection. `Pattern.matches` compiles before matching input; the test explicitly checks invalid `"*"` with null input yields `PatternSyntaxException`, while valid `"a*"` with null yields `NullPointerException`. `String.matches` has its own earlier regex-null check. |

These are source-observed boundaries, not an injected-allocation-failure proof. In particular, a preparation-time fallback comment cannot establish that the whole optimization adds no possible execution-time failure or that its recursive derivation/evaluation has a separately proven stack/resource bound.

## Test evidence read, with limits

| Complete test body | Relevant assertions present in source | What that does not establish |
| --- | --- | --- |
| [M3PatternQueryTest][test-query] | 10,000 generated expressions ×64 strings plus targeted constructs/flags; String versus non-String ungated Matcher; matches/lookingAt results, repeated find result, end flags, spans and group boundaries; explicit admission pins and at least one prune. | No execution receipt here; no exhaustive syntax, unknown-node, 4,096-node, deep-stack or allocation-failure qualification. |
| [M3PatternDpTest][test-dp] | Compiled predicates versus `StringBuilder` Matcher; static/String entry points where applicable; finite generated, targeted and seeded-random expressions; flags/Unicode/surrogates, serialization/null/syntax order, concurrent calls, plan reuse/refusal, 256/257 boundaries and hostile cases with 100,000-unit negative input and 100,001-unit positive input (100,000 `a` units plus `b`). | Does not prove every compiled predicate/node shape, every error path or general-regex DP support. |
| [M3RegexLiteralTQTest][test-literal] | Literal positives/negatives, selected regions, supplementary text, case-insensitive/short-literal result/state parity, anchored result/state parity, mutation visibility. | The literal route alone does not prove general-node query derivation. |
| [M3RegexReuseTest][test-reuse] | Repeated find/reset/find(start), regions/bound flags/usePattern; captures/hasMatch/failed-start exceptions; cache object identity, equal-but-distinct input invalidation, mutable/null reset, over-budget bypass; separate optional allocation/timing mode. | Cost-mode source is not a measured speedup; no complete concurrency or resource-failure qualification. |
| [M3RegexWorkspaceContractTest][test-workspace] | Request-confined reuse versus a fresh non-String Matcher; region/bound flags, captures/snapshot and separate per-worker matcher instances. | Parallel independent workspaces do not make a single mutable Matcher thread-safe. |
| [M3RegexAtomSeamTest][test-seams] | Concat/subrange/repeat corpus with surrogate and line-ending seams; flags, find/anchored operations, captures/state, replacement, split and split-stream comparisons. | Finite seam inputs are not complete backing/JNI/GC/JIT/CDS or all-region qualification. |

The separate [M3PatternQueryDifferential driver][test-driver] in `m3/ports/precompute` was also read. It compares first-find String and non-String results, end flags and spans over `M3RegexOracleCorpus`, emits an ungated outcome digest, and optionally reflects the query/prune counts. Its instructions for stock-versus-patched comparison are not evidence that either run occurred here, and it does not replace the repeated-search/capture tests above.

The source headers specify jtreg modes/modules where needed; use the repository [testing guide](../testing.md) and [contribution prerequisites](../../CONTRIBUTING.md) with a complete matched JDK image. Builds, all listed test runs, full regex regression, policy/recipe verifiers, interpreter/C1/C2/GC/CDS/JNI/native-platform gates, cold/warm cost and retained-memory measurements remain **NOT_RUN** in this pass.

## Coverage and ownership boundary

P02 now has a complete source-body/member reference for its one M3-prefixed adapter file and selected direct consumer paths. This closes neither every inherited regex class nor `M3TQ`'s entire internal API, public JDK behavioral qualification, inline package documentation, runtime acceptance or repository-wide coverage. The [coverage ledger](coverage.md) remains authoritative about those distinctions.

The [porting invariant](../../m3/docs/m3jdk21-porting-invariant.md), [name mapping](../../m3/docs/name-mapping.json), [full-family receiver](../../m3/synexia-import/current-full-borrow/README.md) and per-file rights remain unchanged. The target owns bootstrap Java/HotSpot/JNI integration; the existing public provenance records do not create a second reusable recipe authority or a runtime dependency. Receiving stays **String → arrays → collections → AST/compiler → remaining families**. This documentation adds no source/build/workflow changes or phase advancement.

[query-plans]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/util/regex/M3PatternQuery.java#L35-L139
[query-admission]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/util/regex/M3PatternQuery.java#L141-L225
[query-walker]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/util/regex/M3PatternQuery.java#L227-L306
[pattern-fields]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/util/regex/Pattern.java#L991-L1023
[pattern-entry]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/util/regex/Pattern.java#L1229-L1232
[pattern-matcher]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/util/regex/Pattern.java#L1183-L1193
[pattern-lazy]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/util/regex/Pattern.java#L1534-L1594
[pattern-compile]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/util/regex/Pattern.java#L1913-L2034
[pattern-start]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/util/regex/Pattern.java#L3815-L3888
[pattern-bnm]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/util/regex/Pattern.java#L5674-L5848
[pattern-atoms]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/util/regex/Pattern.java#L4141-L4558
[pattern-predicate]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/util/regex/Pattern.java#L6029-L6062
[pattern-split]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/util/regex/Pattern.java#L1406-L1466
[pattern-stream]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/util/regex/Pattern.java#L6099-L6164
[matcher-fields]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/util/regex/Matcher.java#L149-L152
[matcher-reset]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/util/regex/Matcher.java#L393-L469
[matcher-find]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/util/regex/Matcher.java#L739-L819
[matcher-region]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/util/regex/Matcher.java#L1537-L1549
[matcher-search]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/util/regex/Matcher.java#L1740-L1826
[matcher-replacement]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/util/regex/Matcher.java#L1209-L1436
[string-dispatch]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/lang/String.java#L3372-L3424
[string-replacement]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/lang/String.java#L3478-L3534
[string-split]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/lang/String.java#L3831-L3869
[tq-facts]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/jdk/internal/mindex/M3TQ.java#L54-L327
[test-query]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/test/jdk/java/util/regex/M3PatternQueryTest.java
[test-dp]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/test/jdk/java/util/regex/M3PatternDpTest.java
[test-literal]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/test/jdk/java/util/regex/M3RegexLiteralTQTest.java
[test-reuse]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/test/jdk/java/util/regex/M3RegexReuseTest.java
[test-workspace]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/test/jdk/java/util/regex/M3RegexWorkspaceContractTest.java
[test-seams]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/test/jdk/java/util/regex/M3RegexAtomSeamTest.java
[test-driver]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/ports/precompute/src/test/java/com/m3/precompute/M3PatternQueryDifferential.java
