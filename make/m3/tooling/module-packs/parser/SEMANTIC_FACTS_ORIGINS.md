<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3AST parser-neutral typed facts and multi-source provenance

The original immutable M3ASTIndex remains the ONE structural syntax owner.
These optional immutable primitive sidecars do not reconstruct a parser AST,
retain parser nodes, copy source text, or introduce another AST hierarchy.

## Typed facts (OpenRewrite, Roslyn, ANTLR, javac, JDT)

M3ASTFacts is a per-occurrence, source-generation-bound sparse CSR catalogue.
Typed lanes retain signed longs, exact IEEE-754 double raw bits, booleans,
string dictionary references, and node-to-node relationships. Keys and text
are bounded and interned within the optional immutable sidecar, with repeated
keys allowed for repeated fields or annotations. Empty nodes allocate no
per-node object. No fact entry establishes external symbol/type correctness;
the originating parser/compiler must prove semantic attribution.

## Multi-source origins (Clang, generated code, macro provenance)

M3ASTSourceOrigins attaches one or more role-labelled spelling, expansion,
and generated source ranges to existing occurrence rows. Each role borrows
an original immutable M3File snapshot from a bounded per-index source list.
Source generations are distinct even with identical URI; coordinates are
half-open UTF-16 units relative to that source. Byte positions must be
translated through the existing verified M3Utf8SourceMap first. Origin labels
are provenance claims from the owning parser, not independent proof of input.
Neither this sidecar nor the fact catalogue clears SOURCE_UNVERIFIED.

## Donor concepts, no donor source copied

- Tree-sitter: concrete syntax, named and missing tokens, field roles and byte ranges.
- Roslyn and rust-analyzer/rowan: green immutable storage, red/typed views.
- OpenRewrite: typed semantic metadata, markers and source-preserving LST.
- Clang SourceManager: distinct spelling and expansion source locations.
- ANTLR: rule, terminal, error and repeated child attributes.
- SwiftSyntax and LibCST: trivia, formatting and source metadata fidelity.

## Deliberate pending gates

Real parser adapters, source-generation attestation, incremental green DAG
reuse, typed cross-language symbol resolution, macro expansion correctness,
JMOD/HotSpot/JNI regressions, memory/throughput benchmarks and full build
admission remain separate gates. A structural match is never semantic proof.
