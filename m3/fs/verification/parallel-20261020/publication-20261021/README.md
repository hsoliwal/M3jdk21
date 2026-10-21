# FS publication recheck — 2026-10-21

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

The 792 original FS payload destinations remain unchanged or absent on the inspected current master. `TARGET_RECONCILIATION.json` and `PATH_SEALS.tsv` record the exact base/head scope. Add these prepared entries to current master without replacing later fleet owners. This record does not claim the final publication or merge identity.

## Executed scope

The fresh run compiled 237 selected VFS sources and 62 M3FS owners, strictly compiled five existing test classes, and passed 136 real JUnit tests with no failures, skips, or aborts. All 382 prior qualified source pins matched before and after execution; test classes did not shadow production. Twelve dependency artifacts were pinned; the existing loader checked all five VFS dependency original LICENSE/NOTICE pairs and module descriptor kinds. Full JDK/HotSpot, Maven SDK, hosted CI, and new native-runtime qualification were not rerun.

`fresh/` preserves the exact fresh receipt and logs. Paths inside raw receipts identify the actual original execution environment. `failed-ascii/` preserves the first 135/136 run and its source. That failure occurred in the Unicode fixture before product code under an ASCII native locale. `ENVIRONMENT_CORRECTION.json` and the encoding diagnostic explain why changing only LC_ALL to C.UTF-8 restores the intended fixture environment. No historical result was relabeled.

## Reproduce offline

Supply the exact dependency cache described in `fresh/RESULTS.json`, an actual JDK21, and a receiver containing the exact recorded source pins. The existing verification/provenance paths are part of the original FS payload. The helper refuses drift or a reused output directory.

```sh
python3 m3/fs/verification/parallel-20261020/publication-20261021/replay.py \
  --repository-root . \
  --jdk-home "$JDK21" \
  --dependency-cache "$MAVEN_CACHE" \
  --out /tmp/m3fs-publication-replay
```

`replay-check/` records an executed check of this parameterized helper against the same sealed receiver and dependencies: 136 tests passed. The helper performs no network access, recipe regeneration, remote write, or native-runtime qualification.

## Progress compatibility

The newer collection `M3ProgressMonitor` adds shadow input/output defaults; its existing callbacks and split behavior are unchanged. The FS module retains its `M3FsProgress` callbacks and has no collection-module dependency. These collection changes are outside the exact FS closure. Preserve them along with the newer collection and jmod tooling work. The current global qualification phase remains STRING.
