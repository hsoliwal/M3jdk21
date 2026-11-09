<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# One owner for four mat duplicates of jdk.m3.arrays (NATIVE_ABSORB N7b, 2026-11-09)

## Finding

The RELOCATE v4 mat batch absorbed the whole Synexia mat-collections package into
`jdk.internal.m3.collections.mat`. Several of those classes had already been received earlier into `jdk.m3.arrays`.
The NATIVE_ABSORB census showed the overlap: `MatBitMatrix` and `MatIntPostingIndex` had ABSENT natives, while their
`jdk.m3.arrays` counterparts already had C.

`javap -c -p` was run on the classes of master 72e8fd758303 with names normalized. Eight pairs compile to identical
bytecode:

| mat class | jdk.m3.arrays class | javap -c -p | Retired here |
|---|---|---|---|
| MatCsrIndex | CsrIndex | equal | yes |
| MatHybridIntSet | HybridIntSet | equal | yes |
| MatIntRingBuffer | IntRingBuffer | equal | yes |
| MatIntPostingIndex | IntPostingIndex | equal | yes (removes its 7 ABSENT natives) |
| MatBitMatrix | BitMatrix | equal | no: see below |
| MatBits | SelectionBits | equal | no: see below |
| MatSelection | IntSelection | equal once MatBits = SelectionBits | no: wraps MatBits |
| MatIntList | IntPostingList | not equal: MatIntList adds `plan()` | no: superset |

## Why four pairs stay (needs a user decision)

- **MatBits.** Other mat classes call its package-private members `wordCount`, `word`, `wordsFor` and `ofWords`.
  `SelectionBits` keeps those members package-private in `jdk.m3.arrays`. Retiring `MatBits` would mean making them
  public, which needs the user's admission.
- **MatSelection.** It wraps `MatBits`, so it stays with it.
- **MatBitMatrix.** `MatAdaptiveIntGraph.successors` returns `MatIntList`, but `BitMatrix.setColumns` returns
  `IntPostingList`. Retiring it changes a mat signature, so it is not a pure rename.
- **MatIntList.** It is a superset: `plan()` returns `MatCollectionPlan`, which `jdk.m3.arrays` must not depend on.

## Change

`RETIRE_DUPLICATE_OWNER v1.1` (`retire_duplicates.py RETIRE.tsv <repo>`):
- deletes the four duplicates;
- rewrites their only user, `MatCollectionFactory` (`postingIndex` now yields `jdk.m3.arrays.IntPostingIndex`);
- adds the import.

Provenance lines are never rewritten. `javap -c -p` of `MatCollectionFactory` before and after is equal modulo the
renames (686 lines; one generic signature differs only in normalization).

## Census

ABSENT 14 -> 7, and 196 native methods remain. The rest is MatBitMatrix 2, MatIntRowKernel 2, MemoryPressure 2 and
MarkerTopKNative 1.

## N7c (2026-11-09): MatBits and MatSelection retired

The user admitted on 2026-11-09 ("SelectionBits accessors") making `jdk.m3.arrays.SelectionBits`' word accessors
public: `wordCount()`, `word(int)`, `wordsFor(int)` and `ofWords(int, long[])`.

A fifth accessor of the same family, `wordsCopy()`, is also made public. `MatIdSet` uses it; the first compile's
truncated error list hid that call. It is named here so the admission can be checked.

With these accessors public, `MatBits` retires into `SelectionBits`, and `MatSelection` into `IntSelection`
(`RETIRE_N7c.tsv`; RETIRE_DUPLICATE_OWNER v1.1).

- **Deleted / rewritten:** 2 classes deleted, 17 files rewritten.
- **Bytecode:** javap -c -p of all 30 rewritten mat classes, nested ones included, is equal modulo the two renames.
- **Build:** the module compiles under -Xlint:all -Werror.
- **Default suite:** 2836 tests, 0 failures, 0 errors, 151 skipped.
- **Custody:** resealed; --check reports changed=0.

MatBitMatrix and MatIntList stay as they are; changing them was not admitted.
