<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JEP-CORE-010: Groovy Capability Convergence — Detailed Design Annex

| Field | Value |
| --- | --- |
| Proposal relationship | Detailed annex to `M3JEP-CORE-010` and companion to `M3JEP-GROOVY-001`; no new capability ordinal |
| State | **Draft / implementation HOLD** |
| Canonical owner | Synexia recipes, inventory and qualification evidence |
| Compiler evidence owner | Existing `Maven/m2e/M3CompilationEvidence` and qualified frontend SPIs |
| Intended receiving document | `M3jdk21/doc/m3-guide/m3jep-groovy.md` |
| Product receiving order | String → arrays → collections → AST/compiler → remaining |
| Baseline | Java 21 source, binary, JVM, JNI and observable behavior compatibility |
| Evidence date | 18 November 2026 |

## 1. Purpose and status

This annex proposes convergence of useful Groovy capabilities into the existing Java, compiler and build owners in M3JDK21. The immediate opportunity is the work programmers repeatedly implement around collections, arrays, strings, files, processes, declarative construction and generated boilerplate. Groovy supplies concrete requirements and examples; every admitted capability still needs a precise M3 contract and executable evidence.

This document expands [CORE-010: Pluggable compiler/interpreter/transpiler/atomizer/patternizer][core010]. It does not rename that umbrella, create another registry entry, enable a frontend, or announce shipped APIs. Existing product gates remain **HOLD**. No implementation tests, source-built JDK qualification or installed-image validation are claimed by this annex.

The subsequently registered [M3JEP-GROOVY-001 donor profile][groovy001] owns the cross-cutting Groovy inventory and its proposed opt-in dynamic-dispatch contract. This annex supplies detailed library, transformation and compatibility requirements under the existing feature authorities. The profile and annex are complementary; neither creates a second implementation owner.

This annex contains original design text and source references; it imports no donor implementation.

The proposal does not depend on a popularity judgment about Groovy or Kotlin. Their designs can contribute independently. Existing Java 21 programs retain their meaning. Additional M3 syntax or compiler capabilities remain eligible even when stock Java 21 cannot express them, provided they are explicitly selected and pass their own specification and compatibility gates.

## 2. Existing ownership and inspected source boundary

The original feature relationships below are anchored to M3jdk21 commit `ef590b0a79df0cf857f1493570b23584353b72c2`. GROOVY-001 was subsequently read at `13065e59d15dc773399969ddd68fda740550699d`; this revision connects the existing authorities.

| Existing authority | Responsibility in this annex |
| --- | --- |
| [GROOVY-001][groovy001] | Cross-cutting donor inventory and separately specified dynamic-dispatch profile |
| [CORE-010][core010] | Frontend adapters, typed attribution, stable compiler evidence and reproducible semantic passes |
| [LANG-009: Extensions][lang009] | Lexical extension imports, lookup, ambiguity and binary-name contracts |
| [LANG-026: DSL][lang026] | Typed builder scopes and receiver-shadowing checks |
| [LANG-020: Macros][lang020] | Bounded typed expansion, hygiene, cycles and source maps |
| [LANG-023: Strings][lang023] | Typed templates, precomputation, canonical string storage and Java string boundaries |

The existing [extension-members annex][extensions-annex] demonstrates the companion-document pattern. These documents remain the authorities for their features; this annex adds Groovy-derived requirements without replacing their acceptance criteria.

The reviewed Synexia snapshot `3eab8408cdc9a02076c6a9ca854b5e21fcecdb36` includes [M3ProcessPipeline][process-pipeline], [NativeProcessPipeline][native-pipeline] and [NativeProcessBridge][native-bridge]. These are inventory anchors for consolidation. Their existence does not prove complete process supervision, named-pipe support or target qualification. Existing convergence package names are not approval to create equivalent public product owners.

Before implementation, inventory the current branch, relevant PRs, generated outputs and source history again. Pin the actual donor revisions, dependencies and selected behavior fixtures. Official documentation describes candidate capabilities; it is not a substitute for reading the precise source to be adapted.

## 3. Scope and design rules

The proposed work has four separately tracked forms:

| Form | Proposed result | Admission boundary |
| --- | --- | --- |
| Java APIs | Useful operations in existing JDK owners | Exact public contracts and Java/Groovy compatibility evidence |
| Deterministic transformations | Attributed recipes and inspectable generated Java | Source sealing, fixed points, effect preservation and explicit unsupported cases |
| Opt-in M3 language features | Separately specified syntax and compiler lowering | Grammar, overload, diagnostics and Java 21 compatibility oracles |
| Optional Groovy frontend | An inventory and bounded compatibility proposal | Explicit language/version/runtime scope; full Groovy execution is separate |

