<!-- SPDX-License-Identifier: Apache-2.0 -->
# Decorator fill: commons-collections decorators converged by atoms (2026-10-30)

This is leaf 1 of the commons-collections convergence lane: the `hsoliwal/commons-collections` fork converges into
M3JDK as atoms and patterns, not as file ports. Of the 88 apache scaffold modules from the five-provider inventory,
57 are decorators. This directory holds the generator, the ledgers and the proof for that family.

## Pattern DECORATOR_FILL v1.1

`M3DecoratorFill.java` treats each scaffold method as one atom. It resolves the atom by reflection on Apache Commons
Collections 4.5.0, which is the scaffold's surface version, and classifies it:

| Category | When | Emitted |
|---|---|---|
| OBJECT | declared by `Object` | dropped; the inherited method is used |
| DEFAULT | declared by an interface | `Iface.super.m(..)`, so the same default chain runs |
| THROW, DELEGATE, EQUALS, WRAP, FACTORY | Commons body of that shape | template over the `backing` lane |
| MANUAL | any other body | the class keeps its scaffold; the atom is listed in `METHOD_ATOMS.tsv` |

No provider body is copied. Views use the JDK `java.util.Collections` unmodifiable owners, or the class's own factory or
constructor. `PATTERNS.tsv` lists the categories and the next candidate categories.

## Result

| | v1.1 | v1.2 (SYNC family) | v1.3 (PREDICATED family) |
|---|---|---|---|
| Method atoms classified (57 decorators) | 1,938 | 1,992 (with the view-helper atoms) | 2,098 (with helper and lane-member rows) |
| MANUAL atoms (next leaves) | 521 | 387 | 264 |
| Classes with no MANUAL atom, now owned | 7 | 12 | 22 |

The v1.1 classes are `M3UnmodifiableCollection`, `M3UnmodifiableList`, `M3UnmodifiableQueue`, `M3UnmodifiableSortedSet`,
`M3UnmodifiableNavigableSet`, `M3UnmodifiableBag` and `M3UnmodifiableSortedBag`.

v1.2 adds the Synchronized family: `M3SynchronizedCollection`, `M3SynchronizedBag`, `M3SynchronizedMultiSet`,
`M3SynchronizedQueue` and `M3SynchronizedSortedBag`. The categories are ACCESSOR, SYNC, GUARD, VIEW, FACTORY_NEW and
MODIFIER; their rows are in `PATTERNS.tsv`.

The categories came from an analyst-and-skeptic workflow per family. The skeptic confirmed every SYNC category and refuted
only the proof, which could not see lock scope or the true path of `equals`. Both gaps are closed in v1.2:
- `M3SyncProbe` reports BLOCKS or FREE for every method and view, for `equals(self)`, and for the monitor of a deserialized copy.
- `M3LockTraceProbe` records `Thread.holdsLock` at every backing call.
- `equals` now also receives an equal, non-identical copy of the backing.

Results are in `PROBES.tsv`: 5 classes × 2 probes, 0 mismatches. In a negative control, a planted unsynchronized `peek` is
caught by both probes.

The Transformed and Unmodifiable-map families have proposals with skeptic refutations; they are the next leaves. The
Predicated family is done in v1.3 (below).

## v1.3: PREDICATED family

v1.3 owns 10 Predicated decorators: `M3PredicatedBag`, `M3PredicatedList`, `M3PredicatedMap`, `M3PredicatedMultiSet`,
`M3PredicatedNavigableSet`, `M3PredicatedQueue`, `M3PredicatedSet`, `M3PredicatedSortedBag`, `M3PredicatedSortedMap` and
`M3PredicatedSortedSet`. `M3PredicatedCollection` keeps its scaffold: its 2 MANUAL atoms are `builder(Predicate)` and
`notNullBuilder()`, which return the Commons implementation class `PredicatedCollection.Builder` (RESIDUE_COMMONS_BUILDER,
waiting for a surface decision). Predicated MANUAL atoms went from 76 (plus 1 mis-derived constructor) to 2.

