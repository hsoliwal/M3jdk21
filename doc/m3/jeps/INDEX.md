<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21 Cross-Language Enhancement Proposals

These are **internal M3JEP drafts, not official OpenJDK JEPs or JEP numbers**. They do not claim implementation or inclusion in a Java release. Existing Java 21 meaning is preserved.

Source of truth: 24 original + 46 expansion `LANG-` candidates and 16 cross-cutting proposals. All 86 proposals have a unique stable ID; implementation is `HOLD`.

| M3 proposal | Capability | Receiving stage | State |
| --- | --- | --- | --- |
| [`M3JEP-CORE-001`](proposals/M3JEP-CORE-001.md) | Unsigned and wide integer primitive types | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-CORE-002`](proposals/M3JEP-CORE-002.md) | Extended binary floating-point primitives | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-CORE-003`](proposals/M3JEP-CORE-003.md) | Decimal and fixed-point primitive types | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-CORE-004`](proposals/M3JEP-CORE-004.md) | const primitive facts and compile-time evaluation | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-CORE-005`](proposals/M3JEP-CORE-005.md) | Read-only JNI const arrays and transitive aliases | `ARRAYS` | `DRAFT_HOLD` |
| [`M3JEP-CORE-006`](proposals/M3JEP-CORE-006.md) | Canonical M3String and composable immutable structures | `STRING` | `DRAFT_HOLD` |
| [`M3JEP-CORE-007`](proposals/M3JEP-CORE-007.md) | Dynamic primitive arrays, indexes, offsets and short masks | `ARRAYS` | `DRAFT_HOLD` |
| [`M3JEP-CORE-008`](proposals/M3JEP-CORE-008.md) | Versioned M3AST, M3Class and qualified identifiers | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-CORE-009`](proposals/M3JEP-CORE-009.md) | Compile-time annotations, markup and metadata facts | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-CORE-010`](proposals/M3JEP-CORE-010.md) | Pluggable compiler/interpreter/transpiler/atomizer/patternizer | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-CORE-011`](proposals/M3JEP-CORE-011.md) | Compatible interface/default/abstract extraction | `REMAINING` | `DRAFT_HOLD` |
| [`M3JEP-CORE-012`](proposals/M3JEP-CORE-012.md) | Packed primitives and typed ID, offset and slice lanes | `ARRAYS` | `DRAFT_HOLD` |
| [`M3JEP-CORE-013`](proposals/M3JEP-CORE-013.md) | Static regex and known-schema precomputation | `STRING` | `DRAFT_HOLD` |
| [`M3JEP-CORE-014`](proposals/M3JEP-CORE-014.md) | Universal Maven/m2e language plugin backplane | `REMAINING` | `DRAFT_HOLD` |
| [`M3JEP-CORE-015`](proposals/M3JEP-CORE-015.md) | OSGi and JMOD-compatible module convergence | `REMAINING` | `DRAFT_HOLD` |
| [`M3JEP-CORE-016`](proposals/M3JEP-CORE-016.md) | Deferred object-oriented parent-owned data | `ARRAYS` | `DRAFT_HOLD` |
| [`M3JEP-LANG-001`](proposals/M3JEP-LANG-001.md) | Operators | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-002`](proposals/M3JEP-LANG-002.md) | Indexing | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-003`](proposals/M3JEP-LANG-003.md) | Callable | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-004`](proposals/M3JEP-LANG-004.md) | Containment | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-005`](proposals/M3JEP-LANG-005.md) | Comparison | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-006`](proposals/M3JEP-LANG-006.md) | Adaptation | `REMAINING` | `DRAFT_HOLD` |
| [`M3JEP-LANG-007`](proposals/M3JEP-LANG-007.md) | Interfaces | `REMAINING` | `DRAFT_HOLD` |
| [`M3JEP-LANG-008`](proposals/M3JEP-LANG-008.md) | Properties | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-009`](proposals/M3JEP-LANG-009.md) | Extensions | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-010`](proposals/M3JEP-LANG-010.md) | Nullability | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-011`](proposals/M3JEP-LANG-011.md) | Deconstruction | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-012`](proposals/M3JEP-LANG-012.md) | Algebraic Types | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-013`](proposals/M3JEP-LANG-013.md) | Traits | `REMAINING` | `DRAFT_HOLD` |
| [`M3JEP-LANG-014`](proposals/M3JEP-LANG-014.md) | Data Classes | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-015`](proposals/M3JEP-LANG-015.md) | Value Types | `REMAINING` | `DRAFT_HOLD` |
| [`M3JEP-LANG-016`](proposals/M3JEP-LANG-016.md) | Coroutines | `REMAINING` | `DRAFT_HOLD` |
| [`M3JEP-LANG-017`](proposals/M3JEP-LANG-017.md) | Collections | `COLLECTIONS` | `DRAFT_HOLD` |
| [`M3JEP-LANG-018`](proposals/M3JEP-LANG-018.md) | Pattern Matching | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-019`](proposals/M3JEP-LANG-019.md) | Generics | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-020`](proposals/M3JEP-LANG-020.md) | Macros | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-021`](proposals/M3JEP-LANG-021.md) | Compile Time | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-022`](proposals/M3JEP-LANG-022.md) | Functional | `REMAINING` | `DRAFT_HOLD` |
| [`M3JEP-LANG-023`](proposals/M3JEP-LANG-023.md) | Strings | `STRING` | `DRAFT_HOLD` |
| [`M3JEP-LANG-024`](proposals/M3JEP-LANG-024.md) | Resources | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-025`](proposals/M3JEP-LANG-025.md) | Delegation | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-026`](proposals/M3JEP-LANG-026.md) | Dsl | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-027`](proposals/M3JEP-LANG-027.md) | Inline Calls | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-028`](proposals/M3JEP-LANG-028.md) | Cold Streams | `REMAINING` | `DRAFT_HOLD` |
| [`M3JEP-LANG-029`](proposals/M3JEP-LANG-029.md) | Type Classes | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-030`](proposals/M3JEP-LANG-030.md) | For Comprehension | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-031`](proposals/M3JEP-LANG-031.md) | Higher Kinded | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-032`](proposals/M3JEP-LANG-032.md) | Opaque Types | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-033`](proposals/M3JEP-LANG-033.md) | Query Composition | `COLLECTIONS` | `DRAFT_HOLD` |
| [`M3JEP-LANG-034`](proposals/M3JEP-LANG-034.md) | Span Views | `ARRAYS` | `DRAFT_HOLD` |
| [`M3JEP-LANG-035`](proposals/M3JEP-LANG-035.md) | Async Await | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-036`](proposals/M3JEP-LANG-036.md) | Pattern Properties | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-037`](proposals/M3JEP-LANG-037.md) | Nullable Values | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-038`](proposals/M3JEP-LANG-038.md) | Result Types | `REMAINING` | `DRAFT_HOLD` |
| [`M3JEP-LANG-039`](proposals/M3JEP-LANG-039.md) | Borrowed Views | `ARRAYS` | `DRAFT_HOLD` |
| [`M3JEP-LANG-040`](proposals/M3JEP-LANG-040.md) | Iterator Adaptors | `COLLECTIONS` | `DRAFT_HOLD` |
| [`M3JEP-LANG-041`](proposals/M3JEP-LANG-041.md) | Associated Types | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-042`](proposals/M3JEP-LANG-042.md) | Rust Async | `REMAINING` | `DRAFT_HOLD` |
| [`M3JEP-LANG-043`](proposals/M3JEP-LANG-043.md) | Ranges | `ARRAYS` | `DRAFT_HOLD` |
| [`M3JEP-LANG-044`](proposals/M3JEP-LANG-044.md) | Constraints | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-045`](proposals/M3JEP-LANG-045.md) | Simd | `ARRAYS` | `DRAFT_HOLD` |
| [`M3JEP-LANG-046`](proposals/M3JEP-LANG-046.md) | Consteval | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-047`](proposals/M3JEP-LANG-047.md) | Protocol Extensions | `REMAINING` | `DRAFT_HOLD` |
| [`M3JEP-LANG-048`](proposals/M3JEP-LANG-048.md) | Optional Binding | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-049`](proposals/M3JEP-LANG-049.md) | Task Groups | `REMAINING` | `DRAFT_HOLD` |
| [`M3JEP-LANG-050`](proposals/M3JEP-LANG-050.md) | Property Wrappers | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-051`](proposals/M3JEP-LANG-051.md) | Channels | `REMAINING` | `DRAFT_HOLD` |
| [`M3JEP-LANG-052`](proposals/M3JEP-LANG-052.md) | Defer | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-053`](proposals/M3JEP-LANG-053.md) | Select | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-054`](proposals/M3JEP-LANG-054.md) | Lightweight Tasks | `REMAINING` | `DRAFT_HOLD` |
| [`M3JEP-LANG-055`](proposals/M3JEP-LANG-055.md) | Generators | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-056`](proposals/M3JEP-LANG-056.md) | Comprehensions | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-057`](proposals/M3JEP-LANG-057.md) | Context Managers | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-058`](proposals/M3JEP-LANG-058.md) | Sequence Slicing | `ARRAYS` | `DRAFT_HOLD` |
| [`M3JEP-LANG-059`](proposals/M3JEP-LANG-059.md) | Comptime | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-060`](proposals/M3JEP-LANG-060.md) | Allocators | `ARRAYS` | `DRAFT_HOLD` |
| [`M3JEP-LANG-061`](proposals/M3JEP-LANG-061.md) | Error Unions | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-062`](proposals/M3JEP-LANG-062.md) | Multiple Dispatch | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-063`](proposals/M3JEP-LANG-063.md) | Broadcast Fusion | `ARRAYS` | `DRAFT_HOLD` |
| [`M3JEP-LANG-064`](proposals/M3JEP-LANG-064.md) | Generated Functions | `AST_COMPILER` | `DRAFT_HOLD` |
| [`M3JEP-LANG-065`](proposals/M3JEP-LANG-065.md) | Foreign Memory | `ARRAYS` | `DRAFT_HOLD` |
| [`M3JEP-LANG-066`](proposals/M3JEP-LANG-066.md) | Scoped Context | `REMAINING` | `DRAFT_HOLD` |
| [`M3JEP-LANG-067`](proposals/M3JEP-LANG-067.md) | Structured Concurrency | `REMAINING` | `DRAFT_HOLD` |
| [`M3JEP-LANG-068`](proposals/M3JEP-LANG-068.md) | Value Layout | `REMAINING` | `DRAFT_HOLD` |
| [`M3JEP-LANG-069`](proposals/M3JEP-LANG-069.md) | Vector Compute | `ARRAYS` | `DRAFT_HOLD` |
| [`M3JEP-LANG-070`](proposals/M3JEP-LANG-070.md) | String Templates | `STRING` | `DRAFT_HOLD` |
