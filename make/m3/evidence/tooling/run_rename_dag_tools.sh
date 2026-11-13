#!/bin/bash
# run_rename_dag_tools.sh <tree root> <rename_types.py> [--check] : DROP_PRODUCT_PREFIX over the jdk.m3.tooling leaf (backport-dag,
# atom-jit-gate, recipe-agent-api, cds-training, smart-build, lazy-priority-frontier). Records stay byte for byte:
# migration-recipes and hash-pinned-target-paths (pinned texts, before/after fixtures, task ledgers), make/m3/compatibility
# (maps), the package gate's own baseline, the lazy-priority-frontier application/provenance records and the
# recipe-agent-api custody receipt.
set -u
R=$1; T=$2; shift 2
cd "$R" && python -I "$T" --root . "$@" \
  --package jdk.m3.tooling.dag --package jdk.m3.tooling.atomjit --package jdk.m3.tooling.recipe.agent \
  --package jdk.m3.tooling.cds --package jdk.m3.tooling.smartbuild --package jdk.m3.tooling.priority \
  --exclude make/m3/tooling/jdk-naming/ --exclude make/m3/tooling/migration-recipes/ --exclude make/m3/tooling/hash-pinned-target-paths/ \
  --exclude make/m3/compatibility/ \
  --exclude make/m3/tooling/lazy-priority-frontier/M3_APPLICATION.json \
  --exclude make/m3/tooling/lazy-priority-frontier/SOURCE_PROVENANCE.tsv \
  --exclude make/m3/tooling/recipe-agent-api/source-custody.json \
  --doc make/m3/tooling/backport-dag/ --doc make/m3/tooling/atom-jit-gate/ --doc make/m3/tooling/recipe-agent-api/ \
  --doc make/m3/tooling/cds-training/ --doc make/m3/tooling/smart-build/ --doc make/m3/tooling/lazy-priority-frontier/ \
  --doc make/m3/backports/ --doc doc/m3/recipe-atom-evidence-and-dag.md \
  --string-words --comment-words
