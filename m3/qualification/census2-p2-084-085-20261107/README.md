# Census2 P2-084 + P2-085 receipt

The census2 leaves P2-084 (java semantic census) and P2-085 (CDS training
replay) were delivered by com.synexia receiver crates. Each crate carries
postimages already named for M3JDK paths. The postimages are received here
byte-exact, with one adaptation (`PROVENANCE.tsv`):

| Crate | Postimages | Target |
|-------|-----------|--------|
| `m3jdk-repository-file-census-20261101` (prerequisite of P2-084) | 12 | `m3/tooling/atom-census` repository package, contract TSVs, verify script, workflow |
| `m3jdk-java-semantic-20261101` (P2-084) | 4 | `jdk.m3.tooling.atoms.repository.M3JavaSemantic*` |
| `m3jdk-cds-training-20261101` (P2-085) | 5 | new `m3/tooling/cds-training` (`jdk.m3.tooling.cds`) |

The adaptation renames the CDS schema id `M3-JDK21-CDS-TRAIN-REPLAY/1` to
`M3JDK21-CDS-TRAIN-REPLAY/1` under the JDK-naming PRODUCT_ID rule. The id is
referenced only through `M3CdsIdentity.SCHEMA` and the module README.

Each crate's qualification fixtures (the JNI first-index fixture, the recipe
resources and the recipe tests) stay in com.synexia.

The gates are in `GATES.tsv`; `gate-p2084-wsl.sh` replays the P2-084 gates.
