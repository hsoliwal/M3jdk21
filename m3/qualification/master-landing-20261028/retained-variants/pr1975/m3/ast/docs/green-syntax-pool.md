<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3AST position-free green syntax plane — source-sealed candidate

`M3SyntaxGreenPool` adds an optional immutable-content intern arena to the existing
`M3SyntaxTree` primitive occurrence plane and `M3SyntaxIndex` grammar-aware hashes.
There is no new parser engine, retained per-occurrence Java object, AST type
hierarchy, independent file payload owner or public JDK replacement.

- A green stores a grammar ID, parser-native kind/field, syntax flags, content
  fingerprint, child green handles and ordered node-local literal gaps.
  Positions and parent pointers remain only in the existing M3SyntaxTree occurrences.
- A pool interns a green by the existing subtree SHA-256 as an index candidate,
  **then verifies exact local bytes, child handles and labels** before reuse.
  SHA-256 equality alone is not accepted as proof of byte equality or semantics.
- A source version whose child shifts after an earlier edit reuses the child's
  green identity. Only structurally changed ancestors require new entries.
- Full-fidelity `render(green,maxBytes)` is iterative, reconstructing exact
  original bytes including comments, malformed syntax gaps, missing zero-width
  nodes and source not explicitly represented by a foreign AST.
- Primitive columns and flattened child/gap/literal arenas avoid a per-node
  object graph. Caller-provided entry and literal budgets fail closed.
- The existing `M3SyntaxDelta` maps independently reparsed occurrences. A
  *shared* green ID is structural reuse within one pool, **not** a binding,
  ABI, compilation, execution or type equivalence proof.

### Parser boundaries

Tree-sitter, Roslyn, rowan/rust-analyzer, SwiftSyntax, JDT, JavaParser, javac,
OpenRewrite, Clang and compiler-specific trees can use the six-method
`M3SyntaxTree.ForeignNode` bridge when their child ranges can be represented
as a nested source occurrence hierarchy. Noncontiguous macro expansions and
embedded-language origins use the existing `M3SyntaxOrigins` sidecar; do not
flatten these into false contiguous child spans. Foreign parsers remain
responsible for building typed syntax and semantic bindings.

### Admission state

Source template family: `hsoliwal/com.synexia` canonical
`portable-ast-file-v4-20261026` dataset in the existing OpenRewrite recipe crate.
JDK 21 standalone `javac --release 21 -Xlint:all -Werror`, direct green
contract, and in-memory real-javac -> source-edit -> reparse contract passed.
Maven/JUnit, SDK recipe Results, exact-head CI, native installed JDK/jtreg,
resource benchmarks, and real external parser adapters are **NOT RUN / HOLD**.
These limits prohibit claiming a production-admitted universal AST backend.
