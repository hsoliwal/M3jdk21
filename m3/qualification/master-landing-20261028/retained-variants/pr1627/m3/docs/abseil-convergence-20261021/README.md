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
   sorted/reversed endpoints and the historical bootstrap view candidate. The
   separate identity follow-up refines that public candidate and adds metadata
   helpers in the existing `java.util.Collections` and `IdentityHashMap` owners.

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
rg -n 'IDENTITY-|KNOWN_OWNER_|IDENTITY_ENTRY_VISIBILITY|PROVING_GROUND_SCOPE_EXCLUSION' m3/collections/algorithm-catalog/abseil-convergence-20261021.tsv
```

`FILE_PASSES.tsv` binds every changed runtime file to its before/after digest,
problem categories and declared observation changes. It retains the original
11 pass records and appends three identity refinements: **14 pass records over
13 distinct runtime files**. The public `CollectionViews` first-addition record
and its later identity refinement remain separate, contiguous hash transitions.
These are pass records, not 14 distinct files or proof that every pass has reached
the product. `DONOR_SOURCES.json` pins
the inspected Abseil source and license bytes. The canonical task retains the
sealed source resources, recipe correction memory and exact execution evidence.
No new global catalogue or search engine is introduced; ordinary TSV search
uses the existing catalogue format and repository policy.

The file pass is PIN -> INVENTORY -> COPY -> RECIPE -> COMPILE -> BEHAVIOR ->
CORRECT_RECIPE -> FIXPOINT -> RECEIVER_PROOF. Files are mutated serially; tests
and API comparisons are tied to exact source hashes and their actual stage.
Authoring compilation is recorded separately from checks of qualified emitted
sources. Failed attempts remain in the recipe's evidence and memory. No
arbitrary atom-count cap applies.

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

## Identity refinement and its boundary

Independent review found that the stage-1 public candidate could equal an
identity-backed set while computing a different hash, and that its non-Set
identity-values view could accept and remove a foreign equals-equal reference.
A further regression reproduced an identity-entry visibility error: an admitted
hidden mapping could borrow visibility from a distinct equals-equal key/value
mapping. These are declared repairs to the candidate; the earlier catalogue
and first-addition pass remain historical evidence.

The follow-up uses the existing JDK owners directly. `IdentityHashMap` classifies
its own key/value and entry view implementations. `Collections` iteratively
inspects known unmodifiable, synchronized, checked, checked-map-entry and
map-backed-set wrapper references. `CollectionViews` carries the resulting
ordinary, identity-element or identity-entry kind through nested filters and
known wrappers around filtered views. Identity entries compare both references;
identity set hashes use identity element hashes or identity key/value XORs.

Unwrapped references supply metadata only. The original backing object remains
authoritative for traversal, mutation, checked entry projections and read-only
restrictions. Existing synchronization responsibilities do not change. The
existing Set membership gate remains; non-Set identity values retain their
original callback structure without a new `contains` prepass. Bulk removal and
retention still call the operand's `contains`, including its equality direction.
The new owner helpers are package-private, and the existing public/protected
signatures of all three public owners are unchanged in the authoring comparison.
The historical public class remains an additive API proposal with its original
Apache notice and explicit core API/license admission gate.

The supported metadata boundary is the known JDK owner/wrapper family. No hash
fingerprint, representative-probe heuristic, reflection or new equality framework
is introduced. Arbitrary custom collection, wrapper or subclass equivalence is
not inferred. The separate `com.m3.collections.M3CollectionViews` retains recorded
generic identity defects outside this follow-up; its stage-1 callback repair and
the 540 public identity checks must not be presented as a complete repair of that
proving-ground owner.

The stage-2 authoring fixture passes 540 checks covering direct key/value/entry
views, map-backed sets, known wrapper stacks, nested filters, partial and all-pass
views, nulls, distinct equals-equal references, foreign probes, hash coherence,
entry projection restrictions and removal ownership. The unchanged baseline
passes the legacy 46, bulk 14 and callback 5 controls, while the three nominated
identity regressions fail. Each of the three runtime-file refinements was
compiled and compared for public/protected API parity before the next file.
Independent review reran the 540 checks and all three API comparisons.

Those authoring results are retained separately from the final product proof.
The initial OpenRewrite 8.17.1 failure on the existing `Collections.java`
Javadoc snippet remains recorded. The source host now uses the upstream
8.60.0 snippet fix, explicitly disables incidental annotation processing, and
preserves refusal assertions with a fail-fast negative-host callback. The
POM, SDK harness and current-build README correction were themselves emitted
by the previously qualified recipe host; all four engines and the narrow
Java bootstrap allowlist remain unchanged. The installed framework pack keeps
its independent historical 8.17.1 pin.

All eight implementation and handoff datasets pass their five actual SDK tests and twelve preparation
tests under that emitted host. String has sixteen receiver cases; the other
seven datasets have fifteen. The collection and identity stages were received
serially into the product and each reached zero-write fixed point. Five fresh
actual SDK fixtures also pass all seventeen handoff-writer contracts. The
archived generic fixture passes SDK/preparation but refuses historical engine
custody under both the old and new host; its original pins remain intact.

The final combined source-built product passes all eight public-view fixture
processes without a runtime patch: default-mode bootstrap 77, historical
contract 14,700, refinement 1,828, identity 540 and saturation 2 checks;
interpreter-mode bootstrap 77, refinement 1,828 and identity 540. All three core
owners are loaded from the built `java.base` image. `PRODUCT_ACCEPTANCE.json`
binds these observations to source seals, actual image hashes and canonical
evidence. Its local verification checkpoint is explicitly local; the separate
`SOURCE_RECEIPT.json` binds the actual published canonical source commit.

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

The actual source-built String image has passed the public composite comparison
fixture with **120,923 checks** and the public prefix fixture with **3,747 checks**
in each of the default, disabled-CompactStrings and interpreter modes. Those
public tests use the built image without a runtime-source patch. The existing
path-cursor corpus separately passes **1,746,792 checks in 35,914 scenarios** in
each of JIT and interpreter modes; only its package-private test fixture is
patched into the built image. That corpus explicitly excludes public String
admission and HotSpot/JNI integration claims. The exact product binaries,
commands and limits are retained in the String runtime receipt.

The same final image also passes the complete String mode matrix above.
The independently built collection module uses that product JDK and reports
479 tests across 142 suites: 477 executed, zero failures/errors, and the same
two explicit native-lane skips. Every test report identifies the product's
`java.home`. The changed compactor is rebuilt with the product JNI headers and
passes 8,438 checks under `-Xcheck:jni`; the production C also passes the existing
four-export UBSan empty-payload/state-drift fixture. These module tests do not
claim installation as a product module or qualification of every native backend.
The `images` build creates both CDS archive variants; that is not a complete
GC/CDS test-suite claim.

After fetching the real pinned history, the full-borrow receiver gate reports
**32 families, SOURCE_PIN_ONLY, completion=false**. This validates source-pin
custody rather than runtime completion. The String phase guard fails on both
the exact baseline and the candidate with the same pre-existing
`String successor chain length drift`. The guard and phase policy remain
unchanged; passing product fixtures does not waive this failure or advance a
receiving phase.

Resource measurements state source hashes, workloads, runtime/toolchain and
observed CPU/allocation separately. A smaller scratch array is not a universal
speedup; regressions and host noise remain visible. All-platform native,
full-reactor, complete jtreg/GC/CDS and five-provider-family qualification are
not established by this packet. The global receiving phase remains STRING;
these draft PRs do not advance it or declare a whole collection family complete.
