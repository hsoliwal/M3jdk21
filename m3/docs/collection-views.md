<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Collection views

The M3JDK21 product owns `java.util.CollectionViews` in the original
`java.base` module. This is an integrated JDK implementation, not a
separate `M3+` compatibility library. Its filtered and sorted views
retain their original JDK names and the backing collection remains
the sole element owner.

The stored-representative refinement preserves correct equality/identity
and removes a selected physical List occurrence without accidentally
deleting an earlier hidden equals-equivalent occurrence. Sorted/navigable
views, stable ordering and native collection mask symbols remain unchanged.

## Java 21 qualification

```sh
bash m3/compatibility/collection-views/verify.sh /new/absolute/evidence-path
```

The verifier compiles exact previous/current `java.util.CollectionViews`
sources using `--patch-module java.base`, compares the public
`javap -public -s` descriptor list, expects the prior defect to fail
the first oracle assertion, runs the focused oracle in default,
interpreter, C1 and C2 modes, and runs the existing bootstrap and
collection-view contract tests. It does not build a complete JDK image.

The test-side previous class body is an exact preimage fixture, not a
parallel implementation. Detailed source pins, algorithm trials, donor
catalogue, historical execution logs and dated receipts remain in
canonical Synexia, not public filenames. Target-source admission
remains contingent upon Maven/OpenRewrite, full installed-image,
jtreg/JCK, JNI/GC, cross-platform resource and license gates.