Reuse `java.util.Arrays`, appropriate collection classes, `java.util.stream`, `java.util.function`, `java.io`, `java.nio.file`, `java.nio.channels`, `Process` and `ProcessBuilder` where ownership already exists. Exact placement and signatures require review. Do not create a parallel public `M3*` or `jdk.m3.*` API merely to host an operation owned by a JDK type.

Canonical recipes and reusable convergence work remain in Synexia; M3JDK21 receives qualified outputs. Shipped functionality must not acquire a mandatory Groovy runtime or an unreviewed transitive dependency. Adapted implementations should use compact arrays, offsets, masks and bounded state where those representations preserve the contract. A representation change does not authorize altered identity, ordering, callback counts or exception timing.

## 4. GDK, arrays, collections and streams

The [Groovy Development Kit][gdk] and [Iterable enhancements][iterable] provide a useful operation inventory. The initial candidate families are transformation, selection, grouping, counting, folding, windows, indexing, slicing and explicit copy/view operations.

| Donor vocabulary | Proposed semantic family | Required decisions |
| --- | --- | --- |
| `collect`, `findAll` | Map and filter | Encounter order, null policy, callback evaluation and result ownership |
| `findResult`, `findResults` | Transform and select results | Short-circuiting, null as absence, and representation of a present null |
| `groupBy`, `countBy` | Keyed aggregation | Key equality, order, accumulation budget and overflow |
| `inject` | Fold/reduce | Empty input, initial value, associativity and parallel merge order |
| `collate` | Windows and chunks | Size, step, partial final window and buffer lifetime |
| Indexed traversal and slicing | Index-aware iteration and ranges | Index width, bounds, negative-index policy and aliasing |

This is an inventory mapping, not a commitment to import every donor name. Groovy `collect` commonly denotes transformation, whereas Java `Stream.collect` already has a reduction contract. Prefer a coherent vocabulary and one implementation of each operation.

Primitive arrays need explicit specializations where boxing would defeat the operation's purpose. Object arrays retain component-type checks and observable store failures. Views must declare whether mutations are visible and whether the underlying storage can move; copied results must remain independent when promised. Lazy windows cannot reuse a mutable buffer behind results that callers are entitled to retain.

Grouping, flattening and Cartesian expansion can accumulate substantial state. Specify limits and the spill or refusal policy before optimizing. A lazy stream may defer callbacks and exceptions; it is not an automatic replacement for an eager collection operation. Stable ordering should be required only where the contract requires it, while preserving all comparison information needed for a later ordered result.

Recipes should first identify equivalent custom loops and helpers using typed evidence. They may replace only qualified shapes. Unresolved aliasing, side effects or callback ordering leaves the original code intact with an attributed reason.

## 5. File, stream and process convenience

The proposed I/O contribution is a consistent set of scoped traversal and consumption operations: explicit encodings, binary versus text intent, ownership, cancellation and bounded accumulation. Reuse existing file and channel owners. A convenience method must state whether it closes a caller-supplied resource and how it preserves a primary exception when cleanup also fails.

Groovy's [ProcessGroovyMethods][process-gdk] offers process piping and output-draining helpers. Its documentation distinguishes process exit from completion of output consumption. That distinction should become explicit in proposed M3 supervision: launch, input closure, exit, output drain, timeout and cleanup are separate lifecycle events.

[Java 21 ProcessBuilder][process-builder] already provides `startPipeline`. The proposed value is supervision, predictable error propagation, bounded output and integration with existing process implementations. Commands should retain structured arguments and explicit shell intent; joining arguments into an ambiguous command string is not a valid migration.

Windows named-pipe creation, listening, connection and native asynchronous I/O belong to separate OS infrastructure work. Groovy process helpers do not establish that support. This annex may contribute ergonomic requirements, but does not conflate a subprocess's standard streams with a named local IPC endpoint.

## 6. Extension methods, dispatch and binary compatibility

Groovy [extension modules][metaprogramming] use static helpers with a receiver parameter. They can participate in type checking and static compilation. Runtime categories follow different resolution rules. LANG-009 should assess the static extension model together with Kotlin's [extension design][kotlin-extensions], while retaining its existing lexical-import and ambiguity requirements.

