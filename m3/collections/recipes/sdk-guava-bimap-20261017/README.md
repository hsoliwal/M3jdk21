<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Guava BiMap over M3 array-indexed maps — canonical recipe

This is a bounded Java 21 SDK implementation of the *actual* Guava BiMap interface, not a Guava lookalike and not a claim that the whole five-provider M3 SDK is complete.

**Authority:** Synexia owns this reusable Maven/OpenRewrite source-sealed recipe and category catalogue. M3JDK21 owns the installed `com.m3.collections.interop` runtime and real Guava-JAR qualification. The original public name stays `com.google.common.collect.BiMap`; the companion implementation is `com.m3.collections.interop.M3GuavaBiMap`. It uses *two existing* `M3LinkedHashMap` array/index owners as forward and reverse directories, without a retained per-mapping Entry object. Generic GC-visible reference values remain Java references. It does not introduce new JNI symbols, unsafe references, or a second collection storage engine.

## Per-file source recipe

The original M3GpuToolJoinRecipe/Manifest OpenRewrite SDK source-seal engine is copied **byte-identically** from first-party Synexia. The new M3GuavaBiMapRecipe only selects this task's SHA-256 manifest. It generates three ABSENT Java files, and applies exact-preimage transitions to M3LibraryCollections and sdk-family-bindings.tsv. If an existing target differs, it refuses; do not hand-edit generated output.

Run locally after installing Java 21 and Maven:

```sh
mvn -B -ntp -f synexia-openrewrite-recipes/recipe-crates/sdk-guava-bimap-20261017/pom.xml clean test
```

The M3JDK21 receiver mirrors the *same* source-sealed templates and SDK test; its exact-path Maven invocation additionally uses actual owner/source integration. The build binds the real Guava 33.4.8-jre provider already declared in optional interop pom.xml, rather than supplying lookalike classes.

## Contracts and gates

`put` enforces value uniqueness; `forcePut` evicts the old owner of a value; `inverse().inverse()` retains identity; the inverse, keys, values, and entry views remain live. Nullable keys and values follow Guava HashBiMap's permitted values, not Spring or Apache contracts. The implementation does not retain collection Entry objects as its storage representation. Indexed lookup costs are expected constant time but pathological collisions remain possible. Null/equality/hash behavior, callbacks and view/iterator effects require actual target and provider tests.

**Executed before publication:** selected-source `javac --release 21 -Xlint:all -Werror` and 324001 deterministic checks, using a test-only JDK-backed stand-in for the target M3LinkedHashMap and a minimal BiMap signature. This is *not* actual provider or real M3 compile proof. Real Maven/OpenRewrite SDK tests, Guava HashBiMap randomized JUnit, installed-M3-owner integration, API/inverse-view conformance, packaging, and full release/runtime performance gates are **PENDING** until head CI executes.

JNI is intentionally **not used** for generic Java object key/value hashing, equality or view mutation. The existing m3_index_mask_compactor C17 JNI library remains unchanged; the optional receiver workflow may independently rerun its native mask regression without claiming JNI acceleration for BiMap. No donor solution code from LeetCode/HackerRank/GeeksforGeeks or Guava was copied.

Source expression Copyright 2026 Hitesh Soliwal and contributors, Apache-2.0. Guava BiMap v33.4.8 API reference remains Guava-owned and Apache-2.0; original provider license/NOTICE must remain attached to the Guava binary dependency. Keep PR DRAFT until exact head CI is green. Preserve String → arrays → collections → AST/compiler qualification order.
