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
