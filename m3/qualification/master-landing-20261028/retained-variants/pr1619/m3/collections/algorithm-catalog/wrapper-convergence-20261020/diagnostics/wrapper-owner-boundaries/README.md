<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Filtered wrapper exception and generation diagnostics

## Decision

**Reject the proposed cursor mutation guard. Keep `comparator-failure`,
`readonly-failfast`, and the stale subList mutation contract pending. Preserve the
original required-behavior oracles. Do not classify exceptions by message, stack
trace, exception subclass, or an unproven source/comparator/key whitelist.**

These findings first applied to M3JDK21 revision
`2779aba3f98f31c324be43f731be7914e1a05935`. All artifacts here are isolated
diagnostics. This diagnostic work edited no product implementation or JDK source. The replay
records each unresolved contract's actual nonzero exit code; a successful
diagnostic run means the documented defect was reproduced, not fixed.

The reusable resolution belongs in the existing canonical Synexia recipe and
the actual M3JDK21 owner. The proposed integrated `java.util.CollectionViews` receiver is
`src/java.base/share/classes/java/util/CollectionViews.java` in
`hsoliwal/M3JDK21#1551`, head
`49744f953f717b86ca0811cf34dbbdc7c88f54e9` (source inventory supplied by the
parent review). Its seven factories cover Collection/Set filters and snapshots;
it does not expose filtered navigable-map or filtered-list factories. Consequently, this pass does not introduce unused TreeMap helpers or
new public factories merely to make a diagnostic callable.

## Comparator failure: observed defect

The affected atoms are:

* `M3FilteredViews.NavigableMapView.boundedEntry`.
* `M3MutableFilteredViews.SetView.bounded`.

Both catch every `IllegalArgumentException` from range construction and iterator
creation and treat it as a rejected endpoint. Those operations can execute user
comparators, natural-order `Comparable` methods, and arbitrary wrapper methods.
The recovery then performs further source operations and may return a value,
hiding the original exception.

The existing `comparator-failure` oracle fails because the expected caller-owned
exception is missing. Its first callback must be exactly `5:2`, with no later
comparison callback. The diagnostic confirms that the original JDK range call
throws that same exception with tape `[5:2]`. Replacing it with `floorEntry(5)`
returns `4=4` and records `[5:8, 5:4, 5:6, 4:2]`. A neighbor lookup therefore
changes both the observable comparison schedule and whether the original
exception occurs.

### Why apparently narrow exception guards remain unsound

An exact JDK `TreeMap$AscendingSubMap`, a null public comparator, and an exact
`Integer` query do not prove that range creation cannot execute user code.
For exclusive endpoints, `TreeMap.NavigableSubMap.inClosedRange` compares the
retained high endpoint as the left operand. A legal empty natural-order bounded
map can retain an arbitrary `Comparable` endpoint. The included witness's
`headMap(5, false)` invokes `endpoint.compareTo(5)` and throws its caller-owned
exception. The current filtered map and mutable filtered key set swallow it. The map
recovery then invokes `endpoint.compareTo(endpoint)` as a further callback: its
observed tape is `[endpoint:5, endpoint:self]`. The mutable key set tape is
`[endpoint:5]`. Both return null instead of the exact caller exception.

An additional adversarial subclass witness shows that the exact submap class
does not identify its hidden root class: `AscendingSubMap.comparator()` invokes
the root's overridable comparator accessor, while range validation uses the
root's actual comparator field. This observation is not needed for the generic
obstruction; it prevents mistaking concrete submap identity for a complete
owner proof. The deceptive accessor in that witness is explicitly adversarial.

A third witness captures an actual range-construction exception and later
rethrows that identical object from the user comparator. Its message, original
stack, class, and identity still describe its earlier creation. They cannot
prove the origin of the current throw. Introducing a privately named range
exception class alone would have the same replay problem if callers can first
catch a genuine instance.

### Generic API obstruction

`NavigableMap` and `NavigableSet` expose range construction, navigation, and
iteration; they do not expose endpoint-admission status independently of
exceptions. An arbitrary implementation can either reject a range internally
or propagate the same exception object from a user callback. The outer wrapper
observes an identical failed interface call. It cannot safely infer which
branch ran from the exception object.

