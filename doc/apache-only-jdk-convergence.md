<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21 — enduring Apache-2.0-only convergence roadmap

**State:** proposed long-term direction, recorded 2026-11-18. Not an Apache-only
release, source substitution approval, or completed runtime qualification.

**End state:** an independently complete, source-built and multi-platform
**Apache-2.0-only M3JDK SDK** that preserves Java 21 compatibility and
incorporates the strongest qualified techniques from Apache Harmony, other
independent JDKs, the current M3JDK21 and Synexia. The plan is permanent:
every subsequent work session should continue the backlog instead of
restarting discovery or treating one candidate as full JDK convergence.
This name does not suggest Apache Software Foundation endorsement.

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