The categories, each with a template and a `PATTERNS.tsv` row:

| Category | Shape | Emitted |
|---|---|---|
| LANES_CONSTRUCTOR | flattened Commons constructor; statements s1-s4 only, every value a constructor parameter | same statements over `backing` + policy lanes, Commons null-check order |
| FACTORY_NEW (n-ary) | `return new Self<>(p0, p1[, p2]);`, only with a valid LANES_CONSTRUCTOR | `return new M3Self<>(arg0, ..);` |
| VALIDATE, CHECK_SET_VALUE | lane members made of `if ([L != null && ]!L.test(p)) { throw new IAE(MSG); }` [+ `return p;`] | private members; MSG literals extracted as data |
| GUARD_DELEGATE | elem / entry / each / each-entry guard + DELEGATE (or `super.m` whose body is DELEGATE) | guard + `backing.m(..)` |
| LANE_VIEW | `return W(decorated().m(..), lanes)`, lanes in constructor order | self: `new M3Self<>(..)`; sibling superclass: generated nested helper |
| SELF_CALL | `[return ]m(params, int literals)` on the class surface | same call |
| INNER_VIEW | `return new N(decorated().m(..))`, N a Commons inner class | generated inner helper (PredicatedListIterator) |
| MAP_ITERATOR_ADAPTER | `return new EntrySetToMapIteratorAdapter<>(entrySet());` | H2 template `EntrySetMapIterator` |
| CHECKED_ENTRY_SET | `if (isSetValueChecking()) {..EntrySet..}` with `return L != null;` and a validating checkSetValue | H3 template `CheckedEntrySet` |
| SERIAL_HOOK | transient Commons backing + `defaultWriteObject(); writeObject(map)` hooks | transient `backing` + the same hooks |

Normalizations: SCOPE (depth-1 member lookup, so `PredicatedCollection.Builder` no longer shadows `add`/`addAll`),
RESOLVE_BRIDGE (`entrySet` behind a visibility bridge), ALIAS (backing fields `map`, `collection`, `iterator` and their
accessors, unless shadowed) and DEPRECATED_LANE_TYPE (Commons `Predicate` lanes are typed `java.util.function.Predicate`).
The two generated helpers (`PredicatedSortedSetView` in `M3PredicatedNavigableSet`, `PredicatedListIterator` in
`M3PredicatedList`) are filled by the same generator from the Commons class; only H2 and H3 are fixed templates, each
guarded by a contract check on the Commons classes. The 12 classes owned before v1.3 regenerate byte-identical.

The skeptic verdict on the analyst prototype was FLAWED. Each refutation now has a check and a planted-defect control that
the check catches (`NEGATIVE_CONTROLS.tsv`, 16 of 16 caught):

| Refutation | Check | Control |
|---|---|---|
| R1 a wrong sibling helper passes | `M3PredicatedProbe` VIEW_SWEEP: every method of every view within two hops | NC01 helper addAll without validation, NC02 helper headSet raw |
| R2 MapIterator state machine unchecked | MAP_ITERATOR script (before next, after remove, after reset) | NC03 remove keeps entry, NC04 reset clears entry |
| R3 null-check order untested | differential: every null combination of factory arguments, with messages | NC05 requireNonNull order swapped |
| R4 CHECKED_ENTRY_SET side conditions | rule checks R4_* (v1.3: TransformedMap stays MANUAL; v1.5: an unemitted hook stays MANUAL) + view sweep | NC06 checkSetValue guard removed, NC07 lane swapped |
| R5 `this.F = X` accepted `this.lock = this` | rule checks R5_* | NC08 parameter guard removed |
| R6 super, alias, sibling and lane-order guards | rule checks R6_* | NC09, NC10, NC11, NC12 (one per guard) |
| R7 serialization field order | probe SERIAL: NotSerializableException culprit with non-serializable lanes and/or proxy backing | NC13 default field order |

The skeptic's missed semantics are covered too: per-lane predicate counts (NC14, value lane checked before key lane),
ListIterator backward navigation (NC15) and a null-rejecting policy (NC16). Helpful-NPE text stays excluded because it
names local variables.

