<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Existing deque ring-span receiving contract

Product preimage: 4076ca2133436a90f95efb27096743ba066eff64. Canonical recipe preimage: f1ef0d96d594a32b8aa3bcf7149120a3e7d65eda. Continue the existing collection PRs, preserving their prior array-list, indexed-sort and source-capture changes.

M3JDK21 owns the runtime. The existing M3ArrayDeque and M3LongArrayDeque are refined directly; no mandatory Synexia runtime, renamed public JDK API, replacement collection hierarchy, retained node graph or JNI symbol is introduced.

Generic deque: reuse the existing primitive ring's two-span copying pattern for growth, clone and untyped export; keep Object[] and byte[] flags aligned. Clear retired intervals through physical spans. Replace eager byte-per-occurrence removal scratch with lazy first-rejection-relative long[] words and stable survivor-bit enumeration. Predicate calls and immediate revision checks retain their original order; no compaction precedes successful callback completion. Preserve arbitrary capacity, circular layout, occurrence flags, iterator rules, typed-array failure/partial writes, serialization and exception identity.

Primitive deque: only consolidate logical-tail clearing into private two-span fills. Preserve its different partial-commit-on-predicate-failure contract, throwing element and unread suffix, signed extremes, zero payloads, head/epoch rules, validation and original two-span export/growth owner.

Canonical M3CollectionRingSpansRecipe subclasses the unchanged M3GpuToolJoinRecipe. Its ten-row manifest binds eight real production preimages (six KEEP) and two absent-before regression sources. Four exact SDK Results are the only production/test materialization authority. The host qualifies original -> generic-only -> combined images serially; errors in any SDK phase reject the entire candidate set. Recipe output is not arbitrary AST-equivalence proof.

Required order: diff -> source hygiene -> Java21 strict compile -> tests -> runtime. Preserve source hashes, frozen oracle, native declarations, raw logs and fixed point. Native code is unchanged; header parity is not a fresh native-runtime qualification. Whole-module/source-built JDK image/hosted/platform gates and the all-collection campaign remain separate, pending obligations. No merge or global receiving-phase advancement follows from this finite pass.
