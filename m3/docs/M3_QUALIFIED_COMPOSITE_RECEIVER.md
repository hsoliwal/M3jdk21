<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 qualified composition receiving plan

**Destination: `hsoliwal/M3jdk21`, the integrated Java 21/HotSpot/JNI SDK.**

**Current state: product-owned receiving plan and executable source-preimage gate,
not a deployed runtime, grammar extension, installed image or phase advance.**

The machine record is
[`qualified-composites-20261117.json`](../synexia-import/qualification/qualified-composites-20261117.json).
Run the product-side preflight from the root of a complete M3JDK21 checkout:

```sh
python3 m3/tooling/qualified-composite-receiver/check.py
python3 -m unittest discover -s m3/tooling/qualified-composite-receiver -p 'test_*.py' -v
```

The preflight checks exact Git blob preimages for seven **existing** JDK source
owners, two merged Synexia recipe manifests and their candidate postimage
SHA-256 values. It refuses changed or symlinked target sources, renamed owners,
missing families, an invalid or narrowed qualified namespace, and false claims
that Java21 source, jtreg, JNI/GC or the installed image were qualified. It
is intentionally **not** the JDK builder or an independent second recipe.

## Current recipe custody and target ownership

The source-sealed Maven/OpenRewrite recipes remain in `hsoliwal/com.synexia`:

- [#18252](https://github.com/hsoliwal/com.synexia/pull/18252): qualified
  source scope, merged recipe only, with a VM-local positive `long` namespace,
  program/library/module/loader/provider and source version fields, and a
  distinct occurrence identity. Oracle: 305 local checks.
- [#18269](https://github.com/hsoliwal/com.synexia/pull/18269): add scoped
  graph/path and JVM descriptor overloads to original Synexia owners, merged
  recipe only. Oracle: 402 local checks.

Both isolated Java21 checks used a compile-only `MIndexString` placeholder;
neither recipe was replayed into the actual foundation on `develop` and
qualified with full Maven/OpenRewrite fixed-point and refusal tests, nor into
the M3JDK21 installed image. The receipt's exact recipe manifest Git blobs
and source postimages make this limitation auditable. No placeholder may enter
the product.

For M3JDK21 the receiving destination is the **existing owner**, not a new
public class whose name shadows the JDK:

| Capability | Product source owner | Qualification rule |
| --- | --- | --- |
| Immutable String payload | `java.lang.String`, `java.lang.M3StringOwner` | Keep canonical immutable leaf content global; scoped metadata must not fragment the OS-shared byte/UTF-16 substrate |
| Reflection/class metadata | `jdk.internal.mindex.M3CI`, `M3Class`, `M3Method`, `M3Descriptor`; public `java.lang.Class` unchanged | Class identity is binary name **and defining loader**; library version and source generation qualify metadata, not the Java `Class` identity rule |
| Regex | Existing `java.util.regex.Pattern` / `Matcher` | Planned grammar/automata precompute stays behind exact regex semantics; no separate public substitute |
| Arrays/collections | Existing Java/HotSpot arrays, `java.util.*`, internal primitive/offset/mask owners | Parent-owned lanes, masks, composition, views and mutation semantics require exact alias/JNI/GC and ABI proof |
| AST/compiler | `jdk.compiler` and existing internal M3 AST owners | Source generation and loader/module boundaries qualify bindings; syntax atoms may be shared when identical |
| Paths and domains | `java.nio.file.Path`, filesystem provider, domain-specific M3 internals | Structural path atoms may share, but provider-sensitive equality and occurrences may not be conflated |
| Runtime | HotSpot, Java heap, JNI and class-loading owners | Preserve class initialization, object identity, monitors, GC rooting and exception order |

## Where the `long` UID belongs

`long` is a compact **VM-local index for a fully qualified semantic scope**.
The scope record is the authority, comprising domain, program, module, source
origin/generation, library coordinate and revision, defining class loader,
filesystem provider, schema version and content fingerprint. Do **not** use
the UID itself as a globally persistent identity, or derive identity by
truncating SHA-256 to 64 bits.

Content and meaning follow different equivalence rules:

1. Immutable source/text/descriptor leaves are canonicalized by exact content
   equality. A content hash is a lookup accelerator and requires collision
   verification. OS-shared text must remain shared across programs/versions.
2. Semantic compositions are qualified by scope UID; identical binary class
   names in distinct defining class loaders remain distinct runtime types.
3. Occurrences carry their owner, generation and occurrence ID so a changed
   file or class binding does not accidentally reuse old mutable semantic
   state. Invalidation is generation-scoped; page residency is not identity.
4. A source SHA-256 establishes a content fingerprint **only after actual
   source bytes are checked**; it does not authenticate a library version or
   prove that a dependency came from a trustworthy origin.

This is the distinction needed to share canonical atoms in `M3String`,
`M3Path`, `M3AST`, metadata and runtime structures without equating unrelated
occurrences or violating Java `==`, `equals`, reflection or class loading.

## `const` and the immutable array boundary

The separately proposed `const` qualifier for M3JDK primitives, arrays,
aliases and JNI-backed arrays remains **a language/VM/JNI candidate**. Intended
semantics are read-only propagation across aliases obtained from a const view,
and no mutation through that view. This does not by itself make a shared array
immutable if another mutable alias can still write to it. A proven immutable
canonical backing instead needs exclusive freeze, controlled copy or runtime
ownership/escape enforcement. `final` only freezes a Java field/reference,
not the underlying array elements. Java21 annotations or `UnsupportedOperationException`
cannot intercept an ordinary `array[index] = value` bytecode store without
compiler/VM support. Until those proofs exist, **do not emit the syntax or
claim JNI mutation barriers are installed**.

## Promotion gate: continue from String, not from receipt counts

Admission order remains `STRING → ARRAYS → COLLECTIONS → AST/compiler → REMAINING`.
The twenty-family list in the machine receipt is complete as a **candidate
receiving list**, not twenty target implementations. Each candidate still needs:

1. Canonical Synexia recipe replay against exact real foundation preimages,
   real `MIndexString` closure, Maven/OpenRewrite fixed-point and drift-refusal.
2. Target naming-map admission and appropriate **original** JDK owner/source
   postimages; preserve API, class-loader/provider and identity contracts.
3. Actual Java 21 product build, JNI/HotSpot/GC and full applicable jtreg,
   public ABI, exception/thread/alias and differential behavior evidence.
4. Installed-image execution and comparative CPU, heap and native memory
   measurements; correct per-file OpenJDK and donor license/NOTICE.
5. An explicit revisioned receipt after each gate. A successful preflight here
   does not authorize an admission or bypass String-first ordering.

Other authoritative M3JDK policies remain
[`m3jdk21-porting-invariant.md`](m3jdk21-porting-invariant.md),
[`name-mapping.json`](name-mapping.json), and
[`M3JDK21_NAME_AND_PACKAGE_CONTRACT.md`](M3JDK21_NAME_AND_PACKAGE_CONTRACT.md).