Recovery requires another operation in the internal-rejection case. The frozen
failure contract prohibits further callbacks after a user failure. A probe,
retry, whole-owner scan, or neighbor lookup cannot generically satisfy both
requirements. Eagerly replacing the original range call also changes the
comparison schedule. This is an information boundary in the current public
interfaces, not evidence that filtered navigation is intrinsically impossible.

There are two sound directions: provide a participating owner operation that
separates invalid ranges from thrown failures, or explicitly revise the
observable comparison contract for generic wrappers. This task does not silently
take the second direction.

## Target resolution design: JDK owner control flow

When filtered navigable wrappers are received into an actual integrated owner,
the existing `TreeMap.NavigableSubMap` can provide a package-private operation
with the following contract:

1. Run the current `inRange(query, inclusive)` exactly once.
2. If it returns false, return an explicit invalid-range result, such as a null
   range view. This branch is normal control flow, not caught user failure.
3. If it returns true, construct the same ascending or descending head/tail
   view as the current public operation, with the same constructor comparisons.
4. Let all comparison, constructor, and iterator exceptions propagate unchanged.

Extract the current ascending/descending constructor expressions into
package-private unchecked builders used by both the existing public methods and
the new internal operation. Existing public `headMap` and `tailMap` keep their
validation, exception classes/messages, and callback order. The integrated
filtered wrapper uses the normal iterator from a returned view, or the existing
bounded-owner scan after an explicit invalid-range result. It adds no extra
range validation on the valid path.

`TreeMap.KeySet` and exact JDK `TreeSet` owners can route through their privately
owned map to the same operation. An integrated `java.util` receiver can access
these package-private boundaries without reflection or changing public
interfaces. No new payload store, per-entry object, global service, native
symbol, or retained endpoint cache is needed.

The final `com.m3.collections.M3TreeMap`/`M3TreeSet` proving owners can use the
same pattern around the existing `endpoint` check and slot iterators. That
owner-level qualification does not establish qualification for JDK or opaque
third-party wrappers. Every provider that hides an owner behind a public
wrapper must participate through its own admitted owner boundary or remain
explicitly pending. The owner operation cannot discover an opaque provider's
private range metadata.

This design is source-supported and unimplemented in this diagnostic pass.
Before admission, test ascending and descending nested bounds; every
inclusive/exclusive combination; empty views; comparator-equivalent stored
representatives; arbitrary and interior null ordering; every comparator failure
position; iterator-creation failures; key-set paths; and exact callback tapes on
both valid and invalid query paths. Preserve OpenJDK licensing for any JDK-file
change and create it through the canonical recipe, not a manual target patch.

## Read-only failure: observed defect and preserved effects

`M3FilteredLists.StrictReadOnlyList.iterator()` wraps `AbstractList.iterator()`.
That iterator observes the strict wrapper's `modCount`. The wrapper's `get` and
`size` forward to a filtered view whose physical scans construct fresh backing
iterators. A structural change in the actual backing owner therefore does not
invalidate the retained outer indexed iterator.

The new witness demonstrates this for exact ArrayList and LinkedList owners,
both after an append and after removing/reinserting an element so contents and
size are restored. The owner's already-created iterator throws
`ConcurrentModificationException`; the filtered read-only iterator returns the
next visible element `4`. Checking only current size or contents is not a
generation check.

Changing the strict wrapper to use its filtered delegate's iterator would
restore the backing cursor, but changes required effects. Strict iterator
construction currently performs no predicate callbacks. The filtered delegate's
iterator construction runs `physical(0, true)` and immediately tests `[1, 2]`
in the included fixture. The existing `readonly-effects` oracle also requires a
failed size scan with tape `[1, 2, 4]` to allow a subsequent indexed `next()` to
return `2`, extending the tape to `[1, 2, 4, 1, 2]`. Replacing that path with a
terminally failed filtered cursor is not equivalent.

### Target resolution design: physical generation ownership

An internal generation read from the actual owner can augment the indexed
reader without evaluating predicates or invoking extra caller methods. A
future integrated JDK list receiver can use the existing owner modification
count and actual root links for its admitted backing types. Existing final M3
list owners can expose the same package-private generation boundary. Capture
the correct physical generation when the iterator binds and validate it at
the admitted read operations while retaining the original indexed predicate
path and unconditional mutation refusal.

