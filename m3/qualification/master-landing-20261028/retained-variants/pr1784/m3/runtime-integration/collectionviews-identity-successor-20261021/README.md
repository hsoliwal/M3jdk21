<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21: identity-safe filtered Set successor

This is an **in-place behavioral atom** in the existing `java.util.CollectionViews`
of the integrated M3JDK21 Java 21 product. No M3-on-JDK adapter, alternate
collection store, per-entry node graph or JNI ABI is introduced.

## Exact owner and donor chain

The pinned parent is `m3/java-base-filtered-cursor-reentry-20261021-gpt6`
at `ef24da4c5224ca0f728db3c3c30fa412592c7765`.
Its `CollectionViews.java` source Git blob is
`1df992f3258e3b8573003172d9b7ea7083ac5e18`.

The bounded successor Git blob is
`3a4258b2846c311d812883314d7c178f32feeabb`,
SHA-256 `b739779bddf9a89ec1c0266fbbe70923101cd139974e76c917ed2f45648315fc`.

Canonical first-party recipes are Synexia
[#12865](https://github.com/hsoliwal/com.synexia/pull/12865) (reentrant cursor)
and [#12866](https://github.com/hsoliwal/com.synexia/pull/12866)
(stored occurrences). Corresponding product donors are M3JDK21
[#1770](https://github.com/hsoliwal/M3JDK21/pull/1770)
and [#1773](https://github.com/hsoliwal/M3JDK21/pull/1773).

## Correctness rationale

The evolving parent already handles hidden equal List occurrences, backed
sorted representatives, nested reentrant selection callbacks and SORTED
metadata. However, its generic `Set` path evaluates ordinary
`Objects.equals` against stored values without first checking whether the
concrete Set owner admits that probe. An identity-backed Set can thus claim
that an equal-but-distinct object exists.

The successor changes only these two private behavioral operations:

- `contains`: preserve the owner's membership contract through
  `backing.contains(candidate)`, prefer exact stored reference identity,
  then evaluate the predicate on the admitted stored representative.
- `remove`: apply the same owner admission, remove the visible stored
  reference rather than an earlier equal identity, and preserve normal
  owner `remove` exceptions and mutation semantics.

The parent’s List index deletion, sorted/navigable map/set comparators,
reentrant Spliterator state, ordered cursor metadata, original arrays and
native masks are unchanged. The Set-specific additional admission may require
another scan: **no performance improvement is claimed.**

## Verification

Run from a checkout that retains the pinned parent commit:

```sh
bash m3/runtime-integration/collectionviews-identity-successor-20261021/verify.sh \
  /absolute/path/to/new-output-directory
```

The verifier:

1. SHA-pins the exact parent and successor Git blobs and successor SHA-256.
2. Compiles each real `CollectionViews.java` source with Java 21
   `--patch-module java.base` and compares exported `javap` descriptors.
3. Requires `FilteredRepresentativeContractTest` to reproduce the exact
   identity-backed Set failure on the parent.
4. Runs the successor through the stored-occurrence and identity test
   in default/interpreter/C1/C2 configurations, plus the reentrant callback,
   sorted metadata, bootstrap and collection-view contract suites.

The workflow executes these checks if a GitHub runner actually runs the
steps. A script or scheduled job is **not proof of PASS**. Full built
M3JDK21 runtime, wider jtreg/JCK, JNI/GC/HotSpot, source-sealed serial
OpenRewrite fixed-point replay and source licensing admission remain
separate required gates. Keep this as a draft until those gates are reviewed.
