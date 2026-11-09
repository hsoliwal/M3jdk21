<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Notice and source custody

Copyright © 2026 Hitesh Soliwal and Contributors to the Synexia Project.
First-party task recipe code, fixtures, documentation and changes are
Apache-2.0. The complete license is retained at `licenses/Apache-2.0.txt`.

The canonical recipe owner is `hsoliwal/com.synexia`, within
`synexia-openrewrite-recipes`. This task composes the existing
`M3HashPinnedJavaSnapshotRecipe`, `M3HashPinnedTextSnapshotRecipe`,
`M3EditScope` and `M3ScopeInference` owners pinned to
`81c4f19a5ce5a68da316ff6f2802f2041a684f31`. The existing five-test SDK harness
and thin Results receiver are reused. M3JDK21 owns its receiving product,
public contracts and target qualification; a permitted authoring mirror
does not become another canonical recipe owner.

Frozen receiving preimages originate at M3JDK21
`d47a4b4fdb8e3794ec56768b06aa9e23730c6b8e`. Their bytes and original notices
are preserved. First-party postimage modifications dated 2026-10-17 add
method-local prepared escape masks, the `SWAP` copy correction, the exact
harvester input-binding rename and the regression test/provenance shard.

Reviewed Synexia `M3StaticUsePlan`, `MIndexHornClosure` and `MIndexCsrGraph`
sources retain their Apache-2.0 identity. Their admission, primitive CSR and
monotone worklist patterns inform the refinement; implementation bodies are
not copied. Exact paths and Git object identities are in
`ALGORITHM_CATALOGUE.tsv`.

The gate compiles unchanged repository-owned ASM 9.6 core/tree/analysis
sources, whose BSD-3-Clause notices remain in the receiving repository.
No ASM source is re-licensed or embedded into the recipe implementation.
HotSpot `escape.cpp` is GPL-2.0-only evidence; no body enters this Apache
payload. Challenge/problem pages are taxonomy references only, with no
statement, editorial, solution or submission copied. The license of this
recipe does not relicense any third-party, OpenJDK or retained source.

Build executables and the pre-resolved Maven SDK/JUnit dependencies are
toolchain evidence. Compiled classes, jars, private Maven settings, caches
and transient proof trees are excluded from the publishable recipe packet.
