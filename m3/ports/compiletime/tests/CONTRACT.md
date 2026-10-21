<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3File source-facts delta contract

This candidate adds a pure difference operation to the existing
`com.m3.compiletime.ast.M3FileSourceFacts` value. The record components,
constructor validation, source/context SHA-256 framing, equality and existing
methods remain unchanged. The candidate is materialized into the target only
through the canonical Synexia recipe.

`current.changesFrom(previous)` returns an `int` mask with these independent
reasons:

| Constant | Bit | Difference |
| --- | --- | --- |
| `SOURCE_URI_CHANGED` | 0 | Source URI text differs. |
| `SOURCE_CONTENT_CHANGED` | 1 | Source SHA-256 or UTF-16 length differs. |
| `COMPILATION_CONTEXT_CHANGED` | 2 | Ordered compilation-context SHA-256 differs. |
| `PROVIDER_CHANGED` | 3 | Consumer/compiler projection provider differs. |
| `SCHEMA_CHANGED` | 4 | Projection schema version differs. |
| `PHASE_CHANGED` | 5 | Compiler phase differs. |

Every applicable reason is set. Zero means equality of all seven stored
identity components. It does not certify original file bytes, ABI equivalence,
complete environment capture, dependency completeness or valid build outputs.
A null previous snapshot throws `NullPointerException`; an unknown baseline is
not silently represented as unchanged. Both arguments remain immutable and the
comparison is symmetric because each flag reports inequality.

The source digest continues to describe exact UTF-16 characters, including
unpaired surrogates, rather than original on-disk bytes. Source length is checked
independently of the digest because callers may construct metadata directly.
The context digest remains order-sensitive: unchanged source with a reordered
classpath still reports `COMPILATION_CONTEXT_CHANGED` when the caller includes
those ordered identities. Callers remain responsible for capturing complete,
stable compiler, option, classpath, module-path and processor context.

This operation does not classify Java semantics. A constant-value change, a
method-body change and a comment change each report changed source content.
Only separately qualified compiler/class evidence can decide dependency or ABI
invalidation. No cache, scheduler, parser, filesystem watcher, class-file API or
JNI symbol is added by this candidate.

## Qualification

`M3FileSourceFactsDeltaContract.java` is a standalone Java 21 contract test for
the candidate owner. It exercises every stored identity component, all 128
binary combinations and all pairs of those combinations, null rejection,
equivalent reconstructed records, direct metadata length disagreement,
context/classpath order, and constant/body/comment source changes. Existing
source/context digest vectors and mutable-input capture behavior remain checked.
Finite source-fact tests are not complete Maven/Gradle integration or runtime
promotion evidence.
