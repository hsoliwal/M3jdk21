<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 exact workflow-pin V2 — after pinned-provider tests were repaired

The earlier source-sealed workflow pin V1 admitted the legitimate wrapper recipe
tree SHA `e4ccfffd308feca5a7eab1beb59d1d20b0d1300b`, then hosted
CI [run 39016934427](https://github.com/hsoliwal/M3jdk21/actions/runs/39016934427)
passed that identity gate and reached Maven, where two recently added Java
recipe tests did not compile against the older pinned `M3HashPinnedJavaSnapshotRecipe`
because they called absent `targetPaths()`/`preimageManifest()` helpers.

The canonical Java LST test-repair recipe
`com.synexia.m3.M3Jdk21PinnedProviderTestCompat20261017` has now
source-sealed and regenerated precisely those two test files, preserving
their original `Recipe.run`/fixed-point/refusal proof while deriving
identity facts from the existing hash-pinned manifests. That legally
changes the wrapper recipe directory to verified new Git tree
`879db8ef909678f6eb63994e5b9138c498a21e79`.

This V2 **separate** text snapshot recipe changes only the workflow's
tree pin from `e4ccfffd...` to `879db8ef...`. Its before workflow
Git blob is `48d5b40eb42498b6e2cf7ed834687c409fe713c9`;
its reviewed after-workflow blob is
`fe753525cc3360697f915c4cd0aed1b57df2b6b6`.
The existing standalone verifier blob
`5c813e8aa766ae9bd90a8c3a7e54412a8b5281c8`
and proof tree `c901d303b718fb435b24a8fe1d42f40016a89fcf`
remain unchanged.

Execute actual OpenRewrite `Recipe.run`, test second-pass fixed
point, preimage/occupied/drift refusal before receiving exact workflow
output in M3JDK21. Copy V2 source/test into isolated target Maven module
`m3/collections/wrappers/workflow-pin-20261017/recipe`
to avoid a recursive pin change. Do not disable any of the inherited
Maven, Java21, JNI, provider or installed-source gates.

Only the currently pinned branch is admitted. A further wrapper recipe
change requires another reviewed successor, never last-writer-wins.
