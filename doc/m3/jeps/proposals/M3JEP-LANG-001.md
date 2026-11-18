<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JEP-LANG-001: Operators

| Field | Value |
| --- | --- |
| Proposal | `M3JEP-LANG-001` (Synexia internal, **not** an OpenJDK JEP number) |
| State | **Draft / arithmetic, sequence, set and explicit-policy map named-default candidates; symbolic lowering HOLD** |
| Author | Hitesh Soliwal and contributors to the Synexia Project |
| Kind | M3JDK21 opt-in feature candidate |
| Inspiration | Kotlin, Scala, C++ (ideas only; no third-party source copied) |
| Owning component | `javac_m3_frontend` |
| First receiving stage | `AST_COMPILER` |
| Candidate product library | `jdk.m3.paradigm/jdk.internal.m3.paradigm.contract` |
| Canonical catalogue ID | `LANG-001` |

## Summary

Introduce an opt-in M3JDK21 **operators** capability: `opt_in_named_plus_minus_times_div_rem`. This is a design proposal, not installed Java syntax, a completed compiler feature, or acceptance of an upstream runtime.

## Goals

- Specify the typed contract for opt in named plus minus times div rem and its precise Java 21 boundary.
- Reuse `javac_m3_frontend` and the existing Synexia source-sealed compiler/recipe owners.
- Admit lowering only after deterministic compiler facts and executable differential oracles.

## Non-goals

- Change meaning, overload resolution, class files, or observable behavior of any valid Java 21 program.
- Copy the Kotlin, Scala, C++ implementation, runtime or licensed source wholesale.
- Claim an OpenJDK JEP assignment or a public M3JDK21 target release.

## Motivation

The Kotlin, Scala, C++ ecosystem motivates the **operators** problem category. M3JDK21 seeks the useful capability through its own Java/JNI and compiler contracts rather than equating different source-language semantics.

## Description and proposed semantics

1. Admit `opt_in_named_plus_minus_times_div_rem` only in an explicitly selected M3 source mode. Reserve no new meaning in ordinary Java 21 source.
2. Resolve types and symbols using the existing compiler evidence owner, including source range, classpath/module graph and overload identity. Reject unknown or ambiguous resolution; no token/regex rewrite grants meaning.
3. The implementation begins as opt-in, typed, source-attributed compiler facts; syntax is accepted only after explicit grammar, overload and lowering oracles, not lexical substitutions.
4. Preserve the baseline safeguard: **unchanged java21 operator resolution**. Specify null, error paths, side effects, evaluation order, generics/bridges and binary descriptors before lowering.
5. Record source hashes, inferred atom/logic/structural fingerprints and a qualified versioned owner. Hash or SimHash similarity is evidence only, never equivalence.

## Independent default-method capabilities

An initial Java 21-compatible library candidate uses **existing** virtual arithmetic methods as semantic owners. It introduces no new Java operator grammar and never redefines built-in operations:

| Future M3 expression | Opt-in interface | Existing hook | Java default alias |
| --- | --- | --- | --- |
| `left + right` | `IAdditive<T>` | `add(T)` | `plus(T)` |
| `left - right` | `ISubtractive<T>` | `subtract(T)` | `minus(T)` |
| `left * right` | `IMultiplicative<T>` | `multiply(T)` | `times(T)` |
| `left / right` | `IDivisible<T>` | `divide(T)` | `div(T)` |
| `left % right` | `IRemainder<T>` | `remainder(T)` | `rem(T)` |

`IArithmeticDefaults<T>` combines only the first three, so independent operations remain available to partial implementations. Each default performs exactly one ordinary Java virtual call. Classes may override an alias using existing Java class-over-interface dispatch precedence. Division and remainder have no universal mathematical fallback, so they remain separate. No implicit conversion, boxing, mutation, checked exception, or public `java.lang.Object` extension is introduced.

The existing **final** `ComplexNumber` record supports the first three by implementing the composite interface without changing its methods, components, constructor or record-final status. Other final types can use a separate explicitly selected adapter. `Object.toAdapter(Class)` and the existing `toAdpater(Class)` alias are preserved but are **not** automatically searched for operators; adapter ambiguity must fail closed at compile time. Method names are a Java API facility; the symbolic syntax above is reserved for a later typed M3-only compiler.

## Collection and array operators: separate opt-in contracts

