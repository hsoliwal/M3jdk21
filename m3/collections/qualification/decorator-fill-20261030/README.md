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

| | Count |
|---|---|
| Method atoms classified (57 decorators) | 1,938 |
| MANUAL atoms (next leaves) | 521 |
| Classes with no MANUAL atom, now owned | 7 |

The seven owned classes are `M3UnmodifiableCollection`, `M3UnmodifiableList`, `M3UnmodifiableQueue`,
`M3UnmodifiableSortedSet`, `M3UnmodifiableNavigableSet`, `M3UnmodifiableBag` and `M3UnmodifiableSortedBag`.

The MANUAL atoms group into the next families:

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

## Rerun

From the repository root, with `JAVA_HOME` set to a JDK 21:

```bash
bash m3/collections/qualification/decorator-fill-20261030/run.sh \
  ~/.m2/repository/org/apache/commons/commons-collections4/4.5.0/commons-collections4-4.5.0.jar <unpacked 4.5.0 sources dir>
```

The driver reads the scaffolds from the pinned `SCAFFOLD_REV`, so reruns are idempotent. Generator corrections are
appended to `RECIPE_MEMORY.tsv` in the same commit as the fix.
