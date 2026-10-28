<!-- SPDX-License-Identifier: Apache-2.0 -->
# Source-sealed syntax navigation in M3AST

`M3SyntaxNavigation` indexes the *existing* `M3SyntaxTree`, retaining only a
primitive `int[]` flat direct-child arena, offsets, counts and exclusive
preorder subtree exits. It does not own source bytes, parser instances,
semantic binding objects, mutable tree nodes or source-edit transformations.

- **Byte position**: `deepestAtByte` and `pathAtByte` are always in the original
  half-open physical source-byte coordinate system; they are not UTF-16 offsets.
  `offset == sourceLength` has no containing node, even for zero-length input.
- **Provider fidelity**: kinds and role names remain the provider's strings.
  Error, recovery, missing and synthetic nodes are not discarded or normalized.
  Missing nodes with zero width are ignored during *byte containment* but remain
  addressable as occurrences and direct children.
- **Preorder**: `subtreeExit(node)` returns the first occurrence ordinal *after*
  the node's entire subtree. `containsNode` is inclusive of the ancestor itself.
- **Complexity**: one-time construction O(nodes), additional primitive memory
  O(nodes), direct-child navigation O(1), `deepestAtByte` O(depth × log(widest
  sibling list)). The prior `M3SyntaxTree.deepestAtByte` remains unchanged as a
  behavior/differential oracle.
- **Source generation**: source and full syntax-projection SHA-256 seals are
  exposed; `requireTree` refuses stale source/grammar/projection use. No claim
  about semantic equivalence, symbol identity, or ABI is made by these hashes.

The index can be built from a real `javac` AST after projection via
`M3JavacProjection`, from Tree-sitter/JDT/Clang via the existing six-method
`M3SyntaxTree.ForeignNode` or from an externally sourced syntax snapshot via
`M3SyntaxSnapshot`. It introduces no new universal node hierarchy and no new
runtime dependency on the existing `M3File` module. Source generation checks
against `M3FileSource.sha256()` are exercised in Synexia-only Java21 fixtures.

Provenance: original Apache-2.0 implementation in Synexia. General navigation
concepts are comparative inspiration from rust-analyzer/rowan, Roslyn,
Tree-sitter and Java's `jdk.compiler`; no upstream implementation code copied.

Qualification gates: source-sealed recipe execution, exact Java21 compiler and
JUnit checks, original M3File host/jlink, installed-JDK/JTReg, JNI and
performance/heap measurements remain separately tracked. The standalone
fixture is useful evidence but cannot replace hosted SDK and product gates.
