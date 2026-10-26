<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3AST universal ingress: small contract, authoritative parser

M3JDK21 owns **one** `M3ASTIndex` occurrence projection per admitted source
generation. It does not contain another parser, semantic binder, editable AST,
or foreign node graph. A provider's existing AST remains authoritative; one
bounded read-only visit deposits detached facts into the original eight
primitive lanes. There is no Synexia runtime wrapper.

## First qualified Java 21 slice

`M3ASTInterop.Surface<N>` requires only `kind`, `childCount` and `child`.
Optional methods supply original UTF-16 or UTF-8 byte ranges, a role on each
incoming child edge, syntax flags, names, literals and modifiers. Nodes are
**occurrences**: reused foreign objects encountered twice produce two rows.
`M3ASTIndex.Domain.GENERIC` plus a bounded, explicit grammar/provider identity
keeps unlike provider vocabularies distinguishable; original domains and
their hashes/flags remain unchanged. Optional primitive role-ID storage is
allocated only when a provider supplies an edge role. `M3ASTRelations`
continues to use the original parent/child/sibling rows and structural
hashes, without an alternate tree.

`SourceStamp` comes from the **original parser's source generation**, not a
reconstruction from the candidate M3File. A null stamp leaves all imported
nodes `SOURCE_UNVERIFIED` and never marks ranges exact. A claimed stamp is
checked against URI and exact UTF-16 digest; byte-coordinate certification
also requires the parser's original raw-byte SHA-256. This is a caller-supplied
provenance statement, not a cryptographic attestation of an arbitrary parser.
Unrelated source, unknown ranges, mid-codepoint positions, negative children,
excess depth/nodes and non-UTF-8 byte-coordinate sources are refused. A
provider must freeze its tree for traversal; cycles are bounded by depth.

`jdk.m3.parser.M3File` retains one `String`/M3String text owner and both
existing source identities. A strict original UTF-8 `read` can now lazily
provide `Utf8Offsets`, an exact byte-boundary-to-UTF-16 mapper; unlike a
byte-sized lookup table, it stores **two int arrays only for non-ASCII code
points**. ASCII-only files use empty arrays. Read-only `of(uri, CharSequence)`
and other character sets do not fabricate original UTF-8 byte offsets.
Neither input bytes nor an additional character payload is retained.

`M3File.changeSince(previous, maxComparedUtf16Units)` now identifies a
conservative single-replacement *source-text envelope* between immutable
snapshots with the same URI. Only a shared prefix/suffix is excluded, and no
edit boundary splits a valid surrogate pair. An optional original-byte
SHA-256 comparison reports `SHA256_SAME`, `SHA256_DIFFERENT` or `UNAVAILABLE`
independently of text changes. Same decoded source with different raw bytes
**does not certify** parser or compiler input-generation reuse. This operation
does not touch the provider filesystem, create a second AST, detect disjoint
minimal hunks, determine semantic/ABI changes or update persisted build state.

## Additive parser-role and source-line reconciliation

`Surface.childRolePosition(parent, childOrdinal)` now optionally identifies the
**zero-based position inside a named list-valued field**, independently of the
ordinal across all of the parent's children. Its default `-1` preserves the
previous contract for scalar or unknown edges. The original `M3ASTIndex`
retains these positions in one lazily allocated primitive `int[]` lane,
without an alternative node graph or a retained adapter object. A position
requires a named incoming role in the GENERIC domain; the root and original
domains reject list positions. The lane is absent unless a provider supplies
at least one nonnegative position, so ordinary V1 and GENERIC V2 indexes keep
their exact hash schemas. Positioned GENERIC indexes use an explicit V3
structural schema, incorporating the position of each child edge. Position
alone is not a type, binding, grammar-equivalence or lossless-token claim.

