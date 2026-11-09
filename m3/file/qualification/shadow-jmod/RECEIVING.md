<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Original M3JDK21 Java21 shadow I/O — one-module jlink receiver

**M3JDK21 is the integrated JDK.** This is a bounded product workflow
postimage from [canonical private Synexia PR #13248](https://github.com/hsoliwal/com.synexia/pull/13248),
already merged. It is not an M3-on-JDK overlay, a duplicate Java/C JNI
implementation, or an executable public OpenRewrite recipe.

This normal **draft review branch** starts from exact current
`master@9dd1df62f3d5406760427cb5e840a36c9e7a59ea`. One original
workflow, `.github/workflows/m3-native-shadow-stream-progress-20261020.yml`,
was updated through the authorized GitHub contents API, using its
before Git blob `c4ced36104123241e0176256783045b8f860cfc1`. After
readback Git blob `ca323f4e24950b8d7b5c3b3747f508d926068a6a`
**exactly matches** the original canonical private Synexia named-recipe
template, with only three shell-line edits:

1. Create the existing compiled-module directory and a **separate** JMOD directory.
2. Put the original `com.m3.file.jmod` under that separate directory.
3. Restrict the `jlink` JMOD search path to that one directory, not `build`
   which contains the compiled named module **and** its JMOD.

The product remains single-source. Existing strict
`javac --release 21 -Xlint:all,-module -Werror`, original normal and
`-Xint` shadow contract, JMOD/JLINK calls and host/linked-image tests
remain unchanged in scope. No Java SE API/ABI, module descriptor, JNI
symbol, runtime collection/array owner, or M3File class was modified.

## Actual historical failure and independently confirmed baseline

The old workflow encountered `FindException: Two versions of module
com.m3.file` when both compiled `module-info.class` and
`com.m3.file.jmod` were discoverable through `build`. An isolated strict
Java21 negative/positive jlink test previously reproduced and corrected
that structural issue. The exact current master before source remains
byte-identical to the Synexia recipe preimage; the after source is identical
to its reviewed postimage. This is **source proof, not current-head CI PASS**.

Importantly, [merged M3JDK21 #1984](https://github.com/hsoliwal/M3jdk21/pull/1984)
already executed **all 16 original M3File contracts** on host and linked
image, [run 40338194492](https://github.com/hsoliwal/M3jdk21/actions/runs/40338194492)
PASS. This workflow focuses separately on the original
`M3ShadowStreamsContract` in normal, `-Xint`, and JMOD-linked modes.

The separate original public
`m3-file-shadow-stream-20261020` Maven recipe-custody workflow
[run 40338194889](https://github.com/hsoliwal/M3jdk21/actions/runs/40338194889)
**still FAILS** in `M3TaskRecipeTest.qualifyRawByteHashRecipe` because the
named YAML was historically absent from its active public Maven dataset.
Canonical source custody was moved into [Synexia #13251](https://github.com/hsoliwal/com.synexia/pull/13251).
This receiver does **not** import the private recipe, duplicate the YAML,
skip that test, change the historical gate, or claim its failure resolved.

## Release/promotion hold

Actual current-review-head shadow host/`-Xint`/linked-image CI, other
required M3JDK21 product checks, private canonical Maven/OpenRewrite SDK
(focused jobs still blocked without a runner), source-built installed JDK,
jtreg/JCK/HotSpot/JIT/GC/CDS/JNI, five-provider API/ABI and measured
CPU/heap/native-process cost remain separate admission gates.

**Draft only. No automatic merge, rebase of develop, force-push,
protected-master write or public executable recipe import.**
