The read-only census covers exactly the published 205-source increment, the separate Cia26 increment, and three source-publication provenance files: 234 paths. It found 22 missing source205 files plus the three new provenance files. `coverage.json.publicationAdditions` selects these 25 exact original bodies (667,918 bytes) for append-only publication; it does not modify the frozen receiving context. All 26 Cia source files have verified byte coverage.

The source205 receipt pins PR9358 at `e1cf684627a66ee784cfca0b42553531b2d8e7d4`, tree `3a7fd2a3e29202f3eef9d88fadbbc107b698e6fb`. All 205 receipt path/Git-blob identities agree with the source transport. The census hashes all 2,497 candidate-v3 files and validates 101 additional portable files selected by the producer40, Cir34 and Cia27 mappings. Native24 is already in the frozen candidate.

| Coverage priority | Published source205 | Cia26 | Publication provenance3 |
|---|---:|---:|---:|
| ACTIVE_RECEIVING_BYTES | 25 | 10 | 0 |
| FROZEN_RECEIVING_EVIDENCE | 69 | 7 | 0 |
| EXPLICIT_PORTABLE_FILE | 54 | 0 | 0 |
| PORTABLE_FILE_BYTES | 11 | 3 | 0 |
| PORTABLE_LOGICAL_BYTES | 24 | 6 | 0 |
| MISSING_BYTES | 22 | 0 | 3 |

Each row receives the first applicable classification above; all additional exact byte matches remain in the JSON and TSV. Active byte equality can cover several source aliases and does not mean every source task is installed as an executable owner. Source Synexia canonical paths are distinct from M3 receiving paths. Zero-byte equality alone does not count as coverage.

The 22 missing source205 entries are the top-level verification README, admission `COPIED_INPUTS.json`, all 19 files under `ci-at-pr148-5c1ee94`, and review `COPIED_REVIEW.json`. Their exact source hashes and proposed receiving paths appear in the table below. The seven preparation packet files, all six source admission/owner-resolution documents, and canonical Cit5 bytes are already covered. `QUALIFIED-CIR.json` is covered by its exact loose portable alias.

| Missing source path beneath `verification/ci-current-20261005/` | SHA-256 | Bytes |
|---|---|---:|
| README.md | `c4f6015fb69aa7cf39bc6c6694516060f0589fb36b8c1158fdb478b8afc87703` | 5380 |
| admission-evidence/COPIED_INPUTS.json | `5538624b0a313737802a816a5c2a9126dd6d9952b9589f4f8f033ba704771abf` | 6678 |
| admission-evidence/ci-at-pr148-5c1ee94/ACQUISITIONS.json | `7f53911ce1de3974fbd0bae2290d89f6520d127572f4e02545a7e3b9c6336af8` | 264829 |
| admission-evidence/ci-at-pr148-5c1ee94/INDEX.json | `8a7986f83b3f80b252b1bb65bb38d83dcb8f0bec7ec521eaef7cd7b566a97e81` | 2964 |
| admission-evidence/ci-at-pr148-5c1ee94/README.md | `cdf843c21a56e538ce9a15d5297961073ea08d17ed5120a2d0f7cb57365e7e5b` | 4328 |
| admission-evidence/ci-at-pr148-5c1ee94/REPORT.json | `b3451ffae472498a6d94d01f49aebed75928e68b414ccd6a5ac2655c6391e100` | 13199 |
| admission-evidence/ci-at-pr148-5c1ee94/attempted-job-detail-111763396934.json | `88192edc8ce3b84943535216f9687f6a11440ec491b0aa119a3ec0eed8a18ac7` | 156 |
| admission-evidence/ci-at-pr148-5c1ee94/attempted-job-detail-111763397210.json | `88192edc8ce3b84943535216f9687f6a11440ec491b0aa119a3ec0eed8a18ac7` | 156 |
| admission-evidence/ci-at-pr148-5c1ee94/attempted-job-detail-111763397781.json | `88192edc8ce3b84943535216f9687f6a11440ec491b0aa119a3ec0eed8a18ac7` | 156 |
| admission-evidence/ci-at-pr148-5c1ee94/check-runs.first-page.json | `ec539b8da07e52f73c02465d9a0b8fa34b519d2c86bf30b1c965d311e55f5af6` | 116267 |
| admission-evidence/ci-at-pr148-5c1ee94/combined-status.json | `3c91caab22dad3973962314f0f0d09d9f959792b3f7a5f0719262e27e7f3f685` | 15 |
| admission-evidence/ci-at-pr148-5c1ee94/foundation-command-excerpts.txt | `917ccef20aa289428426ba14ccf7e532e15bbbe0f3cd9ae3a8f3562384e0b41c` | 4747 |
| admission-evidence/ci-at-pr148-5c1ee94/java-test-compile-excerpt.txt | `0d1fca352c18ae7c7759ca0bcc24b3fc099b01bbfbb7bd4dfa257ce0d9a7c26f` | 2654 |
| admission-evidence/ci-at-pr148-5c1ee94/job-111763396934.log | `5bf59102405b8a059dff536683a3344857f1e0a6d938a7288e1c43449f51291d` | 78707 |
| admission-evidence/ci-at-pr148-5c1ee94/job-111763397210.log | `28a1d7372652aa3d704aae587c8e3f0f1763ee9b077a4d6fb806d6c2404b7676` | 14446 |
| admission-evidence/ci-at-pr148-5c1ee94/job-111763397781.log | `03d194f95a632e9e2ccae5f4756253cb3776c23172e4a2f4986874c37302aee1` | 24769 |
| admission-evidence/ci-at-pr148-5c1ee94/python-import-excerpt.txt | `d6f4d5226df1904a87d32d63c16ca289745dd7c68e668151ad53153414aa875e` | 1060 |
| admission-evidence/ci-at-pr148-5c1ee94/run-37310184481-jobs.json | `58cc07d538b7ed3b663ee5baf4ed403da9e87f20b2406ba2ff6b099dcae7ca8e` | 2367 |
| admission-evidence/ci-at-pr148-5c1ee94/run-37310184488-jobs.json | `7551a6c3f4068dd37e5452448929bd37a5cf36c6516728978f02e5a0cd2cfd5e` | 2143 |
| admission-evidence/ci-at-pr148-5c1ee94/run-37310184801-jobs.json | `86711f471b3a400946b9ecd36e4ea02447b12d3b95097ff873d0dd38b7719584` | 2747 |
| admission-evidence/ci-at-pr148-5c1ee94/workflow-runs.first-page.json | `6b28960e4c6643312e0c28fc33420550cbeca8c6cec2cd65026af22139d070c9` | 3535 |
| admission-evidence/review/COPIED_REVIEW.json | `37089aee060fe37bee3ac7e6fabc5f6cf33181de17c000cdf34cce1fd437e055` | 379 |
| source-publication-e1cf/COMMIT.json | `cae989142c43011180dbb7584f2379983fcf9e44823455d8bc281da30588bf55` | 1614 |
| source-publication-e1cf/READBACK.json | `bbee9a38f74068b39b576923d1f1760cd17198fac964c5aefc567d2eed2be490` | 58017 |
| source-publication-e1cf/REMOTE-CI.json | `63e0c850a79fd3c4e278e630457b10e47945023bf537cbc03d0f10db77ef3468` | 56605 |

