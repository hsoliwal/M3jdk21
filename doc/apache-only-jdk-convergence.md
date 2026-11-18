<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21 — Apache SDK superset convergence roadmap

**State:** proposed long-term direction, recorded 2026-11-18. Not an Apache-only
release, source substitution approval, or completed runtime qualification.

**End state:** an independently complete, source-built and multi-platform
**Apache-2.0-only M3JDK SDK** that preserves Java 21 compatibility and
incorporates the strongest qualified techniques from Apache Harmony, other
independent JDKs, the current M3JDK21 and Synexia. The plan is permanent:
every subsequent work session should continue the backlog instead of
restarting discovery or treating one candidate as full JDK convergence.
This name does not suggest Apache Software Foundation endorsement.

**SDK >> JDK is the enduring product invariant.** The M3JDK21
repository is the integrated runtime/SDK product; `M3SDK` is shorthand
for its intended **compatible SDK superset**, not a proposed rename of
`java.*`, the repository, existing CLI tools or public modules. The
eventual Apache-2.0-only distribution must provide the complete
supported JDK plus independently admitted SDK capabilities. This
is a goal, not a current runtime or licensing qualification.

## 1. Two distinct delivery tracks

- **Current product:** `hsoliwal/M3jdk21` remains an OpenJDK-derived JDK;
  GPLv2/Classpath-Exception and other upstream rights stay on their actual
  files. Preserve current Java/HotSpot/JNI semantics, APIs, history, earlier
  optimizations and existing runtime. Introducing an Apache-2.0 module does
  not relicense the current JDK.
- **Future strict Apache-2.0 SDK:** separately qualify the distributed
  source, generated outputs, class libraries, JVM, JIT/GC, compiler,
  bootstrapping, tools, JMOD/JAR/jlink image, native libraries, security,
  resources and packaged third-party dependencies. Every *included* source
  expression and artifact must have proven Apache-2.0 rights. MIT-only,
  BSD-only, GPL-only, EPL-only, unknown and incompatible material requires
  separate authorization or independent replacement; simply being
  "permissive-compatible" is a different release target.
- Host OS and toolchain dependencies must be disclosed separately.
  Java SE compatibility claims and certification/trademarks require their
  own evidence and are not granted by the Apache-2.0 license.

The goal is progressive replacement by lawfully usable source and
independently implemented atoms, **never** a change to the existing
OpenJDK license header or a selective deletion of Git history.

## 2. Canonical ownership and order

`hsoliwal/com.synexia` owns the donor inventory, per-file rights review,
source seals, semantic atomization/patternization, LogicHash/StructuralHash/
SimHash candidate search, Maven/OpenRewrite recipes, counterexamples,
comparative measurement, logs and execution receipts. M3JDK21 is a
**result-only product target**. No private recipes, donor mirrors,
qualification bundles or second algorithm registry go into M3JDK21.

Runtime receiving order is **String -> arrays -> collections ->
AST/compiler -> remaining JDK families**. Parallel read-only donor
research is permitted, but no new phase is declared admitted from a
catalogue, unit-test-only oracle, merge history or isolated benchmark.
Preserve earlier proven behavior and interfaces.

The existing authorities remain controlling:
[AGENTS.md](../AGENTS.md),
[porting and naming invariant](../m3/docs/m3jdk21-porting-invariant.md),
[naming/package contract](../m3/docs/M3JDK21_NAME_AND_PACKAGE_CONTRACT.md),
[current invariants](m3jdk-invariants.md) and
[Apache source borrowing boundary](../M3_APACHE_BORROWING_POLICY.md).

### 2.1 Formal SDK superset and compatibility contract

`M3SDK ⊃ JDK` means all supported Java 21 source, binary, installed-image,
runtime, reflection, serialization, module, security, JNI/JVMTI and
developer-tool behavior remains compatible, together with already
accepted M3JDK21 extensions. A new language/runtime/compiler feature
must have a separately specified version and opt-in behavior. Existing
programs continue to work; stock JDK 21 is not required to implement
M3-only additions. Retain the existing public JDK names, signatures
and original implementation owners. No parallel `M3String` payload
owner, source relabeling, JAR-only overlay, changed JDK API name,
or unapproved `jdk.m3.*` namespace constitutes a superset.

