<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Duplicate `M3BuildImpactPlan.requireSnapshot`: recorded removal (2026-11-06)

master `48537c8e741` carries two `void requireSnapshot(M3FileDeltaLedger.Snapshot)` methods in
`m3/file/src/main/java/jdk/m3/file/M3BuildImpactPlan.java` (lines 135 and 151), so javac refuses
the class (`method requireSnapshot(Snapshot) is already defined`) and `mvn -f m3/file/pom.xml package`
has been red since the 2026-11-06 superset merge (`c9a1d41baaf`). The two bodies differ:

| origin | checks | message |
|---|---|---|
| `2f708ccfd817` 2026-10-30 (removed) | identity (`snapshot != sourceSnapshot`) | file impact belongs to a different M3File generation |
| `ce7051f77ca0` 2026-11-02 (kept) | null, `snapshot.slots() == reasons.length`, identity | file plan/snapshot slot dimensions differ; file plan belongs to different snapshot instance |

The kept method is a strict superset of the removed one: every argument the identity-only method
accepted is accepted, every identity mismatch it refused is refused, and no contract asserts either
message (`grep` over `m3/file/src` and `m3/file/verification`). Both callers
(`M3BuildImpactPlan.java:192`, `M3ReactorImpactPlan.java:55`) are unchanged.

## Recipe step (`apply.py`)

Hash-pinned and idempotent: the target hashing to the preimage `74a13dbc...` loses exactly the older
Javadoc + method + following blank line and gains one `//FLLDCIM: RECIPE` anchor above the kept method;
the result must hash to `1864b923...`; a target already there is a fixed point; anything else is refused.

```text
python -I apply.py            m3/file/src/main/java/jdk/m3/file/M3BuildImpactPlan.java  APPLIED      74a13dbc... -> 1864b923...
python -I apply.py  (again)   m3/file/src/main/java/jdk/m3/file/M3BuildImpactPlan.java  FIXED_POINT  1864b923...
```

Inputs are pinned in [PROVENANCE.tsv](PROVENANCE.tsv); executed gates and the pre-existing Windows-only
`M3FileDeltaSelfTest` receipt failure (with the pristine-master control figures) are in
[GATES.tsv](GATES.tsv).
