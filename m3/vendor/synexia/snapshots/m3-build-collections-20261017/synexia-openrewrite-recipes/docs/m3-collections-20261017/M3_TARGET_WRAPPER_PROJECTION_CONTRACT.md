# M3JDK optional collection projection contract

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

Status: candidate design before target payload authoring. Canonical portable recipes remain in Synexia; target delivery is the explicitly requested M3JDK21 PR. Target baseline is `hsoliwal/M3jdk21@970a34350ebdf6a3f1c5ff5acc8445d0f3515f55`.

## Existing owner review

The complete Git path inventory contains no generic filtered view facade or primitive index projection owner. Existing `m3/collections` is a separately built Apache module `com.m3.collections`; its `M3LongFilteredView` already composes nested predicates, retains late-binding primitive traversal and avoids an exact-fit copy. `M3LongSortedViews` is an existing sorted primitive owner. Preserve both implementations and APIs.

Existing `com.m3.util.M3PrimitiveKind` and package-private `M3DenseHash` live in `m3/ports/collections`. The kind has the required eight exact-width lane semantics and package-private helpers. Reuse these actual target source owners. Do not introduce another primitive-kind enum or hash owner in the new package.

Existing `com.m3.collections.M3IndexMaskCompactor` owns optional native mask selection. It takes detached aligned arrays, unsigned-byte masks only, and explicit JAVA/NATIVE_REQUIRED selection. The primitive index view has signed-int mask behavior and source-generation invalidation. They are separate contracts: no alias, predicate-to-C dispatch, or implicit JNI path is introduced.

Root AGENTS, the complete porting invariant, `name-mapping.json` receiving sequence and relevant owner mapping fragment were read. Intake mode is INDEPENDENT_APACHE_MODULE. This optional module does not advance the ordered String -> arrays -> collections -> AST/compiler receiving state or claim a java.util backend promotion.

## Exact source placement

| Source capability | Target owner | Action |
| --- | --- | --- |
| Qualified M3FilteredViews | `jdk.m3.collections.M3FilteredViews` under `m3/tooling/module-packs/collections/src/main/java/jdk/m3/collections/` | New optional facade, six existing read-only live view overloads; source/contract body preserved after package projection. |
| M3MutableFilteredViews | `jdk.m3.collections.M3MutableFilteredViews` in the same optional package | New optional facade, two existing mutable navigable overloads; source/contract body preserved after package projection. |
| Qualified PrimitiveIndexView | `com.m3.util.M3PrimitiveIndexView` under existing `m3/ports/collections/src/main/java/com/m3/util/` | Add missing primitive projection beside its actual kind owner. Preserve source-generation checks, signed mask semantics, defensive exports and selected-row ordering. |
| PrimitiveSoaStore | `com.m3.util.M3PrimitiveSoaStore` in existing ports package | Exact dependency projection; no storage redesign. |
| M3SortableTopK | `com.m3.util.M3SortableTopK` in existing ports package | Exact dependency projection; no comparator or pressure policy change. |
| M3RuntimeHeap | `com.m3.util.M3RuntimeHeap` in existing ports package | Exact dependency projection; allocation/pressure policy unchanged. |
| Existing target M3PrimitiveKind and M3DenseHash | Existing `com.m3.util` source files | Compile original source paths into the optional module; no duplicate source copy or type substitution. |
| Existing target mask selector and native peer | Existing `com.m3.collections` Java module and `m3_index_mask_compactor.c` | Build the complete existing module and native peer unchanged; package its native library with that module. |

`jdk.m3.collections` exports `jdk.m3.collections` and `com.m3.util`. It uses only java.base APIs. `com.m3.collections` remains a separate module and package; do not merge its package into the new module. The optional pack binds both through the existing `jmod_pack.py` runner. The native library is attached to its existing declaring module with its existing JNI symbol/header/library-name contract.

## Recipe and validation order

1. Freeze independent Synexia oracles and qualify intermediate per-file atoms using actual Maven/OpenRewrite SDK output. Keep failure counterexamples separate.
2. Author source-projection templates with exact donor preimages, package/type mappings, target ABSENT preimages, target postimages, license, attribution and modification notice.
3. Root applies canonical Java/text snapshot engines. No direct target edit or competing recipe implementation.
4. Compile target module and original dependency sources with JDK 21 lint/Werror. The Maven proof POM reuses those source paths; the thin image builder reuses the existing JMOD packager.
5. Run projected independent oracles and unchanged target mask-selector oracle on real JNI, then repeat them on the linked image. Verify module descriptors and ABI exports; plain .jmod files are linking inputs, not runtime classpath entries.
6. Record allocation evidence separately from contract proof. Full custom HotSpot image build, jtreg/CDS/platform coverage and family-wide runtime promotion remain distinct gates; finite optional-module tests do not imply those outcomes.

## Deliberate compatibility holds

The two existing null-navigation projected-TreeSet mismatches remain documented limitations. Mutable bounded-range navigation mismatches retain their independent counterexamples. Neither private wrapper construction nor primitive exact-fit copy elimination changes these behaviors. Existing legacy wrappers, target primitive live views and detached JNI mask selection are not relabeled as equivalent APIs.
