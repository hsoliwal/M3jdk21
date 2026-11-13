<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: GPL-2.0-only -->

# Exact Object.toAdapter full-source lineage — executed source-custody receipt

## 2026-11-04 source inventory and proof

Source authority is exclusively existing first-party Synexia
Maven/OpenRewrite full-source templates. Independently fetched the
three complete `Object.java` source snapshots and current M3JDK21
receiving source with the connected GitHub connector. Both original
recipe manifests from October 18 and November 2 join exactly to the
earlier historical progress manifest. No template ownership changed.

| Stage | SHA-256 (source) | Git blob (source) | Characters |
| --- | --- | --- | ---: |
| 2026-10-17 | `829fb37093cf3e8d11b3c1e3d42f898f0c86f3b697e7a66cefe09934be3bb259` | `2bfef3101224112c40957c954592a100dc59a3fc` | 28,923 |
| 2026-10-18 | `014671527cd647d90f8cab6d6dbd0ca127d918c5a8f38d1ec50be6140eb63735` | `a9899e1c2970f6d5e9172ffb12f5b7399d052b68` | 29,530 |
| 2026-11-02 | `bd21f3da2ff9f8365576946cfe2d5b5557acaace76de2146277e9d1baa436bd4` | `c703c72bea6e792a378bb6d3babc96f22dbf6cb4` | 29,557 |

The receiving branch itself contains the **exact** third-stage Git
blob. Full-source reverse synthesis was actually executed using the
two bounded regions in the immutable new JSON ledger (Git blob
`949c7db8f195cab7fed92d402b99fba6bd6d58f0`):

- Current November 2 → October 18: prefix **2,727** characters,
  suffix **26,597**, replace 233 current fragment characters with
  206 original fragment characters.
- October 18 → October 17: prefix **86**, suffix **26,516**,
  replace 2,928 current fragment characters with 2,321 original.

**PASS:** reconstructed complete October 18 and October 17 source
strings equal their independently fetched canonical GitHub files
*byte for byte*. Their Git blob identities match exactly, and both
forward replay operations reconstruct their successor source images.
All source files have zero non-BMP characters and LF line endings;
prefix/suffix boundaries therefore agree between the JavaScript and
Python Unicode source-index representations.

**PASS (negative controls):** mutating the current-source
`@SuppressWarnings("unchecked")` fragment or the stored current
fragment caused the exact region anchor to mismatch; the untouched
source anchor matched.

The full Python checker
`m3/compatibility/check_object_source_lineage_20261104.py` was
compiled with Python 3.13 and committed byte-for-byte to GitHub
(Git blob `b64087dea35bf5e66717abc9b336ad804755e828`).
Its first-party 10-test suite
`test_object_source_lineage_20261104.py` is source-sealed by Git
blob `38169704acf9ef05b8d24a81fefe195e675ab99c`.

Local run: **9 tests PASS, 1 SKIP**, with the full-repository
integration test skipped because the entire M3JDK21 checkout is not
mounted locally. The original first run failed a deliberately
inaccurate negative fixture (a changed expected *length* triggered
the earlier identity guard); the fixture was corrected to change
the hash **without** changing length. The second run passed.
The failed and corrected records remain in the local session logs.

## CI integration and held gates

The existing #2395 current-source census workflow was reused instead
of creating a second workflow. It now runs the new Python
compile/lint, negative tests and exact three-stage current-source
lineage reconstruction after the original 14-file diagnostic.
The historical `check_object_adapter_progress_receiver_20261017.py`,
its five source manifests and the progress test suite remain
**unchanged and authoritative**.

**NOT EXECUTED locally**: full-checkout `verify()` inside the
actual M3JDK21 repository, the hosted workflow conclusion for this
new head, matched M3JDK21 Java/JNI, native libjava, HotSpot/vtable,
weak-GC/runtime, all seven remaining progress-owner transition
receipts, Maven/OpenRewrite actual `Recipe.run`, JMH and release gates.
A reconstructed source image proves the source history, **not**
observable JDK behavior or retroactive historic-admission success.

The current source-census report's eight mismatches remain eight
until an explicitly approved, independent successor admission
protocol reconciles each of them. This source-lineage receipt gives
**one exact chain**; it is not a mechanism for accepting arbitrary
current hashes.
