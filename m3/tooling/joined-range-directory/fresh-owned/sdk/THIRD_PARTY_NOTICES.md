<!-- SPDX-License-Identifier: Apache-2.0 -->
# Provenance

Retain the Apache-2.0 copyright/notices of JoinedM3StringPiece and its four exact
owner dependencies at M3jdk21 commit73f59218dc4ef1a3ef79aad990e77273db2a2d60.
The refinement reuses that file's own binary search; no third-party implementation
is copied. Preserve the source owner identity in the fixtures.

The source recipe adapts Synexia reversible SDK e790d6fdbfa18e3acccd9b451854f0412a5199f1
and its existing schedule/refusal/report validators. Shared engines1c1b1b568d369f688f6953161636bf36b484bf5a
and38df5f97bbeec8a8a8cfc77091d56bda925c07f2 and POMd79867bcc09ad6329e12418e15907e54b8ab025f
are unchanged. OpenRewrite8.17.1 retains Apache-2.0; JUnit Jupiter5.10.2 retains EPL-2.0.
Existing dependencies and notices remain. New source/runtime/performance proof is pending.

This new proposal adapts the owned fresh-array ownership-transfer pattern in
M3BigString.java (public32152976eddf59c129a16aa0252953b82e367433,
blob9f66122b2dba2e4e0bc417208f18d0f8a2f95dec, lines159-186/454-475),
Apache-2.0. Existing copyright/notices remain. The shared coordinate range view
and M3Rope Window are not adopted because unrelated owner retention would change.
New authoring and proof sources carry Copyright2026 Hitesh Soliwal and Contributors
to the Synexia Project and Apache-2.0. Existing OpenRewrite/JUnit notices above remain.