The implementation inventory must distinguish extension-facing contracts from helper implementation classes. In particular, `ProcessGroovyMethods` documents that its Java-level static methods have weaker compatibility expectations than the Groovy-facing behavior. Do not bind the product to those internal helper signatures as though they were permanent donor APIs.

Adding a real method to a Java receiver can affect existing Groovy dispatch, overload selection or extension behavior. Therefore an additive Java method requires both Java compatibility tests and representative Groovy source/binary tests. An apparent name match is insufficient: inspect erased signatures, inheritance, bridge methods, static versus dynamic compilation, and extension registration.

Proposed M3 extension lookup must preserve normal Java lookup in Java 21 mode. In M3 mode, activation, precedence and null-receiver applicability must be specified. Ambiguity produces a diagnostic. A different classpath must not silently redirect a previously qualified call. Preserve a source map from the written call to its selected target and emitted descriptor.

## 7. Closures and declarative builders

Groovy [closures][closures] include composition and partial application, but also owner/delegate relationships and configurable resolution. Java functional interfaces can supply many useful callable operations without reproducing the full closure object model.

Each proposed adapter must state its capture, identity, exception and invocation rules. A closure that mutates captured state or changes its delegate is not automatically equivalent to a Java lambda. Currying must specify which arguments are captured, when validation occurs and whether argument objects remain live references. Memoization additionally needs explicit cache bounds, key equality, lifetime and failure policy.

[Groovy builders][dsl], including SwingBuilder, motivate typed declarations of object trees, properties, actions and bindings. LANG-026 should receive those requirements together with Kotlin's [builder receiver-scope controls][kotlin-builders]. A proposed builder should identify the active receiver and reject ambiguous outer-scope resolution.

For SWT, Nebula and Swing, declarative construction should produce an inspectable model that adapters materialize under the toolkit's threading and lifecycle rules. Background preparation does not authorize background widget mutation. Bindings must define ownership, listener removal, update ordering and disposal. Build DSLs similarly need declared inputs and effects so their outputs can participate in M3File and Maven invalidation.

## 8. Compile-time transformations and recipes

Groovy AST transformations demonstrate how generated members can become visible to ordinary Java callers. Candidate categories include builders, delegation, constructors, bean notifications, logging, null checks and selected immutable-value conveniences. LANG-020 owns bounded expansion and the compiler boundary.

The proposed implementation begins with deterministic recipes that emit reviewable Java or companion classes. Standard [annotation-processing file generation][filer] and compiler AST mutation are different mechanisms; preserve the Java processing contract rather than hiding a compiler change inside an ordinary processor.

Every recipe should record source identity, resolved symbols, selected contract, transformation version and output identity. Apply serial deterministic passes per file; escalate to package or module scope only when the transformation crosses that boundary. The second application must reach a fixed point. Stale preimages, missing attribution and duplicate installation must be refused.

`@Immutable`, `@Lazy`, `@Memoized` and tail-recursion elimination require separate proofs. Final fields do not guarantee deep immutability. Lazy initialization changes publication and failure timing. Caching changes behavior for impure computations. Eliminating frames can affect observable stack traces. These are explicit feature choices, not blanket cleanup passes.

Groovy's [type-checking documentation][semantics] also distinguishes `@TypeChecked` from `@CompileStatic`: checking dynamic code and changing call binding are separate operations. Ordinary Java already has static typing. M3 should absorb useful compiler facts and transformations without introducing a meaningless static-compilation marker or assuming dynamic and static Groovy have identical semantics.

## 9. Strings, null behavior and numeric contracts

LANG-023 owns template and precomputation requirements. Groovy's [GString documentation][syntax] distinguishes interpolated values from immutable Java strings, including differing hash behavior. The proposed M3 boundary must distinguish a template plan, captured values, deferred suppliers and materialized immutable text.

Only stable materialized content may enter canonical string identity or an interned lexical image. A deferred supplier remains executable state. Define when interpolation runs, how often values are evaluated, which exceptions escape and whether captured mutable objects are observed again. Typed rendering must select its output context; string interpolation alone does not define SQL parameterization, markup escaping or process-argument construction.

Groovy [Elvis and spread operators][operators] also need deliberate treatment. Groovy Elvis tests Groovy truth, while [Kotlin Elvis][kotlin-null] selects a fallback only for null. M3 must specify those as distinct contracts. Safe navigation must evaluate its receiver once and preserve the intended short-circuit behavior. Spread operations need an explicit null-element and null-receiver policy; filtering nulls would change results when null positions are significant.

