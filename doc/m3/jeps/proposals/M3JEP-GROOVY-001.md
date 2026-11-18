<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JEP-GROOVY-001: Groovy Language Donor and Dynamic Dispatch Profile

| Field | Value |
| --- | --- |
| Proposal | `M3JEP-GROOVY-001` (internal M3JDK21 proposal, **not** an OpenJDK JEP) |
| Status | **Draft / implementation HOLD** |
| Author | Hitesh Soliwal and contributors to the Synexia Project |
| Kind | Cross-cutting language-donor profile, not a replacement for existing capability IDs |
| Inspiration | Apache Groovy, with Kotlin as a distinct comparative donor; ideas only |
| Product authority | M3JDK21 Java/JNI/HotSpot, original JDK owners and opt-in compiler mode |
| Recipe and evidence authority | `hsoliwal/com.synexia` |
| First product receiving stage | `AST_COMPILER` after String, arrays and collections gates |

## Summary

Evaluate and progressively admit Groovy's useful **dynamic method and property
dispatch**, explicit runtime metaobject protocol (MOP), closures and delegation,
source-attributed AST transformations, statically compilable DSLs and GDK-style
library conveniences into M3JDK21 as **individually opt-in capabilities**.
Preserve the observable meaning of every valid Java 21 program and every
previously accepted M3 capability. This is not a claim to implement the Groovy
language, its compiler, its runtime ABI, or every Groovy DSL in M3JDK21.

This is a *donor profile*, so the historical 70 `LANG` and 16 `CORE` proposals
retain their stable IDs, statuses and generated manifests. Implementable
capabilities belong to their existing product owner; the new contract unique to
this profile is **bounded, scope-aware dynamic dispatch and invalidation**.

## Motivation: Groovy and Kotlin coexist

Kotlin's statically resolved extensions and type-safe builders address many
tasks previously done with Groovy scripts. Gradle now recommends Kotlin DSL for
new builds and uses it as the default for most newly initialized builds.
Nevertheless, Gradle continues supporting Groovy DSL and Jenkins Scripted
Pipeline remains Groovy-based. Apache Groovy 6.0 was released in September 2026.
The opportunity is to retain **dynamic customization and scripts** where they
offer real value, while preferring static compiler facts and direct calls where
their behavior can actually be proved equivalent.

## Feature inventory and existing M3JEP owners

| Groovy donor capability | Existing M3JEP authority | Receiving disposition |
| --- | --- | --- |
| GDK String methods, interpolation and text helpers | `LANG-023`, `LANG-070`, `CORE-006` | Inventory signatures and semantics under the canonical String owner; avoid copies |
| GDK primitive arrays, ranges and slicing | `CORE-007`, `LANG-034`, `LANG-043`, `LANG-058` | Compose offset/range views; preserve mutation and alias semantics |
| GDK collections, `collect`, `findAll`, `groupBy`, spread and operator views | `LANG-017`, `LANG-033`, `LANG-040`, `LANG-001` | Reuse array/offset/mask owners and typed defaults; test order, equality and mutability |
| Closures, owner/delegate/this, delegation strategies, command-chain DSL syntax | `LANG-003`, `LANG-025`, `LANG-026` | Typed scope graph, deterministic lookup and source-attributed lowering |
| Extension modules, categories and method/property interception | `LANG-009`, `LANG-008`, `CORE-010` | Separate lexical extensions from opt-in dynamic lookup; no global Java dispatch change |
| `methodMissing`, `propertyMissing`, `metaClass`, `ExpandoMetaClass` | **This profile** plus `CORE-008`, `CORE-010` | Specify dynamic call-site identity, scope, invalidation and failure contracts |
| Operator methods (`plus`, `getAt`, `putAt`, `asType`, `compareTo`) | `LANG-001`, `LANG-002`, `LANG-005` | Reuse named defaults; symbolic syntax requires its own compiler proof |
| Traits, AST transforms, annotations, compile-time generated members | `LANG-013`, `LANG-020`, `CORE-009`, `CORE-010` | Phase-ordered typed generation; classfile and annotation-contract proof |
| `@TypeChecked` and `@CompileStatic` | `CORE-008`, `CORE-009`, `CORE-010` | Resolve target symbols and calls; prove or explicitly retain dynamic fallback |
| Groovy truth, coercion, safe navigation, GPath, JSON/XML/regex | `LANG-010`, `LANG-018`, `LANG-033`, `CORE-013` | Separate opt-in compatibility profiles; do not change Java equality, null, or regex |
| Groovy scripts, Gradle DSL and Jenkins Pipeline | `CORE-014`, `CORE-010`, `LANG-026` | Pinned external-integration oracle only; never assume arbitrary scripts are sandboxed |

## Proposed M3 dynamic-dispatch contract

1. **Opt-in boundary.** Only an explicitly selected M3 language profile or
   explicit dynamic API call may use the new semantics. Ordinary Java 21 method
   resolution, field access, overload selection, reflection and bytecode linkage
   remain unchanged. Do not retrofit dynamic calls into `java.lang.Object`.
2. **Call-site identity.** Bind each dynamic site to source/module identity,
   receiver class *and defining class loader*, selector and invocation kind,
   arity and named-argument layout, visible extension scope, and metaobject
   generation. A class name or structural hash alone cannot identify a runtime
   class. Null receivers, categories and static versus instance dispatch are
   distinct cases.
