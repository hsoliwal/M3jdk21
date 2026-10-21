<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Attribution and source lineage

Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
The new task composition, focused trace probe, JUnit adapter, verification
adaptations and documentation use Apache-2.0. Exact source templates and
fixtures retain their existing file-level notices. This notice does not assign
a blanket license to M3JDK21, OpenJDK or an external repository.

The wrapper baseline and shared owners originate at
`hsoliwal/M3JDK21@7c0d2493edbf93ac5cb726667ce83046d3519e3e`. The selected
first-match donor is `M3MutableFilteredViews.java`, Git blob
`37b669dfe22a3b6be9e68aa59f9dd94e96fc3d49`; the receiving wrapper baseline is
Git blob `513a104b589b7834cf873cd46054d233b5302d74`. The new wrapper
postimages retain those notices and add a modification notice. Existing
bounded/navigation/wrapper oracles are preserved at their pinned source bytes.

The recipe harness, POM, copier and emitted-verification pattern derive from
Synexia PR #12650 at `99fe40f05b8c0594427e7b9a062f5fd4791a8734`, particularly
`synexia-openrewrite-recipes/recipe-crates/m3-mask-range-atoms-20261021`.
The sibling native-only task supplied the narrowed task-identity pattern. The
four canonical snapshot/scope owners are reused without changes. The earlier
harness lineage is PR #12605 at
`b6c35b9a1f528dd9e5a3d9f6f4c51ca360db43d7`.
`HARNESS_PROVENANCE.json` records adaptation details and exact hashes.

The retained `M3FilteredRangeFailureContract` fixture is the prior Synexia
packet's authored failure probe. It is used only to verify that this different
candidate preserves the old failing observations; it is not a receiving target
and does not import that packet's intentional production catch repair.

LeetCode Search Insert Position, HackerRank Ice Cream Parlor and GeeksforGeeks
Linear Search were reviewed for category/compatibility comparison. They are
reference-only. No problem statement, editorial or third-party implementation
body is included; no performance claim transfers from a challenge runtime.
The first-party existing owner supplies the adopted implementation pattern.

JDK, Maven, OpenRewrite and JUnit binaries are external build dependencies and
are not redistributed by this packet. The thin verification launcher invokes
the pinned standard JUnit Platform APIs. Generated evidence retains the
observed bytes and is separately identified in provenance. Publishing this
packet is not receiving admission or runtime promotion.

## Target receiving metadata

This target packet receives only the three source paths named in
`RECEIVING.json` from canonical Synexia recipe
`com.synexia.m3.FilteredFirstVisibleRuntime20261021` at
`0a8af61d5ca4361612b46aa071a14d215e328fc3`, published through `https://github.com/hsoliwal/com.synexia/pull/12666`.
The canonical notice above is retained verbatim. Target-specific receiving
metadata and documents use Apache-2.0 under the same stated attribution.

The target stack is M3JDK21 #1642 at
`6d9bad615851f346e3062e3034ccd74cbe2776af`; wrapper/donor source provenance
remains #1603 at `7c0d2493edbf93ac5cb726667ce83046d3519e3e`. Product source
retains its per-file notices and the wrapper's modification notice. The
existing separate collection module LICENSE/NOTICE remain authoritative.
The two previously qualified native-lane files are outside this recipe's
receiving allowlist and must retain their existing postimages.

Canonical qualification and fresh target application/test observations are
separate receipts. This metadata grants no receiving admission, blanket
license, public-contract expansion or global runtime qualification.

The target verification driver adapts the existing selected-source target
verification and canonical D emitted-source verification patterns, retaining
the existing focused and range oracles. `RECEIVING.json` pins those paths and
source hashes. It consumes the canonical output and performs acceptance checks;
it is not another source transformation owner.
