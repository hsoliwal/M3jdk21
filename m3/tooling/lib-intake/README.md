<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Library intake to JMOD (pattern LIB_INTAKE_TO_JMOD v1)

One recipe takes a new library or app into M3JDK21 as an optional JMOD. It runs these steps in order:
- move packages and imports;
- atomize and patternize;
- set the consolidation and refactor baseline;
- write the module descriptor;
- build the JMOD and a linked image.

Every stage calls an owner that already exists, writes one row to `runs/<lib>/STAGES.tsv`, and stops the run on failure.

| Stage | What | Owner |
|---|---|---|
| S1 RELOCATE | packages and imports | OpenRewrite `ChangePackage`, one per package, `recursive: false`, longest first. This is the boundary-checked form of com.synexia `M3BoundaryCheckedPackageRenameRecipe` (#12340). It runs offline through rewrite-maven-plugin 5.23.1 on rewrite 8.17.1 |
| S1b BCP | behaviour and contract preserved | each relocated file must be byte-equal to its original after the textual name map |
| S2 RESIDUE | what ChangePackage does not own | `m3work:relocation-scan` (com.synexia #14520). Java-syntax residue must be 0; other kinds go to per-owner manifests |
| S3 ATOMIZE | atoms and pattern ids | `M3Mop atomize` + `plan` |
| S4 CONSOLIDATE | atoms an M3 owner already holds | `absorb_plan.py` (`m3/collections/qualification/converge-commons-20261030`). OWNER_CONTRACT rows form the consolidation queue, as proposals |
| S5 REFACTOR | ratchet baseline | METHOD_15 / LOOP / CONST_HOIST from the atomizer; MOP refactor passes may only lower them |
| S6 MODULE | `module-info.java` | the module exports every relocated package; `javac --release 21 -Xlint:all` |
| S7 JMOD | JMOD and image | the JDK's own `jmod create` (with `--legal-notices`) and `jmod describe`; `jlink` java.base + jdk.compiler + the module; `--list-modules`, `--validate-modules`, and a smoke run in the image (`runs/<lib>/SMOKE.java`, `// expect:` line) |
| S8 LAYER | the M3 refactor layer | relocated sources, descriptor and notices go to `m3/vendor/third_party-m3/relocated/<lib>`. The upstream copy in `m3/vendor/third_party/<lib>` is never touched |

`INTAKE.tsv` has one row per library: lib, source_dir, mappings (`old=new;...`), module, version, layer_dir, mop_classes,
owner_atoms. The last two are local tool inputs: the M3Mop classes and the atoms of the M3 owners.

```bash
unset MSYS_NO_PATHCONV                      # Maven and Windows tools need path conversion
JAVA_HOME="C:/Program Files/Java/jdk-21" bash m3/tooling/lib-intake/lib_intake.sh <lib> <empty work dir>
```

## First intake: JavaEWAH into `jdk.m3.ewah`

JavaEWAH 1.1.13-m3-source-1 is Apache-2.0, has 62 files and no dependencies. Its packages `com.googlecode.javaewah` and
`com.googlecode.javaewah32` are prefix neighbours, which is exactly the case where a recursive ChangePackage over-matches.
The run was idempotent: two runs gave the same rows.

| Stage | Result |
|---|---|
| S1 | 5 packages moved to `com.m3.collections.ewah` / `ewah32`; 0 old package directories left |
| S1b | 62/62 files byte-equal after the name map |
| S2 | residue 0 of every kind |
| S3 | 1,176 atoms: KNOWN_PATTERN 392, UNPROVEN_LEAF 622, UNKNOWN_PATTERN 162 |
| S4 | 119 OWNER_CONTRACT atoms (consolidation queue), 412 OWNER_SHAPE, 645 GAP |
| S5 | METHOD_15 141, LOOP 211, CONST_HOIST 33 |
| S6 | `jdk.m3.ewah`, 5 exports; 55 javac lint warnings in the donor code (recorded in `s6-javac.log`) |
| S7 | JMOD 156,666 B; the image has 6 modules and validates; the smoke `bitmapOf(1, 5, 9).cardinality()` prints 3 |

The JMOD and the image are build products and are not committed. `runs/JavaEWAH/` keeps the ledgers and logs.
Next leaves, all as proposals:
- consolidate the 119 contract-equal atoms onto their M3 owners;
- lower the S5 baseline with MOP recipes;
- register the module pack in `m3/tooling/module-packs` for the full M3JDK image build.
