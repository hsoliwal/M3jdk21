<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 ports: follow the M3String ownership model

Canonical product policy: [M3JDK21_PORTING_INVARIANT.md](M3JDK21_PORTING_INVARIANT.md).
The machine-readable `porting_policy` and family references extend the existing
[name-mapping.json](name-mapping.json); public JDK APIs keep JDK names and M3
names identify internal replacement/optimization owners.

Synexia is the convergence workspace and code contributor. M3JDK21 is a separate,
self-contained target. Every MIndex family remains in the migration scope;
individual capabilities enter the target through pinned, reviewable recipes.
Moving a capability means adapting its ownership, dependencies, precomputed
facts, Java/JNI implementation and tests, with traceable source history.

Use the existing authorities: [name-mapping.json](name-mapping.json),
[counterpart-naming.md](counterpart-naming.md),
[String/precompute port map](synexia-string-precompute-port-map.tsv), and
[source census](../migration/vi-census.tsv). This document is the execution order,
not another naming registry. The census covers 2,494 matching files in eight
pinned source subtrees; it does not establish complete repository/dependency
coverage. A non-String disposition leaves a capability in the broader accounting
queue; it does not authorize putting it inside `java.lang.String`.

## Names already established by the target

| Synexia responsibility | M3JDK21 counterpart | Boundary |
| --- | --- | --- |
| `MIndexString` | `java.lang.M3String` | Canonical owner plus packed coordinate; public `String` contract retained |
| Scalar resolver/interning and tuple composition | `M3StringOwner`, `M3StringAtom`, `M3StringPool`, `M3StringTuple` | Native/mapped scalar ownership and persistent coordinate composition |
| Canonical/range String precompute | `M3StringFacts`, `M3StringOwner.rangeFacts` | Derived facts keyed to exact owner/range; no second spelling store |
| Prepared literal search and position masks | `M3StringSearchPrecompute`, `M3StringPositionPrecompute` | Bounded weak-owner operation metadata; exact matching remains authoritative |
| Backing contracts | `jdk.internal.mindex.M3StringBacking`, `M3MappedStringBacking` | Target-owned mapped payload and lifetime |
| Trigram query/facts | `jdk.internal.mindex.M3TQ` | Conservative absence filtering and exact trigram-set containment |
| Segmented primitive collections | `M3Address28`, `M3IntLane28`, `M3LongLane28`, `M3BitLane28`, `M3Bits` | Primitive lanes and JNI bit operations; established internal package |
| Version/class views and precompute context | `M3VI`, `M3Release`, `M3CB`, `M3CI`, `M3Class`, `M3PC` | VM-local identity and defining-loader boundaries |
| AST and AST precompute | `M3AST`, `M3ASTPC` | Reserved mappings; source/format/consumer admission still pending |
| Compiler AST precompute | `com.sun.tools.javac.m3.M3ASTPC` | Reserved compiler-module owner; no compiler dependency in `java.base` |

These are the existing target spellings. Preserve them in source, recipes,
tests, mapping and native symbols. New names require per-type review under the
same authority; do not mechanically create a public `M3*` facade for every donor
class or rename the existing `jdk.internal.mindex` package. Multiple donor types
may feed one target responsibility, and one donor may split across target owners.

## Serial port stream

1. Pin the exact source body, public contract, tests, dependencies and licence.
   Reconcile source and target history before selecting an atom to replace.
2. Resolve the target owner from the existing map. Record the source path/blob,
   target path/symbol, adaptation, format/ABI, lifecycle and effective revision.
   Include non-prefix helpers and native symbols in the dependency closure.
3. Execute the existing Maven/OpenRewrite recipe against exact preimages. Keep
   before/after images and guards, fixed-point and rollback checks. Generated
   output is the reviewable replacement; runtime code has no authoring dependency.
4. Compare old/new behavior with independent oracles, seeded combinations and
   deliberately broken variants. For collections include order, nulls, equality,
   mutation/iterator behavior, overflow and concurrency where their contracts
   require them; for strings include UTF-16, ranges, JNI shadows and ownership.
5. Qualify interpreter/mixed/C1/C2, native checks and the matching complete image.
   Admit only the tested execution modes; retain fallback and invalidation paths.
   CI results must refer to the exact source tree being admitted.
6. Publish target receipts and unresolved requirements back to Synexia. Later
   convergence improvements replay through the same mapping and recipes.

Prioritize storage/primitive collection owners, composable String/range facts,
then the concrete String/regex/collection consumers that reuse them. Class/AST
precompute follows definition provenance and loader/module/lifetime review.
Swing consumers belong to `java.desktop`; JavaFX/scene-graph work remains an
independent target unless an explicit module integration is admitted. Compiler
and runtime optimizations require their own module/HotSpot gates.

The target already contains bounded case-sensitive `Pattern.LITERAL` search
filtering and `M3StringSearchPrecompute` consumption of TQ facts. Their existence
does not qualify general regex algebra, captures, lookaround or arbitrary donor
execution engines. Positive filters must fall through to exact semantics.
Facts never become canonical payload; cache eviction affects performance only.

## Current gate and remaining scope

The [TQ successor packet](../tooling/tq-sync/README.md) reconciles the M3 backing
names and current `containsAll` implementation, retains current M3String naming
metadata, and qualifies 201,601 containment pairs per runtime mode. It preserves
the original primitive collection gates. It does not replace the existing
String/regex consumers or promote the overall migration to complete.

All 52 current migration records and 20 gates remain in the authority. Runtime
counterpart presence, recipe reproduction, image compatibility and performance
admission are separate facts. Preserve the OpenJDK 21 API and class identity.
Do not bypass verification, linking, initialization, access control, GC/JNI
lifetime or deoptimization because a precompute/version key matches. Radical
class-file or linkage changes require the separately scoped experimental JVM
project requested by the user.

Preserve Apache-2.0 notices on eligible copied Synexia code and the existing JDK
and third-party licences on their own code. Generic algorithm catalogues provide
problem categories and test ideas; external submissions require verified reuse
rights before they become source donors.
