# JEP 493 runtime-link leaf recovery

Status: additive MODULE candidate; no feature completion or promotion claim.

Exact base: `87590cb96fb0e2dc0f88fae8e01957f7179cd255`.

Merged PR #109 previously installed the five Java classes that are ABSENT from Java 21 and present
in final JDK 24 GA:

- JRTArchive
- LinkableRuntimeImage
- runtimelink/JimageDiffGenerator
- runtimelink/ResourceDiff
- runtimelink/ResourcePoolReader

All five are absent again on this exact current-master base.

Recover the exact reviewed PR #109 blobs and its hash-pinned OpenRewrite crate/test. Do not add
RuntimeImageLinkException: it is absent from final JDK24 GA and remains GA_REMOVED_DO_NOT_ADD.

Authority is MODULE / BEHAVIOR_AND_CONTRACT_PRESERVING for this additive internal implementation
leaf. The full JEP493 feature remains MULTI_MODULE and unpromoted.

Verification: diff -> recipe compile/JUnit/fixed point -> scope proof -> configure -> `make jdk.jlink`.
Only after that leaf compiles may the next existing-owner adaptation pass proceed.
