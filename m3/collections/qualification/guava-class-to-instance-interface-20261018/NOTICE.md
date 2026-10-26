<!-- Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
     SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Notice: Guava test fixtures

Donor: Google Guava (`google/guava`) at commit `f06690fa3e874f65515e8fd338a74d636e2c792f`.

Licence: Apache License, Version 2.0, copyright The Guava Authors. Every fixture keeps its upstream licence
header unchanged.

The two files under `fixtures/` are proof payloads. They keep their bytes and carry no Synexia contributor
line: each Git blob equals the upstream blob recorded in `SOURCE_PROVENANCE.tsv`, and the upstream package
`com.google.common.collect` is kept. Compared with the received copies under
`m3/collections/implementations/guava-33.4.8/guava-tests/test/com/m3/collections/guava/collect/`, they differ
only in lines that name the package (`com.google.common` here, `com.m3.collections.guava` there).

Upstream and result Git blob and SHA-256 of every fixture: `SOURCE_PROVENANCE.tsv` (checked against the
files when this notice was generated).

| Fixture | Upstream path | Lines that differ from the received copy |
|---|---|---|
| `fixtures/com/google/common/collect/ImmutableClassToInstanceMapTest.java` | `guava-tests/test/com/google/common/collect/ImmutableClassToInstanceMapTest.java` | 17 (package declaration); 19 (static import); 25 (import); 26 (import); 27 (import); 28 (import); 29 (import); 30 (import); 31 (import) |
| `fixtures/com/google/common/collect/AndroidIncompatible.java` | `guava-tests/test/com/google/common/collect/AndroidIncompatible.java` | 17 (package declaration); 26 (import); 35 (Javadoc reference) |

The facts above are generated from the receipts in this directory with git hash-object, sha256sum and diff.
