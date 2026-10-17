<!-- SPDX-License-Identifier: Apache-2.0 -->
# Exact wrapper CI tree-pin successor (2026-10-17)

Source recipe: `com.synexia.m3.M3Jdk21WrapperRecipeTreePin20261017`.
Target: `hsoliwal/M3jdk21` `.github/workflows/m3-filtered-sorted-receiver.yml`.

The inherited wrapper workflow hard-pins `m3/collections/wrappers/recipe` subtree
Git identity `53fb42686982de4906dc73c2d8c7ca4d687dc4a1`.
The current independently reviewed additive branch carrying canonical #1025 and
the new writable-map/AVL recipe has the exact subtree Git identity
`e4ccfffd308feca5a7eab1beb59d1d20b0d1300b`.

The independent `m3/collections/wrappers/verify.py` SHA-1 remains
`5c813e8aa766ae9bd90a8c3a7e54412a8b5281c8` and
`m3/collections/wrappers/proof` tree remains
`c901d303b718fb435b24a8fe1d42f40016a89fcf`.

This one-file source-sealed text recipe replaces ONLY the stale recipe subtree
literal, keeping all compilation, Maven, JNI, provenance, runtime and source
checks active. It uses the retained `M3HashPinnedTextSnapshotRecipe` and a
real LST-independent plain-text fixed-point/refusal JUnit, not regex patching of
target files. Its resource is copied into the target's isolated
`m3/collections/wrappers/workflow-pin-20261017/recipe` module so that
adding the receipt does **not** recursively change the pinned recipe tree.

Do not infer a green workflow merely from exact Git hashes. It must execute
the real Maven/OpenRewrite and Java/JNI steps on the target head, and the
strict separate installed-source ledger must still be resolved via M3JDK21 #1002.
Rerun and update the source recipe for any further *legitimate* recipe tree
changes; never weaken or bypass the gate.