`M3File.lineIndex(maxLines)` now lazily caches one source-bound `int[]` of
UTF-16 line starts. It treats CR, LF and CRLF as line terminators and counts
a trailing empty line. A line is one-based, a column is zero-based UTF-16;
`positionAt(offset)` and `offsetAt(line,column)` are exact inverses for
valid offsets, including EOF and offsets inside CRLF (both units belong to
the preceding line). The max-line budget applies before construction and
when accessing a previously cached map. The existing javac AST ingress now
uses this same M3File-owned index instead of allocating its own `SourceLines`
table. Parser-native row/column conventions still require a provider-specific
mapping; these coordinates are not silently equated with UTF-8 bytes.

## Source full spans and strict original-byte change geometry

Foreign “syntax core” and “full source span” are distinct concepts. Roslyn
exposes `Span` without leading/trailing trivia and `FullSpan` including it;
Eclipse JDT can supply extended source ranges, while OpenRewrite preserves
formatting through its LST owner. `Surface.fullStart/fullEnd` now optionally
projects the original parser's **trivia-inclusive envelope** in the same
original coordinate unit as the core range. The original M3ASTIndex retains
it only when supplied, in two lazily allocated primitive `int[]` lanes.
Core text remains available through `sourceText`, certified full text through
`fullSourceText`; neither constructs a second syntax tree. Ranges are
half-open UTF-16 after strict original UTF-8 conversion. A full envelope
must contain its known syntax core, stay nested within any known parent full
envelope and never be certified when source provenance is unverified.
These **geometry** facts are excluded from structural SHA, so V1/V2/V3
signatures remain unchanged. Retaining a provider's span does not make an
incomplete AST a lossless CST; token/trivia and reprint oracles remain needed.

`M3File.utf8ChangeSince(previous, maxComparedUtf16Units)` translates the
existing conservative surrogate-safe single-edit source envelope into the
**exact original UTF-8 byte offsets** of both source generations, requiring
strict-UTF-8 source admission on both sides. It retains no additional byte
payload, and reports the already-separate raw SHA-256 comparison. The output
is a byte interval, not yet a parser mutation or cache-reuse decision.

`Utf8Offsets.pointAt(byteOffset,maxLines)` and `byteAt(Utf8Point,maxLines)`
now provide **zero-based LF-only rows and original UTF-8 byte columns**,
separately from the shared Java-style CR/LF/CRLF UTF-16 line map. A lone CR
counts toward the byte column, while only LF creates a new row. The first
request lazily creates one bounded `int[]` of LF byte starts, retained by the
existing immutable UTF-8 offset owner; EOF is a legal point and partial
multibyte offsets are refused. A cached index still honors smaller budgets.
`M3File.utf8InputEditSince(previous,maxUnits,maxLines)` combines original
strict source-byte offsets and the **three start/old-end/new-end points**
required by Tree-sitter's `TSInputEdit` geometry. It carries raw SHA receipt
status, but **does not invoke Tree-sitter's `ts_tree_edit`, `ts_parser_parse`,
or `ts_tree_get_changed_ranges`**, nor certify that a previously indexed AST
is valid after the edit. A provider must own those native/parser operations.

## Unicode scalar offsets for ANTLR and other codepoint frontends

ANTLR 4.7+ `CharStreams` uses `CodePointCharStream` by default: its token source
indexes count Unicode codepoints, **not Java UTF-16 code units**. Treating
ANTLR `Token.getStartIndex()` as a Java offset would misplace every source
range after a supplementary character. The source-owned, lazy
`M3File.codePointOffsets()` therefore maintains just **one sparse `int[]` of
supplementary-character scalar starts**. `toUtf16` and `toCodePoint` are exact,
bounded inverses at scalar boundaries; inside-surrogate offsets and malformed
UTF-16 sources are refused. BMP-only sources allocate no scalar entries.
No byte buffer, new parser or secondary string is retained.

`M3ASTInterop.Coordinates.CODE_POINTS` now converts the original provider's
core and optional full ranges into the **same canonical UTF-16 half-open
M3ASTIndex lanes**. A matching original URI and UTF-16 source stamp can
certify text-based scalar ranges; unlike original UTF-8 byte coordinates,
this does not require a raw byte SHA when no byte claim was made.
ANTLR's inclusive **token** stop index must still be converted to an exclusive
codepoint end by the adapter, before projection; M3AST never guesses which
token stream or coordinate convention a provider used.

