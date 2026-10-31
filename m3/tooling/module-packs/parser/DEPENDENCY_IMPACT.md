<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3AST dependency impact from original source generations

M3ASTDependencyImpact uses the ONE existing M3ASTIndex plus optional
M3ASTFacts and M3ASTSourceOrigins. The caller supplies a BitSet of edited
occurrence rows, optionally composed from M3FileEdits.Plan.touchedNodes
and touchedOrigins for a changed original M3File generation.
Foreign source binding is never silently certified.

An O(nodes+facts) pass builds primitive reverse NODE_REF edges and
propagates edit impact transitively to referencing nodes and parents.
Unknown source ranges are dirty by default; cycles and duplicate references
are visited only once. Four byte reason bits distinguish DIRECT,
UNKNOWN_RANGE, ANCESTOR, and REFERENCER with schema
M3_AST_DEPENDENCY_IMPACT_V1. There are no second AST trees,
Entry objects, retained parser nodes or second source buffers.

All NODE_REF facts are interpreted as possible dependencies. False-positive
invalidations are intentional. An unchanged candidate is NOT a proof of
semantic equivalence, parser cache validity or permission to skip Maven,
Gradle, JUnit, processors, javac or compatibility testing.

FOSS design concepts (no donor code copied): Tree-sitter changed-ranges;
Roslyn incremental source generations; rust-analyzer/rowan lightweight
syntax projections; OpenRewrite type-attributed LST; Clang source spelling
and macro expansion origins; LibCST whitespace-preserving source facts.

The integrated M3JDK21 is the target SDK, not an M3-on-JDK wrapper.
The native M3FS, parser JMOD, HotSpot/JNI, external parser adapters,
cross-module ABI proofs and performance benchmarks are separate gates.
