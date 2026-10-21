<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 Java/JNI five-provider arrayification receiver
Canonical Synexia PR: https://github.com/hsoliwal/com.synexia/pull/12326.
Canonical committed recipe: b3573b7b02d74ae4a2ed751f660590f6df2282d4.
Pinned exact M3JDK21 preimage master@2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49.
Template bytes for four outputs are exact canonical source. The existing named OpenRewrite
M3GpuToolJoinRecipe/Manifest source is mirrored byte-for-byte, not reimplemented.

New O(k) gather, O(n) stable two-pointer mask select, O(n) triple reversal and
O(n+m) stable merge operate on primitive index arrays, with payload references
in the existing immutable five-library SoA capture. Source APIs, Java callbacks
and JNI ABI unchanged. The distinct provider APIs remain JDK/Guava/Eclipse/
Apache/Spring-owned and detached results do not masquerade as live views.

Local standalone Java21 strict compile & 31,007 seeded differential assertions PASS.
Local check used test-only native backend stand-in: not actual receiver JNI evidence.
Hosted exact-head Maven/OpenRewrite SDK, installed five-provider contract tests,
actual -Xcheck:jni + C17, full JDK/HotSpot and performance evidence PENDING.
The receiving phase remains STRING -> ARRAYS -> COLLECTIONS; do not promote early.
