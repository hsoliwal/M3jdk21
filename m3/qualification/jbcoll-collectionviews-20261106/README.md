# jbcoll-collectionviews-20261106: census leaf L001, `java/util/CollectionViews.java`

M3JDK convergence census leaf L001 (java.base collections, lane owner com-synexia-45). Master
`cf0429ac1d49c99371258e4de943d0c74e150711`; donor hsoliwal/com.synexia develop
`ca1aafc4d94f9dd1ee288226dcca070cc60991c9` (the census pin). This leaf is also the CollectionViews owner review that
gates the two HELD java.base twins of the loss-guard restore.

**Verdict: no product edit.** The live owner already holds every semantic line of both crates. The 7 census lines
are 2 lines the owner later replaced on purpose (SUPERSEDED) and 3 line-wrap or operand-order differences
(FORMAT_EQUIVALENT). Applying the 2 SUPERSEDED lines would be a regression: the owner's own jtreg test fails, and
CopyOnWriteArraySet subclasses lose bulk removal.

**Owner review: no consumer, so the helpers stay held.** CollectionViews calls neither
`IdentityHashMap.collectionViewIdentityKind` nor `Collections.collectionViewMembershipSource` (OWNER_REVIEW.tsv).

## Census rows

| census row | crate | postimage blob (sha256) | status | missing lines |
|---|---|---|---|---|
| L001 `CollectionViews.java` | collectionviews-current-owner | `540ebf535672` (`736b9f6df1d5`) | PARTIAL, added_present=23/28 | 5 |
| L001 `CollectionViews.java` | m3-collectionviews-onepass-cow-20261023 | `3252ddc179bc` (`d0713f7a8c94`) | PARTIAL, added_present=3/5 | 2 (shared with the 5) |
| crate tests (7 files) | both | | APPLIED / APPLIED_SUPERSET | already on master, under `test/jdk/java/util/CollectionViews` |

I re-ran `census.py` for these 3 crates at the census pin, and it reproduced the census rows exactly. I then re-ran
it at `cf0429ac1d49`: the rows and per-row diffs are unchanged.

## The 7 lines (EDITS.tsv, recipe `apply.py` JBCOLL_COLLECTIONVIEWS v1)

| rule | verdict | census line (crate) | owner line | evidence |
|---|---|---|---|---|
| R1, R2 | SUPERSEDED | `backing.getClass() == CopyOnWriteArrayList.class` / `... CopyOnWriteArraySet.class` (both crates) | `backing instanceof CopyOnWriteArrayList<?>` / `... CopyOnWriteArraySet<?>` | see "Why R1/R2 are superseded" below |
| R3 | FORMAT_EQUIVALENT | `DISTINCT \| SORTED \| ORDERED` | `DISTINCT \| ORDERED \| SORTED` | the same constant, 0x15 |
| R4, R5 | FORMAT_EQUIVALENT | one line | wrapped at 100 columns | same tokens |

