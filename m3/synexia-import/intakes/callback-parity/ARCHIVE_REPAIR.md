<!-- SPDX-License-Identifier: Apache-2.0 -->
# Canonical Maven archive correction

This continuation preserves the complete immutable input and execution contract
in CI_EXECUTION.md. The original source archive, five comment identities, all
part/archive/file hashes and target c1acefe582ac84414f1082f095e96bbee167bc33 remain
unchanged. The additional input step reconstructs one exact source file in the
runner's temporary callback-source directory; it is not a JDK source change.

Canonical correction: Synexia PR #9700, commit
01561606c6adec4c5c3a3d81247924c60f9a9fd0. It changes only the Maven archive
preflight in the existing source-owned hosted_apply.py and retains all SDK,
importer, Java/JNI, target hash and replay gates. The correction passed 44 local
archive/orchestration tests, real Maven startup and exact extraction replay.
Those results are not a substitute for the hosted offline SDK lane.

Transport: comment 6034649686 on M3JDK21 #258. The JSON block including its final
LF is 2982 bytes with SHA-256
1a46ee791bb0a61128e8f98cd3434f8026512e6d240bf3e2d123c00f5208511f.
Only synexia-openrewrite-recipes/tools/patternizer/callback-parity/receivers/m3jdk21/hosted_apply.py
may be reconstructed. Required input SHA-256:
5cb0930da45a862207360b684579219c6a208bac5f5c27e2f409caf1d26f96c7.
Required output SHA-256:
3718546fc3fe883a56fdb00ae30c4343efa91d3954fca465dac8928c5994ec4d.

Require the exact comment owner, JSON checksum, original source and target
identities, two uniquely matching edits and exact resulting bytes. Preserve the
base SOURCE_PACKET.json and delta under callback-input. The derived packet binds
every unchanged file to base revision 01554337e0051018e6d932f3775378a01faf88f8 and
only the corrected host to the new Synexia commit. Never describe the mixed-input
packet as a full checkout of the correction commit. Reapplying the delta refuses.

All existing workflow steps, read-only permissions, checkout pin, dependency
versions, timeout, source Result admission and artifact retention remain intact.
Networked provisioning remains separate from offline compile/test/verify.
No successful target application is claimed until actual job Results, native
proof and replay evidence are inspected. No source recipe implementation is
committed into the target. No merge, rebase, force push, default-branch update,
manual payload installation or historical evidence retirement is performed.
