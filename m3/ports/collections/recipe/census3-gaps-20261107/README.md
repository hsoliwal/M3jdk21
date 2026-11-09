<!-- SPDX-License-Identifier: Apache-2.0 -->
# census3-gaps-20261107 (m3/ports/collections)

Census pass 3 member-gap leaves of lane com-synexia-0e on `m3/ports/collections`: **P3-005** (`M3FrozenMap` vs develop
`synexia-mindex/runtime` `com.synexia.mindex.collection.MIndexFrozenMap`) and **P3-011** (`M3CanonicalCollection`,
`M3CompositeRef`, `M3Deque`, `M3DomainTransfer`, `M3FrozenBag`, `M3FrozenDeque`, `M3FrozenList`, `M3FrozenMap`,
`M3FrozenMultiMap` vs develop `synexia-mindex/collections` `com.synexia.mindex.collections.*`). Census pins: com.synexia
develop `78cf2ad4c7e5`, M3jdk21 master `2274e18709b8` (`S:/m3-scratch/eb-conv/census3`); donor re-read at develop-live
head `78cf2ad4c7e5`, owner at master `0581643118d7`.

**Record-only leaf: no member can be ported here.** All 93 unexplained MEMBER_GAPS rows are HOLD_DEP (`DECISION.tsv`).
The nine owners are scaffolds of `receive MIndex scaffold batch S01` (7c2accec550; the module README calls them "the
remaining files of this directory are scaffolds"): every constructor and factory takes `MIndexSpace`,
`MIndexCompositeIndex`, `MIndexDomain`, `MIndexList`, `MIndexMultiMap` or `MIndexCollectionPrecompute`, and the state
lanes the members read (`space`, `index`, `keySpace`, `valueSpace`, `keyDomain`, `valueDomain`) are of those types.
None of them has an M3 owner; they are the com-synexia-4d mindex family. A member restored without its type could not
be reached by any instance, so nothing is ported and nothing is decided about the type shapes.

| owner | rows | first dependency |
|---|---|---|
| `M3FrozenMap` (P3-005 runtime shape) | 11 | `MIndexDomain` key/value domains, `copyOf(M3Map)` |
| `M3FrozenMap` (P3-011 collections shape) | 16 | `MIndexSpace` key/value spaces, `MIndexCompositeIndex` |
| `M3FrozenMultiMap` | 20 | `MIndexSpace`, `MIndexCompositeIndex`, `MIndexMultiMap` |
| `M3Deque` | 10 | `MIndexSpace`, `MIndexCompositeIndex` |
| `M3FrozenBag` | 10 | `MIndexSpace`, `MIndexCompositeIndex` |
| `M3FrozenList` | 10 | `MIndexSpace`, `MIndexCompositeIndex`, `MIndexList` |
| `M3FrozenDeque` | 9 | `MIndexSpace`, `MIndexCompositeIndex` |
| `M3CompositeRef` | 3 | `MIndexCompositeIndex` |
| `M3CanonicalCollection` | 2 | `MIndexCompositeIndex`, `MIndexCollectionPrecompute` |
| `M3DomainTransfer` | 2 | `MIndexSpace` |

HOLD_DEP (4d) hand-over: when the mindex family lands, re-run census3 on these nine owners; the donor blobs are the
`synexia-mindex/collections` and `synexia-mindex/runtime` sources at develop `78cf2ad4c7e5`.

No source change: the build (`mvn -B -ntp -f m3/ports/collections/pom.xml test`, off-reactor; `-Xlint:all -Werror`),
differential, mutant and javap gates do not apply (`GATES.tsv` records the scope and naming gates).
