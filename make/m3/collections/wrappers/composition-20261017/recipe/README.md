# Compose the existing filtered and sorted wrapper fixes

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

## Inventory and review order

The latest requested collection work already has two public implementations. M3JDK21
PR #1050 at `b2df8a4d342286421c7d1eeb91a73e5d69d39bdb` repairs stored-representative
membership and deletion. PR #1051 at `c5e5e90e2dcc2298a372ab39640120dd60a2edb7`
stages validated addAll references. Both are siblings of PR #1036 at
`cb501d5447c659606f4a5d0910379abaa0c36739`; installing either whole-file
postimage over the other would lose an existing fix.

This bounded successor composes those existing atoms. The primary receiving
preimage is #1050. The only production delta is the exact #1051 addAll atom.
The inverse source state is also qualified: #1051 receives the exact #1050
membership/deletion atoms and reaches the same complete source. Each route has
its own source-sealed manifest; historical installers retain their exact pins.

## Frozen contract and scope

Existing public descriptors, sorted/navigable comparator and range ownership,
nulls, duplicates, stored reference identity, deep ordered predicates,
spliterator characteristics, Java callback exception identity, and JNI symbols
remain under their existing regression oracles. Both earlier test sources are
reused unchanged. No new collection hierarchy, element payload store, parser,
recipe engine, runtime dependency, native backend, or public API is introduced.

The earlier staged-addAll contract boundary is retained: stable caller input
keeps values, order, return values and backing optional-operation behavior.
When the input changes during the call, only the actually validated references
are supplied to the backing owner. Custom backing implementations now receive
the temporary staged list; caller callbacks, argument identity and allocation
failure timing may differ. The temporary list costs O(batch size) references.
Mutable element fields and predicates are not frozen. Backing addAll can still
partially mutate before failing; this is not a concurrent transaction.

Membership/removal retain #1050's exact behavior and limitations. This task
does not claim new synchronization, linearizability, arbitrary identity-based
Set equivalence, serialization, or exception parity beyond the inherited tested
collection contracts.

## Recipe custody and qualification

The canonical class-backed Maven/OpenRewrite recipe is
`com.synexia.rewrite.M3WrapperCompositionRecipe`, exposed through the named
`com.synexia.m3.WrapperComposition20261017` and
`com.synexia.m3.WrapperCompositionFromBatch20261017` recipes. It composes the unchanged
existing `M3HashPinnedJavaSnapshotRecipe` provider in Java-LST mode. Its pinned
provider copy remains a verification dependency, not a competing owner.

The SDK tests cover both exact predecessor states, typed changes, preserved
source identity and unrelated code-containing fixtures, every partial state
and input order, forward/inverse convergence, second-application fixed points,
and missing/drift/occupied/duplicate/wrong-representation refusal. Execution
exports only actual OpenRewrite Results after qualification. Runtime proof must
consume those emitted files.

Run with Java 21 and Maven:

```sh
mvn -B -ntp -f pom.xml -Dm3.composition.output=/absolute/new-sdk-output test
M3_COMPOSITION_EMITTED=/absolute/new-sdk-output bash verify.sh /absolute/new-proof-output /absolute/M3jdk21-checkout
```

The checkout provides immutable inherited proof sources and native consumers;
their hashes are checked before use. It may contain the generated production
postimage, but compilation consumes the separately emitted SDK output.
Source installation is a separate exact-preimage receiving step.

The third named recipe,
`com.synexia.m3.WrapperCompositionWorkflow20261017`, uses the unchanged text
snapshot provider to update the existing wrapper workflow. Its historical
manifests remain unchanged. The workflow proves the predecessor against its
original inputs, then proves the new receiver against the successor outputs.
No extra workflow is registered. An exact source mirror in the receiver makes
that qualification independent of access to the private convergence repository;
its receipt and handoff retain canonical source custody.

## Executed qualification

Java 21.0.2+13 and Maven 3.9.9 executed the actual OpenRewrite 8.17.1/JUnit 5.10.2
suite: **7 tests, zero failures, errors or skips**. Both Java routes emitted two
changes and converged on identical combined source; the workflow recipe emitted
one change. Sixteen partial-state/input-order combinations, serial and two-worker
execution, fixed points and source-drift refusals passed. The unchanged historical
predecessor SDK recipes passed their two existing test methods separately.

The combined owner passed the unchanged stored-representative **86,866**, batch
admission **34,281**, and wrapper **964,437** checks in each of tiered and interpreter
modes. Deep filters passed **1,312,160** checks at depth 65,536 and **165,280**
interpreter checks at depth 8,192. Both predecessor counterexamples reproduced
their specific missing-sibling failure. These are finite repeated assertions,
not independent exhaustive proofs.

The unchanged C17 native consumers passed **4,506 + 156** assertions with strict
warnings, fatal UBSan and `-Xcheck:jni`; the range consumer used local-reference
capacity 64. Public descriptors matched both predecessors, and nine unaffected
runtime classes matched byte for byte under identical `-g:none` compilation.
All **89 production Java files** in the complete collections module compiled
with its existing warnings-as-errors settings.

The owning module's Maven test gate was attempted but blocked by the unavailable
source-built dependency `org.junit.jupiter:junit-jupiter:5.10.2-m3-source-1`.
Its POM was not changed to substitute another provider. The independent actual
SDK/JUnit and Java/JNI receipts remain distinct from that blocked gate.
See `evidence/RESULTS.json`, the exact logs, retained source counterexamples and
the receiver application receipt.

## Acceptance boundary

This is maintenance of the independently licensed `m3/collections` proving
ground. It does not advance the String -> arrays -> collections -> AST/compiler
receiving phases or change `java.util` / `java.base` / HotSpot. Full provider/reactor,
source-built M3JDK21, jtreg/JCK, other platforms, performance and process-memory
gates remain separate and unqualified. The successor hosted workflow has been
prepared and its exact SDK/runtime payload executed locally; hosted execution
remains pending. Keep the receiving PR draft while those required gates are open.
