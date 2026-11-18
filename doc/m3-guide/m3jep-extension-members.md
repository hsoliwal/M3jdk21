<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# M3JEP-LANG-009: Extension Members — Detailed Design Annex

**Canonical proposal:** [M3JEP-LANG-009 — Extensions](../m3/jeps/proposals/M3JEP-LANG-009.md). This document is a **detailed technical annex**, not a new M3JEP, registry row, independent feature authority, or official OpenJDK JEP. The canonical [86-proposal registry](../m3/jeps/PROPOSALS.tsv) remains unchanged, with LANG-009 in `DRAFT_HOLD`.

**Scope partition:** LANG-009 owns static receiver extension **lookup and binding**. Computed property expression semantics remain owned by [LANG-008 — Properties](../m3/jeps/proposals/M3JEP-LANG-008.md); extension-operator semantics by [LANG-001 — Operators](../m3/jeps/proposals/M3JEP-LANG-001.md); nullability and generic inference by [LANG-010](../m3/jeps/proposals/M3JEP-LANG-010.md) and [LANG-019](../m3/jeps/proposals/M3JEP-LANG-019.md). Delegated properties [LANG-025](../m3/jeps/proposals/M3JEP-LANG-025.md), DSL provider scopes [LANG-026](../m3/jeps/proposals/M3JEP-LANG-026.md), and protocol extensions [LANG-047](../m3/jeps/proposals/M3JEP-LANG-047.md) remain independently gated. The shared compiler/metadata owners are [CORE-008](../m3/jeps/proposals/M3JEP-CORE-008.md), [CORE-009](../m3/jeps/proposals/M3JEP-CORE-009.md), and [CORE-010](../m3/jeps/proposals/M3JEP-CORE-010.md). Cross-references **do not** grant implementation of another proposal.

| Field | Value |
| --- | --- |
| Title | Statically resolved extension members (LANG-009 integration annex) |
| Owner | M3JDK21, compiler/language feature |
| Type | Feature |
| Scope | M3JDK21 compiler bindings, shared design constraints (other JEP owners unchanged) |
| Status | **Annex candidate — no target implementation or admission** |
| Canonical ID | **M3JEP-LANG-009**, internal Draft / HOLD (not an OpenJDK JEP) |
| Source compatibility | Java 21 unchanged by default |
| Target release | Unscheduled; depends on M3JDK receiving-stage qualification |
| Source baseline | `hsoliwal/M3jdk21@11e64e2cc0924d1e0940b3fbcf29b383097eaef7` |
| Canonical recipe owner | `hsoliwal/com.synexia` |
| Experimental provenance | Synexia PRs #18291, #18301, #18309; #18336 under review when drafted |

## Summary

Add **opt-in, statically selected extension members** to M3JDK21. A compatible external provider can supply methods, computed properties, static members and a restricted set of operator implementations for an existing type without modifying its declaration, inheritance, instance layout, or existing bytecode. Ordinary Java members always retain their currently applicable binding. This is an M3JDK21 one-way language superset proposal; its syntax need not compile on stock Java 21.

## Goals

1. Preserve **all valid Java 21 source, binary, and observable runtime behavior** when the extension feature is disabled.
2. Preserve ordinary Java member selection, visibility, inference, invocation conversion, and side effects when enabled. Do not make a previously valid ordinary expression silently resolve to an extension.
3. Support extension functions, computed read/write properties without backing fields, static type-qualified extensions, generic receiver methods, null-capable receivers, and a restricted operator set.
4. Support both class-level static providers and **member providers** whose enclosing dispatch receiver can be overridden virtually, while selecting the extension receiver statically.
5. Support explicit provider visibility across source and compiled class files, deterministic ambiguities, and separate-compilation ABI verification.
6. Lower accepted members to normal JVM method invocation without reflection, wrapper objects, a runtime registry, or mutable state in the extended target.
7. Keep feature discovery and compilation reproducible, with content-hash-bound metadata caches as *accelerators*, never semantic authority.

## Non-goals

- Changing Java reference equality, `String` concatenation, primitive arithmetic, `Object.equals`, Java operators' precedence or associativity.
- Allowing an extension to acquire private/protected access to an unrelated receiver type.
- Virtual dispatch based on the runtime subtype of the **extension receiver**.
- Inserting fields into existing, final, sealed, record, primitive, array, or JDK types.
- Runtime registration, implicit global search of the classpath, source-text substitution, runtime monkey patching, or an M3JDK dependency on Synexia tooling.
- Treating a third-party index, compiler plugin, or small differential corpus as native `javac` support.

