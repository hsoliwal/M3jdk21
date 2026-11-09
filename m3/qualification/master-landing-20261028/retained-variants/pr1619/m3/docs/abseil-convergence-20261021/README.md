<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 Abseil/category convergence, 2026-10-21

This is a source-first, finite review packet for the integrated M3JDK21 product.
The canonical authoring recipe lives in `hsoliwal/com.synexia`, under
`synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017`.
The task is `tasks/m3-abseil-convergence-20261021`; the existing four class-backed
OpenRewrite engines remain the transformation authority.

## Review units

1. String: preserve current trigram search guards and owner/coordinate storage;
   refine comparison/prefix traversal with the existing cursor; repair the two
   ordered nonpositive-length region comparisons; correct eight ASCII-incompatible
   comment characters. Include fresh behavior, callback, API and JNI evidence.
2. Collections/JNI: preserve existing dynamic-array, offset, physical-index, mask
   and generation owners; repair callback reentry and cache invalidation; pack
   predicate scratch; avoid empty-output native payload reads; restore strict
   sorted/reversed endpoints and the historical bootstrap view candidate.

The second review unit is stacked on the first. Each revision's
`SOURCE_RECEIPT.json` is the authority for which dataset paths that revision
actually received. The catalogue describes both units; its presence does not
mean every listed candidate is installed or admitted.

## Catalogue and serial passes

Search the existing collection catalogues first, then this dated evaluation:

```sh
rg -n 'PACKED_SELECTION|GENERATION_VALIDITY|REENTRANT_CALLBACK_STATE' m3/collections/algorithm-catalog/*.tsv
rg -n 'ABSEIL-|LC-|HR-|GFG-|REJECT|REFERENCE_ONLY' m3/collections/algorithm-catalog/abseil-convergence-20261021.tsv
rg -n 'STRING-|VIEW-|JNI-' m3/collections/algorithm-catalog/abseil-convergence-20261021.tsv
```

`FILE_PASSES.tsv` binds every changed runtime file to its before/after digest,
problem categories and declared observation changes. `DONOR_SOURCES.json` pins
the inspected Abseil source and license bytes. The canonical task retains the
sealed source resources, recipe correction memory and exact execution evidence.
No new global catalogue or search engine is introduced; ordinary TSV search
uses the existing catalogue format and repository policy.

The file pass is PIN -> INVENTORY -> COPY -> RECIPE -> COMPILE -> BEHAVIOR ->
CORRECT_RECIPE -> FIXPOINT -> RECEIVER_PROOF. Files are mutated serially; tests
and API comparisons run on the exact emitted sources. Failed attempts remain
in the recipe's evidence and memory. No arbitrary atom-count cap applies.

## Contracts and explicit repairs

Existing callable public/protected signatures and native exports remain unchanged.
Two explicit inherited endpoint overrides add declarations on the navigable
facade; declared-method reflection is therefore disclosed, not claimed equivalent.
The recovered `java.util.CollectionViews` is an additive historical API proposal;
its historical signatures are checked separately. Its Apache notice is preserved
and core API/license admission remains an explicit review gate.

Repair observations are not labeled equivalence to defective baselines:
nonpositive-length String comparisons honor ordered Java bounds; callback
invalidation survives capture; nested traversals retain the outer action;
read-only endpoints refuse before predicate work; and recovered public views
use actual stored occurrences, single-pass bulk admission and saturating size.
Comparator failures and all-pass filters preserve private permutation ownership.

Abseil provides algorithm/contract comparisons only. No Abseil implementation
body, LeetCode solution, HackerRank submission or GeeksforGeeks article code is
copied. Historical first-party M3 atoms and fixtures retain their original
notices. Java String remains UTF-16; C++ byte-wise memcmp and clamped comparison
results are not substituted for the Java contract. Generic objects, predicates
and comparators do not cross JNI.

## Evidence boundaries

Fresh source recipe SDK execution, receiver fixed point/refusals, module tests,
strict compilation, generated JNI header/export checks, actual native tests and
source-built product execution are recorded independently. A missing old archive
does not confer authority to recreate its logs or inherit its test totals.
Only the freshly retained reports apply to this regenerated task.

Resource measurements state source hashes, workloads, runtime/toolchain and
observed CPU/allocation separately. A smaller scratch array is not a universal
speedup; regressions and host noise remain visible. All-platform native,
full-reactor, complete jtreg/GC/CDS and five-provider-family qualification are
not established by this packet. The global receiving phase remains STRING;
these draft PRs do not advance it or declare a whole collection family complete.
