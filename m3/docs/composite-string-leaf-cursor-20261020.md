<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 canonical composite String leaf cursor — 2026-10-20

Target: intrinsic `java.lang.M3String` in the actual M3JDK21 Java runtime, not a Synexia or M3-to-JDK compatibility layer.

Synexia canonical recipe: https://github.com/hsoliwal/com.synexia/pull/12561
Canonical recipe commit: `110366f92a637461dc74ef2d0607a7116579b195`.
Existing snapshot recipe `M3HashPinnedJavaSnapshotRecipe`, named `com.synexia.m3.M3Jdk21CompositeTreeCursor20261020`.
Target baseline: `d6d76b563050fc13c4fa79bc494edf29f1614b37`.

The already canonical `M3String(owner,packedCoordinate)` stores no string-owned arrays. Its balanced `M3StringTuple` nodes retain child coordinates, and `CodeUnitCursor` caches a leaf-local run. Exact String content equality and prefix checks now use this cursor for sufficiently long values when at least one operand is tuple-backed; short and flat values retain their original allocation-free loops. Existing immutable facts are still consulted first, and exact UTF-16 comparison remains final authority. No extra representation, atom cache, JNI export, parser dependency or java.lang.String API is introduced.

Sealed file identities:
- M3String.java: original `3048c2538f437dc612aab4af2c520fece3a7e8f3eb59aca2ac907d6307f9d0f2`; candidate `cc3cc63f85c8e45316c353c9123dd637496164a2d0a159497d88d30fb15590a0`.
- M3StringCompositeCursorTest.java: newly generated candidate `ba948f18cad471a157d424850bbf172b4dac1c430ba7ad1703e5b2fcebd514ff`.

Verification so far: new jtreg candidate compiled under stock Java21 `--release 21 -proc:none -Xlint:all -Werror`; actual `-XX:+UseM3StringStorage` M3JDK test has NOT been executed. Existing M3String streams/canonical-DAG and String contracts remain mandatory. Full canonical OpenRewrite apply/repeat/refusal, receiver jit/interpreter jtreg, HotSpot/GC/CDS/JNI, performance threshold and retained native RSS remain NOT_RUN. Do not merge while String-first admission and source/inventory successor are blocked.

M3FS uses analogous indexed mapped segment geometry in its own `jdk.m3fs` module. The companion PR is stacked on M3FS #1425. No new dependency from java.base to jdk.m3fs; file content IDs are not lexicon word identities. Preserve original per-file OpenJDK rights and first-party attribution.
