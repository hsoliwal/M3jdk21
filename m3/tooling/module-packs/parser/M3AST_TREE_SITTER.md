# Direct Tree-sitter CST ingress for M3AST (Java 21)

The integrated M3JDK21 parser module's **existing** `M3ASTIndex` is the only
detached primitive syntax representation. `M3ASTInterop.treeSitter(...)` adds a
direct, opt-in projection from an original Tree-sitter node API to that index.
The parser, grammar and native tree retain source-of-truth ownership; there is
no intermediate AST, language-specific node schema, runtime Tree-sitter
dependency, persistent JNI handle or source-text duplication.

## Contract

`M3ASTInterop.TreeSitterNodes<N>` supplies exactly the node's original type,
all-child count/ordinal, byte start/end, `isNamed`, `isMissing`, `isExtra`,
`isError`, `hasError` and optionally `fieldNameForChild`. The caller provides
the original `M3File` UTF-8 **byte snapshot** and an explicit, versioned
grammar identity. The optional `SourceStamp` (URI, UTF-16 digest, raw-byte
digest) is asserted by the original parser owner; it must describe the exact
bytes supplied to Tree-sitter. A missing raw digest cannot certify any range.

The ingress returns `Domain.GENERIC` with source-range/occurrence links in
UTF-16 and the unmodified grammar types as kinds. It keeps **all** children,
including anonymous punctuation, rather than flattening to named children.
Each incoming edge retains its original grammar field (or null). Tree-sitter
field repetition does **not** certify list membership; no list ordinal is
invented and `incomingRolePosition()` remains -1. An anonymous CST terminal
may have `TOKEN` without `NAMED`. `TRIVIA` specifically denotes `isExtra`,
**not** arbitrary whitespace or lossless trivia completeness. `MISSING` and
`ERROR` are distinct; `RECOVERED` includes an error-bearing ancestor via
`hasError`. The generic index never makes semantic/type/binding claims.

As a test-only adapter example, a provider that actually owns `TSNode` can
delegate `type` to `getType()`, `child` to `getChild(i)`, field to
`getFieldNameForChild(i)`, start/end to `getStartByte()/getEndByte()` and flags
to `isNamed()/isMissing()/isExtra()/isError()/hasError()`. The supplier must
ensure grammar ABI compatibility, snapshot exactness, stable traversal and
native tree lifetime through the synchronous projection. No adapter instance
or TSNode pointer is stored in `M3ASTIndex`.

## Incremental source integration

The existing `M3File.utf8InputEditScriptSince(...)` produces a bounded ordered
series of byte/point edits on immutable snapshots. The Tree-sitter owner can
apply each edit to its *old* native tree, reparse the changed source, then
project the **new** root and stamp. The index must not reuse old occurrence
IDs, old provider node handles or stale source receipts across generations.
`ts_tree_edit` and native reparse remain the parser's work; M3AST does not
reimplement the grammar or its incremental engine.

## Proof and non-claims

Two independent source-sealed tests accompany this projection:

- Java 21 `M3ASTTreeSitterContract`: pure-JDK fake-node oracle for punctuation,
  grammar fields, UTF-8 supplementary code points, exact/unverified stamps,
  missing/extra/containing-error flags, budgets and hostile byte boundaries.
- Private `RealTreeSitterM3ASTContract`: **real** Tree-sitter JNI 0.25.3 and
  JSON grammar 0.24.8. It compares each projected row with each actual native
  `TSNode`, tests inserted `MISSING` and `ERROR`, and compares M3AST projections
  from genuinely incremental and cold parsers. The binary grammar/binding are
  qualified **only in private tests**, not shipped in the product SDK.

Future donor qualification should add parser-backed JDT, javac, ANTLR,
OpenRewrite, Roslyn, protobuf and alternate Tree-sitter grammar samples using
their authoritative public node/field APIs. Do not infer a universal semantic
type hierarchy from their syntactic similarities. No claim is made here of
complete parser equivalence, lossless CST round-tripping, semantic ABI safety,
all grammars/platforms, JNI acceleration or benchmark superiority.

Source inspirations, adapted without upstream expressive code:

- Tree-sitter C API and query/field model (MIT):
  https://tree-sitter.github.io/tree-sitter/using-parsers/2-basic-parsing.html
- Tree-sitter incremental editing (MIT):
  https://tree-sitter.github.io/tree-sitter/using-parsers/3-advanced-parsing.html
- Java Tree-sitter bindings (MIT), **private test only**:
  https://github.com/tree-sitter/java-tree-sitter
- Tree-sitter JSON grammar (MIT), **private test only**:
  https://github.com/tree-sitter/tree-sitter-json

The canonical Maven/OpenRewrite recipe and provenance belong in private
Synexia. The product carries only its **exact generated postimages**; the
full receiving JDK's normal file/rewrite/compiler/JNI gates remain mandatory.
