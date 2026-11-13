# JDK build repair — 2026-10-04

Continue from actual Actions run37181307729/job111374285910, not another search or Nebula feature.
Two diagnosed product blockers are invalid CHECK_NULL nesting in jni.cpp and the internal backing
interface inheriting AutoCloseable.close() throws Exception. Reuse the existing Handle-returning
String factories and the sole mapped implementation's no-checked-exception close contract.

JNI factory call syntax changes without changing JNI exports, M3 admission, encoding or lifetime.
The close declaration intentionally narrows the internal inherited throws surface; it is not a
claim of source compatibility for hypothetical out-of-tree implementors. Supported Java SE APIs
and binary method descriptors remain unchanged. Keep existing names and ownership. Do not suppress
warnings or disable coverage/test gates.

The retained native patch engine is loaded in an isolated resource context. Its implementation
hash, full pre/postimage hashes and small native/Java patch are sealed. The existing OpenRewrite
text adapter installs the packet; that is not C++ AST processing. Maven orchestrates backend tests
and explicit apply. The native OpenJDK build and existing String/JNI tests remain product oracles.

The push-only workflow is limited to the named feature branch. Its job may POST-create a new
m3/jni-checked-<exact-base> branch only after native image and selected existing JNI/String proof.
An occupied ref fails; no default branch is written, force-pushed, rebased, squashed or merged. The
commit parent is the exact original GitHub head, never a source-export reconstruction. It records
focused proof and leaves full jtreg/backport/programme admission separate. Failures save logs, not
a product branch. No credentials/settings/private generated keys enter artifacts.

Local bounded proof before publication: original Java warning reproduced; corrected backing and
sole implementation compile with Java21 -Xlint:all -Werror; original mapped-store test passes in
normal and interpreter VMs. Six native-backend tests and five genuine OpenRewrite/JUnit installer
checks pass. These are not a completed HotSpot rebuild or a global 99% coverage claim.