The SDK includes the full JDK (`java`, `javac`, module packaging and
tools), not just a VM or optimized classes. M3SDK is a product-scope
notation; the repository remains `M3jdk21` and the established JDK
module and package names remain authoritative. The strict
Apache-2.0-only *distribution* is a separate release gate: the current
OpenJDK-derived product remains under its actual mixed source licenses.

### 2.2 Entire JDK/SDK is the atomization domain

Inventory and analyze **every relevant source, module, native component,
build input, test and tool**. Atomize into the smallest contract-bearing
semantic leaves, patternize only demonstrated reusable algorithms,
retain interfaces/signatures and inline semantic documentation, and
recompress into existing canonical JDK owners. Do not impose an
arbitrary total number of atoms, but maintain bounded execution and
deterministic per-file serial mutation. Java, C/C++, JNI, scripts,
build definitions and non-source resources require distinct,
appropriate parsers and interpretation; no single syntactic extractor
proves all languages or binary behavior.

| Coverage family | Representative owners and required invariants |
| --- | --- |
| Standard class libraries | `java.base`, String/arrays/collections/streams/regex/math/Unicode, I/O, network, concurrency, serialization, reflection, crypto and security |
| Runtime and VM | HotSpot interpreter, JIT/IR tiers, GC and barriers, class metadata, safepoints, threads, JNI/JVMTI, JFR, diagnostics and portable/native services |
| Compiler and languages | `javac` lexer, parser, symbols, type checking, inference, annotation processing, code generation, compiler API and isolated additive language features |
| JDK and developer tools | `java`, `javap`, `jar`, `jmod`, `jlink`, `jdeps`, `jpackage`, `javadoc`, `jshell`, `jdb`, launchers, debugging and module images |
| Desktop and native platforms | `java.desktop` AWT/Swing, graphics/print/accessibility; OS process pipes, file handles, sockets, memory mapping, native ABI and each platform implementation |
| SDK extensions | Established optional M3 APIs, additional algorithms, languages, tooling and compatible native/build capabilities; no duplicate JDK surface |
| Infrastructure and proof | `make`/configure, bootstrap, JMOD/JAR distribution, source generation, CI, jtreg, regression fixtures, native sanitizers, LICENSE/NOTICE, SBOM and platform release |

The *coverage denominator* is the inspected project tree and its
declared product dependencies, not a favorable subset. Maintain
machine-readable states in existing Synexia registries:
`DISCOVERED` -> `INVENTORIED` -> `ATOMIZED` ->
`PATTERNIZED` -> `RECIPE_VERIFIED` -> `TARGET_TESTED` ->
`RUNTIME_ADMITTED` -> `RELEASE_QUALIFIED`, together
with explicit `HOLD`, `REFUSE`, `RETAIN_INCUMBENT`,
`REFERENCE_ONLY` and `NOT_APPLICABLE`. Every omitted file,
module, unsupported platform and missing proof is disclosed;
a hash match or inventory count never establishes equivalence.
Previously verified units resume from the last verified stage.
Full coverage is a target, **not an achieved claim**.

### 2.3 Best algorithms from every qualified donor family

The donor search space includes **all relevant JDKs**, the complete
`hsoliwal/com.synexia` convergence workspace, its existing recipes
and first-party implementations, and **rights-qualified private
repositories and Synexia labs**. Private repository code is not
automatically Apache-2.0, publicly disclosable or eligible to
copy; keep private source, paths and artifacts inside their
authorized boundary. Pin exact owner, repository, commit, Git
blob, SHA-256, license/NOTICE, interfaces, callers and history.
Use source rights and transitive dependencies to decide whether
a donor is `REFERENCE_ONLY`, `CANDIDATE`, `HOLD` or `REFUSE`.

