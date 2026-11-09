<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# ChangeMap lower bound: recipe replay crate `m3-file-changemap-lower-bound-20261106`

Leaf `m3file-changemap-lower-bound` (rank 1) of the FS/PMEM convergence plan. One file changes:
`m3/file/src/main/java/jdk/m3/file/M3FileEditBatch.java`. `ChangeMap.oldToNew` and `newToOld`
answer a coordinate query in O(log E) through one private first-equality `lowerBound` instead of an
O(E) ordered scan. Public API, record layout, constructors, source ranges, affinity semantics,
exception classes and `touchesOldSpan` are untouched. Complexity statement only; no runtime claim.

## Donor

com.synexia recipe `M3FileChangeMapLowerBound20261029` = `M3HashPinnedJavaSnapshotRecipe` over crate
`m3-file-changemap-lower-bound-20261029`, landed in hsoliwal/com.synexia `4a2e6452a7a` (2026-10-31),
executed oracle receipt 2026-10-29 (`maps=50001 checks=9341740`). The donor manifest pins the
pre-rename owner path `m3/file/src/main/java/com/m3/file/M3FileEditBatch.java`
(blob `87dd9d82`, sha256 `7612ebe0`), so on the renamed owner the canonical recipe refuses with
`required source missing`. Every donor input is pinned in [PROVENANCE.tsv](PROVENANCE.tsv); the donor
README, category review and receipt are copied verbatim under [donor/](donor/).

## Re-seal (`reseal.py`)

Derives the templates of this crate from the donor templates mechanically and refuses otherwise:

1. the package declaration follows the JDK naming rename (`NAMING_RULES.tsv` rule `PKG_COM_M3`);
2. the postimage gains one `//FLLDCIM: RECIPE` anchor line above each changed method
   (`lowerBound`, `oldToNew`, `newToOld`), naming this crate and the donor crate and commit;
3. the derived preimage must equal the live receiver byte for byte.

```text
receiver / resealed preimage   97a80e6709b5c31dae837c7c4845ffb391c9a2914032df76d5720b88f161b634  (blob 5ef6e08f78)
resealed postimage             998abe8ae7f5377c9de623f0b11068994df6935b6488b3dd7a8e89e213f2b119
```

```sh
python -I reseal.py --donor-pre <donor pre/M3FileEditBatch.java.txt> --donor-post <donor M3FileEditBatch.java.txt> \
  --receiver ../../src/main/java/jdk/m3/file/M3FileEditBatch.java \
  --crate-dir src/test/resources/jdk/internal/m3/rewrite/hash-pinned-java/m3-file-changemap-lower-bound-20261106
```

## Replay (`apply.py`)

The local com.synexia recipe jar predates the snapshot recipe, so the replay is the recipe's exact
text contract executed by `apply.py`: a target hashing to the manifest preimage is replaced by the
reviewed template (template hash checked first), a target at the postimage is a fixed point, anything
else is refused; preimage and postimage must agree byte for byte outside the ChangeMap hunk.

```text
python -I apply.py            m3/file/src/main/java/jdk/m3/file/M3FileEditBatch.java  APPLIED      97a80e67... -> 998abe8a...
python -I apply.py  (again)   m3/file/src/main/java/jdk/m3/file/M3FileEditBatch.java  FIXED_POINT  998abe8a...
```

## Canonical recipe proof (`pom.xml`, JUnit)

`jdk.internal.m3.rewrite.M3FileChangeMapLowerBound20261106`
([yml](src/test/resources/META-INF/rewrite/m3-file-changemap-lower-bound-20261106.yml)) runs the
in-repo `jdk.internal.m3.rewrite.M3HashPinnedJavaSnapshotRecipe` (OpenRewrite 8.17.1) over this crate.
`M3FileChangeMapLowerBoundRecipeTest` proves: one exact file admitted with the pinned hashes; the real
Java source is transformed to exactly the postimage and a second run is a fixed point; an unreviewed
preimage and an absent original are refused; only the helper and the two query bodies differ (three
anchors in the postimage, none in the preimage); the recipe is discoverable by id.

```sh
mvn -B -ntp -f m3/file/recipe/changemap-lower-bound-20261106/pom.xml test      # Tests run: 5, Failures: 0, BUILD SUCCESS
```

## Oracle (`M3FileChangeMapBinarySearchProof`, `differential.py`)

`m3/file/src/test/java/jdk/m3/file/M3FileChangeMapBinarySearchProof.java` ports the donor oracle: same
seed, same generator, same verbatim linear-scan reference; the candidate side is the live `ChangeMap`
built through `M3FileEditBatch.replaceBytes/apply` (geometry asserted against the generated edits), and
every answer and exception class feeds one SHA-256 digest. `verify.py` runs it as an executable module
contract on the host and on the linked image (marker `M3_FILE_CHANGEMAP_LOWER_BOUND_V1`).

`differential.py` compiles only the owner closure in three lanes and compares the printed lines:

```text
lane      package      source_sha256  maps   checks    digest
receiver  jdk.m3.file  998abe8a...    50001  9341740   4fe562a0b569cf1027688bfe23628c27da2412416e35d73262dc53a7f1d62d21
donor     com.m3.file  6329ee71...    50001  9341740   4fe562a0b569cf1027688bfe23628c27da2412416e35d73262dc53a7f1d62d21
preimage  jdk.m3.file  97a80e67...    50001  9341740   4fe562a0b569cf1027688bfe23628c27da2412416e35d73262dc53a7f1d62d21
```

```sh
python -I differential.py --java-home <jdk21> --out <scratch> \
  --donor-postimage <donor M3FileEditBatch.java.txt> \
  --preimage src/test/resources/jdk/internal/m3/rewrite/hash-pinned-java/m3-file-changemap-lower-bound-20261106/pre/M3FileEditBatch.java.txt
```

## Gates and memory

[GATES.tsv](GATES.tsv) records every executed gate with its figures, including the pre-existing
master defect that keeps `mvn -f m3/file/pom.xml package` red independently of this leaf
(duplicate `M3BuildImpactPlan.requireSnapshot`, `2f708ccfd817` + `ce7051f77ca0`). Corrections of the
re-seal, replay and oracle are rows of [RECIPE_MEMORY.tsv](RECIPE_MEMORY.tsv). `touchesOldSpan` stays
excluded until it has its own proof and recipe, as in the donor.
