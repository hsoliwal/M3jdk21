<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
SPDX-License-Identifier: Apache-2.0 -->
# M3 source, text and compiler adapters for Lucene/OpenNLP

This contract precedes its implementation. These are review templates for the
existing Synexia recipe crate, not a second canonical recipe or payload owner.
The source pins and executed qualification are recorded by the enclosing pack.

## Existing owners and source disposition

| Requested family | Reused owner | Adapter disposition |
| --- | --- | --- |
| MIndexFile / M3File | `com.m3.runtime.capability.SourceArtifact.Span` | Bounded input-stream cursor over the existing immutable byte owner. No tracked `M3File.java` exists at the inventoried target; full MIndexFile receiving parity is unproved. |
| MIndexString / M3String | `java.lang.String` / stable immutable `CharSequence` | Existing fork `M3TextReaders` and `M3Tokenizers` ingress. Internal M3String activation remains a separate matched-JDK gate. |
| MIndexAST / M3AST | `com.sun.source.util.M3AST` and its existing compiler implementation | A PARSED consumer and event-local projection. No second parser, tree, compiler task, or persistent compiler reference. |
| Search and NLP algorithms | Actual pinned Lucene/OpenNLP fork implementations | Delegation through their existing public APIs; no replacement analyzer, codec, model loader, or search engine. |

The current Lucene fork requires Java 25. The adapter sources use Java 21 language
features, but a binary linked to that fork requires its Java 25 dependency closure.
No Java 21 execution claim follows from the adapter source level alone.

## Independently admitted modules

`com.m3.nlpsearch`, from `src/main/java`, requires the actual
`com.m3.runtime.capability`, `org.apache.lucene.core`, and the aggregated current
`org.apache.opennlp.tools` modules. It does not require `jdk.compiler` or an M3
compiler. Its test entry points are under `src/test/java`.

The optional `com.m3.nlpsearch.compiler`, from `src/compiler/java`, also requires
the runtime adapter and a supplied `jdk.compiler` containing the actual M3AST API
and source-retention implementation. It is not part of a stock-JDK-only image.
Its test entry point is under `src/compiler-test/java`. Module descriptors are
materialized by the enclosing existing descriptor recipe. A compiler API stub or
the M3AST interface alone does not qualify this module's callback behavior.

## Byte cursor contract

`M3SourceInputStream.open(SourceArtifact.Span)` creates only a cursor and a
reference to the selected immutable span. It never calls `Span.copy()` or copies
the payload into a second backing array. Reading into a caller's byte array is
the explicit InputStream output boundary. Every offset is relative to the span;
the immutable owner remains responsible for its original absolute coordinates.

Each stream has independent position and mark state. The initial mark is zero;
`mark(readLimit)` records the current position and ignores the advisory limit
because immutable backing does not expire. Negative read limits are accepted as
an ignored hint, as by `ByteArrayInputStream`. `reset()` returns to the mark.
Unsigned single-byte reads return 0 through 255, EOF is -1, and a valid zero-size
buffer read returns zero even at EOF. Skips are bounded to the selected span;
negative skips do nothing. Buffer geometry uses the JDK bounds checks.

Closing is idempotent and releases only that stream's span reference. It does
not close or modify the source or another stream. After close, read, skip,
available and reset throw `IOException`; mark throws `IllegalStateException`.
`markSupported()` always returns true. Cursor operations are synchronized, but
an application sequence of several calls is not an atomic transaction.

An OpenNLP model can consume the existing owner directly:

```java
try (var input = M3SourceInputStream.open(modelBytes)) {
    var model = new opennlp.tools.tokenize.TokenizerModel(input);
    // The upstream model loader owns its decoded model representation.
}
```

Model ZIP/decompression/size policy remains the existing OpenNLP loader's policy.
The span's byte bound does not claim a bound on the decoded model's heap usage.

## Text and search contracts

All CharSequence ingress requires caller-owned stable immutable backing through
the complete operation, including later indexing of a returned Document. There
is no reflective immutability claim for arbitrary caller implementations.
`toString()` and `subSequence()` are never used to flatten or slice input text.

