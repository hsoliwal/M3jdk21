<!-- SPDX-License-Identifier: Apache-2.0 -->
# Decorator fill: commons-collections decorators converged by atoms (2026-10-30)

This is leaf 1 of the commons-collections convergence lane: the `hsoliwal/commons-collections` fork converges into
M3JDK as atoms and patterns, not as file ports. Of the 88 apache scaffold modules from the five-provider inventory,
57 are decorators. This directory holds the generator, the ledgers and the proof for that family.

## Pattern DECORATOR_FILL v1.1

`M3DecoratorFill.java` treats each scaffold method as one atom. It resolves the atom by reflection on Apache Commons
Collections 4.5.0, which is the scaffold's surface version, and classifies it:

| Category | When | Emitted |
|---|---|---|
| OBJECT | declared by `Object` | dropped; the inherited method is used |
| DEFAULT | declared by an interface | `Iface.super.m(..)`, so the same default chain runs |
| THROW, DELEGATE, EQUALS, WRAP, FACTORY | Commons body of that shape | template over the `backing` lane |
| MANUAL | any other body | the class keeps its scaffold; the atom is listed in `METHOD_ATOMS.tsv` |

No provider body is copied. Views use the JDK `java.util.Collections` unmodifiable owners, or the class's own factory or
constructor. `PATTERNS.tsv` lists the categories and the next candidate categories.

## Result

| | v1.1 | v1.2 (SYNC family) |
|---|---|---|
| Method atoms classified (57 decorators) | 1,938 | 1,992 (with the view-helper atoms) |
| MANUAL atoms (next leaves) | 521 | 387 |
| Classes with no MANUAL atom, now owned | 7 | 12 |

The v1.1 classes are `M3UnmodifiableCollection`, `M3UnmodifiableList`, `M3UnmodifiableQueue`, `M3UnmodifiableSortedSet`,
`M3UnmodifiableNavigableSet`, `M3UnmodifiableBag` and `M3UnmodifiableSortedBag`.

v1.2 adds the Synchronized family: `M3SynchronizedCollection`, `M3SynchronizedBag`, `M3SynchronizedMultiSet`,
`M3SynchronizedQueue` and `M3SynchronizedSortedBag`. The categories are ACCESSOR, SYNC, GUARD, VIEW, FACTORY_NEW and
MODIFIER; their rows are in `PATTERNS.tsv`.

The categories came from an analyst-and-skeptic workflow per family. The skeptic confirmed every SYNC category and refuted
only the proof, which could not see lock scope or the true path of `equals`. Both gaps are closed in v1.2:
- `M3SyncProbe` reports BLOCKS or FREE for every method and view, for `equals(self)`, and for the monitor of a deserialized copy.
- `M3LockTraceProbe` records `Thread.holdsLock` at every backing call.
- `equals` now also receives an equal, non-identical copy of the backing.

Results are in `PROBES.tsv`: 5 classes × 2 probes, 0 mismatches. In a negative control, a planted unsynchronized `peek` is
caught by both probes.

The Predicated, Transformed and Unmodifiable-map families have proposals with skeptic refutations; they are the next
leaves. The refutations are classifier side conditions, the LinkedMap/LinkedHashMap order, serialization field order and
harness blind spots.

The v1.1 MANUAL atoms grouped into these families:

| Family | Atoms |
|---|---|
| SYNC_DELEGATE (Synchronized*) | 98 |
| TRANSFORM (Transformed*) | 45 |
| FACTORY_2ARG (Predicated*, Transformed* factories) | 40 |
| NO_FACTORY_CTOR (Composite*) | 38 |
| MAP_VIEW (map decorators) | 38 |
| FIXED_SIZE | 24 |
| other shapes | 238 |

## Proof

`M3DecoratorDifferential.java` runs a seeded reflective differential of each owned class against the Commons class,
which is used as a test oracle only:

- 8 seeds × 4,000 steps per class.
- Each step either mutates both backings directly, or calls one declared public method on both sides with equal arguments.
- Each step compares return values, exception classes, drained iterators and views (including mutation attempts on them), and the backing state.
- Factory null rejection, factory identity on an already unmodifiable argument, and a serialization round trip are each checked once.

Results:

- `DIFFERENTIAL.tsv`: 7 classes, 64,003 checks each, 0 mismatches.
- `MISMATCHES.tsv`: empty.
- All generated classes compile with `-Xlint:all -Werror`.
- Negative control: a planted defect (`removeIf` delegating instead of throwing) gives 50 mismatches.

`CONVERGE_LEDGER.tsv` lists every fork class, using the columns agreed between the convergence lanes: fork, fork_rev,
fork_class, m3_owner, absorb_state. The counts are 16 ABSORBED and 331 GAP.

## M3IFY primitives

The decorator family is one instance of a general sequence, recorded as the `M3IFY` rows of `PATTERNS.tsv`:

1. ATOMIZE: one atom per method.
2. RESOLVE: find the donor member by reflection.
3. CLASSIFY: map it into a closed category vocabulary, with MANUAL as the typed residue.
4. EMIT: generate the body from a template over M3 lanes.
5. PROVE: run a seeded differential against the donor, which serves only as the oracle.
6. GROW: a repeated residue shape becomes a new category, and every correction becomes a RECIPE_MEMORY row.

Any donor class library can be converged into M3JDK with the same six steps. Only the category vocabulary and the
templates change from one donor to another.

`TASK_PACKET.tsv` declares the M3-SCALE task packet for this run. `run.sh` writes the output contract:
`STATUS.tsv`, `RUN_CONTEXT.tsv`, `PROVENANCE.tsv`, `VERIFY_CONTRACT.tsv`, `OUTPUT_CONTRACT.tsv` and `FINAL_REPORT.md`.

## Rerun

From the repository root, with `JAVA_HOME` set to a JDK 21:

```bash
bash m3/collections/qualification/decorator-fill-20261030/run.sh \
  ~/.m2/repository/org/apache/commons/commons-collections4/4.5.0/commons-collections4-4.5.0.jar <unpacked 4.5.0 sources dir>
```

The driver reads the scaffolds from the pinned `SCAFFOLD_REV`, so reruns are idempotent. Generator corrections are
appended to `RECIPE_MEMORY.tsv` in the same commit as the fix.
