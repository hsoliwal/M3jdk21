# Synexia recipe evolution evidence

This is exact review/reconstruction evidence, not a hermetic toolchain image. Maven, JDK, Python and other dependency caches, compiled binaries, settings and credential material are omitted. Their sealed identities remain in the original manifests where recorded. OS, dynamic-library, shell and standard-library limits in the original tooling/results remain applicable. Packaging executes no proof gate and does not turn a focused or failed result into repository-wide admission.

The original reported status and baseline are preserved below. A result filename alone does not imply success.

| Lane | Reported status | Reported baseline |
| --- | --- | --- |
| cic-producer-v1 | PASS_FOCUSED_PRODUCER | See original marker |

## Contents

Each part is a standalone deterministic tar.gz of content objects and/or JSONL manifest shards. All parts together reconstruct the retained logical paths. Identical source bytes are stored once; large files use ordered 512 KiB chunks.

BUNDLE.json binds every archive part, metadata shard, lane inventory and readable path inventory. `files` shards record original paths, bytes, SHA-256, modes and ordered chunk identities. `omissions` shards record each encountered excluded file and its actual hash when read. `references` shards retain each sealed input identity and state whether its exact content is included, preserved at an alias, or intentionally manifest-only.

Excluded directory names: .git, .mypy_cache, .pytest_cache, .ruff_cache, __pycache__, m2, m3-m2, m3-python, node_modules, toolchain, wheels. Dependency caches are not traversed. Maven/settings and credential files are omitted. First-party compiled classes/shared objects are hash-only; no third-party jars are bundled. Original build-artifact and tooling manifests remain included.

## Verify and reconstruct

Use the packaged acquisition/package_evidence.py, or the identical reviewed helper, with `verify --bundle DIR`. `extract --bundle DIR --output FRESH_DIR` validates every part, object, manifest and reconstructed file before writing any source. These commands check archive integrity only; they do not rerun lint, compiler, tests or runtime gates.

Absolute paths in original proof metadata are preserved verbatim. Replaying commands requires deliberately restoring the declared environment and dependencies; extraction does not install software or rewrite those paths.
