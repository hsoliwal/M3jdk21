<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 five-provider grouped array/index receiver

Canonical source/recipe: `hsoliwal/com.synexia`, `synexia-openrewrite-recipes/recipes/m3jdk21-five-provider-key-groups-20261018`.
Only two new Java source/test files are added; existing vendor wrappers, M3 public
signatures and JNI C17 code remain unchanged. The target-local mirror is exact canonical
resource custody, not another implementation authority. `M3HashPinnedJavaSnapshotRecipe`
owns source transformation. Local `apply.py` is only an exact-hash materializer and
does NOT replace actual Maven/OpenRewrite execution.

Immutable index groups retain existing M3 value/state owner, M3 hash distinct-key
directory, primitive occurrence permutation, prefix spans, group-order indexes, and
zero-copy group windows. Five distinct real outputs use incumbent M3 provider factories:
JDK Map-of-lists, Guava ListMultimap, Eclipse MutableListMultimap, Spring MultiValueMap,
Apache ListValuedMap. Native mask selection only through existing Java/C17 JNI.

Draft until exact-head real Maven/OpenRewrite, all five pinned real providers, and checked
C17/JNI pass. Full upstream API closure, installed-JDK, jtreg/JCK, concurrent semantics,
serialization, CPU and heap comparisons, multi-platform qualification are still pending.
Preserve String -> arrays -> collections -> AST/compiler admission.
