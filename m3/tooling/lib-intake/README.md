<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Library intake to JMOD (pattern LIB_INTAKE_TO_JMOD v2)

One recipe takes new libraries and apps into M3JDK21 as optional JMODs. It runs these steps in order:
- move packages and imports;
- atomize and patternize;
- set the consolidation and refactor baseline;
- write the module descriptor;
- build the JMOD and a linked image.

Every stage calls an owner that already exists, writes one row to `runs/<lib>/STAGES.tsv`, and stops the run on failure.
`lib_intake_batch.sh` runs every `INTAKE.tsv` row and then links all the JMODs into one validated image.

| Stage | What | Owner and gate |
|---|---|---|
| S0 VIEW | release-21 source view | `src/main/java`, then `src/main/versions/<N>` overlays with N ≤ 21, in order (the `jmod_pack.py` release view, at source level). The upstream `module-info` is set aside |
| S1 RELOCATE | packages and imports | OpenRewrite `ChangePackage`, one per package, `recursive: false`, longest first. This is the boundary-checked form of #12340, run offline (rewrite-maven-plugin 5.23.1, rewrite 8.17.1). The same name map, dotted and slash forms, then completes string literals, comments, resource paths, `META-INF/services` and proguard text. Resources move with their packages. Files OpenRewrite cannot parse move by the name map and are listed in `TEXT_FALLBACK.tsv` |
| S1b BCP | behaviour preserved | each Java file must equal `name-map(original)`; each package resource must be byte-equal at its new path |
| S2 RESIDUE | nothing left behind | `m3work:relocation-scan` (com.synexia #14520) plus a slash-form grep: residue of every kind must be 0 |
| S3 ATOMIZE | atoms and patterns | `M3Mop atomize` + `plan` |
| S4 CONSOLIDATE | consolidation queue | `absorb_plan.py` against the M3 owners: OWNER_CONTRACT atoms, as proposals |
| S5 REFACTOR | ratchet baseline | METHOD_15 / LOOP / CONST_HOIST |
| S6 MODULE | `module-info` | `requires` from `jdeps --print-module-deps` over a class-path compile; `requires static` for `compile_deps`; every package exported; upstream `uses`/`provides` name-mapped; `javac --release 21 -Xlint:all` |
| S7 JMOD | JMOD and image | `jmod create` (legal notices only), `jlink`, `--list-modules`, `--validate-modules`. Smoke: `runs/<lib>/SMOKE.java` if present, otherwise `LoadAll` (every class of the module loads in the image, 0 failures) |
| S8 LAYER | the M3 refactor layer | `m3/vendor/third_party-m3/relocated/<lib>`; the upstream copy is never touched |

## `INTAKE.tsv`

The columns are lib, source_dir, mappings, module, version, layer_dir, mop_classes, owner_atoms and compile_deps.

- mappings, module, version and layer_dir accept `auto`. The library's common package root `R` maps to
  `com.m3.lib.<last segment of R>`, the module is `jdk.m3.<same>`, the version comes from the source POM, and the
  layer is `m3/vendor/third_party-m3/relocated/<lib>`.
- `compile_deps` lists annotation jars needed only at compile time; they become `requires static`.
- Rows that cannot be taken in as they are go to `HOLDS.tsv` with the reason and what they need.

```bash
unset MSYS_NO_PATHCONV        # Maven and Windows tools need MSYS path conversion
JAVA_HOME="C:/Program Files/Java/jdk-21" bash m3/tooling/lib-intake/lib_intake_batch.sh <empty work root> [lib ...]
```

## Wave 1

`BATCH.tsv` holds the result per library; `COMBINED_IMAGE.tsv` holds the combined jlink, `--validate-modules` and the
module presence check. Wave 1 covers JavaEWAH (explicit mapping, `com.m3.collections.ewah`), re2j, fastfilter,
pcollections, HdrHistogram, dec (brotli), xz, commonmark, jsoup 1.23.2 and commons-codec 1.22.1. The automatic mapping
gives `jdk.m3.<name>`. zero-allocation-hashing is on hold: it uses the JDK-internal `sun.nio.ch`, so it first needs a
qualified export in M3JDK21's `java.base`.

The JMODs and images are build products and are not committed. Each `runs/<lib>/` keeps that library's ledgers and logs.

**Next leaves, all as proposals:**
- the consolidation queues;
- lowering the refactor baselines with MOP recipes;
- registering the modules in `m3/tooling/module-packs` for the M3JDK21 image build;
- more `INTAKE.tsv` rows from the vendored set.

## Apps

The `INTAKE.tsv` column `main_class` takes the upstream fully qualified main class. The name map relocates it, it is
recorded in the JMOD (`jmod create --main-class`), and `jlink --launcher <lib>=<module>` puts `bin/<lib>` in the image.
Gate S7 checks that the descriptor shows the main class and that the launcher exists.

First app: HdrHistogram's `HistogramLogProcessor`. The image has `com.m3.lib.hdrhistogram.HistogramLogProcessor` as its
main class, a `bin/HdrHistogram` launcher, and all 87 classes load.

## Atomization and patternization across libraries (`catalogue/`)

`pattern_catalogue.py` groups the M3Mop atoms of every intake library, together with the M3 owners, by four CPU keys:
- `patternId`: the M3Mop pattern family;
- `shapeKey`: structure;
- shape plus contract: consolidation candidates;
- `exactSha256`: copied duplicates.

Each family is labelled ACCESSOR, CONSTRUCTOR, ITERATOR, OBJECT_PROTOCOL or ALGORITHM.

Result over 8,406 method atoms from 11 libraries: at exact-structure level the recurrence across libraries is almost all
accessors and constructors, and algorithms are unique to their library.

`near_dup_catalogue.py` reuses the `ts-lhash` skill unchanged (tree-sitter leaves; L-Hash H2+H3+H4 signature, invariant
to renames and literals) and adds library, file, line and method to each leaf. Over 2,083 algorithm-sized leaves in 9
libraries and 4,338 including the M3 owners it finds:
- 34 M3_OWNED_ALGORITHM families (224 leaves), where an M3 owner implements a near-duplicate algorithm. For example,
  HdrHistogram's `oneAtATimeHashStep` matches the `mix` hash of `M3IntHashSet`, `M3IntIntHashMap` and `M3IntLongHashMap`;
- 8 CROSS_LIB_ALGORITHM families (22 leaves).

Every family is a candidate. L-Hash near-duplicates are coarse and some are false friends, so a differential test admits
each consolidation.

## Holds

`HOLDS.tsv` lists:
- zero-allocation-hashing: uses the JDK-internal `sun.nio.ch`;
- jsoup: has an optional dependency on re2j, which this batch relocates. It needs an intake-to-intake dependency column.

## Waves 1 and 2 (2026-10-31)

18 libraries pass every stage, and `lib_intake_combine.sh` links them into one image that validates (`COMBINED_IMAGE.tsv`).
The libraries are JavaEWAH, re2j, fastfilter, pcollections, HdrHistogram (also an app), dec, xz, commonmark,
commons-codec, jsoup (`requires static jdk.m3.re2j`), snakeyaml, commons-lang3, commons-io, antlr4-runtime, commons-net,
classgraph (explicit two-root mapping), fastdoubleparser and jackson-core (`requires jdk.m3.fastdoubleparser`).
`m3/docs/name-mapping.d/lib-intake-<lib>.json` holds 18 name-mapping records, generated from the run ledgers by
`name_mapping.py`.

New in the recipe:
- `intake_deps`: a library depends on another library of the same intake.
- Module names are read correctly from multi-release jars.
- Byte-exact handling of sources that are not valid UTF-8.

`HOLDS.tsv` lists four libraries:
- zero-allocation-hashing: uses the JDK-internal `sun.nio.ch`.
- univocity-parsers: its `Record` collides with `java.lang.Record` under Java 21.
- commons-pool2: its optional proxy support needs cglib.
- commons-text: the vendored source lost one byte.

The catalogues cover 27,991 method atoms in 21 libraries. The L-Hash near-duplicate pass finds 121 cross-library
algorithm families (468 leaves) and 81 families that an M3 owner already implements (473 leaves). All are candidates for
differential admission.
