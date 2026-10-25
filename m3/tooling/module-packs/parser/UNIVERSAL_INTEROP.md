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

**Source coordinate contract:** all published M3AST spans are zero-based,
UTF-16, half-open `[start,end)`. Original UTF-8 byte positions may be
translated at valid codepoint boundaries only; EOF is a valid endpoint.
Provider token-index intervals (ANTLR), inclusive source coordinates and
line/column records must be translated by the adapter using actual original
tokens/source, not guessed by M3AST. Invalid/missing ranges are `(-1,-1)`.

## Open-source design catalogue (conceptual reuse, no donor bodies copied)

| Donor authority | Selected insight | M3AST mapping / remaining gate |
| --- | --- | --- |
| [Tree-sitter](https://tree-sitter.github.io/tree-sitter/using-parsers/2-basic-parsing.html) | Incremental CST; named/anonymous tokens; named child fields; missing and error nodes; source byte ranges | `Surface`, `incomingRole`, `NAMED/TOKEN/MISSING/ERROR` and UTF-8 map. Actual native/JNI grammar adapter and changed-range qualification still pending. |
| [Roslyn red/green](https://github.com/dotnet/roslyn/blob/main/docs/compilers/Design/Red-Green%20Trees.md) | Immutable shared green nodes, lazy positioned red views, full fidelity | Original primitive atoms and optional projected metadata; no second red/green AST. Reuse across source generations still needs a separately proven identity/invalidator. |
| [OpenRewrite LST](https://docs.openrewrite.org/concepts-and-explanations/lossless-semantic-trees) | Full-fidelity trivia, types and recipes; provenance-preserving edits | Borrow an existing LST and record syntax facts; type attribution stays a separate compiler/context sidecar. Actual OpenRewrite ingress and recipe result gates pending. |
| [Eclipse JDT DOM](https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/ASTNode.html) | Structural property descriptors, source ranges and recovery bits | Named edge roles plus original index; preserve JDT binding owner and `ASTRewrite` edits. Existing JDT ingress must be migrated through a contract diff, not reparsed. |
| [ANTLR4](https://www.antlr.org/api/Java/org/antlr/v4/runtime/ParserRuleContext.html) | Rule contexts, alternative numbers, token source intervals and ordered children | Adapter translates original token-inclusive intervals to half-open source positions; direct provider contract missing. |
| [JavaParser](https://github.com/javaparser/javaparser/blob/master/javaparser-core/src/main/java/com/github/javaparser/ast/Node.java) | Token ranges, lexical preservation, comments and user properties | Map only detached facts; foreign lexical printer/edit owner survives. Direct provider contract pending. |
| [LibCST](https://libcst.readthedocs.io/en/latest/metadata.html) | Immutable CST and optional metadata/byte-span providers | Optional sidecar model, source coordinate conversion; no Python runtime in M3JDK21. |
| [Babel ESTree](https://babel.dev/docs/babel-parser) | Dialect-specific node vocabularies and plugin syntax | Explicit provider identity and uninterpreted kind names; no misleading universal semantic node enum. |

## Next compatibility gates, not yet implemented

1. Attach real Tree-sitter (JNI/C11 only if independently justified), ANTLR,
   JavaParser, JDT, OpenRewrite and other parser adapters to their *already
   parsed* trees; compare ranges, roles, errors and token/trivia completeness.
2. Use the original filesystem `com.m3.file.M3File` as the provider observation
   and generation/delta authority, connected to the immutable parser source
   snapshot: reuse metadata
   (file-key/version, mtime, length) as a cheap **change hint**, verify content
   before declaring equality, retain SHA-256 receipts, record precise source
   multi-hunk and class-file deltas and invalidate only affected semantic/ABI
   dependents. The bounded snapshot change envelope above is conservative and
   does not itself complete this integration.
3. Add source-bound optional token/trivia/line/column tables without copying
   source payloads. Require exact reprint/source round-trips before claiming
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