## Motivation

Existing helper functions are discoverable only at their declaring type, while fluent APIs and domain-specific languages often need receiver notation. Kotlin supports extension functions, computed properties and provider scopes; C# 14 adds extension blocks with properties and operators. Java 21 has no general syntactic extension-member lookup. M3JDK21 can support one-way cross-language capability convergence only if it defines lookup, inference, lowering, metadata and compatibility rigorously.

## Proposed source declarations (not implemented)

The first version introduces no new declaration grammar. Providers use normal Java declarations with a *proposed* compiler-visible `java.lang.annotation.Extension` annotation (CLASS retention) and nested `Kind` / `Operator` metadata. This standard-library annotation does not yet exist in the product tree. The Synexia laboratory currently uses a **different** `com.synexia.m3jdk.extensions.M3Extension`; that experimental type is not a target API.

Illustrative proposed declarations:

```java
import java.lang.annotation.Extension;

public final class TextExtensions {
    @Extension
    public static boolean isMissing(String receiver) {
        return receiver == null || receiver.isEmpty();
    }

    @Extension(kind = Extension.Kind.GETTER, name = "lastChar")
    public static char getLastChar(String receiver) {
        return receiver.charAt(receiver.length() - 1);
    }

    @Extension(kind = Extension.Kind.STATIC_METHOD, receiver = String.class)
    public static String missingLabel() {
        return "(missing)";
    }
}
```

The source consumer, with the feature explicitly enabled and the provider imported, can write `text.isMissing()`, `text.lastChar` and `String.missingLabel()`. A receiver-parameter extension uses its first parameter as the receiver; an optional receiver annotation must agree with the erased parameter. Static type extensions instead require an explicit target type and do **not** pass a receiver value.

For a member provider:

```java
class Formatter {
    @Extension
    public String quote(String receiver) { return "[" + receiver + "]"; }

    String example(String input) { return input.quote(); }
}
class OverrideFormatter extends Formatter {
    @Override public String quote(String receiver) { return "<" + receiver + ">"; }
}
```

The binding has two distinct receivers. The extension receiver `String` is selected statically; `Formatter` is a dispatch receiver, and a virtual override of `quote` may change the implementation. The annotation by itself confers no new visibility or override privileges.

## Explicit opt-in and visibility

The final target compiler must specify a named extension flag, provisionally `--enable-m3-extension-members`. The flag name and compiler parsing **are proposals**, not existing M3JDK options. Without it, Java 21 source and binary paths are unchanged. Strict stock-Java `--release 21` must not silently admit M3-only source syntax or attribute semantics; its interaction with the flag must be rejected or explicitly delegated to an M3-target compilation mode with a documented diagnostic.

Visible candidates are only those from the current package, explicitly imported provider classes, imported provider packages, and appropriate static provider-member imports, subject to ordinary module readability, exports, accessibility and lexical hiding. **No global classpath provider scan** is permitted. A local member provider is eligible only when its provider dispatch receiver is in ordinary lexical instance scope.

## Resolution contract — normative for acceptance

For `receiver.member(args)`:

1. Evaluate the syntax without executing expressions; attribute receiver and arguments under Java's established contextual typing and deferred-attribution rules.
2. Resolve all ordinary Java methods with existing `javac` rules, including inherited members, overloads, generic inference, boxing, varargs, target type, visibility, and errors. If an accessible applicable ordinary member is selected, keep it **unchanged**. Never use extensions to heal protected/private access violations, a genuine ambiguity, or an invalid `super`/static invocation.
3. Only in a precisely enumerated *missing or non-applicable member* fallback case, obtain the imported extension candidates for the receiver's **compile-time type**. Filter by declaration kind, accessibility, module, provider reachability, generic bounds and contextual receiver conversions.
4. Resolve the candidates with the same strictly phased overload rules, now considering `receiver` as the first argument for an instance extension. Perform generic inference using the original, non-erased parameter types; erased descriptors are **ABI verification only**.
5. When the best extension is unique, capture its provider symbol, method type, substitutions and source origin in the attributed tree. If no candidate is applicable, preserve Java's original meaningful diagnostic. A tied extension set produces an explicit deterministic ambiguity diagnostic and sorted candidate identities.
6. Emit a statically linked provider method invocation. No discovery at runtime. An extension imported from a different compilation unit must resolve against the actual current provider binary symbols rather than trusting cached index text.

