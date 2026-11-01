<!-- SPDX-License-Identifier: Apache-2.0 -->
# Synexia String derived-cache receipt — M3JDK21

Status: `SOURCE_PIN_ONLY`. This is a target-side mapping and applicability decision, not a runtime port or acceptance receipt.

## Canonical donor

- Repository: `hsoliwal/com.synexia`
- Merged source PR: [#10406](https://github.com/hsoliwal/com.synexia/pull/10406)
- Source branch head incorporated by that PR: `e8f16525b04d26c80973b08aaf01df0068a797c6`
- Merge commit: `b57663033e5de3d21dcdcbcc6b5b35f6142bd88d`
- Production owner: `synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexTupleReferences.java`, Git blob `d1030ade51d01121e30977f37e6241bc95b71b61`
- Canonical recipe: `com.synexia.rewrite.M3StringDerivedCacheRecipe`, source path `synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3StringDerivedCacheRecipe.java`, Git blob `403fbbf278405a9452db0d5a1c33e43aa3c6f66a`
- Recipe template manifest: Git blob `0bd222a8566d0be10c04d7f3475a2a795770c2a3`
- Source regression: `MIndexTupleDerivedCacheTest.java`, Git blob `6b6276bedf9adc6ecd46f097c7714ac14f73c16b`
- Recipe regression: `M3StringDerivedCacheRecipeTest.java`, Git blob `07d7df2af3c09d1d8eb3a4cc413491f5f116aa17`
- Source proof report: `synexia-openrewrite-recipes/verification/string-derived-cache/README.md`, Git blob `3041719e185e8f7cf29f5b64fda61d6e32d2566f`

Synexia's merged receipt reports four reentrant-cache regression cases, 103 Java 21/JNI tests, four OpenRewrite SDK tests, and apply/replay changing 2 then 0 files. These are **donor-side reported results**, not target acceptance.

## Target mapping

- Existing target owner inspected: `src/java.base/share/classes/java/lang/M3StringTuple.java`, blob `0d4cfb75317e68d54244588a474eba98bf7df27b`.
- Existing fact owner inspected: `src/java.base/share/classes/java/lang/M3StringFacts.java`, blob `fcd108ab6181fec11d16eda811b29d18298f3fb1`.
- Target counterpart: `M3StringTuple.computeFacts()` delegates to `M3StringFacts.compose(left.facts(), right.facts())`. In the inspected path, the facts are computed from target-owned children; there is no Synexia-style external resolver callback at that boundary.
- Decision: **do not mechanically copy the `Derived` record or patch target runtime based only on donor similarity**. The exact Synexia bug is triggered by a same-node, same-resolver reentrant callback. Current target evidence does not establish an equivalent callback path or defect.
- Recipe disposition: eligible for `EXTERNAL_RECIPE_TOOL` under the existing Apache-2.0 intake policy. The recipe remains canonically owned by Synexia; M3JDK21 must not gain a runtime dependency on Synexia.
- Runtime disposition: `NOT_APPLICABLE_UNLESS_TARGET_REENTRANCY_IS_DEMONSTRATED`. A future target-specific regression must exercise the actual M3 owner and prove either that the defect cannot occur or that a target-owned fix is required.

## License and compatibility boundary

Synexia's first-party recipe/test/fixture assets are Apache-2.0 candidates. Preserve `Copyright 2026 Hitesh Soliwal and contributors`, SPDX, attribution, and modification notices. The production donor file is mixed `Apache-2.0 AND BSD-3-Clause`; its embedded Protobuf-derived atoms keep their BSD-3-Clause notice and are not reclassified. No donor source body is copied into the GPLv2-with-Classpath-exception `java.base` tree by this receipt.

## Target proof status

- Target preimage: pinned above for `M3StringTuple` and `M3StringFacts`.
- Target source/API/JNI changes: none.
- Target compile/JUnit/jtreg/native/runtime: `NOT RUN` for this receipt.
- Hosted Actions in the current target repository have returned failed workflow records with empty job lists; those records are not a target test result.
- No receiver family or promotion phase advances from this source pin.

This receipt ensures the Synexia capability is catalogued in the receiving pipeline without pretending that source similarity, Apache-2.0 eligibility, or a donor-side pass proves a JDK runtime port.