Select the **best per contract-bearing atom** against the existing
implementation and competing donors. Require behavioral and
adversarial correctness, negative/mutation tests, source/binary
compatibility, asymptotic worst case, p50/p95/p99 latency,
cold/warm CPU, allocation, retained heap/native bytes, GC, startup,
security, concurrency and supported-platform behavior. A winning
microbenchmark does not grant Java equivalence. Prefer the original
implementation when it is superior or replacement cannot be proven.

Use the established Synexia LogicHash, StructuralHash, SimHash,
semantic leaves, category taxonomies, source-sealed CAS and
Maven/OpenRewrite recipe crates. Algorithms and similarity hashes
are candidate signals, not execution receipts or equivalence.
Only the canonical Synexia recipe can qualify reusable transformations;
M3JDK21 receives exact results and its own jtreg/native/build tests.
No extra algorithm engine, duplicate implementation store or
target-local recipe registry may be created.

### 2.4 Cross-session custody and admission sequence

Existing Synexia source and historical inventories remain
authoritative, including `20261112-federated-jdk-donors` and
`20261116-apache-adoption`. Do not repeat prior inventory or
declare those historic counts to be current. Retain named
JMO/Flex/Flexi atoms as `HOLD_OWNER_UNRESOLVED` until
their actual paths, APIs, rights and source identities are found.

For each file: inventory and seal old content -> freeze API and
semantic contract -> atomize -> patternize -> compare JDK/private/
Apache donors -> propose a serial diff -> lint -> compile ->
differential/jtreg -> JNI/native/GC -> matched runtime/image ->
benchmark -> OS/rights closure. Save SHA-256 and actual command,
build, lint, failure and test logs. Preserve earlier accepted
behavior, history and owner identity; do not silently move,
delete, merge, relicense or rename. Neither an unexecuted job
nor a draft PR is product admission.

**No whole-tree atomization, SDK superset compatibility or
Apache-2.0-only release is certified by this roadmap.**
## 3. JDK names are the public source of truth

Keep `java.lang.String`, `java.lang.Class`, `java.util.Arrays`,
`java.util.BitSet`, `java.util.HashMap`,
`java.util.concurrent.ConcurrentHashMap`,
`java.util.regex.Pattern`, `java.io`, `java.nio`, and the other
standard Java classes in their **original public JDK names and module
owners**. Preserve method descriptors, constructors, inheritance,
public/protected APIs, reflection, serialization, exceptions, JNI symbols,
module exports, native lifetimes and Java 21 behavior.

A Harmony or other donor algorithm is not an excuse for
`HarmonyString`, `FastJdkHashMap` or a duplicate exported API.
Qualify the replaceable private implementation atom inside its existing
owner. `java.lang.M3String` remains an internal canonical owner/coordinate
representation; use the already identified M3 arrays, offsets, indexes,
bit masks, and existing internal/JDK companion owners. No second payload
store or automatically introduced `jdk.m3.*` package family. Review the
current naming map before changing an internal symbol, and use JDK-style
names/formatting instead of mechanically prefixing donor types.

**JMO, Flex and Flexi atoms** are existing names raised for convergence.
Their exact source paths, interfaces, tests and license/ownership mapping
have **not yet been identified** in this review. Preserve those names as
`HOLD_OWNER_UNRESOLVED` in the Synexia inventory; do not invent packages,
signatures or equivalence claims, rename them, or discard earlier code.
Resolve their exact hashes and owners before any JDK integration.

## 4. Donor universe and license disposition

