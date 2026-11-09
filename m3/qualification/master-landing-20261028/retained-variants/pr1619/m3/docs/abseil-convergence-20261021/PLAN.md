<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Frozen implementation and review plan

Input product: `hsoliwal/M3jdk21@2779aba3f98f31c324be43f731be7914e1a05935`.
Canonical source: `hsoliwal/com.synexia@361d57154f70e16788bef1006781b89b978732a5`.
Abseil inspection: `22dd800ead4104f3a1fdb64d27dbde822826ca8d`.

1. Compare current owners and historical PR1531/1551/1552/1554; retain stronger
   current code and pin every nominated preimage. Reuse existing catalogues.
2. Reproduce defects with independent observations. Author source-template
   candidates and tests under the canonical Synexia crate; do not edit receivers.
3. Qualify the existing Maven/OpenRewrite owners with current engine hashes,
   protected fixtures, deterministic order/fixed point, drift and missing-input
   refusals. Admit the existing three-path Java-text bootstrap only explicitly.
4. Apply exact qualified results serially to an isolated receiving checkout.
   Verify public/protected surfaces, callbacks/exceptions/ordering, physical
   indices, private result ownership, state generations and native ABI.
5. Build M3JDK21 and execute the nominated fixtures in its installed image.
   Keep module/boot-VM tests separate from actual product proof; retain missing
   prerequisites and any unrelated baseline failures without weakening gates.
6. Append recipe memory and provenance. Publish the canonical source draft PR,
   verify its real commit, then generate target source receipts and submit
   String and stacked collection/JNI draft PRs. No merge or force-push.

Review notes and unqualified resource/platform/API/license gates remain explicit.
The user-authorized implementation scope does not imply blanket target promotion.
