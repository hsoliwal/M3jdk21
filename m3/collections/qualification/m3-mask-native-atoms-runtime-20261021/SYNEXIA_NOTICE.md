<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Attribution and retained source

Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
The authored native-only recipe adaptation, verification driver and documents
use Apache-2.0. Exact source fixtures and templates retain their existing
file-level notices. This notice does not assign a blanket license to M3JDK21,
OpenJDK or any external repository.

The source packet is `hsoliwal/com.synexia` PR #12650 at
`99fe40f05b8c0594427e7b9a062f5fd4791a8734`, specifically
`synexia-openrewrite-recipes/recipe-crates/m3-mask-range-atoms-20261021` and its
hash-pinned Java/text resource directories. The harness, POM, receiver copier
and isolated copier checks are narrowly adapted from that packet. The four
canonical Synexia snapshot/scope engine owners are reused unchanged. The older
harness/copier lineage is Synexia PR #12605 at
`b6c35b9a1f528dd9e5a3d9f6f4c51ca360db43d7`, as retained in the source packet.
`HARNESS_PROVENANCE.json` and `PUBLISHED_LINEAGE.json` pin those origins.

The native baseline and unchanged Java parser-owner fixture originate from
`hsoliwal/M3JDK21` at `7c0d2493edbf93ac5cb726667ce83046d3519e3e`:
`m3/collections/src/main/native/collections/m3_index_mask_compactor.c` and
`m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java`.
Their Apache-2.0 file notices remain intact. The intermediate/final native
postimages and new Java regression are exact copies of the qualified Synexia
packet. The affine-position pattern reuses the existing Java
`M3IndexMaskCompactor.identityPositions` implementation pattern. The preserved
native source lineage and tests remain in the sibling native qualification
packet; this crate does not claim authorship of copied source or rewrite its
notices.

The prior catalogue records GitHub donor comparisons and LeetCode, HackerRank
and GeeksforGeeks problem categories. This native-only composition copies no new
challenge/editorial implementation body. Category references do not grant a
license to copy third-party code. No OpenJDK or Guava implementation body or
new production dependency is introduced by this packet.

Historical evidence remains in the sibling packet and is reused by exact hash,
with no newly claimed native execution. Maven/OpenRewrite dependencies and the
JDK are external build inputs rather than redistributed binaries. Exact new
qualification logs retain their original bytes; generated evidence is labeled
as such in the file provenance ledger. This packet is source and evidence for
review, not a declaration of receiving admission or full JDK qualification.
