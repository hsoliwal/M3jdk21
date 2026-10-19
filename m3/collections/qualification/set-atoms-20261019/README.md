<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Existing collection owner atom preparation

Canonical recipe: hsoliwal/com.synexia PR #12497, commit
53400b3e8c17b295a9080cb1e9856389ba345148.
Named entry: com.synexia.m3.SetAtoms20261019.
Task: synexia-openrewrite-recipes/recipes/keyed-api-20261015/set-atoms-20261019/.
Target preimage: 677437be295b23f7ccf7409be287008944d0e8b0.

## Receiving contract

Receive only the four exact outputs of the actually executed source-sealed
M3SetAtomsRecipe: M3IntHashSet, M3LongHashSet and the two new M3SetAtoms tests.
The unchanged canonical M3GpuToolJoinRecipe/Manifest own source admission;
recipes and reusable verification tooling remain canonical in Synexia.
No runtime dependency, duplicate collection hierarchy or new JNI symbol.

M3IntHashSet uses one private matching-or-empty nonzero probe across add,
remove and contains. Preserve its zero presence lane, backward-shift deletion,
capacity, iteration and modification epoch.

M3LongHashSet separates growth-path tombstone reuse, absent-key insertion-slot
selection and key/occupied/state/size/epoch publication. Preserve its distinct
byte-state/tombstone policy and exact physical-slot order. Do not substitute
the int deletion strategy or change live collection semantics.

Existing names remain com.m3.collections.M3IntHashSet and M3LongHashSet.
Mapping disposition: EXISTING_OWNER_REFINEMENT_NO_NEW_PUBLIC_TYPE_OR_RENAME.
The central m3/docs/name-mapping.json is unchanged; this is an explicit
existing-owner disposition, not admission of a new public API or JDK backend.

## Executed focused proof

Actual Maven/OpenRewrite: seven unskipped tests, all sixteen partial states
in both source orders, source/template/refusal tests, four emitted changes,
zero-change replay. The exact six-source original closure and both serial
candidate stages compile with Java 21 --release 21 -proc:none -Xlint:all -Werror.
Each runs one actual JUnit test before standalone runtime; every standalone
run performs 3,928,394 checks. Original, int-only, combined and combined C1
observations share digest:
77bea42f07806853c64eaaa50fcf06659d1141a573a46d761f3cdae995d87e71

All six original public/protected descriptor snapshots match. Three compiled
wrong variants are rejected. A negative control exposed a digest-length-only
JUnit adapter; the generated adapter now binds the independent original digest.
The original corpus and both runtime templates were not changed. Earlier failed
runs are retained with the final commands and raw outputs in the delivery archive
identified by the canonical evidence/ARTIFACTS.tsv.

## Scope and remaining gates

This is physical preparation of two already-array-backed existing owners.
It is not the full canonical Atomizer/Patternizer fleet run or completion of
all collection forks, graph/lazy/persistent families, wrappers or stream APIs.
The whole supplied dated discovery ledger retains 6,185 filename candidates
across 33 repositories and 1,404 unresolved subtree occurrences. Those figures
are not current source coverage or completed implementations.

Full owning module/provider tests, installed source-built M3JDK21 image, jtreg,
GC/JNI/runtime integration and comparative resource/performance acceptance remain
unexecuted. JNI changes are zero, not a claim that native gates ran. Preserve
String -> arrays -> collections admission. Keep review draft; no default-branch
merge or global collection readiness claim is authorized by this finite proof.
