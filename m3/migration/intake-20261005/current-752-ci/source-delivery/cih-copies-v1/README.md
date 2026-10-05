# Cih source and portable delivery census

The append records **387 distinct declared source paths**: the immutable prior 305, all 50 Cih source publication paths, and all 32 physical Cih portable publication paths. The new 82 paths account for 3,886,509 bytes. This is a content-presence and publication-mapping census; it does not claim remote publication or a consumer result.

The portable copies are fresh ordinary files under `publication-extra/cih-producer-portable-v1/files`, mapped to `m3/migration/intake-20261005/current-752-ci/cih-producer-portable`. The source whitelist, all source bodies, portable whitelist, all portable bodies, and the actual producer RESULT were verified before copying. No proof, archive, candidate, or earlier census was changed.

| Evidence | SHA-256 |
|---|---|
| Source whitelist, 50 paths / 1,789,116 bytes | `9a3cb490fb4f566fca4c47cdcb10e6226e8ea1251e641df15a14f0b85327426b` |
| Portable whitelist, 32 paths / 2,097,393 bytes | `69f12df98f27fcf536e4645109f9e19c75f7ebc290190188fee65adb07266b42` |
| Actual Cih producer-v3 RESULT | `f93e1ef05b82459105b8dacd6258af0caa86f0e2c2221f946e8250397ffa9dcb` |
| Cih BUNDLE.json | `c35af298ba90856010d34112eda5cbc7bd92f61c85fd4cd8e124558f6832ddb9` |
| Physical receiving MAPPING.json | `feb528d99014446264b23a1da7ba6c14155d4a2e32b2d89586167cd177208bd7` |
| source-mapping.json | `4deb8298fbf72f050a2cfe89f5116d993443a11bd75d35a79dabda7976c16795` |
| delivery-audit.json | `360381a3023d6c90e591a123c908f203f9c3775f8d802bac7e42b4a17a23c9b8` |
| plan.json | `858811aca4fbdc277316af707a210d11d0d2544c2c3796a768a2c49d4fc5becd` |
| coverage-append.json | `83e2729fc7dae8769acce8cbdb0a1cd104088cf0d8a5e1162ce8a04e89c70ece` |
| collect.py | `8c6e1cdaf7be9dc89828a7edde785b70f05057da364e79ae3bd311691f1bc80c` |

Both archive parts, all metadata shards and chunk bodies, the declared top files, and all **386 logical file bodies** were reconstructed and verified. References and omissions were not counted as delivered bodies. All 44 qualified task files map explicitly to `evidence/cih-producer-v3/project/...`, rather than to an older authoring version with coincidentally equal bytes. One further source metadata file maps to `CIH-PLAN-AUTHORED.json` inside the archive. The other five source files map to exact loose portable copies. All 50 source bodies therefore have a selected delivery path; none is missing.

The audit also records every exact active-receiving, receiving-evidence, portable-physical, and portable-logical alias found for each Cih source row. These aliases are content observations, not additional files in the declared source universe. The candidate-v5 inventory was verified against all 2,559 rows of consumer-manifest-v5, SHA-256 `8c3d4e6a28d4314a1de4c3a15c3616a371e04ac2eaf6357852c6ce28c1642db4`, with 24 producer outputs. Thirty-six source rows have at least one matching active receiving file; ten have at least one receiving-evidence match. These groups overlap. No executing consumer work directory was read or written.

The two enclosing whitelist documents are **outside the 387 declared source paths**. Their explicit publication additions are in `delivery-audit.json`, totaling 51,278 bytes, under `current-752-ci/source-admission-evidence/source-whitelists/`. Preserve these additions when assembling the final envelope.

The existing 49-file `publication-extra/source-delivery-v1/SELECTION.json`, SHA-256 `a313bd02a2518628fba0c17d77244187e3fc6d073a72aaa3fc300777b85ff5d7`, and every selected body were checked before and after collection and remain unchanged. The retained append helper carries forward its historical pending count of 25; that field is not the current enclosing selection count. The final selector must retain all 49 selected identities and append the two Cih whitelist envelopes and the new coverage procedure/receipt files explicitly. It must not substitute this latest companion alone for the existing selection.

The following two commands completed with exit code 0. They perform publication copies and read-only evidence reconstruction, not producer or consumer proof gates. `collect.py` and the helper require fresh destinations; the commands should not be rerun over these existing outputs.

```bash
/workspace/scratch/5809f5dce3dd/m3-recipe-evolution-20261005/m3-python/bin/python -B /workspace/scratch/5809f5dce3dd/m3-recipe-evolution-20261005/continuation-20261005/m3/research/source-delivery-coverage/cih-copies-v1/collect.py

/workspace/scratch/5809f5dce3dd/m3-recipe-evolution-20261005/m3-python/bin/python -B /workspace/scratch/5809f5dce3dd/m3-recipe-evolution-20261005/continuation-20261005/m3/research/source-delivery-coverage/outer-copies-v1/append_coverage.py --plan /workspace/scratch/5809f5dce3dd/m3-recipe-evolution-20261005/continuation-20261005/m3/research/source-delivery-coverage/cih-copies-v1/plan.json --plan-sha256 858811aca4fbdc277316af707a210d11d0d2544c2c3796a768a2c49d4fc5becd --output /workspace/scratch/5809f5dce3dd/m3-recipe-evolution-20261005/continuation-20261005/m3/research/source-delivery-coverage/cih-copies-v1/coverage-append.json
```

The unchanged append helper is SHA-256 `d123d2aec12ca33606fdc9ff168f67d7a99c071c05abb11c3484cd1887e2c875`. Its actual output reported `allSourcePaths=387`, `newSourcePaths=82`, and the coverage SHA above. Final consumer packaging and its publication envelope remain separate later inputs; neither is inferred or included recursively in this census.