An existing class member with the same name but *inapplicable parameters* does not automatically hide an applicable extension; conversely, an applicable ordinary method always wins. Member references, lambdas, polymorphic signatures, intersection/union receiver types, wildcard captures and qualified `super` require their own acceptance matrix before admission.

## Computed property contract

A GETTER must take its receiver (except static getters) and return non-void. A SETTER must take its receiver plus a value and return void. For a writeable property, getter/setter types must be coherently checked using Java assignment conversions. Property access never changes a receiver's fields, memory layout, serialization, reflection members or construction requirements. A field of the same accessible name retains Java precedence.

Property access is an expression, **not only a statement rewrite**. For `f().p = g()`, evaluate `f()` once, then `g()` once and call the setter once; the expression's type and value must match the converted assigned value according to Java assignment semantics. `f().p += g()`, increment/decrement, chained assignments, exception order, prefix/postfix values and lvalue capture each require specified lowering with no repeated receiver evaluation. Read-only properties reject assignment; write-only properties reject reads.

## Operator contract

Only a finite, explicitly listed operator set is admissible, and only when the corresponding built-in Java operator is **not applicable** to the operand types. The extended compiler retains Java tokenization, precedence, associativity and evaluation order. Operators cannot redefine reference `==` or `!=`, primitive operators, `String` concatenation, short-circuit `&&`/`||`, assignment, `instanceof`, casts, or implicit type conversion. Operators with mutable effects, checked exceptions or overflow preserve their ordinary declared Java behavior. Every admitted operator maps to a uniquely bound provider method and is lowered to a normal invocation.

## Nulls, generics, effects, and bytecode

A nullable reference receiver is passed as an ordinary method argument, **without an implicit null check**; a provider can handle or dereference it. A provider's primitive receiver type follows Java's ordinary conversions; no null-aware primitive extension is invented. Providers gain no private access to an unrelated target.

Every receiver, argument, dispatch receiver and RHS is evaluated exactly once and in the order Java expressions require. Conversion, varargs allocation, field access, exceptions and provider side effects must be checked against a direct-call oracle. No synthetic state is added to receiver instances. Static providers lower to `invokestatic`; a member provider may lower to the appropriate virtual/special provider invocation.

## Binary metadata and cache keys

Use a versioned compiler-visible declaration annotation and canonical, bounded, ordered optional index metadata (the Synexia prototype writes `META-INF/m3/extensions.v1.tsv`). Index rows retain provider binary name, receiver identity, kind, logical name, method name, erased descriptor, varargs and source provenance. Imported binary candidates MUST be validated against the actual class/module symbols, annotation attributes, classfile version and generic Signature. Malformed/forged/stale index rows fail closed; an optional cache must not decide overloads, visibility or generic inference.

Index identity depends on provider bytecode and metadata hashes, classpath/module path, module readability/exports, compiler version, feature flag, receiver/argument/target types, provider scope and inference context. Content equality is not proof of semantic equivalence. Preserve deterministic output across source-file ordering and worker schedules.

## Implementation architecture (existing target owners)

These are **cross-proposal implementation prerequisites**, not a transfer of LANG-001/008 semantics to LANG-009. Each sibling proposal retains its own admission and tests.

| Owner | Required change | Acceptance |
| --- | --- | --- |
| `java.base` annotation owner | Add the proposed public `Extension` declaration API without breaking existing Java 21 calls | API/javap, packaging, installed image |
| `javac` scanner/parser | Reuse Java grammar for annotated static providers and ordinary selection/property/operator expressions | token/AST round trip, source positions |
| `comp/Resolve.java` | Add opt-in, member-first, import-scoped extension resolution within existing inference phases | inheritance, overloads, generics, accessibility |
| `comp/Attr.java` | Attribute extension methods and property/operator lvalues as actual compiler symbols | expression typing and effect order |
| `comp/Lower.java` and `jvm/Gen.java` | Emit correct normal Java/JVM invocation shapes; preserve lvalue values and provider dispatch | bytecode/javap, runtime parity |
| `code/Symbol`, `ClassReader` and metadata | Read provider metadata with exact ABI and module checks; no runtime registry | separate compilation and refusal |
| `test/langtools/tools/javac/m3` | Independent positive/negative jtreg oracles; old source/binary differential suite | host baseline, matched M3JDK image |