Shared rules also reclassified atoms in families that are still scaffolds (for example LANES_CONSTRUCTOR and FACTORY_NEW
in Transformed*, SELF_CALL in CollectionBag, MAP_ITERATOR_ADAPTER in FixedSizeMap, LazyMap and TransformedMap). MANUAL
atoms by family, v1.2 to v1.3: Predicated 76 to 2, Transformed 88 to 59, Composite 74 to 73, Unmodifiable (map family) 65
to 65, FixedSize 39 to 30, Lazy 21 to 16, CollectionBag 14 to 10, Defaulted 7 to 6, other 3 to 3. None of those classes is
written until it has no MANUAL atom and its own differential target.

The v1.1 MANUAL atoms grouped into these families:

| Family | Atoms |
|---|---|
| SYNC_DELEGATE (Synchronized*) | 98 |
| TRANSFORM (Transformed*) | 45 |
| FACTORY_2ARG (Predicated*, Transformed* factories) | 40 |
| NO_FACTORY_CTOR (Composite*) | 38 |
| MAP_VIEW (map decorators) | 38 |
| FIXED_SIZE | 24 |
| other shapes | 238 |

## v1.4: JDK standard

M3JDK21 keeps the source standard of the JDK itself. v1.4 adds a JDK_STANDARD pass to the generator, run on every owned
class after classification, so the emitted code meets `javac -Xdoclint:all/protected` and the M3JdkStandard javac plugin
(`m3/compiler/jdk-standard`). The fixes live in the recipe, not in the 22 outputs:

- **Parameter names:** the names the API declares replace the reflection names `arg0`, `arg1`, ... (576 in v1.3). Donors
  are parsed with javac's own API: the decorated Commons class and its supertypes first, then `src.zip`. A donor name
  that is not lowerCamel is lowered (Commons declares `predicatedQueue(final Queue<E> Queue, ...)`). An unmatched name
  is residue and fails the run.
- **Imports:** explicit single-type imports replace qualified names wherever the simple name is unambiguous; the two
  `Predicate` types stay qualified where both are used.
- **Javadoc:** `@param` tags for type parameters in java.util wording, and Javadoc on every public factory.
- **Layout:** 100 columns, 8-space continuation, breaks at the outermost fitting level. A long string literal is split
  as a fallback; text blocks and trailing line comments are never touched.

Proof: the 22 owned classes compiled with `-g:none` are byte-identical to v1.3, so behaviour is unchanged; two runs are
byte-identical; differential 30/0, probes 28/0, negative controls 16/16 (seven spec rows moved to the v1.4 text, same
planted defects). The generator meets the standard itself: its own layout was applied by the same pass and its classes
compiled with `-g:none` are byte-identical before and after.

## v1.5: Transformed family

v1.5 fills the Transformed decorators. The policy lane is a transformer (`Transformer<..>` in Commons, the
`java.util.function.Function<..>` lane type in the owned class), and the recipe gained five categories:

- **TRANSFORM_LANE:** the Commons hook `return valueTransformer.transform(value);` is a lane member, emitted as
  `return valueTransformer.apply(value);`. With it `checkSetValue` is emittable, so TransformedMap's `entrySet()` is
  CHECKED_ENTRY_SET like PredicatedMap's.
- **TRANSFORM_DELEGATE:** `decorated().m(transform(x))` and the key, value, collection and map forms become
  `backing.m(..)` over the same lane members.
- **TRANSFORM_MEMBERS:** the `transform`, `transformKey`, `transformValue` and `transformMap` members the class calls,
  emitted once per class (identity when a lane is null; maps copy into a `java.util.LinkedHashMap` in iteration order).
- **TRANSFORMED_FACTORY:** `transformedX(owner, lanes)` decorates first, then transforms the existing contents.
- **LANE_VIEW over a plain factory:** a view returned through `transformingX(..)` of the class itself, a same-package
  sibling, or a sibling imported by the Commons source that declares the method (`bag.TransformedBag.uniqueSet()`
  returns `set.TransformedSet.transformingSet(..)`; TransformedSortedBag inherits that body).

