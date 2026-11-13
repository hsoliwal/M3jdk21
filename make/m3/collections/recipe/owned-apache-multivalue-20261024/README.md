<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Apache multivalue v2 -> v3 receiving recipe (D1)

`com.synexia.m3.ApacheMultiValueV2ToV320261024` runs the unchanged canonical engine
`M3HashPinnedJavaSnapshotRecipe` (PlainText custody mode) on crate `apache-multivalue-v2-to-v3`: the exact v2 bodies
of `M3AbstractListValuedMap`, `M3AbstractMultiValuedMap` and `M3AbstractSetValuedMap` (the v2 postimages of
`owned-apache-multivalue-20261018`, kept here as `*.before.txt`) become the counter bodies, and
`M3MultiValuedMapCounterDifferentialTest` is installed. `M3MultiValuedSizeBench` (src/jmh) lies outside the engine's
admitted Maven roots and is pinned by the v4 receipt only. Missing, drifted or duplicated
preimages are refused before any output.

`M3ApacheMultiValueV2ToV3RecipeTest`: 32 transition states (every subset of already-transitioned bodies, the new test
absent or present, both orders), each with a fixed point, and 11 refusals. Recipe custody is not runtime proof; the
differential test and the contract self-tests qualify the bodies.
