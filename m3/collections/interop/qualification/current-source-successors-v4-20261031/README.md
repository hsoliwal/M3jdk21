<!-- SPDX-License-Identifier: Apache-2.0 -->
# Additive interop V4 source gate — two new JUnit files, six atom tests

The actual failure in GitHub Actions run 41203337498 is:
`ValueError: unadmitted interop source addition/deletion`.
The original V3 five-source receipt and parser unit tests **passed**; the
source gate correctly refused two later, unregistered `@Test` classes.

An exact Git tree comparison proves:
- Original predecessor `55473093e8ef3aba832fa1a00a7e6e8ef8a54221`:
  84 source paths.
- Real V3-approved `b123804aff3ac6930e420cfb82b5ce07040049fe`:
  same 84 paths, exactly the five unchanged V3 receipt modifications.
- Failed current `b97ce66fb04ccf9d2f263295adaa86ca7a781f08`:
  86 paths, two added test classes, no other modification or deletion
  relative to the V3-approved tree.

The new exact V4 receipt is one row per added Java test class, with SHA-256,
Git blob identity and the sorted-in-source @Test identities: four methods
for M3Flat3MapProviderTest and two for M3FrozenLongSetProviderTest.
All six remain independently executable in actual Maven Surefire.

`verify_source_v4.py` **executes the complete original V3 prove_current**
on its real approved historical checkout, then original V2 historical
replay, and admits only two added files with exact source/byte/JUnit
checks. No old receipt, source, parser, public Java contract or JNI
symbol is changed. New negative-control tests refuse source deletion,
extra files, changed hashes, missing/double test identities, and aliases.

Use the versioned Maven task rather than manually altering source:
```sh
mvn -B -ntp -f m3/collections/interop/qualification/current-source-successors-v4-20261031/pom.xml verify
```
Requires real pinned checkout directories `approved-interop-20261029`,
`before-interop-20261026`, `historical-product` as in the workflow.
The independent V5 JUnit method census is required after real Surefire,
not replaced by this source-only proof.

**HOLD** until actual Maven/JUnit/native/arm64 receiver gates pass. Never
relax original V3's 84-source equality, replay a fictitious history,
claim 6 synthetic methods executed, or merge a 5,553-file superset
based only on this narrow gate.