3. **Lookup and effects.** Specify and test exact receiver, lexical extension,
   delegate/owner, metaClass and missing-method precedence for each selected
   compatibility profile. Preserve argument evaluation order, exceptions,
   coercion, visibility checks, dispatch recursion guards and side effects.
   Ambiguous or unsupported behavior fails closed with an attributed diagnostic
   or a deliberate opt-in runtime error, not a guessed Java target.
4. **Invalidation and lifetime.** Metaobject changes, extension-scope changes,
   class redefinition and class-loader unloading invalidate affected caches.
   Keep weak or generation-scoped owner references; a cache may not pin obsolete
   loaders, leak mutable global behavior, or reuse a target after a generation
   change. Define concurrency visibility and re-entrant updates.
5. **Static fast path.** When source attribution proves a stable target and
   effect-equivalent conversion, lower to a normal typed call. Otherwise use a
   separately specified bounded `invokedynamic`/method-handle path or refuse.
   Profile-driven dispatch cannot silently differ between interpreted, JIT,
   AOT, or static compilation modes.
6. **AST transformations.** Register transforms explicitly with pinned version,
   phase, deterministic inputs and outputs, emitted member identities and source
   maps. Apply them early enough for typed compiler attribution. Never treat
   raw text substitution or regex cues as an AST transformation.
7. **Canonical data and JNI.** Reuse OS-shared immutable String atoms and existing
   offset-array/bitmap owners. Do not expose writable aliases to frozen atoms.
   Ordinary Java array mutation, JNI Get/Release semantics and GC lifetime rules
   continue to apply. The dynamic adapter cannot create a second retained
   canonical payload.

## Non-goals and important exclusions

- No claim that Kotlin has replaced Groovy everywhere; no mandatory Groovy or
  Kotlin runtime dependency in M3JDK21.
- No source-compatible Groovy compiler, unrestricted `GroovyShell`, arbitrary
  global `ExpandoMetaClass`, or binary compatibility with Groovy's `MetaClass`
  interfaces in this first proposal. Such support requires separate ABI/JNI,
  security and full-language conformance proposals.
- No changes to `java.lang.String`, Java collection APIs, operator semantics or
  class loading solely because Groovy defines a similarly named operation.
- No assumption that dynamic scripting, Gradle build code or Jenkins Pipeline
  execution is safe to run with untrusted input.
- No copying Groovy source, documentation expression or third-party code into
  OpenJDK files without an explicit license-compatible transfer decision.

## Compatibility and verification plan

1. Pin the Apache Groovy release/source revision and licenses; enumerate each
   candidate signature, language grammar form, actual caller and feature owner.
   Compare with Kotlin and existing JDK/Synexia behavior before writing code.
2. Create a source-sealed Maven/OpenRewrite recipe **in Synexia** for each
   admitted atom. Prove exact preimage/postimage SHA-256, fixed point, duplicate
   and stale-source refusal, and compile-before/after compatibility.
3. Differentially test Groovy dynamic versus `@CompileStatic` oracles:
   missing methods, dynamic properties, primitive/numeric coercions, null,
   truthiness, closures and delegate strategies, category visibility, generic
   erasure, annotations, transform phase order and source mapping.
4. Stress dynamic-cache invalidation under concurrent metaClass updates,
   recursive dispatch, class-loader churn, unload/GC, redefinition and nested
   DSL scopes. Measure throughput, call-site allocation and retained memory
   against a pinned baseline, without claiming universal speedups.
5. Test both already-compiled Java 21 clients and unchanged-source Java 21
   recompilation; retain binary descriptors, reflection, exception and
   serialization behavior. Run the target `javac --release 21`, jtreg,
   JNI/GC/native and installed-image gates before product admission.
6. Use external, version-pinned Gradle Groovy/Kotlin DSL and Jenkinsfile
   fixtures as **behavioral oracles**, not a promise of full compatibility or
   permission to execute untrusted build scripts.

## Companion design annex

The [Groovy capability convergence annex](../../../m3-guide/m3jep-groovy.md)
expands the library, process, extension, builder, transformation and language
semantics covered by this profile. It is a detailed annex under CORE-010 and
retains the existing capability IDs. The profile remains the authority for the
proposed opt-in dynamic-dispatch contract.

Document materialization evidence is tracked in canonical Synexia separately
from compiler/runtime feature admission. Publishing either document does not
clear implementation HOLD or establish Groovy source/runtime compatibility.

## Dependencies and delivery

- Receiving sequence: **String -> arrays -> collections -> AST/compiler -> remaining**.
- Existing capability IDs above remain authoritative; this profile does not
  add a 71st independent language feature or amend the 86-proposal historical
  registry. The donor-profile navigator is separate for that reason.
- Compiler/runtime design, native/legal donor review, OpenRewrite execution,
  target gates and product admission: **NOT_RUN / HOLD** until proven.
- Existing Synexia Groovy/Gradle/Kotlin M3AST adapter history must be inventoried
  before inventing another parser, AST owner or data store.

## Primary-source references (ideas, not imported code)

- Apache Groovy language and metaprogramming: https://docs.groovy-lang.org/latest/html/documentation/
- Apache Groovy 6.0 release notes: https://groovy-lang.org/releasenotes/groovy-6.0.html
- Kotlin extension functions and type-safe builders: https://kotlinlang.org/docs/extensions.html and https://kotlinlang.org/docs/type-safe-builders.html
- Gradle DSL guidance: https://docs.gradle.org/current/userguide/best_practices_general.html
- Jenkins Pipeline syntax: https://www.jenkins.io/doc/book/pipeline/syntax/
