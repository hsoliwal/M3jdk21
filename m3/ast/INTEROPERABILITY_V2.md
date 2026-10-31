<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3AST parser-neutral indexing and coordinate admission (candidate v2)

This change extends the existing `M3SyntaxTree` *without inventing a second syntax
owner*. The L0-L5 `M3ASTHierarchy` remains the serial, source-receipt-bounded
project/repository evidence plane. The original source image remains canonical.

## Small interface, exact native grammar

- Keep each parser's **own grammar kind**, child **field/role**, error/missing
  flags and half-open source **byte** spans. Do not falsely map foreign grammar
  nodes to a pretend universal Java AST enumeration.
- `M3SyntaxCoordinates.byteAdapter(adapter, nativeOffsetToByte)` lets a provider
  which reports UTF-16/code-point positions use the existing six-method adapter.
  The caller must supply an exact codec. The existing `M3FileSource.byteAtUtf16`
  is an appropriate checked UTF-8/JDT source codec; `CharSequence.length()` or
  an unverified cast is not.
- `M3SyntaxIndex` adds primitive-indexed field/kind queries and O(source bytes +
  syntax occurrences) bottom-up subtree digests. Each byte is digested in one
  owner gap, not once per ancestor. Grammar and node kinds/fields/flags are
  included; absolute offsets are excluded from the subtree digest so that an
  unchanged subtree moved by an earlier edit can be nominated for reuse.
- `M3SyntaxIndex.Facts` is a separately frozen, sparse, provider-namespaced
  metadata lane for types, symbols, diagnostics and other semantic facts. It
  refuses a different syntax **projection SHA-256**; it never labels a syntactic
  equality as semantic or ABI equivalence.

## Open-source concepts and interoperability boundaries

| Parser/AST family | Reused design insight | Deliberate boundary |
| --- | --- | --- |
| Tree-sitter (MIT) | raw byte spans, grammar-native names/fields, error recovery, incremental changed ranges | grammar-specific parser engine stays upstream |
| rust-analyzer/rowan (MIT/Apache-2.0 upstream terms) | compact lossless green core, lightweight red views, typed projections | M3 computes reusable candidate hashes, *not* actual green interned nodes yet |
| Roslyn (MIT) | source-preserving immutable syntax and shareable subtree structure | hash matches are not compiler semantics or guaranteed pointer reuse |
| OpenRewrite (Apache-2.0) | lossless formatting and semantic type attribution | recipe authoring/execution and authoritative bindings stay in Synexia |
| Eclipse JDT (EPL-2.0) | native Java source ranges, recovery and resolved bindings | requires explicit UTF-16-to-byte conversion and binding provider |
| SwiftSyntax (Apache-2.0 with runtime exception) | present/missing and unexpected tokens | no Swift parser implemented |
| Babel/ESTree (MIT) | node kinds, field roles and source-range/token metadata | no AST-spec normalization that discards source fidelity |
| LibCST (MIT) | codemod-oriented lossless syntax and lazy metadata | no Python grammar or codemod executor installed |
| Clang (Apache-2.0 WITH LLVM-exception) | source manager spelling/expansion locations | macro location policy must be provided separately |

No donor implementation source was copied. These are original Java21 candidate
implementations informed by public design documentation; third-party code
admission would require pinned upstream revision, license and NOTICE first.

## Qualification

The test fixture compiles a real public JDK 21 `JavacTask` AST with a Greek
character and supplementary Unicode, translates UTF-16 positions to physical
UTF-8 bytes, retains the same exact source image, and verifies source-sealed
semantic facts. Separate synthetic tests prove 5000-deep iterative indexed
hashing, edited parent invalidation, unchanged-child hash reuse *candidacy*,
and drift refusal. The cross-module source/edit/AST fixture is Synexia-only;
M3AST does not gain a runtime `com.m3.file` dependency.

Actual Tree-sitter/JDT/JavaParser/OpenRewrite provider bridges, green node
hash-consing, incremental parsing, foreign printer round trips, compiler
bindings, package/module ABI, jtreg, installed JDK and memory benchmarks remain
NOT RUN / HOLD. A finite standalone JDK21 proof is not product admission.

Recipe owner: `hsoliwal/com.synexia`, original `m3-collections-contract-refinement-20261017`
family; successor dataset `portable-ast-file-v2-20261026`. Receivers store only
postimages and data-only custody; no recipe source or executor lives here.