Collections make overloading inherently non-uniform. The compiler must distinguish **structural composition** from element arithmetic and from mutation:

| Type family | Potential M3 operator | Required explicit contract | Initial status |
| --- | --- | --- | --- |
| `List<E>`, ordered sequences | `a + b` | Ordered concatenation into a new logical composition, maintaining source order and duplicates | Java 21 named-default candidate; syntax HOLD |
| `Set<E>` | Union, intersection, difference, symmetric difference | Explicit equality policy; no implicit identity/comparator coercion | Java 21 equals-policy named-default candidate; other policies and syntax HOLD |
| `Map<K,V>` | `a + b` | Explicit key collision and key-identity policy; no inferred source comparator | Java 21 policy-required named-default candidate; implicit default and syntax HOLD |
| Primitive/offset arrays | `a + b` | Explicit concatenation view or materialization policy, bounds, aliasing, copy/JNI ownership | Design HOLD |
| Numeric vectors/matrices | `a + b` | Element-wise typed arithmetic, shape/broadcast contract and overflow/rounding policy | Design HOLD |
| Indexed containers | `a[i]` | Explicit read/write capability, index type, bounds and side-effect rules | Design HOLD |

Candidate M3 set symbols are `|` for union, `&` for intersection, `-` for difference, and `^` for symmetric difference; these are not implemented or assigned to ordinary Java 21 sets. For an M3 list, concatenation should use the existing offset/range/segment composition substrate when possible; parent-owned dynamic arrays, bit masks, lazy materialization and memory-pressure residency must not change what callers observe. A mutable `add` operation cannot silently implement a pure `+` expression. A `Set` must not inherit `List` concatenation semantics by accident. An array `+` must not change Java's built-in numeric or string-concatenation behavior. Generic erasure, covariant arrays, nullable elements, comparator identity, iterator invalidation, concurrent modifications, and serialization are mandatory differential test cases.

### Primitive-long collection library candidate (2026-11-18)

The concrete opt-in Java 21 named-method candidate extends the existing
`jdk.internal.m3.collections.LongCompositeViews` product owner, leaving the
implemented `LongCollection` and `LongSortedSet` interfaces unchanged:

| Java 21 named call | Explicit capability | Defined result | Future M3 symbol (HOLD) |
| --- | --- | --- | --- |
| `asSequenceOperator(a).plus(b)` | `SequenceOperator` | Live, read-only ordered concatenation **with duplicates** | `a + b` |
| `asSetOperator(a).or(b)` | `SetOperator` | Live, read-only sorted union | `a \| b` |
| `asSetOperator(a).and(b)` | `SetOperator` | Live, read-only sorted intersection | `a & b` |
| `asSetOperator(a).minus(b)` | `SetOperator` | Live, read-only sorted left difference | `a - b` |
| `asSetOperator(a).xor(b)` | `SetOperator` | Live, read-only sorted symmetric difference | `a ^ b` |

The two separate functional interfaces supply default methods; explicit adapters
retain the original primitive backing. Each default delegates to existing
composition operations without copying primitive elements or allocating
per-element entry/node objects. A `LongSortedSet` can deliberately be adapted as
a sequence, in which case `plus` preserves duplicates rather than performing
union. No collection type inherits a conflicting operator meaning.

The M3JDK21 candidate compiles on Java 21 and passes 827 executable checks:
live changes, signed-long extremes, read-only behavior, null rejection, default
method overrides and 200 randomized differential set trials against
`java.util.TreeSet`. The Synexia recipe
`com.synexia.m3.CollectionOperatorDefaults20261118` pins the existing product
preimage and exact Java output hashes; replay is fixed-point and drift/duplicate
inputs fail closed. These are candidate-level checks only, not full JDK
source-build, installed-image or cross-platform admission. The ordered gate is
still `String → arrays → collections → AST/compiler → remaining`; symbolic
lowering and promotion remain on HOLD.

### Generic list named-default candidate (2026-11-18)

The additive `jdk.m3.collections.SequenceOperatorViews` class makes the
generic case explicit without modifying `java.util.List`, existing M3JDK
collections, or their binary method descriptors:

```java
List<String> view = SequenceOperatorViews
    .asSequenceOperator(left)
    .plus(right);
```

