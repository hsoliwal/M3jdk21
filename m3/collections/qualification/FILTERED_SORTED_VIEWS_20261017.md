# Filtered and sorted collection views — Java 21 source-receiving packet

**Status:** source-sealed candidate installed in separately built `m3/collections`;
hosted contract/JNI gate not yet reported PASS. No JDK `java.base` source
replaced and no String-phase advancement.

## Exact Synexia owner

- Canonical donor `hsoliwal/com.synexia:develop@11c94feb18155a53cc213708c353791132edc4a7`.
- Recipe/proof branch and PR:
  [Synexia #11995](https://github.com/hsoliwal/com.synexia/pull/11995).
- Named OpenRewrite recipe:
  `com.synexia.m3.Jdk21FilteredSortedViews` delegated to
  `M3HashPinnedJavaSnapshotRecipe` and `M3HashPinnedTextSnapshotRecipe`.
- Java postimage manifest SHA-256:
  `828e24b44e4d0b98c6819bb9dd983d71084ae0661e1d81fe90bd0dd286c50ede`.
- Metadata/CI text-recipe manifest SHA-256:
  `4bcb1eb0b9d032ffb9ba60c09817440b7c56d7f4ead4e851190f1754828e787f`.

All six Java files are the original Synexia first-party sources and existing
differential oracles with only a package rename to `com.m3.collections`.
No new competing filtering algorithm or runtime dependency was introduced.
The original OpenJDK `java.util` interfaces and contracts are unchanged.

## Installed algorithms by contract

- **Read-only lazy live views:** `M3FilteredViews.filter` for
  `Collection`, `Set`, `SortedSet`, `NavigableSet`; and
  `filterEntries` for `SortedMap`, `NavigableMap`. All writes refuse.
- **Mutable backed views:** `M3MutableFilteredViews.filter` for
  `NavigableSet`, `filterEntries` for `NavigableMap`. Only visible
  entries are changed; rejected insertions fail; comparator/range,
  descending, null-map-value, and iterator mutation rules are tested.
- **Primitive-lane precedent:** existing `M3LongFilteredView` and
  `M3LongSortedViews` retain their independent no-boxing behavior. This
  receiver does not duplicate them.
- **JNI boundary:** `M3IndexMaskCompactor` and
  `m3_index_mask_compactor.c` remain the optional native batch path for
  precomputed primitive coordinate/state masks; object predicates,
  comparators and reference ownership remain in Java.

The new source receipt under
`m3/collections/recipe/filtered-sorted-views-20261017/manifest.tsv`
contains exactly six sorted `ABSENT -> sha256` additions. Historical
`installed_source_manifest` and its hash in `synexia-donation.json`
are **unchanged**. The existing `verify_donation.py` is extended by the
canonical sealed text recipe to admit the six known source paths only when
the new exact manifest matches its pinned SHA-256. Unknown files, malformed
rows, duplicate names, missing/aliased receipts or changed bytes fail
closed; an absent successor does not alter legacy behavior.

## Acceptance commands

```sh
python3 -m unittest discover -s m3/collections -p 'test_verify_*.py' -v
# Pinned JAVA_HOME (Java21) and JUNIT_CONSOLE_JAR required for full suite:
python3 m3/collections/verify_donation.py --cost-probe
# Existing JNI mask proof checks native C17 and JDK-backed Java reference.
```

The original GitHub workflow `m3-collections.yml` now discovers both
old and new source-verification tests; its installed-collections job
continues to run the full Java21/JUnit, `-Xcheck:jni` and allocation
qualifiers. A successful source receipt or fixed-point recipe alone is
not Java21 runtime/JNI acceptance.

## Catalogue and donor comparison

Synexia catalogue
`synexia-openrewrite-recipes/catalogs/m3jdk21-filtered-sorted-views-20261017.tsv`
includes the category, asymptotic cost, LeetCode/HackerRank/GFG reference
shapes, Guava/Commons/JDK donor decision and exact Java21 oracle.
Competitive-programming sites are algorithm references, **not source-copy
authorities**. Guava live filtered views and Apache Commons predicated
admission differ by mutation/visibility contract, so the canonical
first-party Synexia algorithms were reused.

When source paths drift, correct and rerun the canonical recipe/receipt;
never hand-fix generated outputs to force a green gate.
