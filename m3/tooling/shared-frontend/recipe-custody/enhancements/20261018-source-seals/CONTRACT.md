<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# M3AST source seals and shared build consumption

## Purpose and authority

Enhance the existing `com.sun.source.util.M3AST` / `com.sun.tools.javac.m3.M3ASTPC`
owners. Maven and Gradle consumers must use the original compiler task and its
existing tree. The source proposal identifies javac, not the JavaCC generator.
Synexia owns the reusable Maven/OpenRewrite recipe. M3JDK21 owns compiler output
and runtime acceptance; each build fork owns its native integration.

This document precedes the source candidate. No verification result is implied.

## Inventoried revisions

| Repository | Revision | Observed relevant state |
| --- | --- | --- |
| hsoliwal/M3jdk21 | 2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49 | Shared compiler API, source seals, lazy identity projections, builtin plugin and Rewrite converter present. |
| hsoliwal/com.synexia | 140c263a90f9803b9c46103c63355a286e0515f0 | Existing m3-ast-build-reverse-20261017 task crate and snapshot recipe owners present. |
| hsoliwal/maven | bd11c44cd98aef16a3d049db0ebd8477608ccf4d | PR 1 native opt-in present; PR 2 session-service atom absent despite merge ancestry. |
| hsoliwal/gradle | 419a77cf61af3924f60678510ed2440230047671 | PR 3 original-task provider SPI present, confirmed by exact blobs. |

The Gradle documentation names an M3JDK provider under
`m3/tooling/module-packs/build-support/gradle-provider`, absent in the inspected
M3JDK module-pack subtree. Its receiver implementation is a separate leaf in this
task. The Maven recovery must preserve both current compiler-configuration hooks.

PR 1192 contains 72 additive M3ASTPC observer lines and a separate AST hierarchy
module missing from the current M3JDK tree. Those are recorded historical recovery
inputs. This source-seal leaf must not overwrite them in a tree that contains them
and does not claim to restore that separate module.

## Locked contract

- Keep all public/protected M3AST declarations, task selection, compiler options,
  source identities, annotation processing and generated-source behavior.
- One M3 session per actual javac context; one original parse per compiler input.
- Projection keys retain object-identity semantics and event-local memoization,
  including null values, failures, and recursion refusal.
- Callback units remain thread-confined and expire after dispatch. Retain only
  detached snapshots beyond the callback; do not exchange live compiler objects
  between Maven/Gradle processes or compilation contexts.
- Retain exact UTF-16 code units, including lone surrogates. Existing SHA-256
  values remain identical; raw byte identity remains a distinct concept.
- PARSE uses the frozen parser input without another source read. ANALYZE source
  requests continue to reread once per event and refuse edits against the seal.
- Existing cache ownership, laziness, isolated contexts, LRU/swap and persisted
  representations remain with their current owners. No second parser or cache.
- Keep original source-capture exception identity and suppressed cleanup errors.
  A failed source capture must not publish the optional primitive image.

## Source atoms to change

1. `M3ASTPC.retainSource`: use the existing session abort/cleanup path when an
   enabled capture throws a runtime exception or error, then rethrow the original.
2. `M3ASTPC.retained`: feed exact high-byte/low-byte UTF-16 code units to SHA-256
   in blocks of at most 4096 code units (8192 temporary bytes), without full-source
   byte materialization or charset replacement of malformed surrogates.
3. `M3ASTPC.LiveUnit.source`: reject a changed length after the existing read and
   before hashing; preserve the same IOException message and memoization.
4. Add a focused langtools source-seal test; preserve the existing sharing suite.

The source-capture cleanup leaf does not establish a general success signal for
all compiler/annotation-processor failures. A COMPILATION event is not by itself
proof that every unrelated compiler operation succeeded.

## Qualification sequence

For each candidate: source patch -> source lint -> compilation -> tests -> runtime.
Stop on failure; record the exact failed stage. Test the real class-backed
OpenRewrite recipe, input sealing, refusal and fixed point before target receipt.
Preserve the original preimage and record the generated postimage SHA-256.

The compiler corpus includes two consumers sharing one original tree/projection,
empty and boundary-sized UTF-16 input, all code-unit values, lone surrogates,
cross-block surrogate pairs, equal/different-length edits, duplicate-URI capture
rejection, cleanup suppression, and absence of output after capture failure.
Compare actual digest results with the unchanged scalar byte-at-a-time oracle.

Compilation and runtime proof must use the actual M3 compiler sources. A stock
JDK with a compiled compiler-module overlay is identified as such and cannot
stand in for a whole source-built JDK image or full jtreg/HotSpot qualification.
Record native-fork, platform and end-to-end build gates individually. No universal
speedup, portable semantic-cache format, or global receiving-stage promotion is
claimed from a finite suite.

## Reuse and disclosure

Reuse the existing first-party compiler source and snapshot recipes with exact
revision/hash custody. Target compiler/test files retain GPLv2 with Classpath
exception headers; the Apache recipe does not relicense its payloads. This work
maintains publicly disclosed compiler/source-sharing mechanisms and implements
the already-public Gradle SPI. It adds no private invention-specific disclosure.
