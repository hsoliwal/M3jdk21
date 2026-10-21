# M3JDK21: comparator-stored representatives in filtered Sets (2026-10-21)

**Product:** M3JDK21. **Owner:** `m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java`. This is an implementation correction to the existing M3JDK21 collection family, not a separate compatibility layer or new wrapper type.

**Review topology:** This PR is deliberately **stacked on PR #1503** (`codex/m3-filtered-sorted-readonly-forward-20261019`) because that parent already modifies `M3FilteredViews`. It adds a narrowly source-pinned successor without overwriting other filtered/sorted candidates (#1522) or altering existing JDK public APIs. After parent admission, review the successor with normal ancestry, then reconcile other outstanding collection changes in reactor/contract order.

## Reproduced contract bug

A `TreeSet<String>` with `String.CASE_INSENSITIVE_ORDER` contains the stored object `"Alpha"`. When accessed through the `Set` or `Collection` overload of `M3FilteredViews.filter`, the existing generic membership scan used `Objects.equals("alpha", "Alpha")`, returning false despite the owner's comparator accepting the alias. More importantly, a filter must evaluate against the **stored representative**, not the query alias. Nested M3 private view layers can hide the original `SortedSet` owner.

The successor resolves only known private M3 `CollectionView`/`SetView` chains iteratively (O(depth), no recursive Java calls or per-element allocations) until it finds a sorted owner. When membership has already been established, a `NavigableSet` selects its retained representative with `ceiling(alias)`, with no temporary tail-range wrapper. A non-navigable `SortedSet` uses `tailSet(alias).first()`. A generic or identity-backed `Set` **retains the prior equality and exact-reference scan**.

The existing parent-owned objects and live changes remain authoritative. We do not turn detached index masks into live predicate views, change comparator ordering, allocate per-row structural objects, change public interfaces, or cross JNI. The stored reference and nullable comparator semantics are explicitly tested. Read-only no-op writes must still throw.

## Reusable serial Java LST recipe

The existing exact-source OpenRewrite `com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe` remains the transformation engine. The new recipe `com.synexia.rewrite.M3FilteredStoredRepresentative20261021` is target-qualified for the exact parent PR #1503 source epoch.

- Original SHA-256: `af1752c4b02eb7a7753424727e58b7c9cd38c3e4c28191ce7499c12d54be7c68`.
- Product postimage SHA-256: `d84bde6ee27d6ff7e83a64423865fa83506949069a77ca7470c53a1cfb170f8b`.
- New proof SHA-256: `ead20dcb382d3bde0aed937af39b20ed509af3014b57fda3ce25b39b5b195866`.

The crate under `m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-filtered-representative-20261021/` contains the original-before fixture, exact postimage, test postimage and two-row manifest. Modify the reusable recipe and replay it **serially per source file**, refusing unknown drift and confirming a fixed point. This is a source/AST admission packet, not yet a claim that full Maven/OpenRewrite source-built dependency closure or JDK runtime gates have passed.

## Executable regression and evidence

```bash
bash m3/collections/qualification/filtered-comparator-representative-20261021/verify.sh
```

This standalone Java 21 verifier checks exact source/template SHA-256 and original preimage, compiles the unmodified PR #1503 owner and asserts it fails **only** on the known comparator-alias regression, then compiles the postimage and executes the exact same seeded contract suite. Coverage includes direct/nested Set and Collection overloads, SortedSet/NavigableSet/descending traversal, case-insensitive aliases, comparator-admitted null, identity-backed and hash-backed sets, live owner updates and immutable mutation refusal.

The original comparator failure was independently reproduced on a small Java 21 source fixture. The proof class itself was compiled with `javac --release 21 -Xlint:all -Werror`; full product-owner source compilation is delegated to the focused GitHub CI. No unsupported performance benchmark or complete Maven test claim is inferred.

## Algorithm catalogue and donor rights

See `m3/collections/algorithm-catalog/filtered-sorted-representative-20261021.tsv`. Guava's mutable filtered-set contract is **not** copied; it differs from M3JDK21's strictly read-only view. TreeSet comparator/ceiling navigation, LeetCode lower-bound search shapes and HackerRank BitSet are evaluated as algorithm patterns only; no third-party donor code is copied or relicensed.

## Remaining gates

Parent PR #1503 has historical frozen-source recipe guards that reject newer postimages. They are intentionally **not** altered here: a qualified successor manifest must be explicitly admitted in the owning recipe lineage. Also pending: actual Maven/OpenRewrite execution/fixed point, full owning module and native/java.base releases, jtreg/JCK, merged source/ABI acceptance, desktop/SWT integration, and Android ART/ARM64 memory/energy/compatibility testing. This child PR must remain draft until its standalone owner contract passes and its parent/recipe closure is accepted.