`SequenceOperator<E>` is a functional capability interface with an overridable
default `plus(List<? extends E>)`. The result retains an immutable array of
**source-list references**, not copied elements, and is a live, read-only
ordered composition. Nested concatenations flatten descriptors, with `get`
using the current lengths as offsets. Duplicate and null elements retain their
positions. The view implements `RandomAccess` only when all source segments do;
its iterator delegates each backing iterator, and its spliterator acquires
them lazily. Backing concurrency and mutation retain their source-dependent
fail-fast guarantees; no stronger global atomic snapshot is promised.

This generic-list default is not an implicit overload for all `List` classes,
does not silently mutate either operand and does not compete with primitive
set algebra. The Java 21 source compiler accepts only named calls; symbolic
syntax requires a separately qualified attributed M3 compiler gate. This is
a source-compiled and executable candidate, **not** full M3JDK receiving
or JDK compatibility admission. The same ordered String/array/collection
gate remains effective.

### Generic set named-default candidate with explicit equality (2026-11-18)

`jdk.m3.collections.SetOperatorViews` adds a distinct opt-in Java 21 interface,
not an overload of `List<E>` or a change to `java.util.Set`:

```java
Set<String> union = SetOperatorViews.asEqualsOperator(left).or(right);
Set<String> intersection = SetOperatorViews.asEqualsOperator(left).and(right);
Set<String> difference = SetOperatorViews.asEqualsOperator(left).minus(right);
Set<String> xor = SetOperatorViews.asEqualsOperator(left).xor(right);
```

`SetOperator<E>` has a `source()` hook and four overridable default methods.
The explicit `asEqualsOperator` selection means equality is **always**
`Objects.equals` plus consistent `hashCode`, regardless of operand source
type. In particular, an `IdentityHashMap`-backed key set's distinct but
equal references intentionally collapse, while differently cased strings
from a case-insensitive `TreeSet` and an equals-based set remain distinct.
The results do **not** implement `SortedSet` or claim to preserve a source
comparator, identity membership, sorted order or comparator-defined equality.
Such alternative policies require distinct typed capabilities and proofs.

Each result retains only the source-set references and operation mode. The
current source elements are projected on each read into a **temporary**
dynamic `Object[]` slot column and two packed `long[]` membership masks;
a local hash-to-slot lookup deduplicates under Java equality, and bitwise
selection computes union/intersection/left difference/symmetric difference.
No secondary per-element nodes are retained by the view. Projection requires
expected O(n+m) time and O(n+m) temporary memory; `contains` scans source
elements, and no universal zero-copy/performance superiority is claimed.
Iteration visits the snapshot taken when the iterator is created; a
spliterator binds on first traversal and reports `ORDERED | DISTINCT` but
not `SIZED`. Later operations reflect subsequent source changes without
mutating either operand; concurrent source mutation during projection has
no stronger guarantee than the backing source provides.

The focused Java 21 self-test passes 59,315 checks, including 350 seeded
randomized `LinkedHashSet` algebra differentials, mask transitions at
63/64/65 and 127/128/129, nulls, explicit identity/comparator projections,
live mutations, read-only rejection and overridable defaults. Its Synexia
hash-pinned Java LST and JEP recipes are independent of target-side recipe
execution. This is a candidate, **not** an installed JDK/runtime qualification;
symbolic `|`, `&`, `-`, `^` remain unavailable in ordinary Java 21.

### Generic map named-default candidate with mandatory collision policy (2026-11-18)

An independent additive `jdk.m3.collections.MapOperatorViews` owner defines:

```java
Map<K, V> result = MapOperatorViews.asEqualsOperator(left)
    .plus(right, MapOperatorViews.CollisionPolicy.KEEP_RIGHT);
```

The `MapOperator<K,V>` interface has an overridable default `plus`, **but no
policy-free `plus` method**. One of `REJECT`, `KEEP_LEFT` or `KEEP_RIGHT` must
be supplied. Cross-source equal key collisions are rejected on touch, retain
the left value, or take the right value respectively; the *left key object*
is retained for either keep policy. New right keys follow unique left keys
in the projected iteration order. The result is Java-`equals`/`hashCode` keyed,
not identity- or comparator-keyed; it does not claim `SortedMap` or source
comparator ordering. Input maps containing distinct keys that are `equals`
under the selected Java policy are **refused**, even with KEEP policies, to
avoid silently collapsing internal source entries. Null keys and values
remain valid in the resulting logical map.

