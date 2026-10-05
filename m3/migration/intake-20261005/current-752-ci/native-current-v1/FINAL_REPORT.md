# Fresh current Java/JNI qualification

**PASS for the admitted Linux x86-64 native owner closure at `9963cc08ff13922b92a0e3db7c30f56fddacba7d`.**
Root tree: `6c055c584ebb5fa177420d1668db73bcb7e7fef4`. The actual result is retained verbatim as `RESULT.json` and, with its complete surrounding evidence, at logical path `evidence/current-v1/RESULT.json` inside the bundle.

## Executed scope

The existing ordered owner accepted all 32 commands: 9 lint, 10 compile, 6 test and 7 runtime commands. The final audit verified 8,147 source/tool inputs, 65 current repository blobs, 45 root-linked proof trees, all four stage chains and their 32 command receipts, and 61 compiled artifact identities. Every stage recorded zero input drift. The admitted source closure contains 31 complete Java sources and the existing native short-flag kernel; production changes are zero.

All 64 operational inputs remain byte-identical to the historically executed native slice at `652f1d18f44c52b49b13fb5e360380a2f49da75d`. Current `AGENTS.md` is the remaining input. All 65 current identities also match the intervening 992 review. Historical results retain their original source revision; this delivery contains a newly executed current result. The independent pre-gate reviewer accepted 36 checks before root authorized this exact sequence.

## Actual observations

| Oracle | Observed assertions and domain |
|---|---|
| Java short flags | 2,895,062 assertions |
| JNI short flags, UBSan OFF | 2,895,084 assertions |
| JNI short flags, UBSan ON | 2,895,084 assertions |
| Actual tree, Java and each JNI configuration | 94,299 assertions per configuration; 5,021 nodes; zero query-triggered provider calls |
| Existing lazy list | 44,148 assertions |
| Existing tree superset | 293,132 assertions across 400 random forests |

All three semantic negative controls exited exactly 1 with their prescribed assertion markers: lost bit 15, missing zero-valued holes and native any-versus-all. Missing native library and ABI version 2 produced the retained explicit refusal marker. These are assertion and scenario counts; no additional JUnit test count or measured speedup is inferred.

## Exact result identities

- RESULT SHA-256: `1e82bec642c8ea8ece15e8caeeed283416c1b612e22c357b20178524b6ab1605`.
- Final audit SHA-256: `5c646be616f481b835bcf65607408f1573bd589716d1ea98cbf56f4616298e33`.
- Patch SHA-256: `b566b4dcc748c07a39ad53bf9ef00ae6a6a4ef0d8659b23e5bfd807ac62aabdb`.
- Source-manifest SHA-256: `0ed036bea4f7a803440d22c1e5937c62fbebb3b6f09bc283d35cd696ffdcbcd2`.
- Command-plan SHA-256: `9274a3a6d33b9d06b43289e4ae941611a1d14d94deb425d6b1ef8ce3263aa41e`.
- Build-artifact manifest SHA-256: `39ff3b0f284b4ad1a5b10cefb9dfbbccaa2095e4cedb2c2f051e2506e9480e5e`.

## Bundle contents and readback

The bundle uses the existing `synexia.evidence.bundle/1` owner unchanged. A small native profile sets a 2 MiB archive-part cap and first tries one complete deterministic tar.gz; splitting occurs only when that archive exceeds the cap. `BUNDLE.json` binds each archive, object and manifest shard. Readable PATHS inventories list every retained logical file and its bytes/SHA-256. Omissions and sealed-input-reference shards distinguish retained content from manifest-only inputs.

All 65 repository files, current source receipts, acquired and root-linked trees, proof runners, source/tool/command manifests, four stage receipts and their stdout/stderr, all 32 per-command receipts and logs, preparation record, pre-gate review, actual result, final audit, and compiled artifact hashes are retained. Existing donor/category metadata remains exact. Old/stopped trials are untouched; the original historical result is included as provenance only.

Following the previous bundle convention, the actual 57 class and 4 shared-library bodies are omitted and their exact identities retained. Other encountered compiler artifacts are also hash-only. The 8,053 tool files (466,293,744 bytes) were rehashed for execution; their installation bodies are excluded. Toolchain manifests and CMake acquisition/provenance remain included. Packaging and readback execute no lint, compiler, test or runtime gate.

Run archive verification from this directory:

```sh
python -B evidence-tools/native_bundle.py verify --bundle evidence-bundle
python -B evidence-tools/native_bundle.py extract --bundle evidence-bundle --output /tmp/synexia-native-current-v1-review
```

The extraction destination must be fresh. Extraction reconstructs exact review evidence; it does not install JDK/GCC/CMake/Checkstyle, recreate omitted binaries, rewrite the original absolute paths, or replay the completed chain. Restoring the exact declared environment and preparing a new lane is required for a new execution. The separately produced `evidence-bundle/VERIFY.json` and `INDEPENDENT-READBACK.json` carry actual readback results.

## Acceptance limits

This result does not grant whole-module/reactor admission, Maven/OpenRewrite native recipe execution, annotation-processing coverage, other platforms, SWT/Jini integration, JDK product-runtime acceptance, a complete hermetic OS/dynamic-library image, or measured performance/memory gains. Strict repository admission and production source changes remain false/zero. The bounded current execution establishes the recorded Java/JNI owner observations only.
