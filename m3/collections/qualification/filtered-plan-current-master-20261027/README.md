# M3JDK21 master-native filtered predicate-plan pass (2026-10-27)

Source owner: `m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java`.

This is a *fresh current-master* refinement. The predecessor [PR #1497](https://github.com/hsoliwal/M3jdk21/pull/1497) and its child [#1876](https://github.com/hsoliwal/M3jdk21/pull/1876) use older source revisions. Copying the whole prior file would discard master's newer **array-indexed membership scans, comparator splitting, native tree range delegation and provenance headers**. This branch changes only the iterator's repeated predicate-plan construction and retains every other master atom byte-for-byte.

- Parent source Git blob: `25e1ba23e0269cd6226e9f0b76fd9a43c6bb1d59`
- Candidate source Git blob: `45ad6df3a51e83c67dc70de7c7acd82bec2305d3`
- Selected operation: lazy immutable predicate-coordinate plan, cached per view using `volatile` publication and a single first-use synchronized computation
- No cached elements, selected row snapshots, JNI Java-reference transfer, new external runtime dependency or public API
- Fresh backing iterator per traversal; predicates evaluated in original innermost-first order
- Prospective tradeoff: remove O(depth) per-iterator array construction, increase retained O(depth) predicate references per deep view. No measured speedup/retained heap improvement
- New master-specific source pin and tests must be revalidated before merging; older source seals remain historical and immutable.

`M3FilteredSortedIteratorPlanTest` and `M3FilteredSortedIteratorPlanSelfTest` were copied with their exact previously passing Git blobs; they must be **rerun on current master**. CI runs the complete current `m3/arrays` and `m3/collections` owner compilation and selected JUnit, including dependencies such as real `M3TreeMap`; not a stub.

Execute from the repository checkout:

```sh
bash m3/collections/qualification/filtered-plan-current-master-20261027/verify.sh
```

The outer `java.base` runtime, JNI-native viewport and full source-built JDK image are not touched or qualified by this pass. Retain existing library licenses and donor provenance.


## Broader selected atom coverage (2026-10-27)

Added `M3FilteredMasterPredicatePlanLifecycleTest` (four JUnit 5 tests)
with pinned Git blob `f6a8b6f3905ad10a5a0405ecb95f588fd83a68e2`.
The selected Maven gate now also runs existing
`M3ReadOnlyMapDefaultsTest`, `M3FilteredViewsTest` and
`M3FilteredNavigableSetTest`. These are intentionally rerun against the
**current** source; their old sealed receipt hashes remain untouched.

See [RECEIPT_RECONCILIATION.md](RECEIPT_RECONCILIATION.md) for evidence
classification of historical filtered workflows. A new test source or
product source hash mismatch refuses before test execution. These new
tests are **committed but not yet claimed passing** at this revision.


## Expanded current-master execution receipt

[GitHub Actions run #40512782665](https://github.com/hsoliwal/M3jdk21/actions/runs/40512782665)
**passed at Git head `c60bce17bb568a0cb76ba55a5d8f9bdc7b78941c`**.
The history audit verified all eight original source Git objects and reported
one expected changed current owner. The actual `m3-arrays` and
`m3-collections` modules compiled successfully under Java 21, then the
following selected JUnit 5 classes ran with **zero failures, errors or skips**:

| JUnit class | Tests |
| --- | ---: |
| `M3FilteredMasterPredicatePlanLifecycleTest` | 4 |
| `M3FilteredSortedIteratorPlanTest` | 5 |
| `M3ReadOnlyMapDefaultsTest` | 1 |
| `M3FilteredViewsTest` | 4 |
| `M3FilteredNavigableSetTest` | 4 |
| `M3FilteredMasterTreePlanTest` | 2 |
| **Total** | **20** |

The companion standalone filter contract reported
`M3FilteredSortedIteratorPlanSelfTest checks=29`.
The action ran all tests on the **selected source revision**; it is not a
full Java/JNI native parity run, complete JDK image, full jtreg, nor an
all-atom coverage proof. This receipt applies to the above Git head;
subsequent documentation-only commits must not be labeled with fresh
execution evidence without their own run.