Every proposed target is under `m3/migration/intake-20261005/current-752-ci/source-admission-evidence/`, preserving the relative suffix shown above. The selected `file` values point directly to original immutable publication bodies. The final transport must snapshot and verify them; this research step creates no duplicate package copies.

Canonical Cit resources retain both their source identity and concrete receiving/archive mapping:

| Canonical resource | SHA-256 | Active receiving path or selected archived source |
|---|---|---|
| META-INF/rewrite/m3-ci-752-text.yml | `4ac9bcaf7e026474b87045c8461b4ab5b95d4fcfc91e2678a2e8dd56f3046a04` | producer:producer-authoring-v2/src/main/resources/META-INF/rewrite/m3-ci-752-text.yml |
| com/synexia/rewrite/hash-pinned-text/m3-ci-752-text/manifest.tsv | `ee1689f026984bb0273a77848df5e4611f1c55ce9ffb45f1d5f1eda9e0a2ba12` | producer:producer-authoring-v2/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-ci-752-text/manifest.tsv |
| com/synexia/rewrite/hash-pinned-text/m3-ci-752-text/target-1.txt | `f6f569ef91e6b17be68a48801d1f97639d288d436c277a6401628bd82e210d3e` | .github/workflows/m3-foundation.yml |
| com/synexia/rewrite/hash-pinned-text/m3-ci-752-text/target-2.txt | `d51301d3ea225e89b000a2256855ab7806a1faf20c5fa493eb5666de1e7880c2` | m3/backports/compatibility_policy.py |
| com/synexia/rewrite/hash-pinned-text/m3-ci-752-text/target-3.txt | `dbd50a08d902a4009c66e22b8e6f499a66bbe4316c2d96d1e47c60194b572937` | m3/backports/test_compatibility_policy.py |

All portable logical file bodies were reconstructed from hash-addressed chunks. Every part, metadata shard, chunk, reconstructed file, and top-file identity was checked. Omission/reference-only records never count as delivered content. Selected entries prefer the current successful authoring/project paths; older equivalent aliases remain visible in `portableLogicalByteMatches`.

| Bundle | Verified logical file bodies | BUNDLE SHA-256 |
|---|---:|---|
| producer | 209 | `a97d19b88e6b0469f5a30b42739f9d6713cf810cca5872b4e771825946e2f856` |
| cir | 206 | `9799b18e1fd42973fd27319240a57d3d7cb5ca20e436e41eb0d0f156ee4f8c89` |
| cia | 124 | `c36ef995a9ba9ef7c72be3c403a14ea580f6886df15a5e553e7d9ca525b6f3f5` |
| native | 250 | `d3a09e407eeb5e352e00ff7167ddb73737a2c488c16e4d382491defa4d706b42` |

`coverage.json` is the exhaustive machine-readable path-to-delivery mapping, input/receipt/bundle seal list, and publication selection. `coverage.tsv` is the flat path mapping. `census.py` is the bounded read-only collector. No compiler/test gate or candidate import was run.

This census records candidate-v3 byte presence only. Consumer-v3 stopped at newly reached A3Lab fixture errors, so this report confers no consumer PASS, runtime acceptance, or product qualification. The next repair increment, final successful receiving proof/bundle, and remote receiving readback need a separate appended census. Existing proofs, candidate bytes and portable bundles were left unchanged.
