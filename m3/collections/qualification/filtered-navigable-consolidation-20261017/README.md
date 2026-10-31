# Filtered/sorted NavigableSet consolidation — M3JDK21 receiver

Status: **DRAFT / bounded collections receiver**, not collection-phase or java.util promotion.

Canonical recipe authority: [hsoliwal/com.synexia#12098](https://github.com/hsoliwal/com.synexia/pull/12098),
branch `m3/filtered-navigable-consolidation-20261017`, recipe commit
`bd70e19257c61288b2c43c0c153ee55429798c84`.

Target baseline: `master@4e29dc9d4141261e8763fbe9ffd6a20e017b1aff`.

This receiver refines the already admitted filtered/sorted collection family from Synexia #11995:

- `M3FilteredViews`: NavigableSet and NavigableMap operations make one backing range lookup then
  scan rejected values through an iterator. This preserves comparator/range behavior while avoiding
  repeated logarithmic tree lookups for each hidden value.
- `M3FilteredNavigableSet`: the older public compatibility class now delegates to
  `M3FilteredViews`; facade and range/descending instances retain the public M3 class.
- `M3FilteredNavigableFacadeSelfTest`: Java21 standalone randomized differential contract plus
  comparator-call work-shape check.

Public method descriptors do not change. The views remain live and read-only. Java predicates and
comparators remain Java callbacks. Existing `M3IndexMaskCompactor`, `M3IndexMaskWindow`,
`M3IndexPathMasks`, state-mask and JNI array-selection owners remain the native path for stable
primitive coordinates. This receiver deliberately does not send arbitrary object callbacks through JNI.

The global receiving sequence remains STRING -> arrays -> collections -> AST/compiler. This PR is
a reviewable collections proving-ground increment only.

## Selected proof

```sh
bash m3/collections/qualification/filtered-navigable-consolidation-20261017/verify.sh
# Broader module gate, when Maven/JUnit dependencies are available:
mvn -B -ntp -f m3/collections/pom.xml test
```

The standalone proof compiles only the exact owners and dependency-free oracles with Java21
`-proc:none -Xlint:all -Werror`, checks exact Git blob identities, runs the existing
`M3FilteredViewsContract` and the new consolidation oracle. It is not a full module/JDK/jtreg gate.

No donor challenge implementation is copied. LeetCode/HackerRank/GeeksforGeeks are category references;
OpenJDK/Guava are contract references. First-party target code remains Apache-2.0 under `m3/**`.