Groovy numeric syntax uses decimal and division rules that differ from Java. No recipe may translate an expression merely because its tokens resemble Java. Preserve operand types, overflow, rounding, scale and exception order. Existing Java arithmetic keeps Java semantics. Optional decimal-oriented syntax or operators need their own typed lowering and boundary fixtures.

## 10. Opt-in syntax and optional frontend inventory

Safe navigation, extension-member notation, ranges, spread operations, typed builder scopes and template conveniences remain possible M3 candidates. Their absence from stock Java 21 is not a rejection criterion. Each candidate needs a grammar, type rules, overload behavior, source maps and a demonstrated lowering. Already-valid Java 21 source must keep its original parse and meaning.

CORE-010 can also inventory an optional Groovy frontend: exact language versions, parser/AST interfaces, source attribution, static versus dynamic modes, compiler plugins and runtime dependencies. A bounded adapter for selected typed source facts is different from complete Groovy execution. Neither a successful parse nor compilation of a small fixture proves support for scripts, metaclasses, categories, dynamic properties, runtime evaluation or ecosystem tools.

The [Groovy 6 release notes][groovy6] describe an incubating concurrency toolkit and a `groovy-concurrent-java` module. Record this as a future inventory candidate. Before any admission, inspect pinned source, dependency closure, Java baseline, class ownership, cancellation, scheduling and module coexistence. Published Java-callable APIs and a stated absence of the full Groovy runtime do not establish suitability for M3JDK's zero-runtime-dependency or compatibility requirements.

## 11. Implementation sequence

1. **Freeze the inventory.** Identify current owners, open work, donor revisions, public contracts and overlapping custom implementations. Record unknowns without treating them as absent functionality.
2. **Specify behavioral rows.** For each candidate, define baseline, proposed API or syntax, receiving owner, observable effects, resource limits and rejection conditions.
3. **Build isolated evidence.** Develop source-sealed recipes and differential fixtures in Synexia. Respect the established String → arrays → collections → AST/compiler → remaining product order; preparatory evidence does not promote a later stage.
4. **Materialize qualified Java operations.** Prefer the existing owner and preserve wrappers only where an API demands them. Include generated changes and their qualification evidence together.
5. **Qualify compiler candidates separately.** Exercise parser, attribution, overload and lowering behavior under explicit M3 selection. Keep unsupported cases uninstalled.
6. **Run target admission gates.** Execute the relevant source-built JDK, jtreg, Maven/OpenRewrite, runtime, installed-image and CI checks. A missing gate remains `NOT_RUN`; a failed gate retains `HOLD`.

## 12. Acceptance matrix

The following checks are proposed requirements, not results reported by this annex.

| Area | Meaningful acceptance evidence | Refusal or HOLD condition |
| --- | --- | --- |
| Collections and arrays | Differential empty/null/duplicate/boundary cases; mutation and alias probes; callback order/count assertions | Changed result ownership, effects or component-type failures |
| Streams and windows | Short-circuit and close probes; retained-window tests; bounded-memory measurement | Hidden eager traversal, retained-buffer corruption or unbounded growth |
| Extension lookup | Java 21 source and old binaries; representative Groovy modes; overload/inheritance/classpath cases | New ambiguity or redirected existing call |
| Closures and builders | Capture mutation, delegate/receiver resolution, listener disposal and exception-order fixtures | Unspecified dynamic behavior or thread-affinity violation |
| Strings and numbers | Deferred interpolation, hash-key boundaries, surrogate/encoding and rounding/overflow fixtures | Mutable text treated as stable identity or altered Java arithmetic |
| Recipes | Exact preimage/output hashes, repeat runs, fixed point, stale/duplicate refusal and minimal negative cases | Missing attribution, nondeterministic output or unsupported rewrite |
| Compiler integration | Grammar negatives, source maps, diagnostics, hygiene/cycles and unchanged annotation processing | Token substitution substitutes for symbol resolution |
| Process/I/O | Concurrent output pressure, completion/drain ordering, cancellation and cleanup failures | Deadlock, lost diagnostics, leaked resources or ambiguous ownership |
| Native receiving work | Relevant OS and architecture execution; JNI and failure-path checks | Simulation presented as native qualification |
| Product readiness | Source-built target, relevant jtreg, installed-image and dependency checks | Focused tests substituted for required target gates |

Performance measurements follow correctness acceptance and must report workload, data distribution, warmup, allocations, retained memory and platform. Reusing arrays or moving work into native code is not itself evidence of a speedup. Broader testing should resolve a concrete remaining risk or a required gate.