This requires the actual owner and view-root identity. An opaque arbitrary
`List` does not expose its modification count. Calling an extra source cursor,
reading values, or eagerly binding a snapshot can change wrapper callbacks,
failure order, or snapshot semantics. Copy-on-write, reversed, and subList
owners therefore require their own source-supported contracts; no blanket
generation policy is inferred from the `List` interface.

### Actual M3 list ownership available for a future design

`M3ArrayList` is final, stores an `Object[]` and size, and uses the inherited
`AbstractList.modCount`. Its inherited iterators and subLists follow that
structural epoch; capacity growth and selected trim/sort operations also advance
it. It does not currently expose a package-private generation accessor. A
package-private accessor added inside the owner could read the existing field
without a transport read or new retained state. A wrapper outside `java.util`
cannot read another subclass's inherited protected field directly.

`M3LinkedList` is final and stores references plus primitive slot links/masks.
Its existing `changed()` advances its `modCount` and its already-created reverse
facade's `modCount`; `reversed()` shares the actual `Storage` and initializes the
reverse epoch. This is a real source-owned reverse link. Its ordinary subList is
still the inherited JDK wrapper with private root metadata, so a root accessor
alone would not establish a generic subList admission path.

The classes named `M3CowList`, `M3FrozenList`, and `M3IndexedList` currently expose
scaffold operations that throw `M3NotYetReceived`; their names are not evidence
of an implemented owner-generation capability. The existing primitive list
interfaces also do not establish the reference `List` contract being diagnosed.
These are target-resolution observations, not an implementation plan admitted in
this pass.

An earlier scratch-only exact-ArrayList/LinkedList auxiliary-cursor experiment
compiled but was not behaviorally qualified. It is excluded from this package,
replay inputs, and admission; its arbitrary applicability boundary was not
accepted as a generic repair.

## Rejected cursor mutation guard

The sealed rejected `M3FilteredLists` postimage adds a `next()`/`previous()`
transport probe before indexed `add`, `set`, and `remove`. It treats a caught
`NoSuchElementException` as an exhausted, validated iterator. The admitted
product keeps its pinned `M3FilteredLists` source instead of receiving this
change. The source atom was proposed by M3JDK21 PR 1538, head
`a02f0d912ff9da60a5dbc0a7832d7f4b56260fa0`; the snapshot here is the exact
forward-reconciled candidate that was reviewed, not a claim that it is the full
donor file.

### New failure on a previously valid write

A lawful lazy `AbstractList` stores `[1, 2, 3]`. Loading physical index 2 throws
a stable caller exception; reading index 1 and writing that index are supported.
After the even-filtered cursor delivers `2`, the original `set(8)` invokes only
`set(1, 8)` and succeeds. The rejected probe first reads physical index 2, throws
the identical caller exception, and prevents the valid write.

| Required write oracle | Pinned source | Rejected guard |
| --- | --- | --- |
| `transport` | Exit 0; tape `[set:1:8]`; stored 8 | Exit 1; same caller exception; tape `[get:2]`; stored 2 |

No concurrent modification, reentrant mutation, or stateful filter is needed.
A cursor movement that restores its index can still execute a new source read
and change the outcome. This baseline-to-candidate regression independently
rejects the donor guard.

### The exact JDK subList guard remains incomplete

Create a `LinkedList` with `[2, 4]`, an empty or full physical subList, and native
and filtered iterators positioned at the span's end. Add and then remove `6`
through the same span. Contents and size are restored, the span remains valid,
and the existing physical iterators are stale.

`AbstractList.SubList.listIterator().next()` checks its span `hasNext()` before
calling the underlying root iterator. At exhaustion it throws
`NoSuchElementException` without checking the root generation. The rejected
guard catches that exception and then mutates through the valid span. A direct
mutation on the original native iterator rejects the same stale epoch with
`ConcurrentModificationException`.

| Required subList oracle | Native iterator | Filtered pinned and rejected source |
| --- | --- | --- |
| `empty-add` | CME | Exit 1; writes owner to `[8, 2, 4]` |
| `exhausted-add` | CME | Exit 1; writes owner to `[2, 4, 8]` |
| `exhausted-set` | CME | Exit 1; writes owner to `[2, 8]` |
| `exhausted-remove` | CME | Exit 1; writes owner to `[2]` |