Why R1/R2 are superseded:
- 29d78c3564c (2026-10-23) landed the onepass-cow `getClass()` dispatch.
- eb55f07a82c (2026-10-27) and b9533511bdb (2026-10-28, PR #2055) replaced it with `instanceof` and kept the
  indexed-removal fallback ("Snapshot iterators refuse removal even for CopyOnWrite subclasses").
- The crates' product baseline `b032048eece5` (2026-10-23) predates both commits.
- `FilteredBulkMutationContractTest` encodes the owner's later contract ("CopyOnWriteArraySet subclass mutation").

How the recipe works:
- `apply.py` pins the owner blob `4a1fcb4ea517`.
- It requires each owner anchor exactly once and each census line absent from the owner.
- With `--donor-git`, it checks each census line against the pinned crate blobs (DONOR.tsv).
- It refuses drift with exit 2. The test was one appended byte: `REFUSED drifted preimage`.
- It rewrites EDITS.tsv to a fixed point. The first run reports `changed=0 plan=CHANGED`; the second reports
  `changed=0 plan=UNCHANGED`.
- `--whatif OUT --rules ...` writes a counterfactual copy outside the repository. The differential and tests use it.

## Differential (CowViewsDifferential.java, classify.py; seed 0x4d334a42434f5731, 1,000,000 ops, 27 subjects)

Subjects:
- ArrayList, LinkedList, ArrayDeque
- CopyOnWriteArrayList and CopyOnWriteArraySet, plus their subclasses and sub-lists
- nested filters
- HashSet, LinkedHashSet, TreeSet with its descendingSet and subSet
- an identity set
- keySet/values/entrySet of HashMap, keySet/values of TreeMap, keySet/values of IdentityHashMap

Operations: removeIf, removeAll, retainAll, clear, remove, contains, add, addAll, size, isEmpty, iteration,
stream count, and iterator remove.

The reference side is a model written with stock JDK 21 calls only. A filtered view is the backing restricted to the
predicate. Bulk removal is `backing.removeIf(visible && condition)`. Membership means a stored equivalent exists and
is visible, using the owner's own equivalence: identity for the IdentityHashMap views and the identity set. The
patched side is `java.util.CollectionViews` on stock JDK 21.0.9 with `--patch-module java.base`.

Every differing line is classified. A line is ROOT when the subject's state was still equal before it, and CASCADE
otherwise. A ROOT line must match a class in classify.py.

| pair | differing | roots (class) | cascade | unclassified |
|---|---|---|---|---|
| stock model vs owner | 100 | 32 IDENTITY_VALUES_EQUALS (13 contains, 19 remove; IdentityHashMap.values only) | 68 | 0 |
| owner vs owner+R1+R2 (census hunk) | 65,918 | 563 COW_SUBCLASS_BULK_UOE (CowSetSub and nested CowSetSub: removeIf/removeAll/retainAll/clear) | 65,355 | 0 |
| stock model vs crate current-owner postimage | 200,515 | 1,631 (COW_SUBCLASS_BULK_UOE, CRATE_COW_NO_FALLBACK_UOE, IDENTITY_VALUES_EQUALS) | 198,884 | 0 |
| stock model vs crate onepass-cow postimage | 200,511 | 1,634 (same classes) | 198,877 | 0 |

What the table shows:
- Every CopyOnWrite subject, sub-list, nested filter, list, set and map view behaves the same in the owner as in the
  stock model.
- The census hunk changes behaviour only for CopyOnWriteArraySet subclasses. Their bulk removal throws
  UnsupportedOperationException, because the subclass's snapshot iterator refuses `remove()` and the owner's indexed
  fallback covers Lists only.
- The two full crate postimages also lack that fallback. They refuse bulk removal on CopyOnWriteArrayList subclasses
  and on CopyOnWrite sub-lists too.

IDENTITY_VALUES_EQUALS is behaviour the owner already had; this leaf did not introduce it. For an
IdentityHashMap.values() view, FilteredCollection.contains and remove first confirm membership with the owner's
identity `contains`. They then take the first `Objects.equals` occurrence. If an identical value is hidden and an equal
value is visible, the view reports it visible, or removes the equal one instead. This is the case the held
`collectionViewIdentityKind` helper served in its retained lineage. It is recorded as an open item, not changed here.

## Owner review of the held twins (OWNER_REVIEW.tsv)

The consumer search was `git grep -E 'collectionViewIdentityKind|collectionViewMembershipSource'` over the whole
master tree, plus both crate postimage blobs.
- Live `CollectionViews.java` has no hits, and neither crate postimage does.
- The only callers are the retained `CollectionViews` variants pr1619 (lines 318/321) and pr1627 (lines 282/285). They
  are in the FilteredCollection constructor of the FilterNode/identityKind lineage.
- Both retained variants are `CONFLICT=1` against live in `JDK_TREE_3WAY_20261106.tsv` (base=NONE).
- Restoring the two helpers alone would add two package-private methods that nothing calls.
- Verdict: **no consumer: helpers stay held.**
- Release condition: a future CollectionViews leaf adopts identity membership for the IdentityHashMap views. That would
  fix IDENTITY_VALUES_EQUALS and give both helpers a consumer.

## Gates (GATES.tsv; `verify.sh`)

| gate | result |
|---|---|
| recipe | PASS. 5 rules: apply=0, superseded=2, format_equivalent=3. Donor blobs 2/2. changed=0, plan=UNCHANGED |
| patch_compile | PASS. `javac -g -Xlint:all -Werror -Xlint:-this-escape -XDstringConcat=inline --patch-module java.base=<dir>` (the restore qualification's java.base flags). The owner, the census-hunk counterfactual and both crate postimages each compile to 9 classes with 0 warnings |
| format_equal | PASS. With `-g:none`, owner and owner+R3+R4+R5 produce 9 byte-identical class files |
| differential | PASS. 0 unclassified across the 4 pairs |
| tests | PASS. The 13 jtreg tests of `test/jdk/java/util/CollectionViews` run as plain mains under `--patch-module` (no jtreg on the host): 13/13. The counterfactual fails exactly `FilteredBulkMutationContractTest` |
| naming | PASS. `jdk_naming.py` plan/apply over the leaf dir: moves=0, rewrites=0, refusals=0 |
| scope | PASS. Only `m3/qualification/jbcoll-collectionviews-20261106/` |

Not run: jtreg itself. M3CollectionViewsTest is out of scope: it tests `Collections.filtered*`, which is backed by
`M3CollectionViews`, not by this class.

Rerun: `JAVA_HOME=<JDK 21> bash verify.sh <com.synexia clone> ca1aafc4d94f9dd1ee288226dcca070cc60991c9 <scratch> 1000000 <base>`
