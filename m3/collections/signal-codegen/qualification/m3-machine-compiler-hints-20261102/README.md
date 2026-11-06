# M3JDK21 machine-oriented pattern-review hints (v1)

This is a native product receiver inside the existing M3JDK21 Java 21 compiler-signal module. It does not add a wrapper around the JDK, a second source parser, or another compiler/runtime. The existing `SortableProcessor` still manufactures the exact `M3CompilerFacts` source-time coordinates. The additive `M3MachineHintRules` reads those fixed primitive lanes and returns only tri-state candidate review masks.

`UNKNOWN` never means zero or no pattern. `CANDIDATE` is an evidence-backed review hint for the existing Synexia M3AST/Patternizer, never an authorization to rewrite bytecode/source, change Java semantics, assume sorted posting geometry, invoke JNI or lower HotSpot IR. Compiler-produced facts are source-time data; all runtime optimization proofs remain separate. The public `@Sortable`, `SortableProcessor`, `M3CompilerFacts`, JNI exports and previously sealed source files are unchanged.

Seven bit positions: FIELD_METHOD_RELATION, INTERFACE_INVOCATION, ITERATIVE_INVOCATION, ALLOCATION_FACTORY_REVIEW, CONDITIONAL_CONTROL, SYNCHRONIZATION, LOOP_BRANCH_REVIEW. A category is known only if all required source/AST coordinates are known; only positive source counts make it a candidate. Two primitive `long` masks are sufficient, and no per-mapping entry objects are retained.

Synexia canonical installer: `com.synexia.m3jdk.MachineCompilerHintReview20261102` / `M3HashPinnedJavaSnapshotRecipe`. The two product Java additions must match the exact Synexia postimage Git blobs and SHA-256; both are absent preimages. Java 21 strict compilation and a deterministic independent oracle are required before receiver promotion. No full JDK, JIT, native or GoF semantic proof is claimed by this document.