The standalone Synexia-side `M3ASTAntlr4RuntimeContract` was compiled and run
against actual `antlr4-runtime:4.11.1` `CharStreams`, `CommonToken`,
`ParserRuleContext`, and `TerminalNode` objects. It checks exact UTF-16 source
ranges for `A😺中B` plus newline while ANTLR returns five scalar token
positions. **It is a runtime-object mapping fixture, not a generated-grammar
parser, hidden-channel/trivia or native lexer proof**; those remain pending.

## One physical observation, one immutable parser source generation

`com.m3.file.M3File` already owns the original provider's bounded, checked
`observe(maxBytes,progress,ByteObserver)`: SHA-256 digest words, raw byte count,
checked modification time, file-key and provider metadata change refusal. A
parser that reopens the same path to build its own source `String` would lose
that single-observation provenance and repeat I/O. The parser-owned
`jdk.m3.parser.M3File.captureObservedUtf8(uri,maxBytes)` now provides a
transient, strictly decoding `Utf8Capture` for the **existing byte observer**:

```java
jdk.m3.parser.M3File.Utf8Capture capture =
        jdk.m3.parser.M3File.captureObservedUtf8(originalPath.toUri(), maxBytes);
com.m3.file.M3File.Observation observation = physical.observe(
        maxBytes, progress, capture::onBytes);
com.m3.file.M3File.Fingerprint raw = observation.fingerprint();
jdk.m3.parser.M3File source = capture.seal(raw.size(), raw.digest0(),
        raw.digest1(), raw.digest2(), raw.digest3());
```

No compiler/parser module requires the other as a runtime dependency. The
physical owner reads/hashes/validates **once**; the borrowed callback decodes
the same bytes into the one eventual Java String with standard strict JDK UTF-8
decoding. Only a 4-byte unfinished-scalar scratch, a reused 4096-char decoder
buffer and a temporary StringBuilder are used; the returned source releases
the temporary builder reference, retaining neither a second raw byte array
nor a physical handle. Byte-chunk boundaries may split multibyte Unicode
characters; incomplete, malformed or oversized sources refuse admission.
`seal` is one-shot, checks the observer's byte count and attaches the original
SHA-256 words **without rehashing**. The caller must supply the exact checked
physical result, after successful observation: a malicious or unrelated
four-word digest cannot be cryptographically authenticated without hashing.

An actual product-to-product Synexia fixture compiles and runs the existing
filesystem `M3File`, `M3ShadowStreams`, progress interface and the parser
module's `M3File` together, including unchanged-snapshot-after-file-mutation
and malformed-source refusal. It passes. A seeded 1,800-byte-sequence oracle
and 300 Unicode-text samples fragmented into five chunk patterns compare
`Utf8Capture` with the original strict JDK decoder; the Java21 interoperability
count advances to **125,825** checks. This remains a **one-observation UTF-8
admission**, not atomic filesystem snapshot isolation or a complete native
incremental reparse/ABI/cached reactor.

**Source coordinate contract:** all published M3AST spans are zero-based,
UTF-16, half-open `[start,end)`. Original UTF-8 byte positions may be
translated at valid byte/codepoint boundaries only. Original codepoint spans
are also mapped exactly, including EOF, with surrogate-safe conversion.
Provider token-index intervals (ANTLR), inclusive source coordinates and
line/column records must be translated by the adapter using actual original
tokens/source, not guessed by M3AST. Invalid/missing ranges are `(-1,-1)`.

## Open-source design catalogue (conceptual reuse, no donor bodies copied)

