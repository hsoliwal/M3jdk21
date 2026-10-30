<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 wave-3 pairwise coordinate refusal refinement

The existing flat primitive categorical pairwise test matrix remains unchanged
for every valid row/column. An invalid negative column could previously access
another row's physical array slot; this exact, versioned Synexia recipe makes
`Cases.at(row,column)` reject all row/column coordinates outside the retained
matrix geometry. It does not introduce per-cell objects, a new interface, or
another public SDK module.

The original `M3IndexedSystemsContract` is extended by five independent refusal
cases. JIT and `-Xint` each PASS `M3_WAVE3_INDEXED_SYSTEMS_PASS checks=17102688`.
The complementary CTL/CSP test still passes 202532 checks per JVM mode.

This correction is candidate source, not Maven/OpenRewrite real scheduler or
full JMOD/image/jtreg proof. Keep the PR draft. Product source comes only from
the corresponding qualified Synexia preimage/postimage recipe; no recipe exists
in the public product fork.