Result: 10 more owned classes (Collection, List, Set, SortedSet, NavigableSet, Queue, Bag, SortedBag, Map, SortedMap),
32 filled in all. TARGETS.tsv has 14 new rows, one per class and lane policy (`both`; maps also `keynull` and
`valuenull`): 44 targets, 0 mismatches. MANUAL atoms 211 to 209 overall; TransformedMultiValuedMap keeps 4 (three
`putAll` loops and its copying factory), recorded as the next candidate in PATTERNS.tsv.

Rule check R4 changed with the pattern: `R4_TRANSFORMING_HOOK` now expects CHECKED_ENTRY_SET, and the negative side
moved to `R4_UNEMITTED_HOOK` (the `checkSetValue` member recorded as not emitted must keep `entrySet()` MANUAL). NC06
(guard replaced by `return true;`) is caught by the new case. The 10 new classes pass `-Xdoclint:all/protected` and
the M3JdkStandard plugin; refilling them gives byte-identical files; the 22 v1.4 classes are unchanged.

## v1.6: Unmodifiable map family

v1.6 extends WRAP and ALIAS to the Unmodifiable maps and adds one helper:

- **WRAP over `super.m()`:** `final Set<K> set = super.keySet(); return UnmodifiableSet.unmodifiableSet(set);` (and
  the inline form) reads as `decorated().keySet()` when the superclass body is the delegate, through the same R6(1)
  guard as `restDelegate`.
- **JDK owners:** `UnmodifiableCollection`, `UnmodifiableSortedMap` and `UnmodifiableMap` factories map to
  `java.util.Collections`; `UnmodifiableEntrySet` on `entrySet()` maps to
  `Collections.unmodifiableMap(backing).entrySet()`; a `Collections.unmodifiableX(..)` call in the Commons source is
  kept as is.
- **ALIAS without an accessor:** UnmodifiableTrie keeps its owner in `delegate`, assigned through a cast local; the
  only instance field of a class outside the decorator chain is an alias when its assignment is a `requireNonNull`.
- **UNMODIFIABLE_MAP_ITERATOR (H4):** no JDK type is an unmodifiable `MapIterator`, so a nested
  `Unmodifiable[Ordered]MapIteratorView` delegates the reads and rejects `remove` and `setValue` with the Commons
  messages.
- **Constructor of a K, V owner:** the backing is typed over the class's own type parameters, and the null message is
  the one of the Commons constructor chain (`"map"`, `"trie"`).