| Family | Intake disposition |
| --- | --- |
| [Apache Harmony classlib](https://github.com/apache/harmony-classlib) and [Harmony/DRLVM/JDK tools](https://github.com/apache/harmony) | **First inspection priority** for historic Apache-2.0 class-library algorithms, tests and natives. Retired in 2011; not a Java 21 implementation. Per-file rights and dependencies must be checked. |
| [TeaVM](https://github.com/konsoletyper/teavm) | Apache-2.0 independent class-library implementations, some historically from Harmony; JavaScript/AOT semantics are not a substitute for full JVM Java 21 contracts. |
| [OpenJ9](https://github.com/eclipse-openj9/openj9), [Eclipse OMR](https://github.com/eclipse-omr/omr) | Evaluate the specifically selectable Apache-2.0 path for VM, JIT, GC, JNI and OS machinery. OpenJ9's normal OpenJDK class-library dependency blocks an automatic Apache-only designation. |
| Apache Commons, Lucene, BCEL, XML, HTTP, Directory, eligible independent collection/search/cache projects | Compare *private behavior* and benchmark within original JDK owners. A public library API or project-level license does not qualify direct replacement. |
| OpenJDK, Corretto, Temurin, Kona, SapMachine, Microsoft/JetBrains/other vendor JDKs, Android ART, GraalVM and non-Apache runtimes | Examine contracts, design, performance, bugs and revision history; **reference-only** absent exact independently established Apache-2.0 source rights. |

Evaluate every appropriate JDK implementation, but select *the best*
per contract-bearing operation on measured worst-case and normal CPU
work, allocations, retained heap, GC/native memory, security, latency,
concurrency, startup, and cross-platform behavior. Do not assume a donor
is faster or equivalent solely from project popularity or similarity.

## 5. Apache Harmony first-wave inspection

The inspected historic Harmony class-library tree contains 37 modules,
including `luni`, `concurrent`, `math`, `regex`, `text`,
`archive`, `nio`, `beans`, `security`, `logging` and
`prefs`. Concrete legacy source exists for `java.lang`,
`java.util`, `java.io`, `java.math` and `java.util.regex`.

1. **String remains active:** compare Harmony text/StringBuilder and
   regex atoms against the present M3String/String/Pattern contracts.
   Retain Unicode, unpaired-surrogate, matching, bootstrap and JNI
   gates. Evaluate the independently tested Two-Way search candidate
   alongside the incumbent and earlier bounded-sentinel proposal.
2. **Arrays next:** examine Harmony `java.util.Arrays` algorithms and
   array-copy/search/sort/range logic against M3's existing array,
   offset and mask owners; prove primitive, object, comparator, null,
   overflow and bounds behavior.
3. **Collections after arrays:** examine Harmony `BitSet`,
   `ArrayList`, `HashMap` and concurrent algorithms, preserving
   equality, iteration, backed views, serialization, blocking,
   interruption and Java Memory Model semantics. Also retain each
   current JDK/Guava/Eclipse Collections/Commons/Spring contract.
4. **Remaining source families:** evaluate math `BitLevel` and
   arithmetic, text/locale/calendar, byte I/O/NIO, XML/security,
   native OS services and compiler/tooling only as their owners
   and Java 21 requirements become fully mapped.

Even within Harmony, individual headers differ. Its `depends/oss`
materials include third-party code whose licenses may not be Apache-2.0;
some historic JSR-166 concurrent files carry public-domain statements
rather than ordinary ASF headers. Pin and inspect **every actual file**,
including LICENSE/NOTICE and dependencies, before considering source
reuse. Store exact donor paths/commits/hashes and admission decisions
in **Synexia**, not as a donor catalogue in this public product tree.

## 6. Runtime/OS distribution closure

An Apache-only VM is not enough without a full independent Java 21 SDK.
Explicit unresolved owners include `javac` parser, attribution,
annotation processing and code generation; `jar`, `jlink`, JMOD,
launcher, modules, crypto/TLS/JCA, ZIP, time/Unicode/locale data,
JVMTI/JNI, AWT/graphics where delivered, GC/class metadata,
native memory safety and installers. JavaParser or BCEL alone is
not a complete javac.

**Proposed** platform priority; every target is `NOT_EXECUTED`
for the strict Apache-only release:

- **P0:** Linux x86-64/glibc, Linux AArch64/glibc, Windows x64,
  macOS ARM64 and x64.
- **P1:** Linux musl, POWER64LE, s390x, RISC-V64; AIX POWER64;
  FreeBSD x64, then additional verified systems.
- **Separate contracts:** Android ARM64, iOS ARM64, and browser/
  WebAssembly/WASI AOT. Never infer desktop JVM/JNI parity.

Native/OS configuration, toolchain, linked libraries, JMOD contents,
class API, JNI/JVMTI, GC/CDS and the installed image must be
verified for each actual OS/architecture/libc/toolchain tuple.

## 7. Source-to-source proof and admission

For every candidate, do the following *in order*:

1. Inventory the actual JDK owner, callers, methods, interfaces,
   previous acceptance, native symbols, tests, existing source SHA-256
   and immutable branch/commit preimage. No movement or deletion.
2. In Synexia pin donor repository, immutable commit, path, Git blob,
   SHA-256, complete file header, rights, NOTICE and transitive
   dependencies; classify `REFERENCE_ONLY`, `CANDIDATE`,
   `REFUSE` or `HOLD`. Source hash similarity never grants equivalence.
3. Decompose to independent semantic leaves; compare with original
   and other donors using Java 21 oracles, edge cases, failure and
   performance evidence, including regressions, resource pressure
   and negative cases. Choose a true contract-preserving atom.
4. Use the existing canonical Maven/OpenRewrite recipe machinery
   in Synexia with immutable before/after hashes, fixed-point and
   stale/duplicate/refusal tests, mutation counterexamples, and
   append-only correction history.
5. Receive an exact postimage in M3JDK21 **only after recipe proof**,
   in the real JDK class/module owner and with target-owned jtreg
   tests; no Synexia runtime dependency or parallel implementation.
6. Verify ordered diff -> lint -> compile -> unit/differential/jtreg
   -> JNI/GC/CDS/runtime -> cross-platform installed image ->
   CPU/heap/native benchmark -> complete source/binary Apache-2.0
   license/NOTICE/SBOM closure. Skipped jobs are `NOT_EXECUTED`.
7. Merge and claim release eligibility only with actual receipts
   for all required gates. Preserve hashes, lineage and prior work;
   no source renaming/relicensing tricks or automatic PR merge.

## 8. Durable priority queue

| Step | Owner and result required |
| --- | --- |
| A. Harmony licence/semantic inventory | Synexia: per-file categories, immutable paths/hashes, dependency and rights refusals. No import into JDK yet. |
| B. Finish current String phase | Synexia recipe plus **matched M3JDK21** compile, jtreg, JNI/GC/CDS, behavior and performance; compare Two-Way, bounded sentinel and incumbent before choosing. |
| C. Arrays and collections | Qualified Harmony/compatible primitive atoms inside existing JDK owners only after the previous runtime phase is admitted. |
| D. Apache-only foundation | Independently integrated Apache-selectable VM, full class libraries, compiler, tools, native and image packaging with no unqualified source. |
| E. Multi-platform release | Rebuild/test per OS tuple; audit distributed artifacts and all legal/Java behavior gates before strict Apache-only status. |

**No current milestone in this document is marked production-admitted.**
This document records direction, not a JDK build, donor copy, source
relicense, Maven execution, license clearance or Java SE certification.

### Reference sources

[Apache Harmony Attic](https://attic.apache.org/projects/harmony.html) ·
[Harmony class-library README](https://github.com/apache/harmony-classlib) ·
[TeaVM class-library policy](https://teavm.org/docs/contributing/general.html) ·
[OpenJ9 license](https://github.com/eclipse-openj9/openj9/blob/master/LICENSE) ·
[OMR license](https://github.com/eclipse-omr/omr) ·
[Apache-2.0 terms](https://www.apache.org/licenses/LICENSE-2.0) ·
[LICENSE/NOTICE packaging guidance](https://infra.apache.org/licensing-howto.html).
