# Synexia canonical recipe public custody

This module is a **public custody mirror**, not the canonical recipe owner.

Canonical owner:
- repository: `hsoliwal/com.synexia`
- pure-int closure: PR `#9497`, exact source head `dcc967a005edb47fd45e1aecc710339b236f28ea`
- scope/multipass kernel: exact Synexia develop head `68322e2b40c3d87ca1e3c1566616d2387fd4e847`
- license: Apache-2.0

Every Java source in this module is a byte-for-byte custody copy of the exact Synexia Git blob
listed in `SOURCE.tsv`. Different families may bind different exact Synexia revisions; the
per-row source head and Git blob are authoritative. M3JDK21 does not claim a divergent
implementation or a new semantic owner.

The local Maven coordinates deliberately match the canonical recipe artifact:

`com.synexia:synexia-openrewrite-recipes:1.0.0-SNAPSHOT`

This allows public proving repositories such as Nebula to install the pinned custody jar when
cross-repository private credentials are unavailable. A target must still bind the canonical
Synexia provenance and may not infer promotion from custody.

Verification:

```bash
mvn -B -ntp -f m3/synexia-import/pure-int-recipe-custody/pom.xml clean verify
```

The test verifies Git-blob identity for every custody source, recipe atom ordering,
behavior-preserving transformation of the admitted Java-21 pure-int leaf, rejection of
broader/unsafe shapes, second-pass fixed point, exact scope promotion, Java-path inference and the
canonical multipass frontier.
