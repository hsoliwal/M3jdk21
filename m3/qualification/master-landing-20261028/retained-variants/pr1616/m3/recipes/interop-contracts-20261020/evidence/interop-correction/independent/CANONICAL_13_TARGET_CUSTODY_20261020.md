<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Independent canonical 13-target byte custody review

**PASS: all 13 manifest targets and all 26 before/after resources agree byte-for-byte with the frozen frontier and the reviewed preimage/candidate files.** Java has 11 targets and text has 2; the resource directories contain exactly the listed files and their manifests.

All four `sdk-owner` Java engines agree byte-for-byte with `canonical-runtime-convergence-20261020` and their recorded SHA-256/Git-blob engine identities. `src/test/java/com/synexia/rewrite/M3TaskRecipeTest.java` and `receive.py` also match the main canonical crate exactly. `SOURCE_PROVENANCE.json` records the 13-target count.

The complete compared path and hash inventory is in `CANONICAL_13_TARGET_CUSTODY_20261020.json`. The candidate-set SHA-256 remains `05f397c6181992c354c6113aca7e2f9039bb8c835bef8e5c14a286ec526176eb`.

This was a read-only byte custody review. It did not execute the SDK, receiver, Maven, publication adapter, or any production code, and it does not replace their separate qualification receipts.