## 13. Decisions still required

The initial public API signatures, complete donor inventory, version compatibility set, exact compiler hooks and first receiving change are unresolved. Existing source anchors establish where investigation starts, not that the inspected commits remain the branch heads or that adjacent work has merged.

This annex therefore records a concrete direction: consolidate Groovy-derived capabilities through existing JDK owners, retain deterministic recipe evidence, and admit additional M3 language features through their existing proposal authorities. Implementation HOLD ends only for individually qualified changes; it does not disappear because the design document has been published.

## Sources and proposal links

Repository links identify the reviewed snapshots. External links identify official documentation consulted for this assessment; their moving `latest` URLs are not donor revision pins. A later implementation must record immutable source revisions and its selected behavioral baseline before qualification.

[core010]: https://github.com/hsoliwal/M3jdk21/blob/ef590b0a79df0cf857f1493570b23584353b72c2/doc/m3/jeps/proposals/M3JEP-CORE-010.md
[lang009]: https://github.com/hsoliwal/M3jdk21/blob/ef590b0a79df0cf857f1493570b23584353b72c2/doc/m3/jeps/proposals/M3JEP-LANG-009.md
[lang026]: https://github.com/hsoliwal/M3jdk21/blob/ef590b0a79df0cf857f1493570b23584353b72c2/doc/m3/jeps/proposals/M3JEP-LANG-026.md
[lang020]: https://github.com/hsoliwal/M3jdk21/blob/ef590b0a79df0cf857f1493570b23584353b72c2/doc/m3/jeps/proposals/M3JEP-LANG-020.md
[lang023]: https://github.com/hsoliwal/M3jdk21/blob/ef590b0a79df0cf857f1493570b23584353b72c2/doc/m3/jeps/proposals/M3JEP-LANG-023.md
[extensions-annex]: https://github.com/hsoliwal/M3jdk21/blob/ef590b0a79df0cf857f1493570b23584353b72c2/doc/m3-guide/m3jep-extension-members.md
[process-pipeline]: https://github.com/hsoliwal/com.synexia/blob/3eab8408cdc9a02076c6a9ca854b5e21fcecdb36/synexia-unix4j-m3/src/main/java/jdk/m3/io/M3ProcessPipeline.java
[native-pipeline]: https://github.com/hsoliwal/com.synexia/blob/3eab8408cdc9a02076c6a9ca854b5e21fcecdb36/synexia-unix4j-m3/src/main/java/com/synexia/unix4jm3/os/NativeProcessPipeline.java
[native-bridge]: https://github.com/hsoliwal/com.synexia/blob/3eab8408cdc9a02076c6a9ca854b5e21fcecdb36/synexia-native-interop/src/main/java/com/synexia/nativeinterop/NativeProcessBridge.java
[gdk]: https://docs.groovy-lang.org/latest/html/documentation/core-gdk.html
[iterable]: https://docs.groovy-lang.org/latest/html/groovy-jdk/java/lang/Iterable.html
[process-gdk]: https://docs.groovy-lang.org/latest/html/gapi/org/codehaus/groovy/runtime/ProcessGroovyMethods.html
[process-builder]: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ProcessBuilder.html
[metaprogramming]: https://docs.groovy-lang.org/latest/html/documentation/core-metaprogramming.html
[closures]: https://docs.groovy-lang.org/latest/html/documentation/core-closures.html
[dsl]: https://docs.groovy-lang.org/latest/html/documentation/core-domain-specific-languages.html
[semantics]: https://docs.groovy-lang.org/latest/html/documentation/core-semantics.html
[syntax]: https://docs.groovy-lang.org/latest/html/documentation/core-syntax.html
[operators]: https://docs.groovy-lang.org/latest/html/documentation/core-operators.html
[groovy6]: https://groovy-lang.org/releasenotes/groovy-6.0.html
[kotlin-extensions]: https://kotlinlang.org/docs/extensions.html
[kotlin-builders]: https://kotlinlang.org/docs/type-safe-builders.html
[kotlin-null]: https://kotlinlang.org/docs/null-safety.html
[filer]: https://docs.oracle.com/en/java/javase/21/docs/api/java.compiler/javax/annotation/processing/Filer.html
[groovy001]: https://github.com/hsoliwal/M3jdk21/blob/13065e59d15dc773399969ddd68fda740550699d/doc/m3/jeps/proposals/M3JEP-GROOVY-001.md
