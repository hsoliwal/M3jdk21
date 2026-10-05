# Fresh current native qualification

The existing Java/JNI native closure passed all 32 sealed commands at Synexia commit `9963cc08ff13922b92a0e3db7c30f56fddacba7d`. See [FINAL_REPORT.md](FINAL_REPORT.md), the exact [RESULT.json](RESULT.json), and [FINAL-AUDIT.json](FINAL-AUDIT.json).

The six canonical packet documents describe the bounded execution and its limits: [STATUS.tsv](STATUS.tsv), [RUN_CONTEXT.tsv](RUN_CONTEXT.tsv), [PROVENANCE.tsv](PROVENANCE.tsv), [VERIFY_CONTRACT.tsv](VERIFY_CONTRACT.tsv) and [OUTPUT_CONTRACT.tsv](OUTPUT_CONTRACT.tsv), together with the final report.

[PREPARED.json](PREPARED.json) is the immutable pre-execution record. Its original `SEALED_NOT_EXECUTED` status is preserved; `RESULT.json` records what subsequently ran. The [evidence bundle](evidence-bundle/BUNDLE.json) includes exact source, tree, runner, manifest, stage and command evidence. [VERIFY.json](evidence-bundle/VERIFY.json) and [INDEPENDENT-READBACK.json](INDEPENDENT-READBACK.json) record separate archive integrity checks.

Run `python -B evidence-tools/native_bundle.py verify --bundle evidence-bundle` to verify archived evidence. Use the same owner with `extract --bundle evidence-bundle --output FRESH_DIR` to reconstruct retained logical paths. Tool installations and compiled binaries are hash-only under the previous bundle convention; extraction is not a complete execution environment. Original absolute command paths remain exact. No new proof gate is run by archive verification.