`M3NlpSearch.utf16Sha256(text)` hashes exact UTF-16 code units in big-endian order,
including unpaired surrogates. `document(uri,text,expectedUtf16Sha256)` and
`update(writer,uri,text,expectedUtf16Sha256)` recompute and compare that seal
before creating a text reader or writing an index. The canonical digest is
lowercase hexadecimal; a mismatch throws `IllegalArgumentException`.
The two-argument document overload computes the seal itself.

Documents contain an exact, unnormalized source-URI StringField; a stored and
indexed UTF-16 source digest; a stored UTF-16 code-unit length; and an unstored
Reader-backed text field. Stored source text is not duplicated. The public field
constants define this additive schema. A returned document is a single-use
reader projection: the caller must index it once or close its text-field reader.
The document and reader retain backing until the reader is closed.

`update` updates by the exact URI term through the caller's existing IndexWriter
and always closes its own reader. The caller owns the writer, analyzer,
directory, transaction/commit/rollback decisions, and concurrency policy. The
adapter never closes or commits the writer. Lucene's own update/failure/segment
semantics remain authoritative. There is no whole-compilation transaction.

`tokenSpans` delegates to existing `M3Tokenizers.simpleSpans`. Its returned OpenNLP
spans are UTF-16 coordinates; the existing SimpleTokenizer configuration and
checkpoint/interruption behavior remain authoritative. Tokenizing does not
change the global singleton configuration. These annotations are distinct from
the caller-selected Lucene analyzer's token positions, offsets, and UTF-8 terms.

File-byte SHA-256, UTF-16-BE source SHA-256, UTF-16 token offsets, Lucene token
positions, and UTF-8 term bytes are distinct domains. No coordinate or seal is
silently converted into another domain.

## Live compiler projection contract

`M3AstNlpSearchConsumer.projection(writer[,checkpoint])` creates an operation key
whose factory accepts only PARSED events. Inside that projection it reads
`Unit.source()` once, tokenizes the existing Source.text(), verifies the provided
UTF-16 seal, and calls the runtime adapter's URI-keyed update. It returns only
detached URI/digest/length and an immutable list of OpenNLP spans. It retains no
Unit, Tree, Trees, Element, TypeMirror, compiler task, or source text afterward.

Consumers that reuse the same Projection instance share this work once within
that event. Equal key names alone do not share it. Each consumer's sink receives
the resulting immutable annotations; sink delivery itself is not memoized.
ANALYZED and finished callbacks do not index or alter the writer.

A tokenization or source-seal failure occurs before index update. If an index
update succeeds and a later sink throws, that pending write remains under the
caller's transaction policy. Existing M3AST dispatch abort/exception behavior
propagates the failure. Source-generation deduplication never crosses events,
compilation tasks, classpaths, processors, or toolchains. The source digest is
not an attributed-semantic cache key.

## Intended qualification

The enclosing ordered gate is PATCH -> LINT -> COMPILE -> TEST -> RUNTIME.
These files do not self-report a pass. Planned checks exercise the actual source
owner, independent byte cursors, sliced ranges, close/mark/reset/bounds, exact
binary output, Unicode split seams with a throwing flatten guard, wrong-seal
refusal, a small actual MAXENT model trained and reloaded from a sliced source
span, persisted-index reopen/update behavior, and the actual M3AST callback
owner when a compatible implementation is supplied. Stock-JDK callback stubs,
model downloads, full upstream suites, matched M3String/native runtime, full JDK
build/jtreg, cross-platform behavior and performance are separate gates.

## Reuse decision

Existing `SourceArtifact.Span`, `M3TextReaders`, `M3Tokenizers`, Lucene Document /
IndexWriter, and M3AST projections are reused unchanged. The missing leaves are
only the byte cursor and source/metadata adapters. No challenge-site or external
implementation text is copied. New adapter expression retains Copyright 2026
Hitesh Soliwal and contributors and Apache-2.0. Existing Apache donor and OpenJDK
rights and notices remain with their original files.
