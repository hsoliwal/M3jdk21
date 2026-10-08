# Synexia pure-int recipe public custody

This module is a **public custody mirror**, not the canonical recipe owner.

Canonical owner:
- repository: `hsoliwal/com.synexia`
- PR: `#9497`
- exact source head: `dcc967a005edb47fd45e1aecc710339b236f28ea`
- license: Apache-2.0

The six Java sources are byte-for-byte custody copies of the source blobs listed in
`SOURCE.tsv`. M3JDK21 does not claim a divergent implementation or a new semantic owner.

The local Maven coordinates deliberately match the canonical recipe artifact:

`com.synexia:synexia-openrewrite-recipes:1.0.0-SNAPSHOT`

This allows public proving repositories such as Nebula to install the pinned custody jar when
cross-repository private credentials are unavailable. A target must still bind the canonical
Synexia provenance and may not infer promotion from custody.

Verification:

```bash
mvn -B -ntp -f m3/synexia-import/pure-int-recipe-custody/pom.xml clean verify
```

The test verifies Git-blob identity for all six sources, recipe atom ordering, behavior-preserving
transformation of the admitted Java-21 pure-int leaf, rejection of broader/unsafe shapes, and
second-pass fixed point.
