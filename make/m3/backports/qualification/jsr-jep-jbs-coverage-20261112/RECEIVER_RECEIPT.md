<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# JSR/JEP/JBS complete-census receiver candidate — 2026-11-12

**Status:** DRAFT / SOURCE_RECEIVED / ADMISSION_HOLD. This is a data-only receiving
receipt for two source files whose bytes are copied without change from the
canonical Synexia recipe output. It is not a transformation recipe, a new
migration engine, an implementation of 85 JEPs or proof of 14,948 JBS fixes.

## Exact provenance

| Role | Identity |
| --- | --- |
| Target parent | `hsoliwal/M3jdk21 master@b98bf8c065818aac3447be165909e2a013325d9a` |
| Canonical recipe lineage | `hsoliwal/com.synexia`, merged PR #14996 |
| Pinned canonical revision | `f4c5a4ba787b185806895697a1aafa70c85d910a` |
| Recipe | `M3HashPinnedTextSnapshotRecipe`, `m3jdk21-jsr-jep-jbs-full-coverage-20261031` |
| Recipe manifest | `synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-jsr-jep-jbs-full-coverage-20261031/manifest.tsv` |
| Product coverage source | `m3/backports/jsr_jep_jbs_coverage.py` |
| Coverage source Git blob | `4a968eb57739cf1da0470bcc255c54ba49c22666` |
| Coverage source declared SHA-256 | `e3c1f8828a04d42e6a262d8e60a5b37ad20ef684c17ae009084cd52599653cf0` |
| Product coverage tests | `m3/backports/test_jsr_jep_jbs_coverage.py` |
| Tests source Git blob | `37fe81de37eb1b50089ed65dfdb7b38a16ba456d` |
| Tests source declared SHA-256 | `425f5308faa4dc5a3d199620ef87ab4c10345b47933bc2b1ac927d280ef75129` |

Both target source paths were absent on the pinned parent. GitHub readbacks of
the receiving branch prove **exact identical Git blob identity and byte content**
to the canonical source resources. Product tests are automatically discoverable
under the existing `m3/backports/pom.xml` `unittest discover -p test_*.py` lane:
the POM is intentionally unchanged.

## Existing target inputs

The receiver consumes existing `m3/backports` owners:
`inventory.py`, `compatibility_queue.py`, `jep_source_witness.py`,
`release_jep_authority.py`, `RELEASE_JEP_AUTHORITY.tsv`,
`JEP_CATALOGUE.tsv` and a complete freshly generated
`UPSTREAM_CHANGES.tsv`/`COMPATIBILITY_QUEUE.tsv` pair with a full,
pinned OpenJDK GA-tag Git object database.

The output is **candidate-only**: six Java SE JSR references (397–402),
85 released JEP references and the upstream GA-commit denominator are not
evidence of feature implementation, issue validation, backport compatibility
or M3JDK21 installed-image acceptance. Java 21 source/ABI and class-file
major version 65 remain unchanged.

## Observed checks and withheld gates

| Check | Status |
| --- | --- |
| Original file absence and target preimage check | PASS |
| Exact Synexia Git blob to receiving branch identity (2/2) | PASS |
| Existing test discovery configuration in target Maven POM | OBSERVED |
| Known imported incumbent target Python symbols | STATIC_OBSERVED |
| Actual canonical Maven/OpenRewrite SDK recipe apply, fixed point and refusal | NOT_EXECUTED_FOR_THIS_RECEIPT |
| Target Python regression suite / Maven verify | NOT_EXECUTED_FOR_THIS_RECEIPT |
| Full authentic 14,948-commit OpenJDK GA replay | NOT_EXECUTED_FOR_THIS_RECEIPT |
| JBS issue database verification and every backport's source/behavior review | NOT_EXECUTED |
| Full M3JDK21 build, jtreg/JNI/HotSpot/GC and installed-image execution | NOT_EXECUTED |

The canonical source PR originally recorded the OpenRewrite SDK/JUnit gate as
unrun; copying a source postimage does **not** retroactively pass that gate.
The receiving PR must stay **draft** until the canonical recipe execution,
target-selected tests and independent product acceptance are verified.
No product source path outside these two qualified-candidate owners, no
standard JDK runtime API, no Maven POM and no recipe code is modified.
