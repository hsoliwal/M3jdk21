# Wrapper boundary refinement — canonical Synexia concept/recipe capsule

Copyright 2026 Hitesh Soliwal and contributors. Apache-2.0.

This is a focused refinement of the existing Synexia purpose wrappers received in
M3JDK21 #992 at `11dde1eac33cea2a8b90baba8c155eabec341144`. The convergence copy is
canonical authoring material; the M3JDK21 copy is exact target custody. Existing
`M3HashPinnedJavaSnapshotRecipe` is reused unchanged, with the existing Maven
recipe module. No new wrapper engine, storage owner or runtime dependency.

## Implemented atoms

- Mapped lists delegate iteration and subranges to their source. Sequential lists
  no longer traverse by repeated indexed get; RandomAccess is retained correctly.
- Flat-mapped emptiness and stream traversal use the existing lazy cursor without
  a hidden size scan. The spliterator binds on first traversal and does not claim
  exact cardinality, sorting, distinctness or non-null values. JDK batch splitting
  may allocate bounded batches; this is not a zero-allocation parallel claim.
- `size()` saturates as the Collection interface requires instead of throwing.
- The existing checked JNI proof now composes flat mapping, filtering and stable
  sorted snapshots before primitive projection. Its C kernel is unchanged.

Public factory signatures, read-only behavior, duplicate/null preservation and
backed ownership remain. Callback timing intentionally becomes lazy on prefix
queries; no promise to evaluate unused mapper callbacks is introduced. Ordinary
owner iterator interference and subList invalidation are preserved, not replaced
by a new synchronization policy. Overflow exception removal corrects a defect in
Collection conformance, not a compatibility promise to retain that defect.

## Recipe and reproduction

`changes.json` defines exact pre/post seals and unique behavioral-block edits.
`prepare.py` prepares immutable templates only, from the pinned Git preimage.
It never treats its authoring substitution as the OpenRewrite qualification.

From the target root:

```
python3 m3/collections/wrappers/boundaries-20261017/prepare.py
mvn -B -ntp -f m3/collections/wrappers/recipe/pom.xml \
  -Dtest=M3WrapperBoundaryRecipeTest \
  -Dm3.boundary.output=/tmp/m3-boundary-sdk-output test
python3 m3/collections/wrappers/boundaries-20261017/prepare.py \
  --install-sdk-output /tmp/m3-boundary-sdk-output
```

Install accepts only actual SDK bytes matching every reviewed postimage and only
exact before/already-after target files. It checks all admission conditions before
serial writes. Partial replay accepts only exact installed postimages. Repair
`changes.json`/fixture/recipe and rerun; never repair generated Java by hand.

The JUnit suite checks typed generation, exact outputs, fixed point, each missing
or drifting preimage, occupied additions, duplicate and non-Java inputs, partial
resume and preservation of unrelated Java. Generated templates, SDK test and
resulting source are part of the receiving PR, not only an external plan.

## Searchable catalogue, ownership and licensing

`CATEGORY_REVIEW.tsv` uses the existing eight-column catalogue schema. Reuse
`m3/collections/counts/src/main/java/com/m3/collections/tooling/M3CountedCatalogue.java`
as a Java source launcher to search this ledger by literal category or purpose:

```
java m3/collections/counts/src/main/java/com/m3/collections/tooling/M3CountedCatalogue.java \
  m3/collections/wrappers/boundaries-20261017/CATEGORY_REVIEW.tsv lazy
```

Mapping disposition: existing `M3MappedViews` and `M3CollectionWrappers` remain
canonical owners; `M3CollectionViews` remains the filter/sorted-snapshot owner;
JNI remains the existing test-only projection peer. No java.base/HotSpot ABI or
public JDK naming changes. This work does not advance the repository's ordered
String -> arrays -> collections -> compiler receiving-stage gate.

All changed implementation expression is first-party Apache-2.0 source already
in the target or newly authored first-party code. JDK APIs are called, not copied.
Guava and challenge sites inform category evaluation only; no solution bodies
are imported or relicensed. Native fold lineage is unchanged.

## Evidence boundaries

Local Java21 strict compilation and the actual refined owner passed 211,585
assertions. The real saturation boundary consumed 2,147,483,647 elements and
passed two more assertions. The old preimage fails the cost oracle with 20,000
indexed gets; the candidate performs zero. An isolated elapsed-time print is
not a comparative benchmark, retained-memory result or performance superiority
claim. Hosted Maven/SDK and checked JNI results must be read from this PR's own
workflow; previous PR results are not substituted. Full reactor/JDK/jtreg/JCK,
concurrency linearizability, serialization, GC/reference counters, compiler
lowering and memory-pressure lifecycle remain distinct unclaimed gates.
