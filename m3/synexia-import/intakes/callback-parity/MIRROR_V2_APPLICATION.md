<!-- SPDX-License-Identifier: Apache-2.0 -->
# Callback mirror-v2 application - 2026-10-10

This additive continuation consumes Synexia PR #10175, source commit
74606ca26ab5906fa50afd7e59fd83974c637a81. The tested recipe implementation is
01b6dc160572c4d4472a3739c5f9a2e897c55626, leaf tree
222e8118689f3ed2c877eb114759259da7e41b2c. Source publication precedes target
implementation publication. Existing README.md and RECEIVER.json remain
historical records of the earlier DOCUMENTED_NOT_INSTALLED intake.

Named recipe: com.synexia.m3.M3Jdk21CallbackMirrorBootstrap.
Canonical owner: hsoliwal/com.synexia, under
synexia-openrewrite-recipes/tools/patternizer/callback-parity/receivers/m3jdk21/mirror-v2/.
Target preimage: e617717424bd0af23881fb383777ae14dc7c8bb4.

The reviewed output is six additions: export.tsv plus five exact source-mirrored
proof payloads. Every vendor target equals m3/vendor/synexia/ + source_path.
All six paths were absent at the target preimage. The original payload revision
839c67201d66fb015de3111c2a8f9a12e72efd04 and its notices/hashes remain unchanged.
No historical short-path export is silently admitted or overwritten.

The export bytes came from an actually executed named OpenRewrite Result.
The five payloads came from the current target SynexiaImporter, unchanged,
including its rollback and symlink checks. Qualification used a bounded exact
source projection, not a full target checkout. Publishing those same generated
bytes into this branch preserves the base tree and unrelated owners; it does
not claim that the recipe scanned the complete target repository.

Fresh offline Maven compile/test/verify passed 13 SDK tests with zero failures,
errors or skips. Strict Java21/C++17, checked JNI and fatal UBSan passed the
retained callback corpus (66,798 cases / 931,428 assertions per run) and lifecycle
corpus (1,320 cases / 26,410 assertions per run). Java/JNI/replay observations
match; named applied replay produced zero changes. Repeated executions are not
additional unique coverage. Source proof events and command hashes are in
Synexia receivers/m3jdk21/evidence/mirror-v2-20261010/.

Disposition: AUTHORING_PROOF_ONLY / DO_NOT_PORT_TO_JDK_RUNTIME. The native
postimage remains at its exact mirrored .cpp.txt path and is compiled explicitly
as C++; there is no automatic JDK build integration. No public API, JNI ABI,
java.base, String, HotSpot or runtime-family phase is changed. Full target
reactor/image/jtreg, platform and performance qualification remain open.
Keep this PR draft until applicable target integration gates are reviewed.
No rebase, merge, force push, default-branch change or old-copy retirement.