Trying `previous()` after exhausted `next()` can reach the owner for a nonempty
span, but the empty span still has no transport element to read. It also retains
the new-read regression. Source mutation is not an acceptable validation probe.
Catching arbitrary mutation refusal and trying another write could suppress
caller failures or repeat a partially completed mutation. Preserving an actual
iterator's mutation authority is a separate design because the filtered cursor's
lookahead can change the physical last-returned element, and writable owners may
provide read-only/snapshot iterators. This pass does not substitute a larger
cursor redesign for a failed guard.

## Executable evidence

Run this package with a Java 21 JDK and a product checkout containing the
collection dependencies:

```bash
python3 run_diagnostics.py --product-root /path/to/M3JDK21 --java-home /path/to/jdk-21
```

An optional `--output /tmp/new-directory` selects a new or empty output directory.
The default is a fresh temporary directory. The script refuses output inside
product or catalogue trees, preventing historical source fixtures from appearing
as live duplicate Java targets. It copies the exact `.java.txt` bytes to proper
Java filenames only in that external directory, compiles with Java 21,
`-Xlint:all,-module`, and `-Werror`, and records commands, source seals, complete
logs, expected version-specific exits, and actual exits in `observations.json`.

Both versions compile against the same current checkout dependencies. The pinned
owner replaces the checkout's `M3FilteredLists` for baseline replay; the rejected
owner overlays that class for rejected replay. The receipt identifies both the
frozen owner revision and the actual dependency checkout revision and hashes.
It does not describe a later dependency checkout as the original complete tree.

The unchanged `M3WrapperConvergenceContract` is preserved byte-for-byte as a
`.java.txt` snapshot. The independent `M3CandidateListBoundaryProof` retains its
required-behavior assertions: its failures are executed and reported, never
converted to passing ordinary tests. `M3ComparatorBoundaryProof`,
`M3ReadonlyBoundaryProof`, and `M3ListMutationGuardCounterexamples` explicitly
assert documented defect observations. Their successful exits mean that the
defects or rejected effects were reproduced.

The four executable diagnostic sources live under the admitted source root
`src/test/java/com/m3/collections/` within this package:

* `src/test/java/com/m3/collections/M3CandidateListBoundaryProof.java`
* `src/test/java/com/m3/collections/M3ComparatorBoundaryProof.java`
* `src/test/java/com/m3/collections/M3ListMutationGuardCounterexamples.java`
* `src/test/java/com/m3/collections/M3ReadonlyBoundaryProof.java`

Their relocation changes only paths. The Java bytes, assertions, packages, class
names, and public APIs are unchanged. The historical `.java.txt` snapshots remain
at this package's root. The canonical Java engine's admitted-source policy is
preserved; no engine exception or ordinary-suite admission was added.

| Frozen original contract mode | Pinned owner replay | Rejected owner replay | Disposition |
| --- | --- | --- | --- |
| `comparator-failure` | Exit 1; expected comparator exception missing | Exit 1; same failure | Pending |
| `readonly-failfast` | Exit 1; expected backing CME missing | Exit 1; same failure | Pending |
| `bounded-map` | Exit 0; 64 checks | Exit 0; 64 checks | Existing result preserved |
| `bounded-set` | Exit 0; 64 checks | Exit 0; 64 checks | Existing result preserved |
| `interior-null` | Exit 0; 6 checks | Exit 0; 6 checks | Existing result preserved |
| `readonly-effects` | Exit 0; 8 checks | Exit 0; 8 checks | Existing result preserved |

`recorded-replay.json` retains the original executed commands, outputs, and source
seals unchanged as historical evidence. `recorded-replay-layout-amended.json`
records a fresh replay using the relocated sources and their byte-preserving
path provenance. `diagnostic-manifest.json` identifies the immutable snapshot
hashes, current source paths, and exact dispositions. A zero script exit verifies those
observations, including the listed nonzero required-behavior exits. It is not a
product acceptance metric, and the diagnostics are not placed in the ordinary
JUnit suite.

No source-built installed image, full JDK build, JNI gate, performance
qualification, or family-completion claim is made by these Java diagnostics.