| Donor authority | Selected insight | M3AST mapping / remaining gate |
| --- | --- | --- |
| [Tree-sitter](https://tree-sitter.github.io/tree-sitter/using-parsers/2-basic-parsing.html) | Incremental CST; named/anonymous tokens; named child fields; missing and error nodes; source byte ranges and LF-based byte edit points | `Surface`, `incomingRole`, list-position lane, `NAMED/TOKEN/MISSING/ERROR`, UTF-8 map and exact three-point edit geometry. Actual native/JNI grammar adapter and changed-range qualification still pending. |
| [Roslyn red/green](https://github.com/dotnet/roslyn/blob/main/docs/compilers/Design/Red-Green%20Trees.md) | Immutable shared green nodes, lazy positioned red views, separate `Span` and trivia-inclusive `FullSpan` | Optional full-span lanes and original primitive atoms; no second red/green AST. Reuse across source generations still needs a separately proven identity/invalidator. |
| [OpenRewrite LST](https://docs.openrewrite.org/concepts-and-explanations/lossless-semantic-trees) | Full-fidelity trivia, types and recipes; provenance-preserving edits | Optional full-span source projection; original LST retains formatting and type attribution authority. Actual OpenRewrite provider and exact reprint gates pending. |
| [Eclipse JDT DOM](https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/ASTNode.html) | Structural property descriptors, scalar/list child slots, normal/extended source ranges and recovery bits | Named/list edges and optional full ranges; preserve JDT bindings and `ASTRewrite`. Existing JDT ingress must be migrated through a contract diff, not reparsed. |
| [ANTLR4](https://github.com/antlr/antlr4/blob/dev/doc/unicode.md) | CodePointCharStream scalar indexes; rule contexts, alternative numbers, inclusive token intervals and ordered children | Single sparse surrogate lane plus `CODE_POINTS` original-range conversion; actual runtime-object fixture PASS. Generated grammar, recovery, trivia and original parser-generation gates pending. |
| [JavaParser](https://github.com/javaparser/javaparser/blob/master/javaparser-core/src/main/java/com/github/javaparser/ast/Node.java) | Token ranges, lexical preservation, comments and user properties | Map only detached facts; foreign lexical printer/edit owner survives. Direct provider contract pending. |
| [LibCST](https://libcst.readthedocs.io/en/latest/metadata.html) | Immutable CST and optional metadata/byte-span providers | Optional sidecar model, source coordinate conversion; no Python runtime in M3JDK21. |
| [Babel ESTree](https://babel.dev/docs/babel-parser) | Dialect-specific node vocabularies and plugin syntax | Explicit provider identity and uninterpreted kind names; no misleading universal semantic node enum. |

## Next compatibility gates, not yet implemented

1. Attach real Tree-sitter (JNI/C11 only if independently justified), ANTLR
   **generated grammar**, JavaParser, JDT, OpenRewrite and other parser
   adapters to their *already parsed* trees; compare ranges, roles, errors
   and token/trivia completeness. The ANTLR runtime-object fixture above
   demonstrates its source-coordinate bridge, not complete grammar coverage.
2. Extend the now-tested **one physical observation → one immutable parser
   snapshot** bridge into build generations and persistent delta state: reuse
   original provider metadata (file-key/version, mtime, length) as a cheap
   **change hint**, verify content before declaring equality, retain SHA-256
   receipts, record precise source multi-hunk and class-file deltas and
   invalidate only affected semantic/ABI dependents. Neither the one-pass
   UTF-8 receiver nor the bounded snapshot envelope alone completes this
   incremental reactor integration.
3. Add source-bound optional token/trivia tables and other provider-specific
   scalar/display-column coordinate bridges without copying source payloads.
   Both Java-style UTF-16 and LF-only UTF-8 line maps are now present; they are not substitutes for
   provider fidelity. Require exact reprint/source round-trips before claiming
   a provider is lossless; an AST that omits whitespace never becomes a CST
   merely because M3File holds its original text.
4. Keep compiler semantic symbols/type bindings and OpenRewrite/JDT rewrite
   plans on their authoritative owner. Reuse leaf hashes to discover equal
   atoms, then independently verify exact retained facts before sharing.
5. Compare memory, throughput and native lifetime behavior against each
   donor's own baseline before adding JNI batching, persistent SQLite facts,
   cross-file green-atom sharing or automatic incremental reparsing.

All generated source refinements must be received from hash-pinned Synexia
Maven/OpenRewrite recipes, verified to a second-run fixed point, then run
against the full parser JMOD, installed M3JDK21 and native/runtime gates.
This first contract does **not** constitute those broader admissions.
