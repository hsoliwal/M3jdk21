<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JEP-LANG-001: Operators

| Field | Value |
| --- | --- |
| Proposal | `M3JEP-LANG-001` (Synexia internal, **not** an OpenJDK JEP number) |
| State | **Draft / arithmetic named defaults available; primitive-long collection named-default candidate; symbolic lowering HOLD** |
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
| `List<E>`, ordered sequences | `a + b` | Ordered concatenation into a new logical composition, maintaining source order and duplicates | Design HOLD |
| `Set<E>` | Union, intersection, difference, symmetric difference | Typed set algebra; preserve equality, iteration/comparator contracts | Design HOLD |
| `Map<K,V>` | `a + b` | Explicit key collision policy and retention of map ordering/comparator semantics | No implicit default |
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
