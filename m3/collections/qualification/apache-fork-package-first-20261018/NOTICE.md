<!-- Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
     SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Notice: Apache Commons Collections reference copies

Donor: Apache Commons Collections (`apache/commons-collections`), received from the fork `hsoliwal/commons-collections` at commit `2d985c27e07bdcbb9516aea44b0dda3f3e487ced`.

Licence: Apache License, Version 2.0. Every copy keeps its Apache Software Foundation licence header unchanged.

The four files under `reference/` are proof payloads of the parity workflow. They keep their bytes and carry
no Synexia contributor line. The `original/` copies are the donor files as received. The `relocated/` copies
differ from them only in lines that name the package (`org.apache.commons.collections4` becomes
`com.m3.collections.apache`).

Git blob of each `original/` copy and SHA-256 of every copy: `DONOR_SHA256.tsv` (checked against the files when
this notice was generated).

| Copy | Lines that differ from the original copy |
|---|---|
| `reference/original/org/apache/commons/collections4/BoundedCollection.java` | none (donor bytes) |
| `reference/relocated/com/m3/collections/apache/BoundedCollection.java` | 17 (package declaration) |
| `reference/original/org/apache/commons/collections4/queue/CircularFifoQueue.java` | none (donor bytes) |
| `reference/relocated/com/m3/collections/apache/queue/CircularFifoQueue.java` | 17 (package declaration); 32 (import) |

The facts above are generated from the receipts in this directory with git hash-object, sha256sum and diff.
