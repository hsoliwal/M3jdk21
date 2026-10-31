<!-- SPDX-License-Identifier: Apache-2.0 -->
# Exact callback SDK execution

This continuation consumes canonical Synexia PR #9584 at
`01554337e0051018e6d932f3775378a01faf88f8` and verifies the target preimage
`c1acefe582ac84414f1082f095e96bbee167bc33`. The existing intake metadata from
#258 is historical: no export or five-file image was installed by that PR.

The new workflow is read-only with respect to GitHub. It neither pushes nor
merges. It checks out the exact target preimage in an isolated runner, acquires
a hash-pinned first-party source packet from five comments on #258, and extracts
that packet only into temporary source input. Recipe source remains canonical in
Synexia; this repository does not acquire a committed second recipe owner.

Packet ZIP SHA-256:
`7c8b1bdeb9515d7ff0b474c182a97e4c93a3a9dc012a335cf3110f85abb2f8a6`.
Comments in order: 6021170423, 6021196557, 6021224546, 6021251481, 6021329398.
Every part, the archive, and every extracted source file are checked before use.
No credentials, SDK binaries, unrelated source or JDK runtime payload are part
of this input packet. The acquisition token exists only in its download step.

Toolchain provisioning is a separate networked preparation step. Maven 3.9.9 is
checked against its SHA-512; the approved source-owned POM resolves pinned SDK
inputs into an isolated Central-only repository. All actual compile/test/verify
commands then run offline. Preparation is not verification. Missing offline
inputs or a failed test block execution; there is no online verification retry.

The genuine eight-test OpenRewrite bootstrap suite must pass before the runner
uses its actual returned export Result. The existing target importer then
materializes only the five sealed proof files in the temporary target checkout.
Strict Java/C++ compilation, actual JNI, fixed-point SDK replay, importer replay,
exact postimage checks and a reviewable candidate patch follow. A skipped test,
encoded source packet, synthetic parser fixture or queued job is not SDK proof.

The intended additions remain authoring/proof-only under
`m3/vendor/synexia/callback-parity/`, never java.base, HotSpot or String.
No public contract or JNI ABI changes are permitted. The M3JDK21 image and full
reactor remain separate qualifications. Existing README.md and RECEIVER.json
are not overwritten by this wiring; application evidence, when actually
produced, must state its exact source, target, execution kind and remaining gates.

Keep the PR draft until the required executed evidence is inspected. Publishing
the successful candidate remains a separate reviewable source write; the workflow
cannot perform it. Preserve existing owners and append-only history. No rebase,
force push, source deletion, copy retirement or default-branch update.
