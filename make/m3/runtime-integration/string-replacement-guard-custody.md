<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Current target acceptance and historical recipe custody

The current target workflow runs product-owned verification only. Canonical
Maven/OpenRewrite authoring, replay, refusal and fixed-point qualification remain
in `hsoliwal/com.synexia`, in the existing task crate
`synexia-openrewrite-recipes/recipe-crates/m3jdk21-migration-verification-20261112`.
The successor dataset is `com.synexia.m3.MigrationVerificationContinuation20261112`.
The [passive continuation receipt](../qualification/migration-verification-20261112/continuation-receipt.json)
binds its exact execution commit and received output hashes.

The workflow verifies both the unchanged historical custody manifest and
`string-replacement-target-sha256.txt`, which seals the current product inputs
and target tests. It never invokes the historical public recipe runner. The
existing compiler-parsed Java route guard now checks the complete flat fallback,
empty-target overflow, exact exception message and UTF-16 insertion loop, in
addition to every earlier canonical and regex route. All 22 earlier replacement
regressions are retained; 14 regressions cover previously unguarded statements.

The complete source invariant checker recognizes the current immutable provenance
spellings, mutable-array snapshot boundaries, exact cursor comparison and the
exception-safe local-atom publication helper. Its pool regressions preserve the
57 earlier semantic mutation cases (four anchors adapted to their current owner)
and add 23 rollback/full-content-key cases. Current frozen-index source is checked
against its complete seeded UTF-16 key, exact duplicate authority, bounded
construction, legacy compatibility builder and original exact payload comparisons.
The String jtreg schedule remains mandatory, including canonical-DAG, streams and
position-owner tests; private recipe tests are refused in product workflows.

The position-precompute owner retains one byte-offset lane and one long-mask lane.
Its published primitive-byte ceiling is corrected to count those lanes exactly
once, plus block signals. The unchanged product invariant test rejects the former
23,330,816-byte estimate and accepts 19,136,512 bytes. This is an accounting repair,
not a claim of measured heap or native-memory reduction.

Source/structural tests and isolated Java owner tests do not qualify an installed
M3JDK21 image. Matching-image build, jtreg, JNI/GC and runtime evidence remain
independent gates; no receiving phase or release status is advanced here.

## Preserved original custody record

The original record below describes the earlier proof packet. Its mirror wording
is historical, not permission to author or execute recipes in this product.
The six historical files and their old receipts remain unchanged migration residue;
the current workflow checks their bytes without executing them.

<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
### Original replacement guard recipe custody

Canonical repository: `hsoliwal/com.synexia`.
Canonical recipe revision: `f3afaa3ff8f9bff6e2251097d6403884a21d6664` (PR #10551).
Canonical crate: `synexia-openrewrite-recipes/crates/m3-string-replacement-guard`.
Canonical Java owner: `synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3StringGuardRepair.java`.
Existing manifest kernel: `M3GpuToolJoinManifest.java`, Git blob
`78d5fb68f13872164973a8918cc7886c5f548bca`, unchanged from Synexia revision
`96bc2d38982814585c785eaa287a52dfc3dbf228`.

The six files mirrored below `m3/vendor/synexia/` preserve their exact Synexia
relative paths and bytes. They are authoring/proof tooling, not a dependency of
`java.base`, HotSpot or JNI. The canonical POM and README remain in Synexia.
Apache-2.0 terms are retained; the existing full license is
`make/m3/history/licenses/Synexia-Apache-2.0.txt`. New first-party expression remains
Copyright 2026 Hitesh Soliwal and contributors. OpenJDK licenses are unchanged.

The mirror is independently hash-checked by
`make/m3/runtime-integration/string-replacement-guard-sha256.txt`.
The recipe modifies only the source invariant checker and generates its two new
proof files plus an application receipt. `String.java` is a hash-pinned KEEP owner.

Execution is KERNEL_REPLAY through the existing manifest/scan kernel, not
OpenRewrite SDK_EXECUTION. The Java route guard uses the JDK compiler parser,
not a second Java parser. Passing the structural or recipe tests does not certify
runtime equivalence. Full matching-JDK image, jtreg/native and repository checks
remain authoritative. Promotion and release are NOT_AUTHORIZED by this packet.