- **INVERSE_CACHE:** a bidi map's inverse view, created once and linked back, over a field of the owned class typed
  with the type parameters swapped (the scaffold keeps Commons' `synchronized`).

Result: UnmodifiableOrderedMap, UnmodifiableTrie and the three Unmodifiable bidi maps are owned (37 filled); MANUAL
atoms 209 to 153. TARGETS.tsv has five new rows (backings `linkedmap`, `trie`, `dualhashbidi`, `treebidi`,
`dualtreebidi`): 49 targets, 0 mismatches, rule checks 13/13, negative controls 16/16. The other Unmodifiable maps
keep 1 to 3 atoms each (UnmodifiableSortedMap's ordered navigation, UnmodifiableMap's IterableMap branch,
UnmodifiableMultiValuedMap's keys); they are the next candidates in PATTERNS.tsv. The
GENERATED block now takes its version from one constant, a comment-only change for the earlier classes (`-g:none`
class files unchanged).

## Generator atoms

The generator is five atom files under one CLI contract
(`M3DecoratorFill <sources> <scaffold> <fqn> <out> <atoms>` and `--rule-check <sources> <out>`). Each can be
replaced alone while that contract holds:

| Atom | Lines | Contract |
|---|---|---|
| `M3DecoratorFill.java` | 2,173 | CLI; atomize, resolve, classify and emit one scaffold |
| `M3DecoratorPatterns.java` | 106 | the regular expressions the classifier matches and the WRAP table (data only) |
| `M3DecoratorTemplates.java` | 365 | helper text blocks H2, H3 and H4 (data only) |
| `M3DecoratorJdkStandard.java` | 1,054 | the JDK_STANDARD pass: parameter names, imports, Javadoc, layout |
| `M3DecoratorRuleCheck.java` | 174 | the R4/R5/R6 classifier checks behind `--rule-check` |

The split was mechanical: line ranges moved byte for byte, with nested classes dedented. The only other edits were
the class names, a record accessor in place of a field read, and package-private access on the core members the
atoms call. Proof: all 37 owned classes are byte-identical to the run before the split; the verdict is PASS;
negative controls 16/16 (generator mutants compile with the unchanged atoms); and the four files pass
`-Xdoclint:all/protected` and M3JdkStandard. `run.sh` compiles the atoms and pins them in PROVENANCE.tsv.

Pass 2 moved the 11 regular expressions and the WRAP table into `M3DecoratorPatterns`, byte for byte.
References were requalified by a lexer that skips string literals, text blocks, character literals and comments,
because the same names occur inside strings (the category `"WRAP"`, the header text). Only identifier tokens in
code changed, on 15 lines. Proof: verdict PASS; the 3,262 JUnit tests pass, including the 37 byte-identical
replays; no owned class changed.

## Pipeline order: atomize, JIT, patternize

`run.sh` runs the stages in this order:

1. **Atomize.** The fill stage makes each scaffold method one atom, one row in `METHOD_ATOMS.tsv`.
2. **JIT.** The `jit` stage (`M3DecoratorJitTest`, 1,561 tests) needs `JUNIT_CONSOLE`. A child JVM runs the warmed
   harness over all 49 targets with `-XX:+PrintCompilation` (`M3DecoratorDifferential --jit`). Each public method of
   each owned class passes when the JIT compiled it and its bytecode fits HotSpot's hot-method inlining limit
   (`FreqInlineSize`, 325 bytes). The bytecode size comes from the class file, through a small JVMS reader in the test.
   PrintCompilation names a method by name and size, so overloads are told apart by size. The tier reached is
   recorded, not asserted: HotSpot drops to tier 2 while the C2 queue is long.
   - `JIT_SNAPSHOT_20261031.tsv`: all 1,561 atoms compiled, 630 by C2.
   - The largest atom is 61 bytes, and 1,524 atoms fit `MaxInlineSize` (35 bytes), so they are inlined even when cold.
3. **Patternize.** The patternizer passes follow: differential, probes, negative controls, and the JUnit atom,
   generator and perf tests.

## JUnit and perf tests per atom

`run.sh` has a `junit` stage, which runs when `JUNIT_CONSOLE` names a junit-platform-console-standalone jar. On
2026-10-31 it ran 3,262 tests and all passed:

- **`M3DecoratorAtomTest` (1,575 tests):** for each of the 49 TARGETS rows, the seeded differential runs in process
  (`M3DecoratorDifferential.run`). Each public method of the owned class is one test. It passes only when the seeded
  steps called the method and no mismatch names it. A class contract test per target covers the factory, identity,
  serialization and backing-state checks.
- **`M3DecoratorGeneratorTest` (126 tests):** one test per generator atom output:
  - each of the 13 rule checks;
  - each of the 37 owned classes refilled from its scaffold in a fresh JVM, byte-identical to the committed file;
  - the layout of each owned class is a fixed point;
  - each helper template (H2, H3, H4 and H4 ordered) compiles in a host class.
- **`M3DecoratorPerfTest` (1,561 tests):** one perf guard per method atom (`M3DecoratorDifferential.perf`). The owned
  decorator and Commons get equal backings and the same seeded arguments. Both are called reflectively with access
  checks off, after 20,000 warm-up calls. The result is the median of 5 rounds of 500 calls, alternating which side
  runs first. A method fails above 4 times the Commons time per call. A target with an atom over the bound is
  measured once more, warm: JIT state left by earlier targets skews a first pass (a probe measured `isEmpty` at 39
  ns first and 5.2 ns warm, equal to Commons). `PERF_SNAPSHOT_20261031.tsv` holds the timings: mean ratio 1.13, and
  1,005 of 1,561 atoms within 10%. This is a guard against pathological regressions, not a benchmark.

New code names every literal (columns, properties, defaults, messages) as a constant.

## Proof

`M3DecoratorDifferential.java` runs a seeded reflective differential of each owned class against the Commons class,
which is used as a test oracle only:

- 8 seeds × 4,000 steps per class.
- Each step either mutates both backings directly, or calls one declared public method on both sides with equal arguments.
- Each step compares return values, exception classes, drained iterators and views (including mutation attempts on them), and the backing state.
- Factory null rejection, factory identity on an already unmodifiable argument, and a serialization round trip are each checked once.

Results:

- `DIFFERENTIAL.tsv`: 30 targets, 0 mismatches. The 12 v1.1/v1.2 targets run 64,003 checks each, unchanged by the v3
  harness. The 18 Predicated targets (backings and the policies both, keynull, valuenull and rejectnull) run 96,007 or
  96,011 checks each: 1,728,154 in total.
- `PROBES.tsv`: 28 probe runs, 0 mismatches (10 lock probes; 18 `M3PredicatedProbe` runs with 124,879 checks).
- `RULE_CHECKS.tsv`: 12 classifier side-condition checks, all PASS.
- `NEGATIVE_CONTROLS.tsv`: 16 planted defects from `NEGATIVE_CONTROLS_SPEC.tsv`, applied by `M3Mutant.java`, all CAUGHT.
- `MISMATCHES.tsv`: empty.
- All 22 generated classes compile with `-Xlint:all -Werror`, `-Xdoclint:all/protected` and `-Xplugin:M3JdkStandard`.
- Earlier negative control: a planted defect (`removeIf` delegating instead of throwing) gives 50 mismatches.

`CONVERGE_LEDGER.tsv` lists every fork class, using the columns agreed between the convergence lanes: fork, fork_rev,
fork_class, m3_owner, absorb_state. After v1.3 the counts are 31 ABSORBED and 316 GAP (v1.1: 16 and 331).

## M3IFY primitives

The decorator family is one instance of a general sequence, recorded as the `M3IFY` rows of `PATTERNS.tsv`:

1. ATOMIZE: one atom per method.
2. RESOLVE: find the donor member by reflection.
3. CLASSIFY: map it into a closed category vocabulary, with MANUAL as the typed residue.
4. EMIT: generate the body from a template over M3 lanes.
5. PROVE: run a seeded differential against the donor, which serves only as the oracle.
6. GROW: a repeated residue shape becomes a new category, and every correction becomes a RECIPE_MEMORY row.

Any donor class library can be converged into M3JDK with the same six steps. Only the category vocabulary and the
templates change from one donor to another.

`TASK_PACKET.tsv` declares the M3-SCALE task packet for this run. `run.sh` writes the output contract:
`STATUS.tsv`, `RUN_CONTEXT.tsv`, `PROVENANCE.tsv`, `VERIFY_CONTRACT.tsv`, `OUTPUT_CONTRACT.tsv` and `FINAL_REPORT.md`.

## Rerun

From the repository root, with `JAVA_HOME` set to a JDK 21:

```bash
bash m3/collections/qualification/decorator-fill-20261030/run.sh \
  ~/.m2/repository/org/apache/commons/commons-collections4/4.5.0/commons-collections4-4.5.0.jar <unpacked 4.5.0 sources dir> [work dir]
```

On Git Bash for Windows, export `MSYS_NO_PATHCONV=1` and pass Windows-style (`H:/...`) paths. The driver reads the
scaffolds from the pinned `SCAFFOLD_REV`, so reruns are idempotent. It exits 0 only when every rule check passes, every
target and probe shows no mismatch, and every negative control is caught. Generator corrections are appended to
`RECIPE_MEMORY.tsv` in the same commit as the fix.
