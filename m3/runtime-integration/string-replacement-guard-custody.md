<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Replacement guard recipe custody

Canonical repository: `hsoliwal/com.synexia`.
Canonical recipe revision: `f3afaa3ff8f9bff6e2251097d6403884a21d6664` (PR #10551).
Canonical crate: `synexia-openrewrite-recipes/crates/m3-string-replacement-guard`.
Canonical Java owner: `synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3StringGuardRepair.java`.
Existing manifest kernel: `M3GpuToolJoinManifest.java`, Git blob
`38df5f97bbeec8a8a8cfc77091d56bda925c07f2`, unchanged from Synexia revision
`96bc2d38982814585c785eaa287a52dfc3dbf228`.

The six files mirrored below `m3/vendor/synexia/` preserve their exact Synexia
relative paths and bytes. They are authoring/proof tooling, not a dependency of
`java.base`, HotSpot or JNI. The canonical POM and README remain in Synexia.
Apache-2.0 terms are retained; the existing full license is
`m3/history/licenses/Synexia-Apache-2.0.txt`. New first-party expression remains
Copyright 2026 Hitesh Soliwal and contributors. OpenJDK licenses are unchanged.

The mirror is independently hash-checked by
`m3/runtime-integration/string-replacement-guard-sha256.txt`.
The recipe modifies only the source invariant checker and generates its two new
proof files plus an application receipt. `String.java` is a hash-pinned KEEP owner.

Execution is KERNEL_REPLAY through the existing manifest/scan kernel, not
OpenRewrite SDK_EXECUTION. The Java route guard uses the JDK compiler parser,
not a second Java parser. Passing the structural or recipe tests does not certify
runtime equivalence. Full matching-JDK image, jtreg/native and repository checks
remain authoritative. Promotion and release are NOT_AUTHORIZED by this packet.
