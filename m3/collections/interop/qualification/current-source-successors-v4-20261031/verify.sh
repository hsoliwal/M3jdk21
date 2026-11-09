#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"
TASK=m3/collections/interop/qualification/current-source-successors-v4-20261031
python3 -m unittest discover -s "$TASK" -p 'test_*.py' -v
python3 "$TASK/verify_source_v4.py" --product . \
  --approved-product approved-interop-20261029 \
  --before-product before-interop-20261026 \
  --historical-product historical-product