No parallel compiler/AST pipeline, M3-only runtime registry or Synexia runtime dependency is authorized by this draft. Reuse the original compiler owners and the canonical Synexia Maven/OpenRewrite recipe system.

## Success metrics and acceptance

Every case must produce exact commit-bound logs, hashes and reproducible results. Acceptance categories:

1. **Java 21 subset**: old `.class` links and executes; unchanged Java source recompiles to equivalent behavior; opt-in disabled is bit-identical for a frozen compiler corpus where determinism permits. Negative cases preserve Java errors and never bind an extension accidentally.
2. **Member and overloads**: existing instance/static/default/inherited methods win; generic inference, boxing, varargs, lambdas and method references; conflicting imports; illegal access; module exports; classpath and module-path binaries.
3. **Provider dispatch**: compile-time receiver selection with an independently virtual enclosing provider; distinct receiver and dispatch types; overriding; `super`; nested scopes.
4. **Properties/operators**: read/write, compound write, prefix/postfix, chained assignments, single evaluation, exceptions; supported and deliberately forbidden operators.
5. **Nullable and Unicode contracts**: a null receiver can reach a provider; `String` extension examples distinguish UTF-16 units, code points and graphemes; locale-sensitive casing is explicit.
6. **Build and reproducibility**: source-sealed recipe fixed-point and stale-preimage refusal in Synexia; actual Maven/OpenRewrite SDK receipt; target source/build/lint/jtreg/VM tests in stage order; package/jmod/javap installed image and runtime on each claimed platform.
7. **Performance**: compare direct-call reference versus admitted extension call, cold/warm compiler time, CPU, allocation, retained heap/native memory, startup, and cache invalidation; no zero-overhead claim without results.

Negative/untested gates are recorded as **PENDING**, never as successful implementation. The target receiving order remains **String → arrays → collections → AST/compiler**; this document cannot advance it.

## Alternatives

- **Explicit provider calls only**: safe but does not supply receiver notation.
- **Kotlin/C# compile-time source conversion alone**: insufficient; fails Java symbol lookup, generic inference, accessor side effects and separate compilation.
- **Global extension registry or reflection**: rejected for determinism, security, modularity and runtime cost.
- **Subclassing every extended type**: impossible for final/record/primitive types and changes identity and API.
- **Default-interface methods and overloaded abstract classes only**: useful for owned APIs, but cannot augment unrelated types.
- **Compiler plugin as final product**: the existing Synexia prototype proves bounded behavior, but lexical resolution is not equivalent to native `Resolve`/`Attr` integration.

## Risks and assumptions

Java 21 source and binary compatibility is mandatory. Adding extension candidates may introduce a new ambiguity in previously invalid extension-call source; that is allowed only when the feature is enabled. Adding an ordinary member later can rebind a freshly recompiled extension source; compile-time binding and binary binding must therefore be tested separately. Generic inference, overload and property evaluation are the highest-risk compiler changes. Final annotation package/flag naming is **unapproved** and must pass naming preflight before becoming a public target API.

## Dependencies, implementation status and proof boundary

- Canonical Synexia metadata prototype: [PR #18291](https://github.com/hsoliwal/com.synexia/pull/18291) and [PR #18301](https://github.com/hsoliwal/com.synexia/pull/18301), merged before drafting.
- Experimental opt-in receiver syntax: [PR #18309](https://github.com/hsoliwal/com.synexia/pull/18309), merged before drafting.
- ABI validation / import scoping: [PR #18336](https://github.com/hsoliwal/com.synexia/pull/18336), draft at drafting time.
- **Existing verified scope**: bounded Synexia Java 21 compiler-plugin prototype (declaration indexing, selected receiver calls/properties/operators, experimental import scope), with recorded local test results in its owning PRs.
- **Not implemented in the M3JDK product**: feature flag, proposed public annotation, native `Resolve`/`Attr`/`Lower` support, all-property and operator semantics, Kotlin-style member provider dispatch, complete lookup, generic inference and module visibility.
- **Not executed for product admission**: exact pinned Maven/OpenRewrite source recipe, naming preflight, full matched-M3JDK `jdk.compiler` build, jtreg, installed-image runtime and performance gates.

**Decision:** DRAFT / UNADMITTED. This JEP-like design is a specification candidate only. It is not an official OpenJDK JEP, a release claim, or permission to deliver unchecked JDK postimages.