The backing maps are kept by reference without copying entries at result
construction. Each read uses a temporary, dynamically grown `Object[]`
key/value column pair, a hash-to-slot lookup, and two `long[]` packed
source-origin bitmaps. Only the current on-demand iterator projection exposes
fresh immutable `Map.Entry` objects to the Java API. Map and entry-set
mutators, including `compute`, `merge`, `replace` and default conditional
operations, always reject writes. The view reflects later source edits on
subsequent reads; a previously created entry iterator is a snapshot, and
entry-set spliterators bind at first traversal. No atomic cross-map state,
serialization compatibility, enhanced concurrency or speedup is claimed.
Projection has expected O(n+m) time and O(n+m) temporary space, subject to
hash-collision worst cases and source-entry iteration costs.

The focused Java 21 oracle passes 182,118 checks, including 350 seeded
randomized map pairs for KEEP_LEFT/KEEP_RIGHT and explicit REJECT collisions,
source duplicate-equality rejection, nullable keys/values, key-object
identity, 63/64/65 packed-bit boundaries, read-only default mutators,
live views, snapshot traversal and overridable named default dispatch.
This is an additive named Java 21 library **candidate**; ordinary Java 21
still has no map `+` operator and the M3 compiler's symbolic lowering remains
HOLD under the ordered receiving gates.

Future symbols are enabled only by explicit M3 language mode and an attributed, unambiguous capability or adapter match. A `List`/matrix ambiguity, `Map` collision without a policy, absent capability, or uncertain effect contract is a compile-time diagnostic—not a guessed overload. Ordinary Java 21 collection code remains byte-for-byte and behaviorally compatible.

## Compatibility

The baseline is Java 21, including source and binary compatibility, verifier/linkage/reflection, JNI and GC, serialization, exception order and public API identity. The proposal is an *additive superset*, not a source-level reinterpretation.

## Risks and failure cases

- Mandatory differential oracle: **typed dispatch overflow null and effect order**.
- Separate obligations: **JDK21 source, binary and observable effect compatibility**.
- Unsupported mapping must fail closed with an attributed diagnostic, not silently select another operator, method or representation.
- Source-first JDK sequencing is still `String → arrays → collections → AST/compiler → remaining`; the library candidate cannot authorize premature symbolic compiler promotion.

## Alternatives

Keep the present Java 21 API/library expression unchanged; provide explicit helper methods where possible; or defer M3 syntax until deterministic contracts exist. Do not adopt a foreign-language parser or evaluator without a pinned rights and behavior review.

## Testing

1. Differential compile/execute existing Java 21 examples and old binaries, including ambiguous overloads, fake code in strings/comments, generic erasure and exception ordering.
2. Add simple, boundary, hostile and pathological tests for **typed dispatch overflow null and effect order**; retain exact source/output hashes and minimal failure counterexamples.
3. Execute source-sealed Maven/OpenRewrite recipe, exact emitted output comparison, stale/duplicate refusal, fixed point and repeated deterministic schedules.
4. At product admission: run JDK `javac --release 21` baseline, relevant jtreg, JNI/GC/native/runtime and installed-image gates; record actual resource measurements where applicable.
5. Exercise partial-capability adapters, final-record client binaries, class overrides, NaN/signed zero, division-by-zero, interpreter/JIT parity, and every proposed collection conflict case before enabling syntax.

## Dependencies and delivery

- Prerequisite: `STRING_ARRAY_COLLECTION_STAGE_ORDER`; receiving order: String → arrays → collections → AST/compiler → remaining.
- Named-method prototype: source-sealed `com.synexia.m3.OperatorDefaults20261117` Java LST recipe, eight candidate Java/JTReg files, and a precompiled legacy client differential oracle. This does **not** constitute a full source-built JDK admission.
- Mapping review: candidate types use an already-owned `jdk.m3.paradigm` internal package and have no source-owner relocation; defer `m3/docs/name-mapping.json` migration records pending the normal explicit name/package review. Do not assert any new public name without that gate.
- Native/legal donor review: **NOT_RUN**. Compiler lowering, source-built JDK, actual jtreg, JNI/GC/native, collection semantics, and full public API gates: **HOLD**.
- Source catalogue: [`LANG-001`](../PROPOSALS.tsv); do not mutate this historical fact row to announce implementation.
