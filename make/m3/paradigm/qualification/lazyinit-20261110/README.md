<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# AnOP @Lazy lands as LazyInit (2026-11-10)

The AnOP batch (#2918) held `com.synexia.anop.Lazy` and its 10 dependents. The sprawl gate refused the name: it is
SECOND_OWNER of `jdk.internal.m3.collections.Lazy`, the lazy-value holder that b29 landed. The user decided on
2026-11-10 to rename the annotation to `LazyInit`, because it marks lazy initialization and the holder implements
it, so the two are distinct concepts.

## Change

RELOCATE_M3_OWNER v6.3 relocates the 11 classes with `Lazy`'s target set to
`jdk.internal.m3.paradigm.annotation.LazyInit`. The relocation renames the type and every reference to it. Lineage
lines keep the donor name.

The 11 classes:
- LazyInit
- HiveMind
- ISpec
- Broadcast
- Colony
- Queen
- Subscribe
- Worker
- IFieldSpec
- IMethodSpec
- IParamSpec

The same recipes as #2918 apply: the banner, SLF4J_TO_PLACEHOLDER_LOGGER v1.1, and DOCLINT_REPAIR v1.1, which is
comment-only and fixed 4 lines in 3 files.

## Proof (PROOF.tsv)

The normalization maps `com/synexia/anop/Lazy` to `.../annotation/LazyInit`, plus the slf4j map and stripped
constant-pool indices.

- **javap -c and javap -v:** 14 class files, all equal.
- **Mutant check:** not applicable. The batch has no concrete class, since all 11 are annotation types, and prove_mat
  only seeds a mutant into a concrete class.

## Gates

- **Sprawl gate:** new=11, refused=0.
- **jdk_naming:** fixed point.
- **Compile:** both modules build under -Werror.
- **Doclint:** -Xdoclint:all,-missing reports 0 errors.
- **Default suite:** the werror reactor (arrays, collections, paradigm) passes: 2841 tests, 0 failures, 0 errors, 151 skipped.
