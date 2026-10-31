# Reviewed navigation correction

## Problem categories and retained owners

This pass concerns bounded predecessor/successor lookup, comparator admission,
nullable selection state, and lazy predicate composition. It refines
`M3FilteredViews` and reuses the existing `M3MutableFilteredViews` delegate and
`M3FilteredNavigableSet` facade. Public methods, the original live backing owner,
stored object representatives, comparator identity, range constructors, and
mutation permissions remain the contract boundaries. No collection payload is
copied and no comparator or predicate crosses JNI.

The receiver advanced from `0d73745d7203123533dce8f8fb264ba2746f6d2d` to
`07ba6e383343d32c83cfdaf641387bbe72bf325d` during review. The later receiver adds
bounded mutable-set and read-only-map recovery with a full scan after
`IllegalArgumentException`, plus `M3BoundedFilteredNavigationContract` and its
JUnit adapter. Those existing tests are retained unchanged and pass this
refinement. The recipe must use the later receiver's source preimages.

## Rejected alternatives and selected refinement

| Candidate | Evidence and decision |
| --- | --- |
| Query range followed by an unrestricted `IllegalArgumentException` catch and retry | Rejected. A deterministic comparator rejection executes twice. A custom owner's range exception can be swallowed and replaced by a normal result. The frozen local donor and the later receiver both contain this failure family. |
| Catch followed by a full bounded scan | Rejected. The same exception ambiguity remains; an exceptional lookup may additionally inspect every member. The newly merged bounded-result coverage is retained as an oracle, without retaining this recovery mechanism. |
| Treat null as present only when first or last | Rejected. A lawful comparator can place null between non-null elements. Rejecting that null member must continue to the next eligible visible member. |
| Direct owner navigation plus an actual-member anchored iterator | Selected for reviewed tree owners. It avoids invalid query range construction, keeps iteration within the original backing view, and never catches comparator or owner failures. |
| Infer arbitrary subclasses or decorators from `instanceof TreeSet/TreeMap` | Rejected. A subclass may override a range operation and have a distinct observable failure contract. |

The admitted anchor domain is explicit: exact `TreeSet`,
`ConcurrentSkipListSet`, `TreeMap`, `ConcurrentSkipListMap`, final `M3TreeSet`,
and final `M3TreeMap` owners, plus the JDK tree maps' own nested range and key
views. JDK nested views are recognized through enclosing-class identity. No
reflective field access, implementation-name string matching, or new backing
owner is used. Known private M3 visibility and permission facades may be peeled
for boundary lookup; their original wrapped source still performs traversal.

Other owners remain opaque. Mutable-set and map lookup retain their original
query range operation for such owners, including exception identity and demand.
The bounded-query enhancement is not claimed for arbitrary opaque decorators
or custom subclasses. A future admission must first establish safe range and
neighbor semantics and add a catalogue record and executable owner probes.
Opaque-owner exceptions are not evidence that a range bound was exceeded.

## Behavioral atoms and callback boundaries

`anchorOwner` determines the reviewed owner domain. `seekFromAnchor` obtains an
actual neighbor and then opens one inclusive range at that stored member.
The wrapped iterator applies predicates in their existing order and stops at
the first visible member. Read-only maps retain immutable detached navigation
entries; set results retain original object references. For balanced tree
owners the lookup uses a bounded number of tree seeks followed by iteration
over rejected members, with constant cursor state. This is an asymptotic
description; it is not a measured speedup or a promise of identical comparator
call counts for every successful lookup.

Read-only sets keep direct external-owner navigation before predicate or range
work. A private M3 filter chain over an admitted tree uses the raw owner's
anchor and the original wrapped traversal, which prevents a null-presence
check from evaluating inner predicates a second time. An inner mutable view
still requires the original range query admission even when empty.

`nullIsAnchor` distinguishes an absent neighbor from an actual stored null
without assuming where null is ordered. A predecessor exists exactly when the
least stored member satisfies the query boundary; a successor exists exactly
when the greatest member satisfies it. If such a member exists and navigation
returned null, that null is a stored candidate. The proof compares only an
actual stored extreme; a comparator which rejects null receives no fabricated
null probe. The proof uses raw owner state for reviewed private chains, never
predicate evaluation as a presence test.

`checkQuery` retains mutable-set and map query admission before anchor lookup,
including empty owners. Read-only `TreeSet` navigation historically returns
null on an empty owner without comparing a query. The local donor's empty
read-only test had incorrectly required a null or rejected-query exception;
its oracle now agrees with the unchanged direct receiver/JDK operation. The
mutable and map admission oracles continue to require their original failures.

## Executable evidence

`M3NavigationFailureContract` and `M3NavigationFailureTest` cover 140 checks:
all four directions, comparator exception identity and single failure demand,
opaque subclass range failures, nested read-only/mutable/facade layers, empty
owner admission, and bounded M3 array-tree owners. Deterministic rejected query
99 produces one original comparison failure: read-only sets compare `99:2`,
and mutable sets/read-only maps compare `99:99`. The opaque set's `headSet(2,
false)` sentinel is propagated after exactly that one original range call.

Final standalone qualification overlays the candidates on the exact later
receiver and compiles 141 Java source inputs with Java 21, `-Xlint:all`, and
`-Werror`. Completed gates include:

| Gate | Result |
| --- | --- |
| New failure/admission contract | 140 checks |
| Unchanged merged bounded navigation contract | 90,431 checks |
| Full navigation boundary corpus | 16,429,309 checks; 7,894,400 queries; tape `be1d11843b3fbe35` |
| Read-only boundary and nested callback corpus | 32,267 checks |
| Deep generic filtered collection | 84 checks at depth 65,536 with a 256 KiB Java stack |
| Original 12 bounded/interior-null counterexamples | All pass |
| Read-only and generic equality/read callback tapes | Byte-identical to the original receiver tapes |
| Existing frozen mask standalone owners | All five Java and five required-JNI runs pass with `-Xcheck:jni` |
| Existing order/window proof | 621,981 checks; unchanged observation digest |

The read-only tape SHA-256 is
`0014cf664c4d395f43df534318d33a92416011168d9690b08bfd52ba8a7ebfb2`.
The deep generic tape SHA-256 is
`7a10a88ba35159ffdefa7de404e5ed70936dee7f3bc7b2c2336811d52c1d351a`.
The full navigation tape differs from the frozen donor because the empty
read-only admission oracle was restored; all result and predicate-prefix
assertions pass. These focused receipts supplement the recipe SDK, build,
API-surface, and full Maven receiver gates; they do not replace them.

The source remains first-party Apache-2.0 Synexia/M3 work. This correction
reuses existing collection owners and local behavioral atoms. It copies no
third-party source text and creates no new JNI ABI or native implementation.
