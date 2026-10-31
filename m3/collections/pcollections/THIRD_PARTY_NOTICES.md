<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->

# PCollections superset notices

The `org.pcollections` module is the unchanged MIT-licensed donor at
[`hrldcpr/pcollections` revision `1f2212e7bf935451ea421ca3d499ef3c96c45e5c`](https://github.com/hrldcpr/pcollections/tree/1f2212e7bf935451ea421ca3d499ef3c96c45e5c).
Its complete production Java surface and original test sources reside in
`m3/vendor/third_party/pcollections`. Original copyright headers remain in every
donor file. The exact upstream `LICENSE` bytes are stored as `LICENSE.txt`, with
that path-only mapping recorded in `DONOR_PROVENANCE.tsv`. No upstream Java file
is renamed or rewritten. The Maven adapter is separately Apache-2.0 licensed.

The new `com.m3.pcollections` classes implement the donor interfaces and extend its
unmodifiable bases. They are independently authored Apache-2.0 implementations
of documented paged sequence, bitmap-ranked hash trie, binary search, packed
ordered-slot, and stable primitive-index merge algorithms. Donor concrete classes,
factories, serialization layouts, and documented/historical behavior remain
available in the separate module.

Existing Apache-2.0 `com.m3.collections` owners supply live filtered/sorted wrappers
and the JNI primitive mask selector. Their notices remain authoritative. The
canonical Synexia recipe's algorithm catalogue records evaluated GitHub donors,
LeetCode/HackerRank/GFG problem references, licenses, rejected approaches and
verification rationale. Problem descriptions and illustrative third-party code
were not copied into the runtime classes.

The proof suite uses the original PCollections tests without rewriting them,
JUnit Platform Console Standalone 1.12.2 (EPL-2.0) and AssertJ Core 3.27.7
(Apache-2.0). These are test inputs, not shipped runtime module dependencies.
The preserved upstream Gradle file specifies older test dependency versions;
the Maven adapter and receipts explicitly identify the executed proof matrix.

No automatic Jackson deserialization of the new M3 concrete classes is claimed.
The accessible `hsoliwal/jackson-datatypes-collections` fork provides integration
references, but its PCollections deserializers select donor concrete classes.
