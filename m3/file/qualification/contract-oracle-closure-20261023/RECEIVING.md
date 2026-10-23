<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 M3File missing executable contract receiving receipt

**Product identity:** M3JDK21 is the integrated JDK. M3File is the
existing file/module implementation, not a new filesystem API or an
application wrapper. This is a bounded *verification repair*.

Canonical source recipe:
[Synexia #13230](https://github.com/hsoliwal/com.synexia/pull/13230),
`com.synexia.m3.M3Jdk21FileContractClosure20261023`.
The original executable recipe, source template, Maven proof crate,
fixtures, category catalogue and improvement-memory events remain in
private Synexia; none of that is copied to M3JDK21.

Original receiving ancestor:
[M3JDK21 #1883](https://github.com/hsoliwal/M3jdk21/pull/1883),
commit `eb05acf7f357dc7a8a2043a97706410de6aafcdf`.

## Exact source identity and one atom

Target: `m3/file/verify.py`.
Before Git blob: `755ddf0edbf613e983a690e4379b75cbddcbe6b0`.
Before SHA-256: `3053db98fbfde335bbcab84b1f941218c91185e8f1ec62dadf3be4ef3dec466a`.
After SHA-256: `603867d300e35acd9b58fbef59be0d7235ffd067431c8317e78c72d40cc8b7d3`.

The existing verifier has 11 entries in the Java21 module-contract map
but there are 13 existing executable main-class contract source files.
The sole transformation is to re-admit the original source contract
names and exact observed PASS markers:

- `M3FileMoveCandidatesContract` → `M3_FILE_MOVES_PASS`,
  the previously merged M3JDK21 #1772 atom and mandatory row in
  `m3/history/merged-atoms-preservation-20261022.tsv`.
- `M3FileObservationContract` → `M3_FILE_OBSERVATION_PASS`,
  the already-existing bounded streamed bytes/progress/shadow/SHA-256
  observation contract.

The original strict `test_sources == set(contracts)` gate,
real `javac`, original M3File module `jar` and `jlink` image,
host + linked-image execution of every class, exact source/class hashes
and stored JSON test receipts remain unchanged. No marker-only changes,
skip flags, suppressed failures, product API moves, service changes,
JNI symbol changes or native implementations.

## Evidence and hard gates

- Current source preimage + candidate template Git/sha comparison:
  PASS at candidate publication; product readback required.
- Synexia real Maven/OpenRewrite SDK application, duplicate/missing/
  drift rejection and source fixed point: QUEUED, NOT PASS.
- Existing M3 merged-atom history preservation gate:
  must execute and report PASS on **this** head.
- Existing M3File source+class `m3/file/verify.py` Java21
  host AND linked image, all **13** module tests:
  must execute and report PASS on **this** head.
- Separate previously successful #1883 Java21/JNI test is source history;
  this PR does not modify its Java/C code and does not claim its
  results were rerun on this child head.
- Full installed source-built M3JDK21 JDK, jtreg/JCK/HotSpot/JNI/GC/
  CDS/cross-platform performance/release gates remain NOT VERIFIED.

**State: DRAFT / HOLD.** No automatic merge, rebases of develop,
public-recipe copies, or project-wide blanket naming changes.
